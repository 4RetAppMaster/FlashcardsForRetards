package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.FlashcardDatabase
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import com.example.data.repository.FlashcardRepository
import com.example.util.CsvParser
import com.example.util.SampleFileGenerator
import com.example.util.XlsxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FlashcardRepository
    private val sharedPrefs = application.getSharedPreferences("flashcards_prefs", Context.MODE_PRIVATE)

    val allDecks: StateFlow<List<Deck>>

    private val _activeDeck = MutableStateFlow<Deck?>(null)
    val activeDeck = _activeDeck.asStateFlow()

    private val _activeCards = MutableStateFlow<List<Flashcard>>(emptyList())
    val activeCards = _activeCards.asStateFlow()

    private var originalCards: List<Flashcard> = emptyList()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled = _isShuffled.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _showingSide = MutableStateFlow(0)
    val showingSide = _showingSide.asStateFlow()

    private val _startMode = MutableStateFlow("random")
    val startMode = _startMode.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    init {
        val db = FlashcardDatabase.getInstance(application)
        repository = FlashcardRepository(db.deckDao)

        allDecks = repository.allDecks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        _startMode.value = sharedPrefs.getString("flashMode", "random") ?: "random"

        viewModelScope.launch(Dispatchers.IO) {
            SampleFileGenerator.ensureSampleFileExists(application)
        }

        viewModelScope.launch {
            allDecks.collect { decks ->
                if (decks.isNotEmpty() && _activeDeck.value == null) {
                    val savedDeckId = sharedPrefs.getString("lastSelectedDeckId", null)
                    val targetDeck = decks.find { it.id == savedDeckId } ?: decks.first()
                    selectDeck(targetDeck)
                }
            }
        }
    }

    fun setMode(mode: String) {
        if (_startMode.value == mode) return
        _startMode.value = mode
        sharedPrefs.edit().putString("flashMode", mode).apply()
        resetInitialSide()
    }

    fun selectDeck(deck: Deck) {
        _activeDeck.value = deck
        sharedPrefs.edit().putString("lastSelectedDeckId", deck.id).apply()
        loadCardsForDeck(deck.id)
    }

    private fun loadCardsForDeck(deckId: String) {
        viewModelScope.launch {
            val cards = withContext(Dispatchers.IO) {
                repository.getCardsForDeck(deckId)
            }
            originalCards = cards
            _isShuffled.value = false
            _activeCards.value = cards
            _currentIndex.value = 0
            resetInitialSide()
        }
    }

    private fun resetInitialSide() {
        val cards = _activeCards.value
        val index = _currentIndex.value
        if (cards.isNotEmpty() && index in cards.indices) {
            val totalSides = cards[index].sides.size
            if (totalSides > 0) {
                _showingSide.value = when (_startMode.value) {
                    "A" -> 0
                    "B" -> if (totalSides > 1) 1 else 0
                    else -> (0 until totalSides).random()
                }
            }
        }
    }

    fun flipCard() {
        val cards = _activeCards.value
        val index = _currentIndex.value
        if (cards.isNotEmpty() && index in cards.indices) {
            val totalSides = cards[index].sides.size
            if (totalSides > 0) {
                _showingSide.value = (_showingSide.value + 1) % totalSides
            }
        }
    }

    fun nextCard() {
        val cards = _activeCards.value
        if (cards.isNotEmpty()) {
            _currentIndex.value = (_currentIndex.value + 1) % cards.size
            resetInitialSide()
        }
    }

    fun prevCard() {
        val cards = _activeCards.value
        if (cards.isNotEmpty()) {
            _currentIndex.value = (_currentIndex.value - 1 + cards.size) % cards.size
            resetInitialSide()
        }
    }

    fun toggleShuffle() {
        if (originalCards.size <= 1) return
        if (_isShuffled.value) {
            _isShuffled.value = false
            _activeCards.value = originalCards
            _currentIndex.value = 0
            resetInitialSide()
            return
        }
        viewModelScope.launch {
            val base = originalCards
            val shuffled = withContext(Dispatchers.Default) {
                var next = base.shuffled()
                var attempts = 0
                while (next == base && attempts < 5) {
                    next = base.shuffled()
                    attempts++
                }
                if (next == base) {
                    next = base.reversed()
                }
                next
            }
            _isShuffled.value = true
            _activeCards.value = shuffled
            _currentIndex.value = 0
            resetInitialSide()
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteDeck(deckId)
            }
            if (_activeDeck.value?.id == deckId) {
                _activeDeck.value = null
                originalCards = emptyList()
                _isShuffled.value = false
                _activeCards.value = emptyList()
                _currentIndex.value = 0
                _showingSide.value = 0
            }
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val lastImportedDeck = withContext(Dispatchers.IO) {
                var lastDeck: Deck? = null
                val contentResolver = context.contentResolver

                for (uri in uris) {
                    try {
                        val fileName = getFileName(context, uri) ?: "Vocabulary"
                        val displayName = fileName.substringBeforeLast(".")

                        val rawRows = contentResolver.openInputStream(uri)?.use { inputStream ->
                            when {
                                fileName.endsWith(".xlsx", ignoreCase = true) -> XlsxParser.parse(inputStream)
                                fileName.endsWith(".csv", ignoreCase = true) -> CsvParser.parse(inputStream)
                                else -> null
                            }
                        }

                        if (!rawRows.isNullOrEmpty()) {
                            repository.importDeck(
                                deckId = fileName,
                                deckName = displayName,
                                rawRows = rawRows
                            )
                            lastDeck = Deck(id = fileName, name = displayName)
                        } else {
                            _importError.value = context.getString(R.string.error_parse_file, fileName)
                        }
                    } catch (e: Exception) {
                        Log.e("FlashcardViewModel", "Error importing file", e)
                        _importError.value = context.getString(
                            R.string.error_reading_file,
                            e.localizedMessage ?: "Unknown error"
                        )
                    }
                }
                lastDeck
            }

            lastImportedDeck?.let { selectDeck(it) }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) return cursor.getString(index)
                }
            }
        }
        val path = uri.path ?: return null
        val cut = path.lastIndexOf('/')
        return if (cut != -1) path.substring(cut + 1) else path
    }
}

package com.example.data.repository

import com.example.data.local.DeckDao
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val deckDao: DeckDao) {
    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks()

    suspend fun getCardsForDeck(deckId: String): List<Flashcard> {
        return deckDao.getCardsForDeck(deckId)
    }

    suspend fun importDeck(deckId: String, deckName: String, rawRows: List<List<String>>) {
        val nonEmptyRows = rawRows.filter { row -> row.any { it.isNotBlank() } }
        val isFourColumnSheet = nonEmptyRows.any { it.size >= 3 }

        val deck = Deck(id = deckId, name = deckName)
        val cards = nonEmptyRows.mapNotNull { row ->
            val sides = if (isFourColumnSheet) {
                val sideAMain = row.getOrNull(0)?.trim().orEmpty()
                val sideASample = row.getOrNull(1)?.trim().orEmpty()
                val sideBMain = row.getOrNull(2)?.trim().orEmpty()
                val sideBSample = row.getOrNull(3)?.trim().orEmpty()

                listOf(
                    formatSide(sideAMain, sideASample),
                    formatSide(sideBMain, sideBSample)
                )
            } else {
                listOf(
                    row.getOrNull(0)?.trim().orEmpty(),
                    row.getOrNull(1)?.trim().orEmpty()
                )
            }

            if (sides.all { it.isBlank() }) null else Flashcard(deckId = deckId, sides = sides)
        }
        deckDao.insertDeckWithCards(deck, cards)
    }

    private fun formatSide(main: String, sample: String): String {
        if (main.isEmpty() && sample.isEmpty()) return ""
        if (sample.isEmpty()) return main
        val wrappedSample = if (sample.startsWith("(") && sample.endsWith(")")) {
            sample
        } else {
            "($sample)"
        }
        return if (main.isEmpty()) wrappedSample else "$main\n$wrappedSample"
    }

    suspend fun deleteDeck(deckId: String) {
        deckDao.deleteCardsForDeck(deckId)
        deckDao.deleteDeck(deckId)
    }
}

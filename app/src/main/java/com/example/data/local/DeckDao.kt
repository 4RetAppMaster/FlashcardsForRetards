package com.example.data.local

import androidx.room.*
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY dateImported DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardsForDeck(deckId: String): List<Flashcard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<Flashcard>)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: String)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: String)

    @Transaction
    suspend fun insertDeckWithCards(deck: Deck, cards: List<Flashcard>) {
        deleteCardsForDeck(deck.id)
        insertDeck(deck)
        insertFlashcards(cards)
    }
}

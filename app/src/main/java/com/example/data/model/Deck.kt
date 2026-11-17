package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey val id: String,
    val name: String,
    val dateImported: Long = System.currentTimeMillis()
)

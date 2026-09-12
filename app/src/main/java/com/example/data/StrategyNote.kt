package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strategy_notes")
data class StrategyNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // "Strategy", "Note", "Trade Plan", "AI Prompt"
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyNoteDao {
    @Query("SELECT * FROM strategy_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<StrategyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StrategyNote)

    @Delete
    suspend fun deleteNote(note: StrategyNote)

    @Query("DELETE FROM strategy_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}

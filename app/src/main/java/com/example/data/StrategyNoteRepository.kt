package com.example.data

import com.example.data.live.LiveObservationDao
import com.example.data.live.LiveObservationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class StrategyNoteRepository(
    private val strategyNoteDao: StrategyNoteDao,
    val liveObservationDao: LiveObservationDao? = null
) {
    val allNotes: Flow<List<StrategyNote>> = strategyNoteDao.getAllNotes()

    val allObservations: Flow<List<LiveObservationEntity>> = 
        liveObservationDao?.getAllObservations() ?: emptyFlow()

    suspend fun insert(note: StrategyNote) {
        strategyNoteDao.insertNote(note)
    }

    suspend fun delete(note: StrategyNote) {
        strategyNoteDao.deleteNote(note)
    }

    suspend fun deleteById(id: Int) {
        strategyNoteDao.deleteNoteById(id)
    }

    suspend fun insertObservation(observation: LiveObservationEntity): Long {
        return liveObservationDao?.insertObservation(observation) ?: 0L
    }

    suspend fun settleObservation(
        sequenceId: String,
        outcome: String,
        settlementPrice: Double,
        settledTimestampMs: Long
    ): Int {
        return liveObservationDao?.settleObservation(
            sequenceId = sequenceId,
            outcome = outcome,
            settlementPrice = settlementPrice,
            settledTimestampMs = settledTimestampMs
        ) ?: 0
    }

    suspend fun getPendingObservations(currentTimestampMs: Long): List<LiveObservationEntity> {
        return liveObservationDao?.getPendingObservationsForSettlement(currentTimestampMs) ?: emptyList()
    }

    suspend fun clearObservations() {
        liveObservationDao?.clearLedger()
    }
}


package com.example.data.live

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveObservationDao {

    @Query("SELECT * FROM live_observation_ledger ORDER BY timestampMs DESC")
    fun getAllObservations(): Flow<List<LiveObservationEntity>>

    @Query("SELECT * FROM live_observation_ledger WHERE actualOutcome != 'PENDING' ORDER BY timestampMs DESC")
    fun getSettledObservations(): Flow<List<LiveObservationEntity>>

    @Query("SELECT * FROM live_observation_ledger WHERE sequenceId = :sequenceId LIMIT 1")
    suspend fun getObservationBySequenceId(sequenceId: String): LiveObservationEntity?

    @Query("SELECT * FROM live_observation_ledger WHERE actualOutcome = 'PENDING' AND settlementTimestampMs <= :currentTimestampMs")
    suspend fun getPendingObservationsForSettlement(currentTimestampMs: Long): List<LiveObservationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObservation(observation: LiveObservationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObservations(observations: List<LiveObservationEntity>)

    /**
     * Decoupled Settlement Update:
     * Strictly updates only the settlement outcome and settlement price fields.
     * Preserves the immutable evaluation snapshot (timestamp, strike, score, features).
     */
    @Query("""
        UPDATE live_observation_ledger 
        SET actualOutcome = :outcome, 
            settlementPrice = :settlementPrice, 
            settledTimestampMs = :settledTimestampMs 
        WHERE sequenceId = :sequenceId AND actualOutcome = 'PENDING'
    """)
    suspend fun settleObservation(
        sequenceId: String,
        outcome: String,
        settlementPrice: Double,
        settledTimestampMs: Long
    ): Int

    @Query("SELECT COUNT(*) FROM live_observation_ledger")
    suspend fun getTotalObservationCount(): Int

    @Query("SELECT COUNT(*) FROM live_observation_ledger WHERE actualOutcome != 'PENDING'")
    suspend fun getSettledObservationCount(): Int

    @Query("DELETE FROM live_observation_ledger")
    suspend fun clearLedger()
}

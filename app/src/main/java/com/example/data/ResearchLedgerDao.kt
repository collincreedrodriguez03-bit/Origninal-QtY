package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResearchLedgerDao {

    @Query("SELECT * FROM research_prediction_ledger WHERE isSyntheticData = 0 ORDER BY predictionTimestampMs DESC")
    fun getAllAuthenticPredictions(): Flow<List<ResearchPredictionEntity>>

    @Query("SELECT * FROM research_prediction_ledger WHERE trackId = :trackId AND isSyntheticData = 0 ORDER BY predictionTimestampMs DESC")
    fun getPredictionsByTrack(trackId: String): Flow<List<ResearchPredictionEntity>>

    @Query("SELECT COUNT(*) FROM research_prediction_ledger WHERE trackId = :trackId AND isSyntheticData = 0 AND actualOutcome != 'PENDING'")
    suspend fun getSettledCountByTrack(trackId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: ResearchPredictionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<ResearchPredictionEntity>)

    @Query("DELETE FROM research_prediction_ledger WHERE isSyntheticData = 1")
    suspend fun purgeSyntheticDataIfAny()

    @Query("DELETE FROM research_prediction_ledger")
    suspend fun clearLedger()
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence entity for authentic historical backtest predictions and live research ledger.
 *
 * RESEARCH DATA INTEGRITY RULE:
 * Synthetic test data is strictly prohibited from entering this ledger.
 * Only authentic market data backtests or live predictions are persisted.
 */
@Entity(tableName = "research_prediction_ledger")
data class ResearchPredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val predictionTimestampMs: Long,
    val windowStartTimestampMs: Long,
    val settlementTimestampMs: Long,
    val strikePrice: Double,
    val settlementPrice: Double?,
    val trackId: String, // e.g. "TRACK_D_QUANT", "TRACK_A_RANDOM", "TRACK_B_PERSISTENCE", "TRACK_C_MOMENTUM", "TRACK_E_GEMINI"
    val predictedDirection: String, // "UP", "DOWN", "NO_TRADE", "BULLISH", "BEARISH"
    val rawModelScore: Double, // 0.0 to 100.0
    val actualOutcome: String, // "WON", "LOST", "FLAT", "PENDING"
    val modelVersion: String,
    val datasetSha256Checksum: String,
    val isSyntheticData: Boolean = false,
    val retrospectiveBadge: String? = null,
    val featureRsi: Double = 0.0,
    val featureMomentum: Double = 0.0,
    val featureEmaSpread: Double = 0.0,
    val featureVolatility: Double = 0.0,
    val featureStrikeDelta: Double = 0.0
)

package com.example.data.live

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 3: Persistent Live Heuristic Radar Observation Ledger.
 *
 * RESEARCH DATA INTEGRITY RULES:
 * 1. Immutability: The signal evaluation snapshot (timestamp, BTC price, strike, remaining time,
 *    direction, score, and feature contributions) is immutable after creation.
 * 2. Strict No-Lookahead / Anti-Leakage: Outcome and settlement price are NEVER available
 *    at signal creation time. Records are inserted with actualOutcome = "PENDING" and
 *    settlementPrice = null.
 * 3. Settlement Decoupling: Settlement outcome is recorded strictly after window expiration.
 */
@Entity(tableName = "live_observation_ledger")
data class LiveObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceId: String,                    // Unique UUID for this evaluation tick
    val timestampMs: Long,                     // UTC timestamp in milliseconds
    val formattedTimestampUtc: String,         // Formatted UTC string (e.g., "2026-08-28 05:30:10 UTC")
    val btcPrice: Double,                      // Current BTC spot price at evaluation
    val contractWindowId: String,              // Contract window identifier (e.g. "05:15 - 05:30 UTC")
    val windowStartTimestampMs: Long,          // Window start boundary timestamp
    val settlementTimestampMs: Long,           // Window settlement boundary timestamp
    val strikePrice: Double,                   // Research strike price (S_0)
    val timeRemainingSeconds: Int,             // Seconds remaining in 15m cycle
    val direction: String,                     // "UP", "DOWN", "NO TRADE"
    val rawModelScore: Double,                 // 0.0 to 100.0
    val emaContribution: Double,               // EMA score contribution (-0.8 to +0.8)
    val momentumContribution: Double,          // Momentum score contribution (-0.6 to +0.6)
    val rsiContribution: Double,               // RSI score contribution (-0.5 to +0.5)
    val strikeBufferContribution: Double,      // Strike distance buffer contribution (-0.4 to +0.4)
    val modelVersion: String = "v2.0-quant-multival",
    val isStrongSignal: Boolean,               // Fixed frozen definition: rawModelScore >= 70.0 || rawModelScore <= 30.0
    val actualOutcome: String = "PENDING",     // "PENDING", "WON", "LOST", "FLAT", "NO_TRADE"
    val settlementPrice: Double? = null,       // BTC price at window settlement (null at creation)
    val settledTimestampMs: Long? = null,      // Timestamp of settlement event (null at creation)
    val baselineSha256Checksum: String = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
    val featureEmaSpread: Double = 0.0,
    val featureMomentum: Double = 0.0,
    val featureRsi: Double = 0.0,
    val featureDeltaToStrike: Double = 0.0,
    val isStaleData: Boolean = false
)

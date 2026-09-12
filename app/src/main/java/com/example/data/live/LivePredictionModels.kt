package com.example.data.live

import com.example.ui.ConnectionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * UTC 15-Minute Contract Window state.
 * Synchronized with official 15-minute expiration boundaries (:00, :15, :30, :45 UTC).
 * Enforces strict mathematical isolation between the immutable research strike (S_0)
 * and the display-only UI What-If strike.
 */
data class LiveContractWindow(
    val windowStartTimestampMs: Long,
    val settlementTimestampMs: Long,
    val researchStrikePrice: Double,       // Immutable research strike at window start (S_0)
    val whatIfStrikePrice: Double,         // Display-only / What-If strike for UI scenarios
    val currentSpotPrice: Double,
    val secondsRemaining: Int,
    val formattedTimer: String,
    val formattedWindowRange: String,
    val statusLabel: String,
    val researchDeltaToStrike: Double,     // Spot - S_0 (strictly fed to research model)
    val whatIfDeltaToStrike: Double,       // Spot - S_ui (what-if display only)
    val isAboveResearchStrike: Boolean,
    val isAboveWhatIfStrike: Boolean,
    val isStrikeLocked: Boolean = true
) {
    // Backwards-compatible aliases for existing bindings (strictly mapped to immutable research strike)
    val strikePrice: Double
        get() = researchStrikePrice

    val deltaToStrike: Double
        get() = researchDeltaToStrike

    val isAboveStrike: Boolean
        get() = isAboveResearchStrike

    val openStrikePrice: Double
        get() = researchStrikePrice

    val windowLabel: String
        get() = formattedWindowRange

    val localWindowLabel: String
        get() {
            val localFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "${localFormat.format(Date(windowStartTimestampMs))} - ${localFormat.format(Date(settlementTimestampMs))} Local"
        }

    val formattedCountdown: String
        get() = formattedTimer

    companion object {
        fun calculateCurrentWindow(
            nowMs: Long = System.currentTimeMillis(),
            currentSpot: Double,
            lockedResearchStrike: Double? = null,
            lockedWindowStartMs: Long? = null,
            whatIfStrike: Double? = null
        ): LiveContractWindow {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = nowMs
            }
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.SECOND)

            val windowIndex = minute / 15 // 0, 1, 2, 3
            val startMinute = windowIndex * 15

            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = nowMs
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val windowStartMs = startCal.timeInMillis

            val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = windowStartMs + (15 * 60_000L)
            }
            val settlementMs = endCal.timeInMillis

            val secondsIntoCycle = (minute % 15) * 60 + second
            val secondsRemaining = (900 - secondsIntoCycle).coerceIn(0, 900)
            val mm = secondsRemaining / 60
            val ss = secondsRemaining % 60
            val formattedTimer = String.format(Locale.US, "%02d:%02d", mm, ss)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val formattedRange = "${timeFormat.format(Date(windowStartMs))} - ${timeFormat.format(Date(settlementMs))} UTC"

            // 1. IMMUTABLE RESEARCH STRIKE (S_0):
            // If a locked strike exists for THIS EXACT windowStartMs, it MUST be preserved without mutation.
            // Otherwise (window rollover or initial boot), lock to currentSpot (the candle open at T) if valid (> $1000).
            val researchStrike = if (lockedWindowStartMs == windowStartMs && lockedResearchStrike != null && lockedResearchStrike > 1000.0) {
                lockedResearchStrike
            } else if (currentSpot > 1000.0) {
                currentSpot
            } else {
                0.0
            }

            // 2. UI WHAT-IF STRIKE (S_ui):
            // Optional what-if strike for UI scenarios. Defaults to researchStrike if unset.
            val uiStrike = if (whatIfStrike != null && whatIfStrike > 1000.0) {
                whatIfStrike
            } else {
                researchStrike
            }

            val resDelta = currentSpot - researchStrike
            val resIsAbove = resDelta >= 0.0

            val whatIfDelta = currentSpot - uiStrike
            val whatIfIsAbove = whatIfDelta >= 0.0

            val status = when {
                secondsRemaining <= 60 -> "🚨 CRITICAL EXPIRY (<1m)"
                secondsRemaining <= 180 -> "⚠️ FINAL 3 MINS"
                abs(resDelta) < 10.0 -> "⚠️ STRIKE PINNED (±$10)"
                resIsAbove -> "✓ BULLISH BUFFER (+$${String.format(Locale.US, "%.1f", resDelta)})"
                else -> "✓ BEARISH BUFFER (-$${String.format(Locale.US, "%.1f", abs(resDelta))})"
            }

            return LiveContractWindow(
                windowStartTimestampMs = windowStartMs,
                settlementTimestampMs = settlementMs,
                researchStrikePrice = Math.round(researchStrike * 100.0) / 100.0,
                whatIfStrikePrice = Math.round(uiStrike * 100.0) / 100.0,
                currentSpotPrice = currentSpot,
                secondsRemaining = secondsRemaining,
                formattedTimer = formattedTimer,
                formattedWindowRange = formattedRange,
                statusLabel = status,
                researchDeltaToStrike = Math.round(resDelta * 100.0) / 100.0,
                whatIfDeltaToStrike = Math.round(whatIfDelta * 100.0) / 100.0,
                isAboveResearchStrike = resIsAbove,
                isAboveWhatIfStrike = whatIfIsAbove,
                isStrikeLocked = true
            )
        }
    }
}

/**
 * Feature snapshot computed from authentic real-time market data.
 */
data class LiveFeatureSnapshot(
    val timestampMs: Long,
    val spotPrice: Double,
    val strikePrice: Double,
    val deltaToStrike: Double,
    val rsi: Double,
    val momentum: Double,
    val ema9: Double,
    val ema21: Double,
    val emaSpread: Double,
    val volatility: Double,
    val dataFreshness: ConnectionState,
    val isStale: Boolean
)

data class LiveContributingFactor(
    val name: String,
    val scoreContribution: Double,
    val description: String
)

/**
 * Explicit 10-Second Live Prediction Record.
 */
data class LivePredictionLogRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(timestampMs)),
    val direction: String, // "UP", "DOWN", "NO TRADE"
    val rawModelScore: Double, // 0.0 to 100.0
    val calibratedProbabilityText: String = "UNCALIBRATED (N < 300)",
    val currentBtcPrice: Double,
    val strikePrice: Double,
    val deltaToStrike: Double,
    val timeRemainingSeconds: Int,
    val formattedTimeRemaining: String,
    val modelVersion: String = "v2.0-quant-multival",
    val dataFreshness: ConnectionState,
    val reasoning: String,
    val contributingFactors: List<LiveContributingFactor> = emptyList(),
    val featureSnapshot: LiveFeatureSnapshot? = null,
    val isStale: Boolean = false
) {
    val recommendedAction: String
        get() = when (direction) {
            "UP" -> "BUY UP (YES)"
            "DOWN" -> "BUY DOWN (NO)"
            else -> "NO TRADE (WAIT)"
        }

    val calibratedProbabilityLabel: String
        get() = calibratedProbabilityText

    val indicatorsSnapshot: LiveFeatureSnapshot?
        get() = featureSnapshot
}

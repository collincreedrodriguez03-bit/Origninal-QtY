package com.example.data.quant

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Feature inputs for the unified quantitative model.
 * Fed point-in-time from either streaming live ticks or strictly bounded historical candles.
 */
data class QuantitativeFeatures(
    val ema9: Double,
    val ema21: Double,
    val momentum: Double,
    val rsi: Double,
    val deltaToStrike: Double
) {
    val emaSpread: Double
        get() = ema9 - ema21
}

data class QuantitativeContributingFactor(
    val name: String,
    val scoreContribution: Double,
    val description: String
)

/**
 * Standardized output of the unified quantitative model.
 */
data class QuantitativeModelOutput(
    val direction: String, // "UP", "DOWN", "NO TRADE"
    val rawCompositeScore: Double, // signed float sum [-4.90 .. +4.90]
    val rawModelScore: Double, // 0.0 to 100.0 (50.0 for NO TRADE, 55..95 for UP, 5..45 for DOWN)
    val calibratedProbabilityText: String,
    val reasoning: String,
    val factors: List<QuantitativeContributingFactor>,
    val modelVersion: String = SharedQuantitativeModel.MODEL_VERSION
) {
    val backtestDirection: String
        get() = when (direction) {
            "UP" -> "BULLISH"
            "DOWN" -> "BEARISH"
            else -> "NO_TRADE"
        }

    val isTradeable: Boolean
        get() = direction == "UP" || direction == "DOWN"

    val directionalConfidence: Double
        get() = when (direction) {
            "UP" -> rawModelScore
            "DOWN" -> (100.0 - rawModelScore).coerceIn(55.0, 95.0)
            else -> 50.0
        }
}

/**
 * SINGLE SOURCE OF TRUTH for quantitative 15-minute prediction.
 * Called identically by:
 * 1. HistoricalBacktestEngine (Track D)
 * 2. LivePredictionEngine
 */
object SharedQuantitativeModel {

    const val MODEL_VERSION = "v2.0-quant-multival"

    // =========================================================================
    // 17 HEURISTIC CONSTANTS (GRADE C — PENDING WALK-FORWARD EMPIRICAL TUNING)
    // =========================================================================
    const val EMA_SPREAD_HIGH = 10.0
    const val EMA_SPREAD_LOW = 2.0
    const val WEIGHT_EMA_STRONG = 1.5
    const val WEIGHT_EMA_WEAK = 0.8

    const val MOM_FAST_VELOCITY = 15.0
    const val MOM_BASE_VELOCITY = 3.0
    const val WEIGHT_MOM_STRONG = 1.2
    const val WEIGHT_MOM_WEAK = 0.6

    const val RSI_OVERSOLD = 28.0
    const val RSI_OVERBOUGHT = 72.0
    const val WEIGHT_RSI_EXTREME = 1.4
    const val WEIGHT_RSI_CHANNEL = 0.5

    const val STRIKE_DELTA_STRONG = 40.0
    const val STRIKE_DELTA_WEAK = 10.0
    const val WEIGHT_DELTA_STRONG = 0.8
    const val WEIGHT_DELTA_WEAK = 0.4

    const val DEADBAND_THRESHOLD = 0.50
    const val RAW_SCORE_MULTIPLIER = 12.0

    /**
     * Evaluates quantitative features to produce a standardized prediction output.
     */
    fun evaluate(
        features: QuantitativeFeatures,
        sampleSizeN: Int = 0
    ): QuantitativeModelOutput {
        var score = 0.0
        val factors = mutableListOf<QuantitativeContributingFactor>()

        // 1. FACTOR A: EMA 9/21 Dynamic Ribbon Spread
        val emaSpread = features.emaSpread
        val emaScore = when {
            emaSpread > EMA_SPREAD_HIGH -> WEIGHT_EMA_STRONG
            emaSpread > EMA_SPREAD_LOW -> WEIGHT_EMA_WEAK
            emaSpread < -EMA_SPREAD_HIGH -> -WEIGHT_EMA_STRONG
            emaSpread < -EMA_SPREAD_LOW -> -WEIGHT_EMA_WEAK
            else -> 0.0
        }
        score += emaScore
        factors.add(
            QuantitativeContributingFactor(
                name = "EMA 9/21 Dynamic Ribbon",
                scoreContribution = emaScore,
                description = if (emaSpread >= 0) "+$${String.format(Locale.US, "%.1f", emaSpread)} Bullish Expansion" else "-$${String.format(Locale.US, "%.1f", abs(emaSpread))} Bearish Rejection"
            )
        )

        // 2. FACTOR B: Momentum Rate-of-Change Velocity
        val mom = features.momentum
        val momScore = when {
            mom > MOM_FAST_VELOCITY -> WEIGHT_MOM_STRONG
            mom > MOM_BASE_VELOCITY -> WEIGHT_MOM_WEAK
            mom < -MOM_FAST_VELOCITY -> -WEIGHT_MOM_STRONG
            mom < -MOM_BASE_VELOCITY -> -WEIGHT_MOM_WEAK
            else -> 0.0
        }
        score += momScore
        factors.add(
            QuantitativeContributingFactor(
                name = "Price Momentum / Velocity",
                scoreContribution = momScore,
                description = if (mom >= 0) "+$${String.format(Locale.US, "%.1f", mom)}/window Upside Push" else "-$${String.format(Locale.US, "%.1f", abs(mom))}/window Downside Pressure"
            )
        )

        // 3. FACTOR C: RSI Channel & Mean-Reversion
        val rsi = features.rsi
        val rsiScore = when {
            rsi < RSI_OVERSOLD -> WEIGHT_RSI_EXTREME // Oversold -> high probability bounce (UP)
            rsi > RSI_OVERBOUGHT -> -WEIGHT_RSI_EXTREME // Overbought -> high probability pullback (DOWN)
            rsi in 50.0..62.0 && mom >= 0 -> WEIGHT_RSI_CHANNEL // Bullish trend channel
            rsi in 38.0..50.0 && mom < 0 -> -WEIGHT_RSI_CHANNEL // Bearish trend channel
            else -> 0.0
        }
        score += rsiScore
        factors.add(
            QuantitativeContributingFactor(
                name = "RSI Channel & Mean-Reversion",
                scoreContribution = rsiScore,
                description = "RSI ${String.format(Locale.US, "%.1f", rsi)} (${when { rsi < 30.0 -> "Oversold Bounce Zone"; rsi > 70.0 -> "Overbought Pullback Zone"; else -> "Neutral Channel" }})"
            )
        )

        // 4. FACTOR D: Strike Distance Buffer (Delta to S_0)
        val delta = features.deltaToStrike
        val deltaScore = when {
            delta > STRIKE_DELTA_STRONG -> WEIGHT_DELTA_STRONG
            delta > STRIKE_DELTA_WEAK -> WEIGHT_DELTA_WEAK
            delta < -STRIKE_DELTA_STRONG -> -WEIGHT_DELTA_STRONG
            delta < -STRIKE_DELTA_WEAK -> -WEIGHT_DELTA_WEAK
            else -> 0.0
        }
        score += deltaScore
        factors.add(
            QuantitativeContributingFactor(
                name = "Strike Distance Buffer",
                scoreContribution = deltaScore,
                description = if (delta >= 0) "+$${String.format(Locale.US, "%.1f", delta)} Above Strike" else "-$${String.format(Locale.US, "%.1f", abs(delta))} Below Strike"
            )
        )

        // 5. DIRECTION RESOLUTION WITH STRICT DEADBAND FILTER
        val direction = when {
            score >= DEADBAND_THRESHOLD -> "UP"
            score <= -DEADBAND_THRESHOLD -> "DOWN"
            else -> "NO TRADE"
        }

        // 6. RAW MODEL SCORE MAPPING (0.0 to 100.0)
        val rawModelScore = when (direction) {
            "UP" -> (50.0 + min(45.0, score * RAW_SCORE_MULTIPLIER)).coerceIn(55.0, 95.0)
            "DOWN" -> (50.0 - min(45.0, abs(score) * RAW_SCORE_MULTIPLIER)).coerceIn(5.0, 45.0)
            else -> 50.0
        }

        // 7. CALIBRATION REPRESENTATION
        val calibratedText = if (sampleSizeN < 300) {
            "UNCALIBRATED (N < 300: $sampleSizeN/300)"
        } else {
            val prob = if (direction == "UP") rawModelScore else (100.0 - rawModelScore)
            "${String.format(Locale.US, "%.1f", prob)}% (N=$sampleSizeN)"
        }

        // 8. HUMAN-READABLE RATIONALE
        val rationale = when (direction) {
            "UP" -> "🟢 BULLISH SIGNAL: EMA spread (+$${String.format(Locale.US, "%.1f", emaSpread)}) and momentum (+$${String.format(Locale.US, "%.1f", mom)}) confirm upside alignment with RSI at ${String.format(Locale.US, "%.1f", rsi)}."
            "DOWN" -> "🔴 BEARISH SIGNAL: EMA spread rejected (-$${String.format(Locale.US, "%.1f", abs(emaSpread))}) with downside momentum (-$${String.format(Locale.US, "%.1f", abs(mom))}) and RSI at ${String.format(Locale.US, "%.1f", rsi)}."
            else -> "⚪ NO TRADE: Confluence indicators in neutral consolidation range (|score| < 0.50). Awaiting strong setup."
        }

        return QuantitativeModelOutput(
            direction = direction,
            rawCompositeScore = Math.round(score * 100.0) / 100.0,
            rawModelScore = Math.round(rawModelScore * 10.0) / 10.0,
            calibratedProbabilityText = calibratedText,
            reasoning = rationale,
            factors = factors,
            modelVersion = MODEL_VERSION
        )
    }
}

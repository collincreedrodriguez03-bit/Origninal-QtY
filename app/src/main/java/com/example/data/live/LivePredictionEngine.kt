package com.example.data.live

import com.example.data.quant.QuantitativeContributingFactor
import com.example.data.quant.QuantitativeFeatures
import com.example.data.quant.SharedQuantitativeModel
import com.example.ui.ConnectionState
import java.util.Locale

object LivePredictionEngine {

    const val MODEL_VERSION = SharedQuantitativeModel.MODEL_VERSION

    /**
     * Evaluates live market conditions approximately every 10 seconds.
     * Enforces fail-closed NO TRADE on stale data, weak signal, or missing features.
     * Delegates core scoring to SharedQuantitativeModel (Single Source of Truth).
     */
    fun evaluate(
        features: LiveFeatureSnapshot,
        secondsRemaining: Int,
        sampleSizeN: Int = 0,
        historyTickCount: Int = 30
    ): LivePredictionLogRecord {
        val now = System.currentTimeMillis()
        val mm = secondsRemaining / 60
        val ss = secondsRemaining % 60
        val formattedTimer = String.format(Locale.US, "%02d:%02d", mm, ss)

        // 1. FAIL-CLOSED CHECK: Stale or Invalid Market Data
        if (features.isStale || features.dataFreshness == ConnectionState.DATA_STALE || features.dataFreshness == ConnectionState.DISCONNECTED || features.dataFreshness == ConnectionState.INVALID_DATA) {
            return LivePredictionLogRecord(
                timestampMs = now,
                direction = "NO TRADE",
                rawModelScore = 50.0,
                calibratedProbabilityText = "N/A (DATA STALE)",
                currentBtcPrice = features.spotPrice,
                strikePrice = features.strikePrice,
                deltaToStrike = features.deltaToStrike,
                timeRemainingSeconds = secondsRemaining,
                formattedTimeRemaining = formattedTimer,
                modelVersion = MODEL_VERSION,
                dataFreshness = features.dataFreshness,
                reasoning = "⚠️ DATA STALE: Live market data connection interrupted or stale. Trading strictly halted.",
                contributingFactors = emptyList(),
                featureSnapshot = features,
                isStale = true
            )
        }

        // 2. FAIL-CLOSED CHECK: Insufficient Candle History (< 21 completed 1m candles)
        if (historyTickCount < 21) {
            return LivePredictionLogRecord(
                timestampMs = now,
                direction = "NO TRADE",
                rawModelScore = 50.0,
                calibratedProbabilityText = "N/A (WARMING UP)",
                currentBtcPrice = features.spotPrice,
                strikePrice = features.strikePrice,
                deltaToStrike = features.deltaToStrike,
                timeRemainingSeconds = secondsRemaining,
                formattedTimeRemaining = formattedTimer,
                modelVersion = MODEL_VERSION,
                dataFreshness = features.dataFreshness,
                reasoning = "⏳ INSUFFICIENT HISTORY: Accumulating completed 1-minute candles ($historyTickCount/21 completed) for EMA(21) / RSI(14) calculation.",
                contributingFactors = emptyList(),
                featureSnapshot = features,
                isStale = false
            )
        }

        // 3. Delegate to SharedQuantitativeModel (Single Source of Truth)
        val quantFeatures = QuantitativeFeatures(
            ema9 = features.ema9,
            ema21 = features.ema21,
            momentum = features.momentum,
            rsi = features.rsi,
            deltaToStrike = features.deltaToStrike
        )

        val quantOutput = SharedQuantitativeModel.evaluate(
            features = quantFeatures,
            sampleSizeN = sampleSizeN
        )

        val liveFactors = quantOutput.factors.map { factor ->
            LiveContributingFactor(
                name = factor.name,
                scoreContribution = factor.scoreContribution,
                description = factor.description
            )
        }

        return LivePredictionLogRecord(
            timestampMs = now,
            direction = quantOutput.direction,
            rawModelScore = quantOutput.rawModelScore,
            calibratedProbabilityText = quantOutput.calibratedProbabilityText,
            currentBtcPrice = features.spotPrice,
            strikePrice = features.strikePrice,
            deltaToStrike = features.deltaToStrike,
            timeRemainingSeconds = secondsRemaining,
            formattedTimeRemaining = formattedTimer,
            modelVersion = quantOutput.modelVersion,
            dataFreshness = features.dataFreshness,
            reasoning = quantOutput.reasoning,
            contributingFactors = liveFactors,
            featureSnapshot = features,
            isStale = false
        )
    }
}

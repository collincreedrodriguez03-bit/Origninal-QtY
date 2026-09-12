package com.example.data.quant

import com.example.data.backtest.HistoricalCandle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Standardized feature extraction engine used IDENTICALLY by both:
 * 1. Historical Backtesting Engine (HistoricalBacktestEngine.kt)
 * 2. Live Prediction Engine & Live Runtime (TradingViewModel.kt & LivePredictionEngine.kt)
 *
 * Ensures mathematical parity across:
 * - RSI(14) on 1-minute candle closes
 * - EMA(9) on 1-minute candle closes
 * - EMA(21) on 1-minute candle closes
 * - 15-minute momentum (Close[t] - Close[t-15m])
 * - Strike distance buffer (CurrentSpot - StrikePrice)
 */
object QuantitativeFeatureExtractor {

    const val MINIMUM_REQUIRED_CANDLES = 21 // Minimum required for EMA(21) initialization
    const val DEFAULT_LOOKBACK_CANDLES = 30

    /**
     * Extracts standardized QuantitativeFeatures from a sequential list of 1-minute candles.
     */
    fun extractFeatures(
        candles: List<HistoricalCandle>,
        currentSpot: Double,
        strikePrice: Double
    ): QuantitativeFeatures? {
        if (candles.size < MINIMUM_REQUIRED_CANDLES) return null

        val closes = candles.map { it.close }
        val ema9 = calculateEma(closes, 9)
        val ema21 = calculateEma(closes, 21)
        val rsi = calculateRsi(closes, 14)
        val momentum = calculate15mMomentum(closes)
        val deltaToStrike = currentSpot - strikePrice

        return QuantitativeFeatures(
            ema9 = ema9,
            ema21 = ema21,
            momentum = momentum,
            rsi = rsi,
            deltaToStrike = deltaToStrike
        )
    }

    /**
     * RSI(14) calculated over 1-minute candle closes.
     * Uses standard 14-period Wilder / simple average gain/loss ratio over the past 14 intervals (15 closes).
     */
    fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size < period + 1) return 50.0
        val recentCloses = closes.takeLast(period + 1)
        var gains = 0.0
        var losses = 0.0
        for (i in 1 until recentCloses.size) {
            val change = recentCloses[i] - recentCloses[i - 1]
            if (change > 0) gains += change else losses += abs(change)
        }
        val avgGain = gains / period.toDouble()
        val avgLoss = losses / period.toDouble()
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Exponential Moving Average (EMA) calculated over candle closes.
     * Formula: EMA_today = Price_today * (2 / (N + 1)) + EMA_yesterday * (1 - (2 / (N + 1)))
     */
    fun calculateEma(closes: List<Double>, period: Int): Double {
        if (closes.isEmpty()) return 0.0
        if (closes.size < period) return closes.average()
        val k = 2.0 / (period + 1.0)
        var ema = closes.take(period).average()
        for (i in period until closes.size) {
            ema = (closes[i] * k) + (ema * (1.0 - k))
        }
        return ema
    }

    /**
     * 15-Minute Momentum calculated as Close[now] - Close[15 candles ago].
     * If fewer than 16 candles exist, falls back to Close[now] - Close[oldest].
     */
    fun calculate15mMomentum(closes: List<Double>): Double {
        if (closes.size < 2) return 0.0
        val lookbackIndex = max(0, closes.size - 1 - 15)
        return closes.last() - closes[lookbackIndex]
    }
}

/**
 * Aggregates live raw price ticks into strictly completed 1-minute OHLCV candles.
 * Ensures incomplete current-minute candles are NEVER leaked into technical feature calculations.
 */
class CandleAggregator(
    val maxCompletedHistorySize: Int = 60
) {
    private val completedCandlesList = mutableListOf<HistoricalCandle>()
    private var buildingCandleOpenMs: Long? = null
    private var buildingOpen: Double = 0.0
    private var buildingHigh: Double = 0.0
    private var buildingLow: Double = 0.0
    private var buildingClose: Double = 0.0
    private var buildingVolume: Double = 0.0

    /**
     * Ingests a live tick (price, timestampMs, optional volume).
     * Returns true if a new 1-minute candle was completed and sealed.
     */
    @Synchronized
    fun addTick(price: Double, timestampMs: Long, volume: Double = 1.0): Boolean {
        val minuteEpochMs = (timestampMs / 60_000L) * 60_000L
        var completedNewCandle = false

        val currentOpenMs = buildingCandleOpenMs
        if (currentOpenMs == null) {
            // First tick ever
            buildingCandleOpenMs = minuteEpochMs
            buildingOpen = price
            buildingHigh = price
            buildingLow = price
            buildingClose = price
            buildingVolume = volume
        } else if (minuteEpochMs > currentOpenMs) {
            // A new minute has rolled over: finalize the previous candle
            val finalizedCandle = HistoricalCandle(
                timestamp = currentOpenMs,
                open = buildingOpen,
                high = buildingHigh,
                low = buildingLow,
                close = buildingClose,
                volume = buildingVolume,
                isSynthetic = false
            )
            completedCandlesList.add(finalizedCandle)
            if (completedCandlesList.size > maxCompletedHistorySize) {
                completedCandlesList.removeAt(0)
            }
            completedNewCandle = true

            // Initialize the new building candle
            buildingCandleOpenMs = minuteEpochMs
            buildingOpen = price
            buildingHigh = price
            buildingLow = price
            buildingClose = price
            buildingVolume = volume
        } else {
            // Tick belongs to the current in-progress minute
            buildingHigh = max(buildingHigh, price)
            buildingLow = min(buildingLow, price)
            buildingClose = price
            buildingVolume += volume
        }

        return completedNewCandle
    }

    /**
     * Seeds completed candles (e.g., from initial REST API historical fetch).
     */
    @Synchronized
    fun seedCompletedCandles(candles: List<HistoricalCandle>) {
        completedCandlesList.clear()
        completedCandlesList.addAll(candles.takeLast(maxCompletedHistorySize))
    }

    /**
     * Returns ONLY completed 1-minute candles.
     * Incomplete current-minute candles are strictly excluded.
     */
    @Synchronized
    fun getCompletedCandles(): List<HistoricalCandle> {
        return completedCandlesList.toList()
    }

    /**
     * Returns the in-progress (incomplete) candle, if any.
     */
    @Synchronized
    fun getIncompleteBuildingCandle(): HistoricalCandle? {
        val openMs = buildingCandleOpenMs ?: return null
        return HistoricalCandle(
            timestamp = openMs,
            open = buildingOpen,
            high = buildingHigh,
            low = buildingLow,
            close = buildingClose,
            volume = buildingVolume,
            isSynthetic = false
        )
    }

    /**
     * Returns true if enough completed 1-minute candles exist to compute EMA(21) and RSI(14).
     */
    @Synchronized
    fun hasSufficientHistory(requiredCount: Int = QuantitativeFeatureExtractor.MINIMUM_REQUIRED_CANDLES): Boolean {
        return completedCandlesList.size >= requiredCount
    }

    /**
     * Extracts standard quantitative features from completed candles.
     * Strictly fails-closed (returns null) if completed candles < 21.
     */
    @Synchronized
    fun extractFeatures(currentSpot: Double, strikePrice: Double): QuantitativeFeatures? {
        return QuantitativeFeatureExtractor.extractFeatures(
            candles = completedCandlesList,
            currentSpot = currentSpot,
            strikePrice = strikePrice
        )
    }

    @Synchronized
    fun clear() {
        completedCandlesList.clear()
        buildingCandleOpenMs = null
    }
}

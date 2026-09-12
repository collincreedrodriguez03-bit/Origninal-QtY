package com.example

import com.example.data.backtest.HistoricalCandle
import com.example.data.live.LiveContractWindow
import com.example.data.live.LiveFeatureSnapshot
import com.example.data.live.LivePredictionEngine
import com.example.data.quant.CandleAggregator
import com.example.data.quant.QuantitativeFeatureExtractor
import com.example.data.quant.QuantitativeFeatures
import com.example.data.quant.SharedQuantitativeModel
import com.example.ui.ConnectionState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test Suite for CandleAggregator & Live/Historical Feature Parity:
 * 1. Live ticks aggregate into correct 1-minute candles
 * 2. Incomplete candles are not used in feature calculations
 * 3. Live feature calculations match historical calculations for identical candle data
 * 4. RSI, EMA, and Momentum inputs are identical
 * 5. Strike remains immutable throughout the 15-minute window
 * 6. Stale data produces fail-closed NO TRADE
 * 7. Insufficient history (< 21 completed candles) produces fail-closed NO TRADE
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CandleAggregatorAndFeatureParityTest {

    @Test
    fun test1_liveTicksAggregateIntoCorrect1MinuteCandles() {
        val aggregator = CandleAggregator()

        // 1m window starting at T = 1700000000000 (which is 1700000000000 % 60000 == 20000, so window start is 1699999980000)
        val windowStartMs = 1700000040000L // aligned to minute boundary
        val minuteStart = (windowStartMs / 60_000L) * 60_000L

        // Feed ticks within the same 1-minute window
        aggregator.addTick(price = 95000.0, volume = 1.0, timestampMs = minuteStart + 1000L) // Open = 95000.0
        aggregator.addTick(price = 95200.0, volume = 2.0, timestampMs = minuteStart + 15000L) // High
        aggregator.addTick(price = 94800.0, volume = 1.5, timestampMs = minuteStart + 30000L) // Low
        aggregator.addTick(price = 95150.0, volume = 0.5, timestampMs = minuteStart + 55000L) // Close = 95150.0

        // In the middle of the minute, 0 completed candles exist
        assertEquals(0, aggregator.getCompletedCandles().size)
        assertNotNull(aggregator.getIncompleteBuildingCandle())

        // Feed tick in next minute -> seals the previous candle
        val nextMinuteStart = minuteStart + 60_000L
        aggregator.addTick(price = 95180.0, volume = 1.0, timestampMs = nextMinuteStart + 2000L)

        val completed = aggregator.getCompletedCandles()
        assertEquals(1, completed.size)

        val candle = completed.first()
        assertEquals(minuteStart, candle.timestamp)
        assertEquals(95000.0, candle.open, 0.001)
        assertEquals(95200.0, candle.high, 0.001)
        assertEquals(94800.0, candle.low, 0.001)
        assertEquals(95150.0, candle.close, 0.001)
        assertEquals(5.0, candle.volume, 0.001)
    }

    @Test
    fun test2_incompleteCandlesAreNotUsedInFeatureCalculations() {
        val aggregator = CandleAggregator()
        val baseMs = 1700000000000L - (1700000000000L % 60_000L)

        // Add 25 completed 1m candles
        val initialCandles = (0 until 25).map { i ->
            val minuteMs = baseMs + (i * 60_000L)
            HistoricalCandle(
                timestamp = minuteMs,
                open = 95000.0 + i * 10,
                high = 95000.0 + i * 10 + 15,
                low = 95000.0 + i * 10 - 5,
                close = 95000.0 + i * 10 + 5,
                volume = 10.0
            )
        }
        aggregator.seedCompletedCandles(initialCandles)

        val completedBefore = aggregator.getCompletedCandles().size
        assertEquals(25, completedBefore)

        // Inject wild incomplete candle ticks in the current 26th minute
        val curMinuteMs = baseMs + (25 * 60_000L)
        aggregator.addTick(price = 150000.0, volume = 999.0, timestampMs = curMinuteMs + 1000L)
        aggregator.addTick(price = 200000.0, volume = 999.0, timestampMs = curMinuteMs + 2000L)

        // Completed candles count MUST remain 25, unaffected by incomplete candle
        assertEquals(25, aggregator.getCompletedCandles().size)

        val features = aggregator.extractFeatures(currentSpot = 95255.0, strikePrice = 95000.0)
        assertNotNull(features)

        // Verify EMA21 is calculated ONLY on the 25 completed candles (not distorted by 200000.0 tick)
        val expectedEma21 = QuantitativeFeatureExtractor.calculateEma(
            aggregator.getCompletedCandles().map { it.close },
            21
        )
        assertEquals(expectedEma21, features!!.ema21, 0.001)
        assertTrue("EMA21 should be close to 95000-95300 range", features.ema21 < 96000.0)
    }

    @Test
    fun test3_liveFeatureCalculationsMatchHistoricalCalculationsForIdenticalData() {
        val baseMs = 1700000000000L - (1700000000000L % 60_000L)
        val candles = mutableListOf<HistoricalCandle>()

        // Generate 30 minutes of realistic candle data
        var price = 96000.0
        val deltas = listOf(
            5.0, -8.0, 12.0, 15.0, -3.0, -10.0, 8.0, 14.0, -6.0, 20.0,
            -15.0, 7.0, 9.0, -12.0, 18.0, -5.0, 22.0, -14.0, 11.0, 16.0,
            -8.0, 19.0, -4.0, 25.0, -10.0, 15.0, -7.0, 30.0, -12.0, 18.0
        )

        for (i in deltas.indices) {
            val open = price
            price += deltas[i]
            candles.add(
                HistoricalCandle(
                    timestamp = baseMs + i * 60_000L,
                    open = open,
                    high = maxOf(open, price) + 5.0,
                    low = minOf(open, price) - 5.0,
                    close = price,
                    volume = 25.0
                )
            )
        }

        val currentSpot = price
        val strikePrice = candles.first().open

        // 1. Calculate features via Historical path (QuantitativeFeatureExtractor)
        val historicalFeatures = QuantitativeFeatureExtractor.extractFeatures(
            candles = candles,
            currentSpot = currentSpot,
            strikePrice = strikePrice
        )
        assertNotNull(historicalFeatures)

        // 2. Feed same candles into live CandleAggregator
        val liveAggregator = CandleAggregator()
        liveAggregator.seedCompletedCandles(candles)

        val liveFeatures = liveAggregator.extractFeatures(
            currentSpot = currentSpot,
            strikePrice = strikePrice
        )
        assertNotNull(liveFeatures)

        // 3. Mathematical parity verification
        assertEquals(historicalFeatures!!.rsi, liveFeatures!!.rsi, 1e-6)
        assertEquals(historicalFeatures.ema9, liveFeatures.ema9, 1e-6)
        assertEquals(historicalFeatures.ema21, liveFeatures.ema21, 1e-6)
        assertEquals(historicalFeatures.momentum, liveFeatures.momentum, 1e-6)
        assertEquals(historicalFeatures.deltaToStrike, liveFeatures.deltaToStrike, 1e-6)

        // 4. Model score verification
        val histScore = SharedQuantitativeModel.evaluate(historicalFeatures)
        val liveScore = SharedQuantitativeModel.evaluate(liveFeatures)

        assertEquals(histScore.direction, liveScore.direction)
        assertEquals(histScore.rawModelScore, liveScore.rawModelScore, 1e-6)
        assertEquals(histScore.rawCompositeScore, liveScore.rawCompositeScore, 1e-6)
        assertEquals(histScore.calibratedProbabilityText, liveScore.calibratedProbabilityText)
    }

    @Test
    fun test4_rsiEmaMomentumInputsAreIdentical() {
        val closes = listOf(
            100.0, 102.0, 101.0, 104.0, 103.0, 105.0, 107.0, 106.0, 108.0, 110.0,
            109.0, 111.0, 113.0, 112.0, 115.0, 114.0, 116.0, 118.0, 117.0, 119.0, 121.0
        )

        // RSI 14 calculation check
        val rsi = QuantitativeFeatureExtractor.calculateRsi(closes, 14)
        assertTrue("RSI should be between 0 and 100", rsi in 0.0..100.0)

        // EMA 9 and EMA 21 check
        val ema9 = QuantitativeFeatureExtractor.calculateEma(closes, 9)
        val ema21 = QuantitativeFeatureExtractor.calculateEma(closes, 21)
        assertTrue("EMA 9 should be higher than EMA 21 in uptrend", ema9 > ema21)

        // Momentum check (15-minute momentum)
        val mom = QuantitativeFeatureExtractor.calculate15mMomentum(closes)
        assertEquals(121.0 - closes[closes.size - 1 - 15], mom, 0.001)
    }

    @Test
    fun test5_strikeRemainsImmutableThroughout15MinuteWindow() {
        val windowStartMs = 1700000000000L - (1700000000000L % (15 * 60_000L))
        val initialSpotAtOpen = 96250.0

        // At T = 0m (window start)
        val windowT0 = LiveContractWindow.calculateCurrentWindow(
            nowMs = windowStartMs + 5000L,
            currentSpot = initialSpotAtOpen,
            lockedResearchStrike = null,
            lockedWindowStartMs = null,
            whatIfStrike = null
        )
        assertEquals(initialSpotAtOpen, windowT0.researchStrikePrice, 0.001)

        val lockedStrike = windowT0.researchStrikePrice
        val lockedStart = windowT0.windowStartTimestampMs

        // At T = 7m (mid-window, spot moves +$500, what-if strike is set by user to $99,000)
        val windowT7 = LiveContractWindow.calculateCurrentWindow(
            nowMs = windowStartMs + (7 * 60_000L),
            currentSpot = 96750.0,
            lockedResearchStrike = lockedStrike,
            lockedWindowStartMs = lockedStart,
            whatIfStrike = 99000.0
        )

        // Research strike MUST remain strictly locked to initial S_0 = 96250.0
        assertEquals(96250.0, windowT7.researchStrikePrice, 0.001)
        assertEquals(lockedStart, windowT7.windowStartTimestampMs)
        assertEquals(500.0, windowT7.researchDeltaToStrike, 0.001) // 96750 - 96250 = +500

        // Display-only what-if strike is isolated
        assertEquals(99000.0, windowT7.whatIfStrikePrice, 0.001)
    }

    @Test
    fun test6_staleDataProducesFailClosedNoTrade() {
        val staleFeatures = LiveFeatureSnapshot(
            timestampMs = System.currentTimeMillis() - 30_000L,
            spotPrice = 96500.0,
            strikePrice = 96000.0,
            deltaToStrike = 500.0,
            rsi = 80.0,
            momentum = 50.0,
            ema9 = 96500.0,
            ema21 = 96000.0,
            emaSpread = 500.0,
            volatility = 20.0,
            dataFreshness = ConnectionState.DATA_STALE,
            isStale = true
        )

        val prediction = LivePredictionEngine.evaluate(
            features = staleFeatures,
            secondsRemaining = 450,
            sampleSizeN = 500,
            historyTickCount = 30
        )

        assertEquals("NO TRADE", prediction.direction)
        assertEquals(50.0, prediction.rawModelScore, 0.001)
        assertTrue(prediction.reasoning.contains("DATA STALE"))
    }

    @Test
    fun test7_insufficientHistoryProducesFailClosedNoTrade() {
        val validFeatures = LiveFeatureSnapshot(
            timestampMs = System.currentTimeMillis(),
            spotPrice = 96500.0,
            strikePrice = 96000.0,
            deltaToStrike = 500.0,
            rsi = 80.0,
            momentum = 50.0,
            ema9 = 96500.0,
            ema21 = 96000.0,
            emaSpread = 500.0,
            volatility = 20.0,
            dataFreshness = ConnectionState.CONNECTED,
            isStale = false
        )

        // Only 10 completed candles (< 21 required)
        val prediction = LivePredictionEngine.evaluate(
            features = validFeatures,
            secondsRemaining = 450,
            sampleSizeN = 500,
            historyTickCount = 10
        )

        assertEquals("NO TRADE", prediction.direction)
        assertEquals(50.0, prediction.rawModelScore, 0.001)
        assertTrue(prediction.reasoning.contains("INSUFFICIENT HISTORY"))
    }
}

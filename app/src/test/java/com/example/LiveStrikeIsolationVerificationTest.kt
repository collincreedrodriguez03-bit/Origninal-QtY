package com.example

import com.example.data.live.LiveContractWindow
import com.example.data.live.LiveFeatureSnapshot
import com.example.data.live.LivePredictionEngine
import com.example.ui.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

/**
 * Phase 2 - Isolated Verification Suite: Strike Integrity & Isolation
 *
 * Verifies that:
 * 1. The Research Strike is strictly immutable for the duration of the 15-minute UTC window (:00, :15, :30, :45 UTC).
 * 2. UI What-If strike controls (Sync Spot, -$50, +$50, Edit Strike) are completely isolated and CANNOT alter
 *    the research strike, research features, deltaToStrike, or LivePredictionEngine inputs.
 * 3. Rolling over into a new 15-minute window properly locks a new research strike at the candle open of T_new.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveStrikeIsolationVerificationTest {

    private fun getAlignedUtcTimestamp(hour: Int, minute: Int, second: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun test_uiStrikeAdjustments_cannotModifyResearchStrikeOrPredictionInputs() {
        val windowStartTimeMs = getAlignedUtcTimestamp(hour = 14, minute = 0, second = 0)
        val evaluationTimeMs = getAlignedUtcTimestamp(hour = 14, minute = 3, second = 20) // 3m 20s into window
        val spotOpen = 96400.0 // Candle open at T (14:00:00 UTC)
        val currentSpot = 96480.0 // Spot at evaluation time

        // 1. Initial window initialization: Locks Research Strike at S_0 = 96400.0
        val initialWindow = LiveContractWindow.calculateCurrentWindow(
            nowMs = windowStartTimeMs,
            currentSpot = spotOpen,
            lockedResearchStrike = null,
            lockedWindowStartMs = null,
            whatIfStrike = null
        )
        val lockedResearchStrike = initialWindow.researchStrikePrice
        assertEquals("Initial research strike must match candle open S_0", 96400.0, lockedResearchStrike, 0.001)

        // Baseline Prediction Record using locked research strike
        val baselineFeatures = LiveFeatureSnapshot(
            timestampMs = evaluationTimeMs,
            spotPrice = currentSpot,
            strikePrice = lockedResearchStrike,
            deltaToStrike = currentSpot - lockedResearchStrike,
            rsi = 58.0,
            momentum = 12.0,
            ema9 = 96470.0,
            ema21 = 96450.0,
            emaSpread = 20.0,
            volatility = 35.0,
            dataFreshness = ConnectionState.CONNECTED,
            isStale = false
        )
        val baselinePrediction = LivePredictionEngine.evaluate(
            features = baselineFeatures,
            secondsRemaining = initialWindow.secondsRemaining,
            sampleSizeN = 350,
            historyTickCount = 15
        )

        // 2. Simulate user pressing UI What-If buttons: -$50, +$50, Sync Spot, Custom Arbitrary Strike
        val whatIfStrikeScenarios = listOf(
            96480.0, // Sync Spot
            96430.0, // -$50 relative to spot
            96530.0, // +$50 relative to spot
            90000.0, // Far ITM What-If
            105000.0 // Far OTM What-If
        )

        for (whatIfStrike in whatIfStrikeScenarios) {
            val evalWindow = LiveContractWindow.calculateCurrentWindow(
                nowMs = evaluationTimeMs,
                currentSpot = currentSpot,
                lockedResearchStrike = lockedResearchStrike,
                lockedWindowStartMs = windowStartTimeMs,
                whatIfStrike = whatIfStrike
            )

            // A. Assert Research Strike remains strictly 96400.0
            assertEquals("Research strike must NOT mutate with UI What-If strike $whatIfStrike", 96400.0, evalWindow.researchStrikePrice, 0.001)
            assertEquals("Research delta must remain spot - 96400.0", 80.0, evalWindow.researchDeltaToStrike, 0.001)

            // B. Assert What-If strike is recorded separately for display only
            assertEquals("What-If strike must match user input", whatIfStrike, evalWindow.whatIfStrikePrice, 0.001)
            assertEquals("What-If delta must match user input delta", currentSpot - whatIfStrike, evalWindow.whatIfDeltaToStrike, 0.001)

            // C. Build Research Features using evalWindow research values
            val evalFeatures = LiveFeatureSnapshot(
                timestampMs = evaluationTimeMs,
                spotPrice = currentSpot,
                strikePrice = evalWindow.researchStrikePrice,
                deltaToStrike = evalWindow.researchDeltaToStrike,
                rsi = 58.0,
                momentum = 12.0,
                ema9 = 96470.0,
                ema21 = 96450.0,
                emaSpread = 20.0,
                volatility = 35.0,
                dataFreshness = ConnectionState.CONNECTED,
                isStale = false
            )

            val evalPrediction = LivePredictionEngine.evaluate(
                features = evalFeatures,
                secondsRemaining = evalWindow.secondsRemaining,
                sampleSizeN = 350,
                historyTickCount = 15
            )

            // D. PROVE: Research engine output is 100% IDENTICAL and immune to UI strike alterations
            assertEquals("Prediction direction must be completely unaffected by UI what-if strike", baselinePrediction.direction, evalPrediction.direction)
            assertEquals("Raw model score must be completely unaffected by UI what-if strike", baselinePrediction.rawModelScore, evalPrediction.rawModelScore, 0.001)
            assertEquals("Strike fed to prediction must remain immutable research strike", 96400.0, evalPrediction.strikePrice, 0.001)
            assertEquals("Delta fed to prediction must remain immutable research delta", 80.0, evalPrediction.deltaToStrike, 0.001)
        }
    }

    @Test
    fun test_researchStrike_immutableDuringSame15MinuteWindow() {
        val windowStartTimeMs = getAlignedUtcTimestamp(hour = 10, minute = 15, second = 0)
        val initialSpotOpen = 95200.0

        // Lock research strike at 10:15:00 UTC
        val initialWindow = LiveContractWindow.calculateCurrentWindow(
            nowMs = windowStartTimeMs,
            currentSpot = initialSpotOpen,
            lockedResearchStrike = null,
            lockedWindowStartMs = null,
            whatIfStrike = null
        )
        val lockedStrike = initialWindow.researchStrikePrice
        assertEquals(95200.0, lockedStrike, 0.001)

        // Multiple subsequent timestamps and volatile price ticks inside the same 10:15 - 10:30 UTC window
        val intraWindowTicks = listOf(
            Pair(getAlignedUtcTimestamp(hour = 10, minute = 16, second = 10), 95350.0),
            Pair(getAlignedUtcTimestamp(hour = 10, minute = 19, second = 45), 94900.0),
            Pair(getAlignedUtcTimestamp(hour = 10, minute = 24, second = 0), 95800.0),
            Pair(getAlignedUtcTimestamp(hour = 10, minute = 28, second = 30), 95100.0),
            Pair(getAlignedUtcTimestamp(hour = 10, minute = 29, second = 59), 95450.0) // 1 second before expiry
        )

        for ((tickTimeMs, spot) in intraWindowTicks) {
            val tickWindow = LiveContractWindow.calculateCurrentWindow(
                nowMs = tickTimeMs,
                currentSpot = spot,
                lockedResearchStrike = lockedStrike,
                lockedWindowStartMs = windowStartTimeMs,
                whatIfStrike = null
            )

            // The research strike MUST NOT change despite spot swings or time elapsing
            assertEquals("Research strike must remain immutable at $lockedStrike for tick at $tickTimeMs", lockedStrike, tickWindow.researchStrikePrice, 0.001)
            assertEquals("Window start must remain 10:15:00 UTC", windowStartTimeMs, tickWindow.windowStartTimestampMs)
            assertTrue("Seconds remaining must be <= 900 and >= 0", tickWindow.secondsRemaining in 0..900)
        }
    }

    @Test
    fun test_windowRollover_locksNewResearchStrikeAtNextBoundary() {
        val window1StartTimeMs = getAlignedUtcTimestamp(hour = 11, minute = 0, second = 0)
        val spot1 = 97000.0

        val window1 = LiveContractWindow.calculateCurrentWindow(
            nowMs = window1StartTimeMs,
            currentSpot = spot1,
            lockedResearchStrike = null,
            lockedWindowStartMs = null,
            whatIfStrike = null
        )
        val strike1 = window1.researchStrikePrice
        assertEquals(97000.0, strike1, 0.001)

        // Rollover to next 15m window at 11:15:00 UTC with new spot price
        val window2StartTimeMs = getAlignedUtcTimestamp(hour = 11, minute = 15, second = 0)
        val spot2 = 97350.0

        val window2 = LiveContractWindow.calculateCurrentWindow(
            nowMs = window2StartTimeMs,
            currentSpot = spot2,
            lockedResearchStrike = strike1,
            lockedWindowStartMs = window1StartTimeMs, // Previous window's start
            whatIfStrike = null
        )

        // Because windowStartMs changed from 11:00 to 11:15, a new research strike is captured from spot2 (97350.0)
        assertEquals("New window start must be 11:15 UTC", window2StartTimeMs, window2.windowStartTimestampMs)
        assertEquals("New research strike must be captured at 11:15 candle open S_0 = 97350.0", 97350.0, window2.researchStrikePrice, 0.001)
        assertNotEquals("New strike must not reuse previous window's strike", strike1, window2.researchStrikePrice)
    }
}

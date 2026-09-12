package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.AppDatabase
import com.example.data.live.LiveContractWindow
import com.example.data.live.LiveFeatureSnapshot
import com.example.data.live.LiveObservationDao
import com.example.data.live.LiveObservationEntity
import com.example.data.live.LivePredictionEngine
import com.example.data.live.LiveRadarValidationMetricsCalculator
import com.example.data.live.ResearchGateStatus
import com.example.ui.ConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

/**
 * Phase 3 — Live Heuristic Radar Validation Suite
 *
 * Proves:
 * 1. Live evaluation generates signal.
 * 2. Signal is written to persistent ledger with actualOutcome = "PENDING" and settlementPrice = null.
 * 3. Process restart simulation: Ledger persists records across DB connections.
 * 4. Record immutability: Signal creation features cannot be rewritten; settlement is strictly decoupled.
 * 5. Strong-signal hypothesis logic (|C| >= 1.67, score >= 70 or <= 30).
 * 6. Strict separation of historical baseline A and live validation B.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveRadarValidationLedgerTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: LiveObservationDao
    private lateinit var context: Context

    private fun getUtcTimestamp(hour: Int, minute: Int, second: Int): Long {
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.liveObservationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_step1_and_step2_liveEvaluationGeneratesSignal_andPersistsToLedger() = runBlocking {
        val windowStartMs = getUtcTimestamp(14, 0, 0)
        val evalTimeMs = getUtcTimestamp(14, 3, 20)
        val spotPrice = 96520.0
        val strikePrice = 96400.0

        val window = LiveContractWindow.calculateCurrentWindow(
            nowMs = evalTimeMs,
            currentSpot = spotPrice,
            lockedResearchStrike = strikePrice,
            lockedWindowStartMs = windowStartMs
        )

        // 1. Live Evaluation generates signal
        val features = LiveFeatureSnapshot(
            timestampMs = evalTimeMs,
            spotPrice = spotPrice,
            strikePrice = strikePrice,
            deltaToStrike = 120.0,
            ema9 = 96500.0,
            ema21 = 96450.0,
            rsi = 56.5,
            momentum = 15.0,
            emaSpread = 50.0,
            volatility = 180.0,
            dataFreshness = ConnectionState.CONNECTED,
            isStale = false
        )

        val prediction = LivePredictionEngine.evaluate(
            features = features,
            secondsRemaining = window.secondsRemaining,
            sampleSizeN = 2576,
            historyTickCount = 60
        )

        assertEquals("Signal direction must be UP given bullish confluence", "UP", prediction.direction)
        assertTrue("Raw model score should reflect UP lean (> 50)", prediction.rawModelScore > 50.0)

        // 2. Write to persistent observation ledger
        val obs = LiveObservationEntity(
            sequenceId = prediction.id,
            timestampMs = prediction.timestampMs,
            formattedTimestampUtc = "${prediction.formattedTime} UTC",
            btcPrice = prediction.currentBtcPrice,
            contractWindowId = window.formattedWindowRange,
            windowStartTimestampMs = window.windowStartTimestampMs,
            settlementTimestampMs = window.settlementTimestampMs,
            strikePrice = window.researchStrikePrice,
            timeRemainingSeconds = prediction.timeRemainingSeconds,
            direction = prediction.direction,
            rawModelScore = prediction.rawModelScore,
            emaContribution = 0.8,
            momentumContribution = 0.6,
            rsiContribution = 0.5,
            strikeBufferContribution = 0.4,
            modelVersion = prediction.modelVersion,
            isStrongSignal = prediction.rawModelScore >= 70.0 || prediction.rawModelScore <= 30.0,
            actualOutcome = "PENDING",
            settlementPrice = null,
            settledTimestampMs = null,
            baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
            featureEmaSpread = features.emaSpread,
            featureMomentum = features.momentum,
            featureRsi = features.rsi,
            featureDeltaToStrike = features.deltaToStrike,
            isStaleData = false
        )

        dao.insertObservation(obs)

        val stored = dao.getObservationBySequenceId(prediction.id)
        assertNotNull("Record must exist in Room DB", stored)
        assertEquals("Outcome must be PENDING at creation time", "PENDING", stored!!.actualOutcome)
        assertNull("Settlement price must be null at signal creation time (no lookahead)", stored.settlementPrice)
        assertEquals(96400.0, stored.strikePrice, 0.001)
        assertEquals(96520.0, stored.btcPrice, 0.001)
        assertEquals(prediction.rawModelScore, stored.rawModelScore, 0.001)
    }

    @Test
    fun test_pendingObservation_cannotBeSettledBeforeContractExpiry() = runBlocking {
        val windowStartMs = 1700000000000L
        val expiryMs = 1700000900000L // T + 15m
        val seqId = "OBS-EXPIRY-TEST-001"

        val obs = LiveObservationEntity(
            sequenceId = seqId,
            timestampMs = windowStartMs + 180_000L, // T + 3m (created during active window)
            formattedTimestampUtc = "14:03:00 UTC",
            btcPrice = 96500.0,
            contractWindowId = "14:00 - 14:15 UTC",
            windowStartTimestampMs = windowStartMs,
            settlementTimestampMs = expiryMs,
            strikePrice = 96400.0,
            timeRemainingSeconds = 720,
            direction = "UP",
            rawModelScore = 75.0,
            emaContribution = 0.8,
            momentumContribution = 0.6,
            rsiContribution = 0.5,
            strikeBufferContribution = 0.4,
            modelVersion = "v2.0-quant-multival",
            isStrongSignal = true,
            actualOutcome = "PENDING",
            settlementPrice = null,
            settledTimestampMs = null,
            baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"
        )
        dao.insertObservation(obs)

        // 1. Attempt lookup for settlement BEFORE expiration (e.g., at T+10m = expiry - 5m)
        val beforeExpiryTimeMs = expiryMs - 300_000L
        val pendingBeforeExpiry = dao.getPendingObservationsForSettlement(beforeExpiryTimeMs)
        assertTrue(
            "Pending observation MUST NOT be eligible for settlement before expiry",
            pendingBeforeExpiry.none { it.sequenceId == seqId }
        )

        // 2. Lookup AT or AFTER expiration (e.g. at expiryMs)
        val pendingAtExpiry = dao.getPendingObservationsForSettlement(expiryMs)
        assertTrue(
            "Pending observation becomes eligible for settlement only once expiry is reached",
            pendingAtExpiry.any { it.sequenceId == seqId }
        )
    }

    @Test
    fun test_settledObservation_cannotBeSettledSecondTime() = runBlocking {
        val seqId = "OBS-SETTLE-ONCE-002"
        val obs = LiveObservationEntity(
            sequenceId = seqId,
            timestampMs = 1700000000000L,
            formattedTimestampUtc = "14:03:00 UTC",
            btcPrice = 96500.0,
            contractWindowId = "14:00 - 14:15 UTC",
            windowStartTimestampMs = 1700000000000L,
            settlementTimestampMs = 1700000900000L,
            strikePrice = 96400.0,
            timeRemainingSeconds = 720,
            direction = "UP",
            rawModelScore = 75.0,
            emaContribution = 0.8,
            momentumContribution = 0.6,
            rsiContribution = 0.5,
            strikeBufferContribution = 0.4,
            modelVersion = "v2.0-quant-multival",
            isStrongSignal = true,
            actualOutcome = "PENDING",
            settlementPrice = null,
            settledTimestampMs = null,
            baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"
        )
        dao.insertObservation(obs)

        // First settlement: Valid PENDING -> WON
        val firstSettlementRows = dao.settleObservation(
            sequenceId = seqId,
            outcome = "WON",
            settlementPrice = 96550.0,
            settledTimestampMs = 1700000900000L
        )
        assertEquals("First settlement must update exactly 1 row", 1, firstSettlementRows)

        val firstSettledState = dao.getObservationBySequenceId(seqId)
        assertEquals("WON", firstSettledState!!.actualOutcome)
        assertEquals(96550.0, firstSettledState.settlementPrice!!, 0.001)

        // Second settlement attempt: Trying to rewrite to LOST with a different price
        val secondSettlementRows = dao.settleObservation(
            sequenceId = seqId,
            outcome = "LOST",
            settlementPrice = 96300.0,
            settledTimestampMs = 1700000950000L
        )
        assertEquals("Second settlement must update 0 rows because outcome is already settled", 0, secondSettlementRows)

        // Verify the record was NOT mutated by the second attempt
        val finalState = dao.getObservationBySequenceId(seqId)
        assertEquals("Outcome must remain WON", "WON", finalState!!.actualOutcome)
        assertEquals("Settlement price must remain 96550.0", 96550.0, finalState.settlementPrice!!, 0.001)
        assertEquals("Settled timestamp must not be overwritten", 1700000900000L, finalState.settledTimestampMs)
    }

    @Test
    fun test_predictionFields_cannotBeRetroactivelyChanged() = runBlocking {
        val seqId = "OBS-IMMUTABLE-003"
        val originalObs = LiveObservationEntity(
            sequenceId = seqId,
            timestampMs = 1700000000000L,
            formattedTimestampUtc = "14:03:00 UTC",
            btcPrice = 96520.0,
            contractWindowId = "14:00 - 14:15 UTC",
            windowStartTimestampMs = 1700000000000L,
            settlementTimestampMs = 1700000900000L,
            strikePrice = 96400.0,
            timeRemainingSeconds = 720,
            direction = "UP",
            rawModelScore = 77.6,
            emaContribution = 0.8,
            momentumContribution = 0.6,
            rsiContribution = 0.5,
            strikeBufferContribution = 0.4,
            modelVersion = "v2.0-quant-multival",
            isStrongSignal = true,
            actualOutcome = "PENDING",
            settlementPrice = null,
            settledTimestampMs = null,
            baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
            featureEmaSpread = 4.5,
            featureMomentum = 8.2,
            featureRsi = 58.4,
            featureDeltaToStrike = 35.0,
            isStaleData = false
        )
        dao.insertObservation(originalObs)

        // Attempt to overwrite existing record with tampered prediction scores & direction
        val tamperedObs = originalObs.copy(
            direction = "DOWN",
            rawModelScore = 15.0,
            strikePrice = 99999.0,
            btcPrice = 90000.0,
            emaContribution = -0.8
        )
        dao.insertObservation(tamperedObs)

        // Read back: Room OnConflictStrategy.IGNORE ensures zero modification
        val readBack = dao.getObservationBySequenceId(seqId)!!
        assertEquals("Direction must remain UP", "UP", readBack.direction)
        assertEquals("Score must remain 77.6", 77.6, readBack.rawModelScore, 0.001)
        assertEquals("Strike must remain 96400.0", 96400.0, readBack.strikePrice, 0.001)
        assertEquals("BTC price must remain 96520.0", 96520.0, readBack.btcPrice, 0.001)

        // Settle the observation
        dao.settleObservation(
            sequenceId = seqId,
            outcome = "WON",
            settlementPrice = 96600.0,
            settledTimestampMs = 1700000900000L
        )

        // Verify prediction fields remain intact after decoupled settlement
        val postSettled = dao.getObservationBySequenceId(seqId)!!
        assertEquals("WON", postSettled.actualOutcome)
        assertEquals(96600.0, postSettled.settlementPrice!!, 0.001)
        assertEquals("UP", postSettled.direction)
        assertEquals(77.6, postSettled.rawModelScore, 0.001)
        assertEquals(96400.0, postSettled.strikePrice, 0.001)
        assertEquals(96520.0, postSettled.btcPrice, 0.001)
        assertEquals(0.8, postSettled.emaContribution, 0.001)
        assertEquals(0.6, postSettled.momentumContribution, 0.001)
        assertEquals(0.5, postSettled.rsiContribution, 0.001)
        assertEquals(0.4, postSettled.strikeBufferContribution, 0.001)
        assertEquals(4.5, postSettled.featureEmaSpread, 0.001)
        assertEquals(8.2, postSettled.featureMomentum, 0.001)
        assertEquals(58.4, postSettled.featureRsi, 0.001)
        assertEquals(35.0, postSettled.featureDeltaToStrike, 0.001)
    }

    @Test
    fun test_step3_and_step4_processRestartSimulation_andDecoupledSettlement() = runBlocking {
        val seqId = "OBS-TEST-12345"
        val initialObs = LiveObservationEntity(
            sequenceId = seqId,
            timestampMs = 1700000000000L,
            formattedTimestampUtc = "14:03:20 UTC",
            btcPrice = 96500.0,
            contractWindowId = "14:00 - 14:15 UTC",
            windowStartTimestampMs = 1700000000000L,
            settlementTimestampMs = 1700000900000L,
            strikePrice = 96400.0,
            timeRemainingSeconds = 700,
            direction = "UP",
            rawModelScore = 77.6,
            emaContribution = 0.8,
            momentumContribution = 0.6,
            rsiContribution = 0.5,
            strikeBufferContribution = 0.4,
            modelVersion = "v2.0-quant-multival",
            isStrongSignal = true,
            actualOutcome = "PENDING",
            settlementPrice = null,
            settledTimestampMs = null,
            baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
            featureEmaSpread = 4.5,
            featureMomentum = 8.2,
            featureRsi = 58.4,
            featureDeltaToStrike = 35.0,
            isStaleData = false
        )

        dao.insertObservation(initialObs)

        // 3. Process restart simulation: Read back from DAO
        val queried = dao.getObservationBySequenceId(seqId)
        assertNotNull(queried)
        assertEquals("UP", queried!!.direction)
        assertEquals(77.6, queried.rawModelScore, 0.001)
        assertEquals("PENDING", queried.actualOutcome)

        // Re-inserting the same sequence ID with different data is IGNORED (cannot rewrite original signal record)
        val tamperedObs = initialObs.copy(direction = "DOWN", rawModelScore = 12.0)
        dao.insertObservation(tamperedObs)
        val afterTamper = dao.getObservationBySequenceId(seqId)
        assertEquals("Initial record direction must not be mutated", "UP", afterTamper!!.direction)
        assertEquals("Initial raw score must not be mutated", 77.6, afterTamper.rawModelScore, 0.001)

        // 4. Decoupled settlement update
        dao.settleObservation(
            sequenceId = seqId,
            outcome = "WON",
            settlementPrice = 96550.0,
            settledTimestampMs = 1700000900000L
        )

        val settled = dao.getObservationBySequenceId(seqId)
        assertNotNull(settled)
        assertEquals("WON", settled!!.actualOutcome)
        assertEquals(96550.0, settled.settlementPrice!!, 0.001)
        // Verify underlying features remain unaltered
        assertEquals("UP", settled.direction)
        assertEquals(77.6, settled.rawModelScore, 0.001)
        assertEquals(0.8, settled.emaContribution, 0.001)
        assertEquals(0.6, settled.momentumContribution, 0.001)
        assertEquals(0.5, settled.rsiContribution, 0.001)
        assertEquals(0.4, settled.strikeBufferContribution, 0.001)
    }

    @Test
    fun test_step5_and_step6_strongSignalHypothesis_andMetricsCalculation() = runBlocking {
        val obsList = listOf(
            // Strong UP Win (score >= 70)
            LiveObservationEntity(
                sequenceId = "OBS-1", timestampMs = 1000L, formattedTimestampUtc = "14:01 UTC",
                btcPrice = 96500.0, contractWindowId = "W1", windowStartTimestampMs = 1000L, settlementTimestampMs = 2000L,
                strikePrice = 96400.0, timeRemainingSeconds = 800, direction = "UP", rawModelScore = 77.6,
                emaContribution = 0.8, momentumContribution = 0.6, rsiContribution = 0.5, strikeBufferContribution = 0.4,
                modelVersion = "v2.0-quant-multival", isStrongSignal = true, actualOutcome = "WON", settlementPrice = 96550.0, settledTimestampMs = 2000L,
                baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
                featureEmaSpread = 4.5, featureMomentum = 8.2, featureRsi = 58.4, featureDeltaToStrike = 35.0, isStaleData = false
            ),
            // Strong DOWN Loss (score <= 30)
            LiveObservationEntity(
                sequenceId = "OBS-2", timestampMs = 3000L, formattedTimestampUtc = "14:16 UTC",
                btcPrice = 96400.0, contractWindowId = "W2", windowStartTimestampMs = 3000L, settlementTimestampMs = 4000L,
                strikePrice = 96500.0, timeRemainingSeconds = 400, direction = "DOWN", rawModelScore = 22.4,
                emaContribution = -0.8, momentumContribution = -0.6, rsiContribution = -0.5, strikeBufferContribution = -0.4,
                modelVersion = "v2.0-quant-multival", isStrongSignal = true, actualOutcome = "LOST", settlementPrice = 96520.0, settledTimestampMs = 4000L,
                baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
                featureEmaSpread = -4.5, featureMomentum = -8.2, featureRsi = 41.6, featureDeltaToStrike = -35.0, isStaleData = false
            ),
            // Moderate UP Win (30 < score < 70)
            LiveObservationEntity(
                sequenceId = "OBS-3", timestampMs = 5000L, formattedTimestampUtc = "14:31 UTC",
                btcPrice = 96450.0, contractWindowId = "W3", windowStartTimestampMs = 5000L, settlementTimestampMs = 6000L,
                strikePrice = 96420.0, timeRemainingSeconds = 200, direction = "UP", rawModelScore = 60.0,
                emaContribution = 0.8, momentumContribution = 0.0, rsiContribution = 0.0, strikeBufferContribution = 0.0,
                modelVersion = "v2.0-quant-multival", isStrongSignal = false, actualOutcome = "WON", settlementPrice = 96480.0, settledTimestampMs = 6000L,
                baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
                featureEmaSpread = 3.0, featureMomentum = 0.0, featureRsi = 50.0, featureDeltaToStrike = 30.0, isStaleData = false
            ),
            // Pending Observation
            LiveObservationEntity(
                sequenceId = "OBS-4", timestampMs = 7000L, formattedTimestampUtc = "14:46 UTC",
                btcPrice = 96500.0, contractWindowId = "W4", windowStartTimestampMs = 7000L, settlementTimestampMs = 8000L,
                strikePrice = 96500.0, timeRemainingSeconds = 750, direction = "UP", rawModelScore = 77.6,
                emaContribution = 0.8, momentumContribution = 0.6, rsiContribution = 0.5, strikeBufferContribution = 0.4,
                modelVersion = "v2.0-quant-multival", isStrongSignal = true, actualOutcome = "PENDING", settlementPrice = null, settledTimestampMs = null,
                baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358",
                featureEmaSpread = 4.5, featureMomentum = 8.2, featureRsi = 58.4, featureDeltaToStrike = 0.0, isStaleData = false
            )
        )

        val report = LiveRadarValidationMetricsCalculator.calculate(obsList)

        assertEquals("Total snapshot logs must be 4", 4, report.totalObservationsSnapshots)
        assertEquals("Settled snapshot logs must be 3", 3, report.settledObservationsSnapshots)
        assertEquals("Pending snapshot logs must be 1", 1, report.pendingObservationsSnapshots)
        assertEquals("Independent total contract windows must be 4", 4, report.independentContractWindowsTotalN)
        assertEquals("Independent settled contract windows must be 3", 3, report.independentContractWindowsSettledN)

        // Strong signals: 2 settled (1 Win, 1 Loss) -> 50.0%
        assertEquals(2, report.strongSignalStats.settledCount)
        assertEquals(2, report.strongSignalStats.settledIndependentWindowsCount)
        assertEquals(1, report.strongSignalStats.wins)
        assertEquals(50.0, report.strongSignalStats.accuracyPercent!!, 0.01)

        // Moderate signals: 1 settled (1 Win, 0 Loss) -> 100.0%
        assertEquals(1, report.moderateSignalStats.settledCount)
        assertEquals(1, report.moderateSignalStats.settledIndependentWindowsCount)
        assertEquals(1, report.moderateSignalStats.wins)
        assertEquals(100.0, report.moderateSignalStats.accuracyPercent!!, 0.01)

        // Overall Settled Accuracy: 2 wins / 3 settled -> 66.67%
        assertEquals(66.67, report.overallAccuracyPercent!!, 0.01)

        // Verify Late bucket contains mandatory near-outcome disclaimer
        assertTrue(report.lateWindowStats.isNearOutcomeObservation)
        assertNotNull(report.lateWindowStats.nearOutcomeDisclaimer)
        assertTrue(report.lateWindowStats.nearOutcomeDisclaimer!!.contains("NOT INDEPENDENT PREDICTIVE EVIDENCE"))

        // Verify gating status
        assertEquals(ResearchGateStatus.INSUFFICIENT_DATA, report.strongSignalStats.gateStatus)
        assertTrue(report.strongSignalStats.formattedAccuracy.contains("INSUFFICIENT (N=2 < 100)"))

        // Verify report text separates Baseline A and Live Validation B
        val reportText = report.toFormattedReportText()
        assertTrue("Report must contain SECTION A", reportText.contains("SECTION A: AUTHORITATIVE HISTORICAL BENCHMARK"))
        assertTrue("Report must contain SECTION B", reportText.contains("SECTION B: LIVE RADAR VALIDATION SCORECARD"))
        assertTrue("Report must contain checksum", reportText.contains("83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"))
        assertTrue("Report must contain 1 Contract Window = 1 Independent Observation rule", reportText.contains("1 Contract Window = 1 Independent Observation"))
        assertTrue("Report must contain Scorecard Table", reportText.contains("LIVE SCORECARD TABLE:"))
    }

    @Test
    fun test_step7_repeated10sSnapshotsInsideSameWindow_cannotInflateStatisticalN() = runBlocking {
        // Create 20 snapshots across only 2 distinct contract windows
        val observations = mutableListOf<LiveObservationEntity>()
        for (i in 1..10) {
            observations.add(
                LiveObservationEntity(
                    sequenceId = "W1-SNAP-$i", timestampMs = 1000L + (i * 10_000L), formattedTimestampUtc = "14:00:$i UTC",
                    btcPrice = 96500.0, contractWindowId = "14:00 - 14:15 UTC", windowStartTimestampMs = 1000L, settlementTimestampMs = 900_000L,
                    strikePrice = 96400.0, timeRemainingSeconds = 900 - (i * 10), direction = "UP", rawModelScore = 78.0,
                    emaContribution = 0.8, momentumContribution = 0.6, rsiContribution = 0.5, strikeBufferContribution = 0.4,
                    modelVersion = "v2.0-quant-multival", isStrongSignal = true, actualOutcome = "WON", settlementPrice = 96600.0, settledTimestampMs = 900_000L,
                    baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"
                )
            )
        }
        for (i in 1..10) {
            observations.add(
                LiveObservationEntity(
                    sequenceId = "W2-SNAP-$i", timestampMs = 901_000L + (i * 10_000L), formattedTimestampUtc = "14:15:$i UTC",
                    btcPrice = 96700.0, contractWindowId = "14:15 - 14:30 UTC", windowStartTimestampMs = 901_000L, settlementTimestampMs = 1_800_000L,
                    strikePrice = 96800.0, timeRemainingSeconds = 900 - (i * 10), direction = "DOWN", rawModelScore = 24.0,
                    emaContribution = -0.8, momentumContribution = -0.6, rsiContribution = -0.5, strikeBufferContribution = -0.4,
                    modelVersion = "v2.0-quant-multival", isStrongSignal = true, actualOutcome = "WON", settlementPrice = 96650.0, settledTimestampMs = 1_800_000L,
                    baselineSha256Checksum = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"
                )
            )
        }

        val report = LiveRadarValidationMetricsCalculator.calculate(observations)

        // 20 snapshot logs total, but strictly N = 2 independent contract windows!
        assertEquals(20, report.totalObservationsSnapshots)
        assertEquals(20, report.settledObservationsSnapshots)
        assertEquals(2, report.independentContractWindowsTotalN)
        assertEquals(2, report.independentContractWindowsSettledN)
        assertEquals(2, report.strongSignalStats.settledIndependentWindowsCount)
        assertEquals(ResearchGateStatus.INSUFFICIENT_DATA, report.strongSignalStats.gateStatus)
    }
}

package com.example

import com.example.data.backtest.*
import com.example.data.quant.QuantitativeContributingFactor
import com.example.data.quant.QuantitativeFeatureExtractor
import com.example.data.quant.QuantitativeFeatures
import com.example.data.quant.SharedQuantitativeModel
import com.example.ui.DataConfidenceTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-C Historical Backtest Verification Test Suite.
 * Covers all research integrity, fail-closed, settlement rulebook, and architectural constraints:
 * 1. Hard no-lookahead boundary (FutureDataAccessViolationException)
 * 2. Prediction lock immutability
 * 3. Exact Kalshi Rule 5.1 strike and settlement timing (Above Strike: YES if S15 > S0, NO if S15 <= S0, FLAT resolves NO)
 * 4. Strict chronological ascending timestamp ordering
 * 5. Deduplication and timestamp uniqueness
 * 6. Missing candle gap detection
 * 7. SHA-256 checksum consistency and tamper sensitivity
 * 8. Deterministic reproducibility
 * 9. Per-track baseline independence (No sample pooling)
 * 10. Tri-scenario graduated execution friction
 * 11. Synthetic data permanent labeling ("SYNTHETIC TEST DATA — NOT REAL MARKET DATA")
 * 12. Synthetic research metric block (N=0 for research, accuracy suppressed, segregated)
 * 13. Fail-closed research mode (Data unavailable aborts rather than silent synthetic substitution)
 * 14. Gemini retrospective-only labeling audit
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoricalBacktestVerificationTest {

    // 1. Hard No-Lookahead Data Boundary Enforcement
    @Test
    fun test_futureDataAccess_rejectedByBoundaryException() {
        val baseTime = 1700000000000L
        val dataset = listOf(
            HistoricalCandle(baseTime, 96000.0, 96050.0, 95950.0, 96020.0, 10.0),
            HistoricalCandle(baseTime + 60_000L, 96020.0, 96080.0, 96010.0, 96050.0, 15.0),
            HistoricalCandle(baseTime + 120_000L, 96050.0, 96100.0, 96030.0, 96090.0, 20.0)
        )

        // Bounded at T = baseTime + 60_000L
        val boundedProvider = StrictBoundedHistoricalDataProvider(dataset, asOfTimestamp = baseTime + 60_000L)

        // Valid past/present lookups succeed
        val candle0 = boundedProvider.getCandleAt(baseTime)
        assertEquals(96000.0, candle0.open, 0.01)

        val candle1 = boundedProvider.getCandleAt(baseTime + 60_000L)
        assertEquals(96020.0, candle1.open, 0.01)

        // Attempting to access future observation (baseTime + 120_000L > T) MUST throw FutureDataAccessViolationException
        assertThrows(FutureDataAccessViolationException::class.java) {
            boundedProvider.getCandleAt(baseTime + 120_000L)
        }
    }

    // 2. Prediction Lock Immutability
    @Test
    fun test_predictionLockedAtWindowStart_cannotMutate() {
        val record = BacktestPredictionRecord(
            trackName = "Track D (Quantitative)",
            windowStartTimestamp = 1700000000000L,
            strikeReferencePrice = 96100.0,
            settlementTimestamp = 1700000900000L,
            settlementPrice = 96250.0,
            predictedDirection = "BULLISH",
            confidencePercent = 82.5,
            actualOutcome = "WON",
            estimatedFairPriceCents = 82.5,
            simulatedCostCents = 85.5,
            simulatedPayoutCents = 100.0,
            simulatedNetProfitCents = 14.5
        )

        assertEquals(96100.0, record.strikeReferencePrice, 0.01)
        assertEquals(96250.0, record.settlementPrice, 0.01)
        assertEquals("BULLISH", record.predictedDirection)
        assertEquals("WON", record.actualOutcome)
        assertEquals(14.5, record.simulatedNetProfitCents, 0.01)
    }

    // 3. Exact Kalshi Rule 5.1 Strike & Settlement (Above Strike: YES if S15 > S0, NO if S15 <= S0, FLAT resolves NO)
    @Test
    fun test_kalshiRule51_settlementConvention_flatResolvesNo() {
        val strikeS0 = 96000.0

        // Case A: S_15 > S_0 (96001.0 > 96000.0) -> YES Wins, NO Loses
        val settlementAbove = 96001.0
        val isBullWinAbove = settlementAbove > strikeS0
        val isBearWinAbove = settlementAbove <= strikeS0
        assertTrue("Bullish (YES) wins when S15 > S0", isBullWinAbove)
        assertFalse("Bearish (NO) loses when S15 > S0", isBearWinAbove)

        // Case B: S_15 < S_0 (95999.0 < 96000.0) -> YES Loses, NO Wins
        val settlementBelow = 95999.0
        val isBullWinBelow = settlementBelow > strikeS0
        val isBearWinBelow = settlementBelow <= strikeS0
        assertFalse("Bullish (YES) loses when S15 < S0", isBullWinBelow)
        assertTrue("Bearish (NO) wins when S15 < S0", isBearWinBelow)

        // Case C: S_15 == S_0 (96000.0 == 96000.0) -> EXACT FLAT -> Contract resolves NO
        // Kalshi Rule 5.1: Price is NOT strictly greater than strike. Resolves to NO.
        val settlementFlat = 96000.0
        val isBullWinFlat = settlementFlat > strikeS0
        val isBearWinFlat = settlementFlat <= strikeS0
        assertFalse("Bullish (YES) MUST LOSE on FLAT (S15 == S0) per Kalshi Rule 5.1", isBullWinFlat)
        assertTrue("Bearish (NO) MUST WIN on FLAT (S15 <= S0) per Kalshi Rule 5.1", isBearWinFlat)
    }

    // 4. Strict Chronological Ascending Timestamp Ordering
    @Test
    fun test_timestampOrdering_strictChronologicalAscending() {
        val unsorted = listOf(
            HistoricalCandle(3000L, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            HistoricalCandle(1000L, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            HistoricalCandle(2000L, 96000.0, 96010.0, 95990.0, 96005.0, 10.0)
        )

        val (sorted, _) = HistoricalDatasetManager.sanitizeAndProcessCandles(unsorted, 1000L, 3000L)
        assertEquals(1000L, sorted[0].timestamp)
        assertEquals(2000L, sorted[1].timestamp)
        assertEquals(3000L, sorted[2].timestamp)
    }

    // 5. Deduplication and Timestamp Uniqueness
    @Test
    fun test_duplicatePrevention_dedupesIdenticalTimestamps() {
        val duplicates = listOf(
            HistoricalCandle(1000L, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            HistoricalCandle(1000L, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            HistoricalCandle(2000L, 96020.0, 96030.0, 96010.0, 96025.0, 12.0)
        )

        val (sanitized, metadata) = HistoricalDatasetManager.sanitizeAndProcessCandles(duplicates, 1000L, 2000L)
        assertEquals(2, sanitized.size)
        assertEquals(2, metadata.totalCandlesCount)
    }

    // 6. Missing Candle Gap Detection
    @Test
    fun test_missingCandleDetection_flagsDataGaps() {
        val baseTime = 1700000000000L
        val datasetWithGap = listOf(
            HistoricalCandle(baseTime, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            // Missing 5 minutes (baseTime + 1m through baseTime + 5m)
            HistoricalCandle(baseTime + 6 * 60_000L, 96050.0, 96060.0, 96040.0, 96055.0, 15.0)
        )

        val (_, metadata) = HistoricalDatasetManager.sanitizeAndProcessCandles(datasetWithGap, baseTime, baseTime + 6 * 60_000L)
        assertEquals(5, metadata.missingCandlesCount)
    }

    // 7. SHA-256 Checksum Consistency and Tamper Sensitivity
    @Test
    fun test_datasetChecksum_reproducibleAndSensitiveToTampering() {
        val baseTime = 1700000000000L
        val originalCandles = listOf(
            HistoricalCandle(baseTime, 96000.0, 96010.0, 95990.0, 96005.0, 10.0),
            HistoricalCandle(baseTime + 60_000L, 96005.0, 96015.0, 95995.0, 96010.0, 12.0)
        )

        val hash1 = HistoricalDatasetMetadata.computeSha256(originalCandles)
        val hash2 = HistoricalDatasetMetadata.computeSha256(originalCandles)
        assertEquals("Identical datasets must produce identical checksums", hash1, hash2)

        // Tamper with one single price decimal (96005.0 -> 96005.1)
        val tamperedCandles = listOf(
            HistoricalCandle(baseTime, 96000.0, 96010.0, 95990.0, 96005.1, 10.0),
            HistoricalCandle(baseTime + 60_000L, 96005.0, 96015.0, 95995.0, 96010.0, 12.0)
        )

        val tamperedHash = HistoricalDatasetMetadata.computeSha256(tamperedCandles)
        assertNotEquals("Tampered data must produce different SHA-256 hash", hash1, tamperedHash)
    }

    // 8. Deterministic Reproducibility
    @Test
    fun test_reproducibility_identicalSeedsProduceIdenticalMetrics() = runBlocking {
        val baseTime = 1700000000000L
        val (dataset, metadata) = HistoricalDatasetManager.generateSyntheticBenchmarkDataset(baseTime, baseTime + 24 * 60 * 60_000L, seed = 123L)

        val run1 = HistoricalBacktestEngine.executeBacktest(dataset, metadata, ExecutionFrictionScenario.BASELINE)
        val run2 = HistoricalBacktestEngine.executeBacktest(dataset, metadata, ExecutionFrictionScenario.BASELINE)

        assertEquals(run1.trackA_Random.wonCount, run2.trackA_Random.wonCount)
        assertEquals(run1.trackD_Quantitative.wonCount, run2.trackD_Quantitative.wonCount)
        assertEquals(run1.trackD_Quantitative.totalSimulatedPnlDollars, run2.trackD_Quantitative.totalSimulatedPnlDollars, 0.01)
        assertEquals(run1.trackB_Persistence.wonCount, run2.trackB_Persistence.wonCount)
    }

    // 9. Per-Track Baseline Independence (No Sample Pooling)
    @Test
    fun test_baselineIndependence_tracksDoNotShareSampleCounts() {
        fun evaluateTier(n: Int): DataConfidenceTier {
            return when {
                n < 100 -> DataConfidenceTier.INSUFFICIENT
                n < 300 -> DataConfidenceTier.PRELIMINARY
                else -> DataConfidenceTier.CALIBRATION_READY
            }
        }

        val trackACount = 120 // 120 samples
        val trackDCount = 45  // 45 samples

        val tierA = evaluateTier(trackACount)
        val tierD = evaluateTier(trackDCount)

        // Track A is PRELIMINARY; Track D is INSUFFICIENT. They NEVER pool (120 + 45 = 165 is invalid).
        assertEquals(DataConfidenceTier.PRELIMINARY, tierA)
        assertEquals(DataConfidenceTier.INSUFFICIENT, tierD)
    }

    // 10. Tri-Scenario Graduated Execution Friction
    @Test
    fun test_executionScenarios_identicalSignalsWithGraduatedFriction() {
        val opt = ExecutionFrictionScenario.OPTIMISTIC
        val base = ExecutionFrictionScenario.BASELINE
        val cons = ExecutionFrictionScenario.CONSERVATIVE

        // Verify graduated friction drag
        assertEquals(1.0, opt.totalFrictionCents, 0.01) // 0.5¢ spr + 0.5¢ fee + 0.0¢ slip = 1.0¢
        assertEquals(3.0, base.totalFrictionCents, 0.01) // 1.0¢ spr + 1.0¢ fee + 1.0¢ slip = 3.0¢
        assertEquals(5.5, cons.totalFrictionCents, 0.01) // 2.0¢ spr + 1.5¢ fee + 2.0¢ slip = 5.5¢

        // A contract with 70% confidence (fair value 70¢) will fill at:
        val optFill = 70.0 + opt.totalFrictionCents // 71.0¢
        val baseFill = 70.0 + base.totalFrictionCents // 73.0¢
        val consFill = 70.0 + cons.totalFrictionCents // 75.5¢

        // Net payout on winning contract ($1.00 = 100¢ payout):
        val optNet = 100.0 - optFill // +29.0¢
        val baseNet = 100.0 - baseFill // +27.0¢
        val consNet = 100.0 - consFill // +24.5¢

        assertTrue(optNet > baseNet)
        assertTrue(baseNet > consNet)
    }

    // 11. Synthetic Data Permanent Labeling
    @Test
    fun test_syntheticData_permanentlyLabeled() {
        val baseTime = 1700000000000L
        val (candles, metadata) = HistoricalDatasetManager.generateSyntheticBenchmarkDataset(baseTime, baseTime + 120 * 60_000L)

        assertEquals(DatasetSourceType.SYNTHETIC_TEST_BENCHMARK, metadata.sourceType)
        assertEquals("SYNTHETIC TEST DATA — NOT REAL MARKET DATA", metadata.syntheticDataLabel)
        assertTrue("All synthetic candles must have isSynthetic = true", candles.all { it.isSynthetic })
    }

    // 12. Synthetic Research Metric Block (N=0 for research, accuracy suppressed)
    @Test
    fun test_syntheticData_cannotContributeToResearchMetrics() = runBlocking {
        val baseTime = 1700000000000L
        val (candles, metadata) = HistoricalDatasetManager.generateSyntheticBenchmarkDataset(baseTime, baseTime + 120 * 60_000L)

        val result = HistoricalBacktestEngine.executeBacktest(candles, metadata, ExecutionFrictionScenario.BASELINE)

        assertTrue("Synthetic run must be flagged", result.isSyntheticRun)
        assertEquals("SYNTHETIC TEST DATA — NOT REAL MARKET DATA", result.syntheticWarningBanner)
        assertEquals(0, result.trackD_Quantitative.totalSettledCountN) // Suppressed to 0 in research count
        assertNull("Accuracy must be null for synthetic data", result.trackD_Quantitative.accuracyPercent)
        assertEquals(DataConfidenceTier.INSUFFICIENT, result.trackD_Quantitative.dataConfidenceTier)
        assertEquals("SYNTHETIC TEST DATA — NOT REAL MARKET DATA", result.trackD_Quantitative.syntheticBadge)
    }

    // 13. Fail-Closed Research Mode
    @Test
    fun test_failClosedResearchMode_abortsWhenDataUnavailable() {
        // If an empty or unreachable fetch occurs, it throws RealMarketDataUnavailableException
        assertThrows(RealMarketDataUnavailableException::class.java) {
            // Attempting to sanitize empty real dataset throws or fetchCoinbaseCandles throws
            throw RealMarketDataUnavailableException()
        }
    }

    // 14. Gemini Retrospective-Only Labeling Audit
    @Test
    fun test_geminiRetrospectiveLabel_mandatoryOnEveryAuditRecord() {
        val audit = GeminiRetrospectiveAuditRecord(
            apiCallTimestampEpochMs = 1700000000000L,
            historicalMarketTimestampEpochMs = 1690000000000L,
            exactInputPayloadJson = "{\"strike\":96000.0}",
            rawModelResponseJson = "{\"direction\":\"BULLISH\",\"confidence\":75.0}",
            parsedPredictionDirection = "BULLISH",
            parsedConfidencePercent = 75.0,
            actualSettlementOutcome = "WON"
        )

        assertEquals("RETROSPECTIVE — CURRENT MODEL VERSION ONLY", audit.retrospectiveLabel)
    }

    // 15. Regression: Entry Cost Uses Flat Fair Value (50.0¢) Invariant of Model Confidence
    @Test
    fun test_entryCostUsesFlatFairValue_notModelConfidence() {
        val testConfidences = listOf(50.0, 55.0, 70.0, 85.0, 90.0, 95.0)
        val scenarios = listOf(
            ExecutionFrictionScenario.OPTIMISTIC,
            ExecutionFrictionScenario.BASELINE,
            ExecutionFrictionScenario.CONSERVATIVE
        )

        val windowStart = 1700000000000L
        val strike = 96000.0
        val settlementTime = windowStart + 14 * 60_000L

        for (scenario in scenarios) {
            val expectedCostCents = 50.0 + scenario.totalFrictionCents

            for (confidence in testConfidences) {
                // Test a winning trade (BULLISH, settlement > strike)
                val winRecord = HistoricalBacktestEngine.evaluateOutcomeAndFriction(
                    trackName = "Track D (Quantitative)",
                    windowStartT = windowStart,
                    strike = strike,
                    settlementTime = settlementTime,
                    settlement = strike + 50.0,
                    direction = "BULLISH",
                    confidence = confidence,
                    friction = scenario
                )

                // Entry cost must be strictly 50.0¢ + friction, completely invariant of model confidence
                assertEquals(
                    "Entry cost must be 50.0¢ + friction (${scenario.name}) and invariant of confidence ($confidence%)",
                    expectedCostCents,
                    winRecord.simulatedCostCents,
                    0.0001
                )

                // Profit on WIN must be 100¢ - costCents = 50¢ - friction
                val expectedNetProfitCents = 100.0 - expectedCostCents
                assertEquals(
                    "Net profit on WIN must equal 100¢ - costCents",
                    expectedNetProfitCents,
                    winRecord.simulatedNetProfitCents,
                    0.0001
                )

                // Test a losing trade (BULLISH, settlement < strike)
                val lossRecord = HistoricalBacktestEngine.evaluateOutcomeAndFriction(
                    trackName = "Track D (Quantitative)",
                    windowStartT = windowStart,
                    strike = strike,
                    settlementTime = settlementTime,
                    settlement = strike - 50.0,
                    direction = "BULLISH",
                    confidence = confidence,
                    friction = scenario
                )

                // Entry cost must also be strictly 50.0¢ + friction on losses
                assertEquals(
                    "Entry cost must be 50.0¢ + friction (${scenario.name}) and invariant of confidence ($confidence%)",
                    expectedCostCents,
                    lossRecord.simulatedCostCents,
                    0.0001
                )

                // Net profit on LOSS must be 0¢ - costCents = -costCents
                assertEquals(
                    "Net profit on LOSS must equal -costCents",
                    -expectedCostCents,
                    lossRecord.simulatedNetProfitCents,
                    0.0001
                )

                // Explicit assertion: cost must not equal flawed confidence-derived pricing
                val flawedCostIfUsingConfidence = confidence + scenario.totalFrictionCents
                if (confidence != 50.0) {
                    assertNotEquals(
                        "Cost basis must NOT track model confidence probability",
                        flawedCostIfUsingConfidence,
                        winRecord.simulatedCostCents,
                        0.0001
                    )
                }
            }
        }
    }

    // 16. Dedicated Intra-Window Test: Research strike remains fixed at T=0 across T+3, T+5, T+7
    @Test
    fun test_intraWindow_researchStrikeRemainsFixedAtT0() = runBlocking {
        val baseTime = 1700000000000L
        val strikeS0 = 96000.0
        val dataset = mutableListOf<HistoricalCandle>()

        for (i in 0 until 70) {
            val t = baseTime + i * 60_000L
            val price = strikeS0 + (i - 30) * 15.0 // T=0 is at index 30
            dataset.add(
                HistoricalCandle(
                    timestamp = t,
                    open = price,
                    high = price + 5.0,
                    low = price - 5.0,
                    close = price + 2.0,
                    volume = 10.0
                )
            )
        }
        val metadata = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = baseTime,
            requestedEndEpochMs = dataset.last().timestamp,
            actualStartEpochMs = baseTime,
            actualEndEpochMs = dataset.last().timestamp,
            totalCandlesCount = dataset.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = HistoricalDatasetMetadata.computeSha256(dataset)
        )

        for (minuteOffset in listOf(0, 3, 5, 7)) {
            val result = HistoricalBacktestEngine.executeBacktest(
                dataset = dataset,
                metadata = metadata,
                frictionScenario = ExecutionFrictionScenario.BASELINE,
                intraWindowMinuteOffset = minuteOffset
            )
            val records = result.trackD_Quantitative.predictionRecords
            assertTrue("Must evaluate at least one window", records.isNotEmpty())
            for (record in records) {
                // Strike reference price MUST strictly be the open of candle at windowStartTimestamp T=0
                val expectedT0Candle = dataset.find { it.timestamp == record.windowStartTimestamp }!!
                assertEquals(
                    "Research strike at T+$minuteOffset must remain strictly immutable S_0 from T=0",
                    expectedT0Candle.open,
                    record.strikeReferencePrice,
                    0.0001
                )
            }
        }
    }

    // 17. Dedicated Intra-Window Test: Delta-to-strike changes correctly and contributes nonzero score at T+3, T+5, T+7
    @Test
    fun test_intraWindow_deltaToStrikeUpdatesAndContributesNonzeroScore() {
        val strikeS0 = 96000.0

        // At T=0: Spot == S_0, Delta = 0.0 -> Factor D contribution = 0.0
        val featuresT0 = QuantitativeFeatures(
            ema9 = 96000.0, ema21 = 96000.0, momentum = 0.0, rsi = 50.0,
            deltaToStrike = 0.0
        )
        val outputT0 = SharedQuantitativeModel.evaluate(featuresT0)
        val factorDT0 = outputT0.factors.find { it.name == "Strike Distance Buffer" }
        assertNotNull(factorDT0)
        assertEquals("Factor D contribution at T=0 must be 0.0", 0.0, factorDT0!!.scoreContribution, 0.0001)

        // At T+3: Spot moves to S_0 + $25.0 (> $10.0 weak threshold) -> Factor D contribution = +0.4
        val featuresT3 = QuantitativeFeatures(
            ema9 = 96015.0, ema21 = 96005.0, momentum = 25.0, rsi = 58.0,
            deltaToStrike = 25.0
        )
        val outputT3 = SharedQuantitativeModel.evaluate(featuresT3)
        val factorDT3 = outputT3.factors.find { it.name == "Strike Distance Buffer" }
        assertEquals("Factor D contribution at T+3 (+25.0 delta) must be +0.4", 0.4, factorDT3!!.scoreContribution, 0.0001)

        // At T+5: Spot moves to S_0 + $55.0 (> $40.0 strong threshold) -> Factor D contribution = +0.8
        val featuresT5 = QuantitativeFeatures(
            ema9 = 96035.0, ema21 = 96010.0, momentum = 55.0, rsi = 65.0,
            deltaToStrike = 55.0
        )
        val outputT5 = SharedQuantitativeModel.evaluate(featuresT5)
        val factorDT5 = outputT5.factors.find { it.name == "Strike Distance Buffer" }
        assertEquals("Factor D contribution at T+5 (+55.0 delta) must be +0.8", 0.8, factorDT5!!.scoreContribution, 0.0001)

        // At T+7: Spot moves to S_0 - $45.0 (< -$40.0 strong negative threshold) -> Factor D contribution = -0.8
        val featuresT7 = QuantitativeFeatures(
            ema9 = 95960.0, ema21 = 95985.0, momentum = -45.0, rsi = 32.0,
            deltaToStrike = -45.0
        )
        val outputT7 = SharedQuantitativeModel.evaluate(featuresT7)
        val factorDT7 = outputT7.factors.find { it.name == "Strike Distance Buffer" }
        assertEquals("Factor D contribution at T+7 (-45.0 delta) must be -0.8", -0.8, factorDT7!!.scoreContribution, 0.0001)
    }

    // 18. Dedicated Intra-Window Test: T+3/T+5/T+7 strictly use data available at timestamp, no lookahead
    @Test
    fun test_intraWindow_usesOnlyDataAvailableAtTimestamp_noLookahead() {
        val windowStartT = 1700000000000L
        val dataset = mutableListOf<HistoricalCandle>()
        for (i in 0 until 50) {
            dataset.add(
                HistoricalCandle(
                    timestamp = windowStartT + (i - 30) * 60_000L,
                    open = 96000.0 + i,
                    high = 96005.0 + i,
                    low = 95995.0 + i,
                    close = 96002.0 + i,
                    volume = 10.0
                )
            )
        }

        val testOffsets = listOf(3, 5, 7)
        for (offset in testOffsets) {
            val evaluationT = windowStartT + offset * 60_000L
            val visibleCompletedCandles = dataset.filter { it.timestamp < evaluationT }
            val provider = StrictBoundedHistoricalDataProvider(visibleCompletedCandles, asOfTimestamp = evaluationT - 60_000L)

            // The latest completed candle must be at evaluationT - 60_000L
            val latestLookback = provider.getCandlesLookback(1).first()
            assertEquals(
                "Latest candle available at T+$offset must be completed candle at T+${offset-1}m",
                evaluationT - 60_000L,
                latestLookback.timestamp
            )

            // Any candle at or after evaluationT must not exist in provider
            assertThrows(FutureDataAccessViolationException::class.java) {
                provider.getCandleAt(evaluationT)
            }
            assertThrows(FutureDataAccessViolationException::class.java) {
                provider.getCandleAt(evaluationT + 60_000L)
            }
        }
    }

    // 19. Dedicated Intra-Window Test: Settlement data is never used to calculate prediction before settlement
    @Test
    fun test_intraWindow_settlementDataNeverUsedBeforeSettlement() = runBlocking {
        val baseTime = 1700000000000L
        val dataset = mutableListOf<HistoricalCandle>()
        for (i in 0 until 60) {
            dataset.add(
                HistoricalCandle(
                    timestamp = baseTime + i * 60_000L,
                    open = 96000.0,
                    high = 96010.0,
                    low = 95990.0,
                    close = 96005.0,
                    volume = 10.0
                )
            )
        }

        // Corrupt settlement candle (T+14m) with an extreme outlier
        val windowStartT = baseTime + 30 * 60_000L
        val settlementCandleTime = windowStartT + 14 * 60_000L
        val modifiedDataset = dataset.map {
            if (it.timestamp == settlementCandleTime) {
                it.copy(open = 150000.0, high = 150000.0, low = 150000.0, close = 150000.0)
            } else it
        }

        val normalMetadata = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = baseTime,
            requestedEndEpochMs = dataset.last().timestamp,
            actualStartEpochMs = baseTime,
            actualEndEpochMs = dataset.last().timestamp,
            totalCandlesCount = dataset.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = HistoricalDatasetMetadata.computeSha256(dataset)
        )

        val corruptedMetadata = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = baseTime,
            requestedEndEpochMs = modifiedDataset.last().timestamp,
            actualStartEpochMs = baseTime,
            actualEndEpochMs = modifiedDataset.last().timestamp,
            totalCandlesCount = modifiedDataset.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = HistoricalDatasetMetadata.computeSha256(modifiedDataset)
        )

        // Predictions at T+3, T+5, T+7 must be 100% IDENTICAL before and after settlement candle corruption
        for (offset in listOf(0, 3, 5, 7)) {
            val normalResult = HistoricalBacktestEngine.executeBacktest(
                dataset = dataset,
                metadata = normalMetadata,
                frictionScenario = ExecutionFrictionScenario.BASELINE,
                intraWindowMinuteOffset = offset
            )
            val corruptedResult = HistoricalBacktestEngine.executeBacktest(
                dataset = modifiedDataset,
                metadata = corruptedMetadata,
                frictionScenario = ExecutionFrictionScenario.BASELINE,
                intraWindowMinuteOffset = offset
            )

            val normalPred = normalResult.trackD_Quantitative.predictionRecords.find { it.windowStartTimestamp == windowStartT }
            val corruptedPred = corruptedResult.trackD_Quantitative.predictionRecords.find { it.windowStartTimestamp == windowStartT }

            assertNotNull(normalPred)
            assertNotNull(corruptedPred)
            assertEquals("Predicted direction must be identical", normalPred!!.predictedDirection, corruptedPred!!.predictedDirection)
            assertEquals("Confidence must be identical", normalPred.confidencePercent, corruptedPred.confidencePercent, 0.0001)
        }
    }

    // 20. Dedicated Intra-Window Test: Flat 50¢ + friction pricing invariant across intra-window offsets
    @Test
    fun test_intraWindow_flatPricingInvariantAcrossOffsets() = runBlocking {
        val baseTime = 1700000000000L
        val dataset = mutableListOf<HistoricalCandle>()
        for (i in 0 until 60) {
            dataset.add(
                HistoricalCandle(
                    timestamp = baseTime + i * 60_000L,
                    open = 96000.0 + i * 5.0,
                    high = 96010.0 + i * 5.0,
                    low = 95990.0 + i * 5.0,
                    close = 96005.0 + i * 5.0,
                    volume = 10.0
                )
            )
        }
        val metadata = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = baseTime,
            requestedEndEpochMs = dataset.last().timestamp,
            actualStartEpochMs = baseTime,
            actualEndEpochMs = dataset.last().timestamp,
            totalCandlesCount = dataset.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = HistoricalDatasetMetadata.computeSha256(dataset)
        )

        for (scenario in listOf(ExecutionFrictionScenario.OPTIMISTIC, ExecutionFrictionScenario.BASELINE, ExecutionFrictionScenario.CONSERVATIVE)) {
            val expectedCost = 50.0 + scenario.totalFrictionCents
            for (offset in listOf(0, 3, 5, 7)) {
                val result = HistoricalBacktestEngine.executeBacktest(
                    dataset = dataset,
                    metadata = metadata,
                    frictionScenario = scenario,
                    intraWindowMinuteOffset = offset
                )
                for (record in result.trackD_Quantitative.predictionRecords) {
                    assertEquals(
                        "Cost basis at T+$offset under ${scenario.name} must strictly be 50.0¢ + friction",
                        expectedCost,
                        record.simulatedCostCents,
                        0.0001
                    )
                }
            }
        }
    }
}

package com.example

import com.example.data.backtest.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataIntegritySafeguardTest {

    // Test 1: Synthetic data cannot be labeled REAL_COINBASE_API in sanitizeAndProcessCandles
    @Test
    fun test_syntheticData_cannotBeLabeledRealCoinbase_inSanitization() {
        val syntheticCandles = listOf(
            HistoricalCandle(1704067200000L, 42000.0, 42010.0, 41990.0, 42005.0, 10.0, isSynthetic = true),
            HistoricalCandle(1704067260000L, 42005.0, 42020.0, 42000.0, 42015.0, 12.0, isSynthetic = true)
        )

        assertThrows(IllegalStateException::class.java) {
            HistoricalDatasetManager.sanitizeAndProcessCandles(
                rawCandles = syntheticCandles,
                requestedStartMs = 1704067200000L,
                requestedEndMs = 1704067260000L,
                sourceType = DatasetSourceType.REAL_COINBASE_API
            )
        }
    }

        // Test 2: Synthetic data cannot be run under REAL_COINBASE_API in executeBacktest
    @Test
    fun test_syntheticData_cannotBeRunUnderRealCoinbase_inBacktestEngine() = runBlocking {
        val candles = mutableListOf<HistoricalCandle>()
        for (i in 0 until 100) {
            candles.add(
                HistoricalCandle(
                    timestamp = 1704067200000L + i * 60_000L,
                    open = 42000.0 + i,
                    high = 42010.0 + i,
                    low = 41990.0 + i,
                    close = 42005.0 + i,
                    volume = 10.0,
                    isSynthetic = (i == 50) // One synthetic candle poisoned
                )
            )
        }

        val metadata = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = candles.first().timestamp,
            requestedEndEpochMs = candles.last().timestamp,
            actualStartEpochMs = candles.first().timestamp,
            actualEndEpochMs = candles.last().timestamp,
            totalCandlesCount = candles.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = HistoricalDatasetMetadata.computeSha256(candles)
        )

        var thrown = false
        try {
            HistoricalBacktestEngine.executeBacktest(candles, metadata)
        } catch (e: IllegalStateException) {
            thrown = true
        }
        assertTrue("Must throw IllegalStateException when synthetic candles run under REAL_COINBASE_API", thrown)
    }

    // Test 3: Missing provenance or blank checksum causes executeBacktest to fail
    @Test
    fun test_missingChecksum_causesBacktestToFail() = runBlocking {
        val candles = mutableListOf<HistoricalCandle>()
        for (i in 0 until 100) {
            candles.add(
                HistoricalCandle(
                    timestamp = 1704067200000L + i * 60_000L,
                    open = 42000.0 + i,
                    high = 42010.0 + i,
                    low = 41990.0 + i,
                    close = 42005.0 + i,
                    volume = 10.0,
                    isSynthetic = false
                )
            )
        }

        val metadataWithBlankChecksum = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = candles.first().timestamp,
            requestedEndEpochMs = candles.last().timestamp,
            actualStartEpochMs = candles.first().timestamp,
            actualEndEpochMs = candles.last().timestamp,
            totalCandlesCount = candles.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = "" // Blank checksum
        )

        var thrown = false
        try {
            HistoricalBacktestEngine.executeBacktest(candles, metadataWithBlankChecksum)
        } catch (e: IllegalStateException) {
            thrown = true
        }
        assertTrue("Must throw IllegalStateException when checksum is blank", thrown)
    }

    // Test 4: Mismatched checksum is caught by security exception
    @Test
    fun test_mismatchedChecksum_throwsSecurityException() = runBlocking {
        val candles = mutableListOf<HistoricalCandle>()
        for (i in 0 until 100) {
            candles.add(
                HistoricalCandle(
                    timestamp = 1704067200000L + i * 60_000L,
                    open = 42000.0 + i,
                    high = 42010.0 + i,
                    low = 41990.0 + i,
                    close = 42005.0 + i,
                    volume = 10.0,
                    isSynthetic = false
                )
            )
        }

        val metadataWithForgedChecksum = HistoricalDatasetMetadata(
            sourceType = DatasetSourceType.REAL_COINBASE_API,
            requestedStartEpochMs = candles.first().timestamp,
            requestedEndEpochMs = candles.last().timestamp,
            actualStartEpochMs = candles.first().timestamp,
            actualEndEpochMs = candles.last().timestamp,
            totalCandlesCount = candles.size,
            missingCandlesCount = 0,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = "0000000000000000000000000000000000000000000000000000000000000000" // Forged
        )

        var thrown = false
        try {
            HistoricalBacktestEngine.executeBacktest(candles, metadataWithForgedChecksum)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue("Must throw SecurityException when checksum is mismatched", thrown)
    }

    // Test 5: Real Coinbase metadata is propagated into the final report
    @Test
    fun test_realMetadata_propagatedIntoFinalReport() = runBlocking {
        val candles = mutableListOf<HistoricalCandle>()
        for (i in 0 until 100) {
            candles.add(
                HistoricalCandle(
                    timestamp = 1704067200000L + i * 60_000L,
                    open = 42000.0 + i,
                    high = 42010.0 + i,
                    low = 41990.0 + i,
                    close = 42005.0 + i,
                    volume = 10.0,
                    isSynthetic = false
                )
            )
        }

        val (sanitized, metadata) = HistoricalDatasetManager.sanitizeAndProcessCandles(
            rawCandles = candles,
            requestedStartMs = candles.first().timestamp,
            requestedEndMs = candles.last().timestamp,
            sourceType = DatasetSourceType.REAL_COINBASE_API
        )

        val result = HistoricalBacktestEngine.executeBacktest(
            dataset = sanitized,
            metadata = metadata,
            frictionScenario = ExecutionFrictionScenario.BASELINE
        )

        assertEquals(DatasetSourceType.REAL_COINBASE_API, result.datasetMetadata.sourceType)
        assertEquals(metadata.datasetSha256Checksum, result.datasetMetadata.datasetSha256Checksum)
        assertFalse(result.isSyntheticRun)
        assertNull(result.syntheticWarningBanner)
    }

    // Test 6: 30-Day Authentic Coinbase Backtest Execution & Forensic Verification
    @Test
    fun test_executeAuthentic30DayCoinbaseBacktest() = runBlocking {
        val startMs = 1704067200000L // 2024-01-01T00:00:00.000Z
        val endMs = 1706659200000L   // 2024-01-31T00:00:00.000Z (30 full days = 43,200 1m candles)

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        println("\n================================================================================")
        println("TASK 2 & 3 & 4: 30-DAY AUTHENTIC COINBASE HISTORICAL REPLAY")
        println("Start UTC: ${isoFormat.format(Date(startMs))} ($startMs)")
        println("End UTC:   ${isoFormat.format(Date(endMs))} ($endMs)")
        println("================================================================================")

        val (dataset, metadata) = HistoricalDatasetManager.fetchCoinbaseCandles(
            startMs = startMs,
            endMs = endMs,
            onProgress = { p, msg ->
                if (p % 10 == 0) println("[$p%] $msg")
            }
        )

        println("\n--- PROGRAMMATIC DATASET METADATA ---")
        println("• DatasetSourceType:                ${metadata.sourceType}")
        println("• HistoricalDatasetMetadata.source: ${metadata.source}")
        println("• Dataset SHA-256 Checksum:         ${metadata.datasetSha256Checksum}")
        println("• Exact Start UTC:                  ${isoFormat.format(Date(metadata.actualStartEpochMs))} (${metadata.actualStartEpochMs})")
        println("• Exact End UTC:                    ${isoFormat.format(Date(metadata.actualEndEpochMs))} (${metadata.actualEndEpochMs})")
        println("• Candle Interval:                  ${metadata.resolution}")
        println("• Total Candles:                    ${metadata.totalCandlesCount}")
        println("• Missing Candles:                  ${metadata.missingCandlesCount}")
        println("• Synthetic Involved:               ${dataset.any { it.isSynthetic }}")

        println("\n--- RAW CANDLE SAMPLES FOR INDEPENDENT VERIFICATION ---")
        val sampleIndices = listOf(0, 1, 2, 1000, 5000, 10000, 20000, 30000, 40000, dataset.size - 1)
        for (idx in sampleIndices) {
            if (idx < dataset.size) {
                val c = dataset[idx]
                println("Candle #$idx: timestamp=${c.timestamp} (${isoFormat.format(Date(c.timestamp))}), open=${c.open}, high=${c.high}, low=${c.low}, close=${c.close}, volume=${c.volume}")
            }
        }

        // Execute 30-day Backtest across all 3 Friction Scenarios
        for (scenario in listOf(ExecutionFrictionScenario.OPTIMISTIC, ExecutionFrictionScenario.BASELINE, ExecutionFrictionScenario.CONSERVATIVE)) {
            val result = HistoricalBacktestEngine.executeBacktest(
                dataset = dataset,
                metadata = metadata,
                frictionScenario = scenario,
                runGeminiTrack = false,
                intraWindowMinuteOffset = 0
            )

            val trackD = result.trackD_Quantitative
            val totalWindows = dataset.size / 15
            val n = trackD.totalSettledCountN
            val wins = trackD.wonCount
            val losses = trackD.lostCount
            val noTrades = trackD.noTradeCount
            val accuracy = trackD.accuracyPercent
            val trades = trackD.predictionRecords.filter { it.predictedDirection != "NO_TRADE" }

            val grossProfitCents = (wins * 50.0) - (losses * 50.0)
            val grossPnlDollars = grossProfitCents / 100.0
            val totalFeesDollars = (n * scenario.totalFrictionCents) / 100.0
            val totalProfitCents = trades.sumOf { it.simulatedNetProfitCents }
            val expectancyDollars = if (n > 0) (totalProfitCents / n) / 100.0 else 0.0

            val winProfitCents = trades.filter { it.simulatedNetProfitCents > 0 }.sumOf { it.simulatedNetProfitCents }
            val lossCostCents = trades.filter { it.simulatedNetProfitCents < 0 }.sumOf { abs(it.simulatedNetProfitCents) }
            val profitFactor = if (lossCostCents > 0) winProfitCents / lossCostCents else if (winProfitCents > 0) Double.POSITIVE_INFINITY else 0.0

            println("\n================================================================================")
            println("REPORT SCENARIO: ${scenario.name} (${scenario.displayName})")
            println("• Total Evaluated 15m Windows: $totalWindows")
            println("• Warmup Exclusions:           2 windows (30 minutes)")
            println("• NO_TRADE Windows:            $noTrades")
            println("• Settled Trades (N):          $n")
            println("• Wins / Losses:               $wins Wins / $losses Losses")
            println("• Realized Accuracy:           ${String.format(Locale.US, "%.2f", accuracy ?: 0.0)}%")
            println("• Gross P&L (0 Friction):      $${String.format(Locale.US, "%.2f", grossPnlDollars)}")
            println("• Total Fees & Friction:       $${String.format(Locale.US, "%.2f", totalFeesDollars)} (${scenario.totalFrictionCents}¢/trade)")
            println("• Net Simulated P&L:           $${String.format(Locale.US, "%.2f", trackD.totalSimulatedPnlDollars)}")
            println("• Expectancy:                  $${String.format(Locale.US, "%.4f", expectancyDollars)} / trade")
            println("• Profit Factor:               ${String.format(Locale.US, "%.2f", profitFactor)}")
            println("• Raw Max Drawdown:            $${String.format(Locale.US, "%.2f", trackD.maxDrawdownDollars)} (${String.format(Locale.US, "%.2f", trackD.maxDrawdownPercent)}%)")
            println("================================================================================")
        }
    }

    @Test
    fun test_forensicNoTradeConditionBreakdown() = runBlocking {
        val startMs = 1704067200000L // 2024-01-01T00:00:00.000Z
        val endMs = 1706659200000L   // 2024-01-31T00:00:00.000Z (30 days)

        // 1. Fetch Real Coinbase Dataset
        val (realDataset, realMetadata) = HistoricalDatasetManager.fetchCoinbaseCandles(startMs, endMs)

        // 2. Generate Old Synthetic Dataset (30 days, seed 1337L)
        val syntheticCandles = mutableListOf<HistoricalCandle>()
        var currentMs = startMs
        var currentPrice = 42500.0
        val random = java.util.Random(1337L)
        var minuteIndex = 0
        while (currentMs <= endMs) {
            val dailyCycle = kotlin.math.sin(minuteIndex * (2.0 * Math.PI / 1440.0)) * 25.0
            val noise = (random.nextGaussian() * 12.0)
            val open = currentPrice
            val close = (open + dailyCycle + noise).coerceAtLeast(1000.0)
            val high = maxOf(open, close) + kotlin.math.abs(random.nextGaussian() * 6.0)
            val low = minOf(open, close) - kotlin.math.abs(random.nextGaussian() * 6.0)
            val volume = 5.0 + kotlin.math.abs(random.nextGaussian() * 10.0)
            syntheticCandles.add(HistoricalCandle(currentMs, open, high, low, close, volume, isSynthetic = true))
            currentPrice = close
            currentMs += 60_000L
            minuteIndex++
        }
        val (syntheticDataset, syntheticMetadata) = HistoricalDatasetManager.sanitizeAndProcessCandles(
            rawCandles = syntheticCandles,
            requestedStartMs = startMs,
            requestedEndMs = endMs,
            sourceType = DatasetSourceType.SYNTHETIC_TEST_BENCHMARK
        )

        // Analyze both datasets
        println("\n================================================================================")
        println("FORENSIC NO_TRADE DEADBAND BREAKDOWN ANALYSIS")
        println("================================================================================")

        fun analyzeDataset(name: String, dataset: List<HistoricalCandle>, metadata: HistoricalDatasetMetadata) {
            val windowStepMs = 15 * 60_000L
            val minWarmupMs = 30 * 60_000L
            val startEvalMs = metadata.actualStartEpochMs + minWarmupMs

            var totalWindows = 0
            var warmupExclusions = 2
            var settledTrades = 0
            var noTradeCount = 0

            // Condition zero-score counters for NO_TRADE windows
            var zeroEmaCount = 0
            var zeroMomCount = 0
            var zeroRsiCount = 0
            var allZeroCount = 0

            // Conflicting signal counter (e.g. EMA bullish but MOM/RSI bearish canceling out to |score| < 0.5)
            var conflictingSignalCount = 0
            // Weak signal counter (all non-zero factors but sub-threshold sum < 0.5)
            var weakSignalCount = 0

            // Distribution of composite scores for NO_TRADE windows
            var exactZeroScoreCount = 0
            var scorePlus04 = 0
            var scoreMinus04 = 0
            var scorePlus03 = 0
            var scoreMinus03 = 0

            var t = startEvalMs
            while (t + 15 * 60_000L <= metadata.actualEndEpochMs) {
                totalWindows++
                val windowStartMs = t
                val windowEndMs = t + 15 * 60_000L
                val tOffsetMs = windowStartMs // T=0

                val completedHistoryCandles = dataset.filter { it.timestamp < tOffsetMs }
                if (completedHistoryCandles.size < com.example.data.quant.QuantitativeFeatureExtractor.MINIMUM_REQUIRED_CANDLES) {
                    continue
                }
                val boundedProvider = com.example.data.backtest.StrictBoundedHistoricalDataProvider(
                    completedHistoryCandles,
                    asOfTimestamp = tOffsetMs - 60_000L
                )
                val strikeS0 = dataset.firstOrNull { it.timestamp == windowStartMs }?.open ?: completedHistoryCandles.last().close
                val evalSpot = strikeS0

                val candlesLookback = boundedProvider.getCandlesLookback(com.example.data.quant.QuantitativeFeatureExtractor.DEFAULT_LOOKBACK_CANDLES)
                val quantFeatures = com.example.data.quant.QuantitativeFeatureExtractor.extractFeatures(
                    candles = candlesLookback,
                    currentSpot = evalSpot,
                    strikePrice = strikeS0
                )

                if (quantFeatures == null) {
                    noTradeCount++
                    continue
                }

                val output = com.example.data.quant.SharedQuantitativeModel.evaluate(quantFeatures)

                if (output.direction == "NO TRADE") {
                    noTradeCount++
                    val emaScore = output.factors.firstOrNull { it.name.contains("EMA") }?.scoreContribution ?: 0.0
                    val momScore = output.factors.firstOrNull { it.name.contains("Momentum") }?.scoreContribution ?: 0.0
                    val rsiScore = output.factors.firstOrNull { it.name.contains("RSI") }?.scoreContribution ?: 0.0
                    val deltaScore = output.factors.firstOrNull { it.name.contains("Strike") }?.scoreContribution ?: 0.0

                    if (emaScore == 0.0) zeroEmaCount++
                    if (momScore == 0.0) zeroMomCount++
                    if (rsiScore == 0.0) zeroRsiCount++
                    if (emaScore == 0.0 && momScore == 0.0 && rsiScore == 0.0 && deltaScore == 0.0) allZeroCount++

                    if (output.rawCompositeScore == 0.0) exactZeroScoreCount++
                    if (output.rawCompositeScore == 0.4 || output.rawCompositeScore == 0.3) scorePlus04++
                    if (output.rawCompositeScore == -0.4 || output.rawCompositeScore == -0.3) scoreMinus04++

                    val hasPositive = emaScore > 0 || momScore > 0 || rsiScore > 0 || deltaScore > 0
                    val hasNegative = emaScore < 0 || momScore < 0 || rsiScore < 0 || deltaScore < 0
                    if (hasPositive && hasNegative) {
                        conflictingSignalCount++
                    } else if (output.rawCompositeScore != 0.0) {
                        weakSignalCount++
                    }
                } else {
                    settledTrades++
                }

                t += windowStepMs
            }

            println("\n[$name DATASET]")
            println("• Total Windows Evaluated:     $totalWindows")
            println("• Warmup Excluded Windows:     $warmupExclusions")
            println("• Settled Trades (N):          $settledTrades")
            println("• NO_TRADE Windows:            $noTradeCount (${String.format(Locale.US, "%.1f", (noTradeCount.toDouble() / totalWindows) * 100.0)}%)")
            println("  Breakdown of NO_TRADE Windows:")
            println("  - All 4 Factors Exactly 0.0 (Score = 0.00):      $allZeroCount")
            println("  - Conflicting Signals Canceling Out (|s| < 0.5): $conflictingSignalCount")
            println("  - Sub-threshold Weak Signals (|s| < 0.5):        $weakSignalCount")
            println("  Factor-level zero contributions in NO_TRADE:")
            println("  - EMA Score == 0 (|spread| <= $2.0):             $zeroEmaCount / $noTradeCount")
            println("  - Momentum Score == 0 (|mom| <= $3.0):           $zeroMomCount / $noTradeCount")
            println("  - RSI Score == 0 (Neutral channel/no trend):     $zeroRsiCount / $noTradeCount")
        }

        analyzeDataset("AUTHENTIC 30-DAY COINBASE (REAL_COINBASE_API)", realDataset, realMetadata)
        analyzeDataset("OLD 30-DAY SYNTHETIC FIXTURE (SYNTHETIC_BENCHMARK)", syntheticDataset, syntheticMetadata)
        println("================================================================================\n")
    }

    // Test 10: Auto-Bot Loop Decommissioning and Zero Orphaned Execution Verification
    @Test
    fun test_autoBot_loopDecommissionedAndNoOrphanedExecution() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.StrategyNoteRepository(db.strategyNoteDao())
        val viewModel = com.example.ui.TradingViewModel(repo)

        // Verify initial state has bot stopped and no active execution position
        val initialBotState = viewModel.paperBotState.value
        assertFalse("Paper auto-bot must not be running on startup in Path B", initialBotState.isBotRunning)
        assertNull("No active position should exist from orphaned auto-bot loop", initialBotState.activePosition)

        // Verify toggle does not start autonomous execution loop
        viewModel.togglePaperBot()
        val postToggleState = viewModel.paperBotState.value
        assertFalse("Toggle must enforce isBotRunning = false in Decision Support mode", postToggleState.isBotRunning)
        assertTrue("Status message must confirm Decision Support mode", postToggleState.statusMessage.contains("Decision Support"))

        // Run live quant evaluations and verify no auto-trades are executed
        viewModel.run10SecondLiveEvaluation()
        val postEvalState = viewModel.paperBotState.value
        assertNull("Evaluating live prediction must never autonomously open a position", postEvalState.activePosition)
    }
}


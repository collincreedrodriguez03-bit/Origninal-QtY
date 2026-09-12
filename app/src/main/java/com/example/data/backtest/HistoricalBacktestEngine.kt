package com.example.data.backtest

import com.example.api.GeminiApiClient
import com.example.data.quant.QuantitativeFeatureExtractor
import com.example.data.quant.QuantitativeFeatures
import com.example.data.quant.SharedQuantitativeModel
import com.example.ui.DataConfidenceTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object HistoricalBacktestEngine {

    /**
     * Executes walk-forward 15-minute historical backtest over the provided dataset.
     * Supports optional intraWindowMinuteOffset (0 to 14 minutes into the 15m cycle)
     * to evaluate prediction efficacy at various points within the contract duration.
     */
    suspend fun executeBacktest(
        dataset: List<HistoricalCandle>,
        metadata: HistoricalDatasetMetadata,
        frictionScenario: ExecutionFrictionScenario = ExecutionFrictionScenario.BASELINE,
        runGeminiTrack: Boolean = false,
        intraWindowMinuteOffset: Int = 0,
        onProgress: ((progressPercent: Int, status: String) -> Unit)? = null
    ): BacktestRunResult = withContext(Dispatchers.Default) {
        if (dataset.size < 60) {
            throw IllegalArgumentException("Insufficient historical data for backtesting. Minimum 60 1-minute candles required.")
        }

        // Structural Safeguard 1: SHA-256 Checksum Verification
        val computedSha256 = HistoricalDatasetMetadata.computeSha256(dataset)
        if (metadata.datasetSha256Checksum.isNotBlank() && computedSha256 != metadata.datasetSha256Checksum) {
            throw SecurityException("CRITICAL DATA INTEGRITY FAILURE: Dataset SHA-256 checksum mismatch. Expected ${metadata.datasetSha256Checksum}, computed $computedSha256.")
        }

        // Structural Safeguard 2: Fail-closed if dataset contains synthetic data but metadata claims REAL_COINBASE_API
        if (metadata.sourceType == DatasetSourceType.REAL_COINBASE_API) {
            val syntheticCount = dataset.count { it.isSynthetic }
            if (syntheticCount > 0) {
                throw IllegalStateException("CRITICAL DATA INTEGRITY FAILURE: $syntheticCount synthetic candles present in dataset asserting REAL_COINBASE_API.")
            }
            if (metadata.datasetSha256Checksum.isBlank()) {
                throw IllegalStateException("CRITICAL DATA INTEGRITY FAILURE: REAL_COINBASE_API dataset missing SHA-256 checksum.")
            }
        }

        // Structural Safeguard 3: If dataset contains synthetic candles, sourceType MUST be SYNTHETIC_TEST_BENCHMARK
        if (dataset.any { it.isSynthetic } && metadata.sourceType != DatasetSourceType.SYNTHETIC_TEST_BENCHMARK) {
            throw IllegalStateException("CRITICAL DATA INTEGRITY FAILURE: Synthetic candles present but sourceType is ${metadata.sourceType}.")
        }

        val windowStepMs = 15 * 60_000L // 15-minute intervals
        val minWarmupMs = 30 * 60_000L // 30-minute warmup for indicators

        val startEpoch = dataset.first().timestamp + minWarmupMs
        val endEpoch = dataset.last().timestamp - windowStepMs

        val trackARecords = mutableListOf<BacktestPredictionRecord>()
        val trackBRecords = mutableListOf<BacktestPredictionRecord>()
        val trackCRecords = mutableListOf<BacktestPredictionRecord>()
        val trackDRecords = mutableListOf<BacktestPredictionRecord>()
        val trackERecords = mutableListOf<BacktestPredictionRecord>()

        val geminiAuditLogs = mutableListOf<GeminiRetrospectiveAuditRecord>()

        // Collect distinct 15-minute start timestamps T
        val windowStartTimestamps = mutableListOf<Long>()
        var cursorT = startEpoch
        while (cursorT <= endEpoch) {
            windowStartTimestamps.add(cursorT)
            cursorT += windowStepMs
        }

        val totalWindows = windowStartTimestamps.size
        val safeOffsetMs = intraWindowMinuteOffset.coerceIn(0, 14) * 60_000L

        for ((index, t) in windowStartTimestamps.withIndex()) {
            val progress = ((index.toDouble() / totalWindows.toDouble()) * 100.0).toInt().coerceIn(0, 99)
            if (index % 10 == 0) {
                onProgress?.invoke(progress, "Replaying historical window ${index + 1}/$totalWindows at T=$t...")
            }

            // Evaluation point: T + offset
            val evaluationT = t + safeOffsetMs

            // 1. Identify Strike (S_0) - locked immutably at window start T (OPEN of 1m candle starting at T)
            val strikeCandle = dataset.find { it.timestamp == t }
                ?: dataset.filter { it.timestamp <= t }.lastOrNull()
                ?: continue
            val strikeS0 = strikeCandle.open

            // 2. Identify Evaluation Spot at evaluationT
            // If evaluating at window open (safeOffsetMs == 0), Spot is exactly S_0 (deltaToStrike = 0.0).
            // If evaluating intra-window (safeOffsetMs > 0), Spot is the CLOSE of the latest completed candle at evaluationT - 60_000L.
            val evalSpot = if (safeOffsetMs == 0L) {
                strikeS0
            } else {
                val lastCompletedCandle = dataset.find { it.timestamp == evaluationT - 60_000L }
                    ?: dataset.filter { it.timestamp < evaluationT }.lastOrNull()
                    ?: strikeCandle
                lastCompletedCandle.close
            }

            // 3. Strict Bounded Historical Provider: strictly provides COMPLETED candles before evaluationT (timestamp < evaluationT)
            val completedHistoryCandles = dataset.filter { it.timestamp < evaluationT }
            if (completedHistoryCandles.size < QuantitativeFeatureExtractor.MINIMUM_REQUIRED_CANDLES) {
                continue
            }
            val boundedProvider = StrictBoundedHistoricalDataProvider(completedHistoryCandles, asOfTimestamp = evaluationT - 60_000L)

            // 4. S_15: CLOSE of candle at T + 14m
            val settlementTime = t + 14 * 60_000L
            val settlementCandle = dataset.find { it.timestamp == settlementTime }
                ?: dataset.filter { it.timestamp in (t + 1..t + 15 * 60_000L) }.lastOrNull()
                ?: continue
            val settlementS15 = settlementCandle.close

            // 4. Track A: Random 50/50 Baseline
            val randomPrediction = evaluateRandomBaseline(evaluationT)
            val recordA = evaluateOutcomeAndFriction(
                trackName = "Track A (Random 50/50)",
                windowStartT = t,
                strike = strikeS0,
                settlementTime = settlementTime + 60_000L,
                settlement = settlementS15,
                direction = randomPrediction.first,
                confidence = randomPrediction.second,
                friction = frictionScenario,
                isSynthetic = dataset.any { it.isSynthetic }
            )
            trackARecords.add(recordA)

            // 5. Track B: Persistence Baseline (sign of preceding completed 1m candle [T-1m, T))
            val persistencePrediction = evaluatePersistenceBaseline(boundedProvider, evaluationT)
            val recordB = evaluateOutcomeAndFriction(
                trackName = "Track B (Persistence)",
                windowStartT = t,
                strike = strikeS0,
                settlementTime = settlementTime + 60_000L,
                settlement = settlementS15,
                direction = persistencePrediction.first,
                confidence = persistencePrediction.second,
                friction = frictionScenario,
                isSynthetic = dataset.any { it.isSynthetic }
            )
            trackBRecords.add(recordB)

            // 6. Track C: Simple Momentum Baseline (sign of 15m price change [T-15m, T))
            val momentumPrediction = evaluateMomentumBaseline(boundedProvider)
            val recordC = evaluateOutcomeAndFriction(
                trackName = "Track C (Simple Momentum)",
                windowStartT = t,
                strike = strikeS0,
                settlementTime = settlementTime + 60_000L,
                settlement = settlementS15,
                direction = momentumPrediction.first,
                confidence = momentumPrediction.second,
                friction = frictionScenario,
                isSynthetic = dataset.any { it.isSynthetic }
            )
            trackCRecords.add(recordC)

            // 7. Track D: Quantitative Multi-Factor Model (strict point-in-time calculation via QuantitativeFeatureExtractor)
            val quantPrediction = evaluateQuantitativeModel(boundedProvider, currentSpot = evalSpot, strikePrice = strikeS0)
            val recordD = evaluateOutcomeAndFriction(
                trackName = "Track D (Quantitative Multi-Factor)",
                windowStartT = t,
                strike = strikeS0,
                settlementTime = settlementTime + 60_000L,
                settlement = settlementS15,
                direction = quantPrediction.first,
                confidence = quantPrediction.second,
                friction = frictionScenario,
                isSynthetic = dataset.any { it.isSynthetic }
            )
            trackDRecords.add(recordD)

            // 7. Track E: Gemini Experimental Retrospective (if enabled)
            if (runGeminiTrack && GeminiApiClient.isGeminiKeyConfigured() && index < 20) { // Limit live API queries during backtest
                val rsi = calculateRsi(boundedProvider.getCandlesLookback(15))
                val recentCandles = boundedProvider.getCandlesLookback(25)
                val ema9 = calculateEma(recentCandles.map { it.close }, 9)
                val ema21 = calculateEma(recentCandles.map { it.close }, 21)
                val geminiResult = GeminiApiClient.queryGeminiPrediction(
                    btcSpot = strikeS0,
                    strikePrice = strikeS0,
                    rsi = rsi,
                    momentum = strikeS0 - (recentCandles.firstOrNull()?.close ?: strikeS0),
                    ema9 = ema9,
                    ema21 = ema21,
                    volDelta = 0.0, // Hard zero
                    minutesRemaining = 15
                )

                if (geminiResult != null) {
                    val recordE = evaluateOutcomeAndFriction(
                        trackName = "Track E (Gemini Retrospective)",
                        windowStartT = t,
                        strike = strikeS0,
                        settlementTime = settlementTime + 60_000L,
                        settlement = settlementS15,
                        direction = geminiResult.direction,
                        confidence = geminiResult.probability,
                        friction = frictionScenario,
                        isSynthetic = dataset.any { it.isSynthetic }
                    )
                    trackERecords.add(recordE)

                    geminiAuditLogs.add(
                        GeminiRetrospectiveAuditRecord(
                            geminiModelNameAndVersion = "gemini-2.5-flash",
                            promptTemplateVersion = "v1.0.4-p0c-retrospective",
                            apiCallTimestampEpochMs = System.currentTimeMillis(),
                            historicalMarketTimestampEpochMs = t,
                            exactInputPayloadJson = "{\"strike\":$strikeS0,\"rsi\":$rsi,\"ema9\":$ema9,\"ema21\":$ema21,\"minutesRemaining\":15}",
                            rawModelResponseJson = "{\"direction\":\"${geminiResult.direction}\",\"confidence\":${geminiResult.probability}}",
                            parsedPredictionDirection = geminiResult.direction,
                            parsedConfidencePercent = geminiResult.probability,
                            actualSettlementOutcome = recordE.actualOutcome
                        )
                    )
                }
            }
        }

        onProgress?.invoke(100, "Compiling multi-track summaries and calibration buckets...")

        val isSyntheticDataset = !metadata.sourceType.isPermittedForResearchMetrics
        val summaryA = compileTrackSummary("TRACK_A", "Track A: Random 50/50 Baseline", trackARecords, isSynthetic = isSyntheticDataset)
        val summaryB = compileTrackSummary("TRACK_B", "Track B: Persistence Baseline", trackBRecords, isSynthetic = isSyntheticDataset)
        val summaryC = compileTrackSummary("TRACK_C", "Track C: Simple Momentum Baseline", trackCRecords, isSynthetic = isSyntheticDataset)
        val summaryD = compileTrackSummary("TRACK_D", "Track D: Quantitative Multi-Factor", trackDRecords, isSynthetic = isSyntheticDataset)
        val summaryE = compileTrackSummary(
            "TRACK_E",
            "Track E: Gemini Experimental",
            trackERecords,
            isRetrospective = true,
            retrospectiveBadge = "RETROSPECTIVE — CURRENT MODEL VERSION ONLY",
            isSynthetic = isSyntheticDataset
        )

        BacktestRunResult(
            datasetMetadata = metadata,
            frictionScenario = frictionScenario,
            trackA_Random = summaryA,
            trackB_Persistence = summaryB,
            trackC_Momentum = summaryC,
            trackD_Quantitative = summaryD,
            trackE_Gemini = summaryE,
            runTimestampEpochMs = System.currentTimeMillis()
        )
    }

    /**
     * Track A: Random 50/50 Baseline.
     * Deterministic pseudo-random seed based on window start timestamp T.
     */
    private fun evaluateRandomBaseline(t: Long): Pair<String, Double> {
        val seed = t xor 0x5DEECE66DL
        val rng = Random(seed)
        val isBull = rng.nextBoolean()
        return Pair(if (isBull) "BULLISH" else "BEARISH", 50.0)
    }

    /**
     * Track B: Persistence Baseline.
     * Predicts the directional sign of the immediately preceding completed 1m candle [T-1m, T).
     * If open == close, looks back up to 15 candles for the most recent non-flat candle.
     */
    private fun evaluatePersistenceBaseline(provider: BoundedHistoricalDataProvider, t: Long): Pair<String, Double> {
        val lookback = provider.getCandlesLookback(15)
        if (lookback.isEmpty()) return Pair("NO_TRADE", 50.0)

        // Iterate backwards from the most recent completed candle
        for (i in lookback.indices.reversed()) {
            val candle = lookback[i]
            if (candle.close > candle.open) {
                return Pair("BULLISH", 52.0)
            } else if (candle.close < candle.open) {
                return Pair("BEARISH", 52.0)
            }
        }
        return Pair("NO_TRADE", 50.0)
    }

    /**
     * Track C: Simple Momentum Baseline.
     * Evaluates 15-minute price change over [T - 15m, T).
     */
    private fun evaluateMomentumBaseline(provider: BoundedHistoricalDataProvider): Pair<String, Double> {
        val candles = provider.getCandlesLookback(15)
        if (candles.size < 2) return Pair("NO_TRADE", 50.0)

        val open15mAgo = candles.first().open
        val currentClose = candles.last().close

        return when {
            currentClose > open15mAgo -> Pair("BULLISH", 55.0)
            currentClose < open15mAgo -> Pair("BEARISH", 55.0)
            else -> Pair("NO_TRADE", 50.0)
        }
    }

    /**
     * Track D: Quantitative Multi-Factor Model (Single Source of Truth: SharedQuantitativeModel).
     * Computes RSI(14), EMA(9)/EMA(21), Momentum, and Strike Delta strictly from data <= evaluation time
     * using QuantitativeFeatureExtractor (mathematically identical to live runtime).
     */
    private fun evaluateQuantitativeModel(
        provider: BoundedHistoricalDataProvider,
        currentSpot: Double,
        strikePrice: Double
    ): Pair<String, Double> {
        val candles = provider.getCandlesLookback(QuantitativeFeatureExtractor.DEFAULT_LOOKBACK_CANDLES)
        val quantFeatures = QuantitativeFeatureExtractor.extractFeatures(
            candles = candles,
            currentSpot = currentSpot,
            strikePrice = strikePrice
        ) ?: return Pair("NO_TRADE", 50.0)

        val output = SharedQuantitativeModel.evaluate(quantFeatures)
        return Pair(output.backtestDirection, output.directionalConfidence)
    }

    /**
     * Evaluates binary settlement outcome and calculates realistic simulated execution friction.
     *
     * SETTLEMENT SPECIFICATION — KALSHI RULE 5.1 (Price Threshold Markets: 'Above Strike'):
     * - A contract resolves to YES (BULLISH wins) if and only if S_15 > S_0 (strictly greater).
     * - A contract resolves to NO (BEARISH wins) if and only if S_15 <= S_0 (less than or equal).
     * - If S_15 == S_0 (FLAT): The market resolves to NO. BULLISH settles as LOST, BEARISH settles as WON.
     */
    internal fun evaluateOutcomeAndFriction(
        trackName: String,
        windowStartT: Long,
        strike: Double,
        settlementTime: Long,
        settlement: Double,
        direction: String,
        confidence: Double,
        friction: ExecutionFrictionScenario,
        isSynthetic: Boolean = false
    ): BacktestPredictionRecord {
        val isWin = when (direction) {
            "BULLISH" -> settlement > strike // Strictly greater for YES under Kalshi Rule 5.1
            "BEARISH" -> settlement <= strike // Less than or equal for NO under Kalshi Rule 5.1 (FLAT resolves NO)
            else -> false
        }

        val outcome = when {
            direction == "NO_TRADE" -> "FLAT"
            isWin -> "WON"
            else -> "LOST"
        }

        // At-The-Money (ATM) contract market price at window start (T=0) is 50.0¢
        val fairPriceCents = 50.0

        // Simulated Execution Price (cents): Base 50¢ ATM price + total friction (half-spread + taker fee + slippage)
        val costCents = (fairPriceCents + friction.totalFrictionCents).coerceIn(1.0, 99.0)
        val payoutCents = if (outcome == "WON") 100.0 else 0.0
        val netProfitCents = if (direction == "NO_TRADE") 0.0 else payoutCents - costCents

        return BacktestPredictionRecord(
            trackName = trackName,
            windowStartTimestamp = windowStartT,
            strikeReferencePrice = strike,
            settlementTimestamp = settlementTime,
            settlementPrice = settlement,
            predictedDirection = direction,
            confidencePercent = confidence,
            actualOutcome = outcome,
            estimatedFairPriceCents = Math.round(fairPriceCents * 10.0) / 10.0,
            simulatedCostCents = Math.round(costCents * 10.0) / 10.0,
            simulatedPayoutCents = payoutCents,
            simulatedNetProfitCents = Math.round(netProfitCents * 10.0) / 10.0,
            isSyntheticData = isSynthetic,
            executionLabel = friction.label
        )
    }

    /**
     * Compiles summary metrics, data confidence tier, and calibration bins for a track.
     * Synthetic datasets are flagged with isSyntheticBenchmark = true and permanently labeled.
     */
    private fun compileTrackSummary(
        trackId: String,
        trackName: String,
        records: List<BacktestPredictionRecord>,
        isRetrospective: Boolean = false,
        retrospectiveBadge: String? = null,
        isSynthetic: Boolean = false
    ): TrackPerformanceSummary {
        val settledTrades = records.filter { it.predictedDirection != "NO_TRADE" }
        val n = settledTrades.size
        val wins = settledTrades.count { it.actualOutcome == "WON" }
        val losses = settledTrades.count { it.actualOutcome == "LOST" }
        val noTrades = records.count { it.predictedDirection == "NO_TRADE" }

        // Data Confidence Tier per track (Only applies to authentic research data)
        val tier = when {
            isSynthetic -> DataConfidenceTier.INSUFFICIENT
            n < 100 -> DataConfidenceTier.INSUFFICIENT
            n < 300 -> DataConfidenceTier.PRELIMINARY
            else -> DataConfidenceTier.CALIBRATION_READY
        }

        // Accuracy is suppressed (null) if sample size is INSUFFICIENT or if data is synthetic
        val accuracy = if (tier == DataConfidenceTier.INSUFFICIENT || isSynthetic) {
            null
        } else {
            if (n > 0) Math.round((wins.toDouble() / n.toDouble()) * 1000.0) / 10.0 else null
        }

        // PnL in Dollars (cents / 100)
        var cumulativePnlCents = 0.0
        var peakPnlCents = 0.0
        var maxDrawdownDollars = 0.0

        for (r in settledTrades) {
            cumulativePnlCents += r.simulatedNetProfitCents
            if (cumulativePnlCents > peakPnlCents) {
                peakPnlCents = cumulativePnlCents
            }
            val dd = (peakPnlCents - cumulativePnlCents) / 100.0
            if (dd > maxDrawdownDollars) {
                maxDrawdownDollars = dd
            }
        }

        val totalPnlDollars = Math.round(cumulativePnlCents) / 100.0
        val maxDrawdownPct = if (peakPnlCents > 0) Math.round((maxDrawdownDollars / (peakPnlCents / 100.0)) * 1000.0) / 10.0 else 0.0

        // Calibration Bins for N >= 300 (suppressed for synthetic data)
        val calibrationBins = if (tier == DataConfidenceTier.CALIBRATION_READY && !isSynthetic) {
            computeCalibrationBins(settledTrades)
        } else {
            emptyList()
        }

        return TrackPerformanceSummary(
            trackId = trackId,
            trackName = trackName,
            isRetrospectiveOnly = isRetrospective,
            retrospectiveBadge = retrospectiveBadge,
            isSyntheticBenchmark = isSynthetic,
            totalSettledCountN = if (isSynthetic) 0 else n,
            wonCount = wins,
            lostCount = losses,
            noTradeCount = noTrades,
            dataConfidenceTier = tier,
            accuracyPercent = accuracy,
            totalSimulatedPnlDollars = totalPnlDollars,
            maxDrawdownDollars = Math.round(maxDrawdownDollars * 100.0) / 100.0,
            maxDrawdownPercent = maxDrawdownPct,
            calibrationBins = calibrationBins,
            predictionRecords = records
        )
    }

    /**
     * Groups predictions into reliability bins (e.g. 50-60%, 60-70%, 70-80%, 80-90%, 90-100%).
     */
    private fun computeCalibrationBins(trades: List<BacktestPredictionRecord>): List<CalibrationReliabilityBin> {
        val ranges = listOf(
            Pair(50.0, 60.0),
            Pair(60.0, 70.0),
            Pair(70.0, 80.0),
            Pair(80.0, 90.0),
            Pair(90.0, 100.0)
        )

        return ranges.map { (minProb, maxProb) ->
            val inBin = trades.filter { it.confidencePercent >= minProb && it.confidencePercent < maxProb }
            val total = inBin.size
            val wins = inBin.count { it.actualOutcome == "WON" }
            val realizedAcc = if (total > 0) Math.round((wins.toDouble() / total.toDouble()) * 1000.0) / 10.0 else 0.0
            val expectedMid = (minProb + maxProb) / 2.0

            CalibrationReliabilityBin(
                binRangeLabel = "${minProb.toInt()}% - ${maxProb.toInt()}%",
                expectedProbPercent = expectedMid,
                totalPredictionsInBin = total,
                actualWinsInBin = wins,
                realizedAccuracyPercent = realizedAcc
            )
        }
    }

    // Indicator Helpers (Delegates to QuantitativeFeatureExtractor for parity)
    private fun calculateRsi(candles: List<HistoricalCandle>): Double {
        return QuantitativeFeatureExtractor.calculateRsi(candles.map { it.close }, 14)
    }

    private fun calculateEma(prices: List<Double>, period: Int): Double {
        return QuantitativeFeatureExtractor.calculateEma(prices, period)
    }
}

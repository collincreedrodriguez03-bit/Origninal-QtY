package com.example.data.backtest

import com.example.ui.DataConfidenceTier
import java.security.MessageDigest

/**
 * Dataset source type separating real Coinbase market data from synthetic dev/test data.
 */
enum class DatasetSourceType(
    val displayName: String,
    val isPermittedForResearchMetrics: Boolean,
    val mandatoryAuditLabel: String
) {
    REAL_COINBASE_API(
        displayName = "COINBASE_REST_API (Real Historical Market Data)",
        isPermittedForResearchMetrics = true,
        mandatoryAuditLabel = "AUTHENTIC COINBASE HISTORICAL OHLCV"
    ),
    SYNTHETIC_TEST_BENCHMARK(
        displayName = "SYNTHETIC_BENCHMARK (Dev/Test Offline Engine)",
        isPermittedForResearchMetrics = false,
        mandatoryAuditLabel = "SYNTHETIC TEST DATA — NOT REAL MARKET DATA"
    )
}

/**
 * Exception thrown when real market data is unavailable and fail-closed research mode aborts.
 * Synthetic data is NEVER silently substituted in research mode.
 */
class RealMarketDataUnavailableException(
    override val message: String = "DATA UNAVAILABLE: Real Coinbase historical market data could not be retrieved. Backtest aborted. No research result generated. (Fail-closed research mode active)."
) : Exception(message)

/**
 * Single 1-minute historical OHLCV candle.
 */
data class HistoricalCandle(
    val timestamp: Long, // Epoch ms at candle OPEN
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isSynthetic: Boolean = false
)

/**
 * Exception thrown when any component attempts to query historical data beyond horizon T.
 * Provides architectural enforcement of the hard no-lookahead boundary.
 */
class FutureDataAccessViolationException(
    val requestedTimestamp: Long,
    val boundedAsOfTimestamp: Long,
    override val message: String = "HARD NO-LOOKAHEAD BOUNDARY VIOLATED: Requested observation at timestamp $requestedTimestamp which is greater than bounded horizon T ($boundedAsOfTimestamp)."
) : SecurityException(message)

/**
 * Time-bounded data provider interface for historical replay.
 */
interface BoundedHistoricalDataProvider {
    val asOfTimestamp: Long
    val isSyntheticData: Boolean
    fun getCandlesLookback(count: Int): List<HistoricalCandle>
    fun getCandleAt(timestamp: Long): HistoricalCandle
    fun getVolumeLookback(count: Int): List<Double>
}

/**
 * Concrete implementation enforcing hard no-lookahead boundary at timestamp T.
 */
class StrictBoundedHistoricalDataProvider(
    private val fullDataset: List<HistoricalCandle>,
    override val asOfTimestamp: Long
) : BoundedHistoricalDataProvider {

    override val isSyntheticData: Boolean = fullDataset.any { it.isSynthetic }

    private val visibleSlice: List<HistoricalCandle> by lazy {
        fullDataset.filter { it.timestamp <= asOfTimestamp }
    }

    override fun getCandlesLookback(count: Int): List<HistoricalCandle> {
        return visibleSlice.takeLast(count)
    }

    override fun getCandleAt(timestamp: Long): HistoricalCandle {
        if (timestamp > asOfTimestamp) {
            throw FutureDataAccessViolationException(timestamp, asOfTimestamp)
        }
        return visibleSlice.find { it.timestamp == timestamp }
            ?: throw NoSuchElementException("Candle not found in bounded historical horizon for timestamp $timestamp.")
    }

    override fun getVolumeLookback(count: Int): List<Double> {
        return visibleSlice.takeLast(count).map { it.volume }
    }
}

/**
 * Dataset metadata with SHA-256 integrity checksum and physical synthetic separation.
 */
data class HistoricalDatasetMetadata(
    val sourceType: DatasetSourceType = DatasetSourceType.REAL_COINBASE_API,
    val source: String = sourceType.displayName,
    val symbol: String = "BTC-USD",
    val resolution: String = "1m",
    val requestedStartEpochMs: Long,
    val requestedEndEpochMs: Long,
    val actualStartEpochMs: Long,
    val actualEndEpochMs: Long,
    val totalCandlesCount: Int,
    val missingCandlesCount: Int,
    val retrievalTimestampEpochMs: Long,
    val datasetSha256Checksum: String,
    val syntheticDataLabel: String? = if (!sourceType.isPermittedForResearchMetrics) "SYNTHETIC TEST DATA — NOT REAL MARKET DATA" else null
) {
    companion object {
        fun computeSha256(candles: List<HistoricalCandle>): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val canonicalData = StringBuilder()
            candles.sortedBy { it.timestamp }.forEach { c ->
                canonicalData.append("${c.timestamp},${c.open},${c.high},${c.low},${c.close},${c.volume},${c.isSynthetic}\n")
            }
            val hashBytes = digest.digest(canonicalData.toString().toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Execution friction scenarios for simulated execution.
 */
enum class ExecutionFrictionScenario(
    val displayName: String,
    val halfSpreadCents: Double,
    val takerFeeCents: Double,
    val slippageCents: Double,
    val totalFrictionCents: Double,
    val label: String = "SIMULATED EXECUTION — NOT HISTORICAL FILLS"
) {
    OPTIMISTIC("Optimistic (0.5¢ spr / 0.5¢ fee / 0.0¢ slip)", 0.5, 0.5, 0.0, 1.0),
    BASELINE("Baseline (1.0¢ spr / 1.0¢ fee / 1.0¢ slip)", 1.0, 1.0, 1.0, 3.0),
    CONSERVATIVE("Conservative (2.0¢ spr / 1.5¢ fee / 2.0¢ slip)", 2.0, 1.5, 2.0, 5.5)
}

/**
 * Single prediction and settlement record for a 15-minute window [T, T+15m).
 */
data class BacktestPredictionRecord(
    val trackName: String,
    val windowStartTimestamp: Long, // T
    val strikeReferencePrice: Double, // S_0 (Open of 1m candle at T)
    val settlementTimestamp: Long, // T + 15m
    val settlementPrice: Double, // S_15 (Close of 1m candle at T + 14m)
    val predictedDirection: String, // BULLISH, BEARISH, NO_TRADE
    val confidencePercent: Double,
    val actualOutcome: String, // WON, LOST, FLAT
    val estimatedFairPriceCents: Double,
    val simulatedCostCents: Double,
    val simulatedPayoutCents: Double,
    val simulatedNetProfitCents: Double,
    val isSyntheticData: Boolean = false,
    val executionLabel: String = "SIMULATED EXECUTION — NOT HISTORICAL FILLS",
    val syntheticLabel: String? = if (isSyntheticData) "SYNTHETIC TEST DATA — NOT REAL MARKET DATA" else null
)

/**
 * Audit log entry specifically for retrospective Gemini historical queries.
 */
data class GeminiRetrospectiveAuditRecord(
    val geminiModelNameAndVersion: String = "gemini-2.5-flash",
    val promptTemplateVersion: String = "v1.0.4-p0c-retrospective",
    val apiCallTimestampEpochMs: Long,
    val historicalMarketTimestampEpochMs: Long,
    val exactInputPayloadJson: String,
    val rawModelResponseJson: String,
    val parsedPredictionDirection: String,
    val parsedConfidencePercent: Double,
    val actualSettlementOutcome: String,
    val retrospectiveLabel: String = "RETROSPECTIVE — CURRENT MODEL VERSION ONLY"
)

/**
 * Calibration reliability bin for N >= 300 empirical calibration analysis.
 */
data class CalibrationReliabilityBin(
    val binRangeLabel: String, // e.g. "70% - 80%"
    val expectedProbPercent: Double,
    val totalPredictionsInBin: Int,
    val actualWinsInBin: Int,
    val realizedAccuracyPercent: Double
)

/**
 * Independent performance summary for a single track.
 */
data class TrackPerformanceSummary(
    val trackId: String, // TRACK_A, TRACK_B, TRACK_C, TRACK_D, TRACK_E
    val trackName: String,
    val isRetrospectiveOnly: Boolean = false,
    val retrospectiveBadge: String? = null,
    val isSyntheticBenchmark: Boolean = false,
    val syntheticBadge: String? = if (isSyntheticBenchmark) "SYNTHETIC TEST DATA — NOT REAL MARKET DATA" else null,
    val totalSettledCountN: Int, // Set to 0 for synthetic data in research mode
    val wonCount: Int,
    val lostCount: Int,
    val noTradeCount: Int,
    val dataConfidenceTier: DataConfidenceTier,
    val accuracyPercent: Double?, // null if INSUFFICIENT (N < 100) or if synthetic
    val totalSimulatedPnlDollars: Double,
    val maxDrawdownDollars: Double = 0.0,
    val maxDrawdownPercent: Double,
    val calibrationBins: List<CalibrationReliabilityBin> = emptyList(),
    val predictionRecords: List<BacktestPredictionRecord> = emptyList()
)

/**
 * Full Backtest Execution Result.
 */
data class BacktestRunResult(
    val datasetMetadata: HistoricalDatasetMetadata,
    val frictionScenario: ExecutionFrictionScenario,
    val trackA_Random: TrackPerformanceSummary,
    val trackB_Persistence: TrackPerformanceSummary,
    val trackC_Momentum: TrackPerformanceSummary,
    val trackD_Quantitative: TrackPerformanceSummary,
    val trackE_Gemini: TrackPerformanceSummary,
    val runTimestampEpochMs: Long = System.currentTimeMillis(),
    val isSyntheticRun: Boolean = !datasetMetadata.sourceType.isPermittedForResearchMetrics,
    val syntheticWarningBanner: String? = if (isSyntheticRun) "SYNTHETIC TEST DATA — NOT REAL MARKET DATA" else null,
    val ohlcDisclosure: String = "DISCLOSURE: 1-minute OHLC historical data does not reveal the exact intraminute price path or exact moment the market crossed the strike.",
    val settlementRuleCitation: String = "SETTLEMENT RULE: Kalshi Rule 5.1 (Above Strike): YES wins if S_15 > S_0; NO wins if S_15 <= S_0 (FLAT settles NO).",
    val executionDisclosure: String = "SIMULATED EXECUTION — NOT HISTORICAL FILLS"
)

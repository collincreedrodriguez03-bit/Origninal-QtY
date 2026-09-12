package com.example.data.live

import java.util.Locale

/**
 * Phase 3A: Controlled Live Radar Validation Metrics Calculator.
 *
 * Implements strict research criteria:
 * 1. Independent Sample Definition: N = Number of distinct 15-minute contract windows (NOT individual 10-second snapshots).
 * 2. Strong-Signal Hypothesis: Evaluates signals with rawModelScore >= 70.0 (Strong UP) or <= 30.0 (Strong DOWN).
 * 3. Near-Outcome Leakage Guard: Late-window (<5m) observations are explicitly labeled as time-to-settlement observations, NOT independent predictive evidence.
 * 4. Strict Separation of Baseline A (Historical 46.27%, N=2,576) and Live Validation B.
 * 5. Zero Profit / Financial Edge Labeling: Metrics report conditional hit rates, sample sizes (N), and accuracies only.
 */
enum class ResearchGateStatus(val label: String) {
    INSUFFICIENT_DATA("INSUFFICIENT DATA (N < 100)"),
    PRELIMINARY("PRELIMINARY (100 <= N < 300)"),
    CALIBRATION_READY("CALIBRATION READY (N >= 300)")
}

data class BucketAccuracy(
    val bucketName: String,
    val totalCount: Int, // Total observational snapshots (10s telemetry logs)
    val settledCount: Int, // Total settled snapshots
    val independentWindowsCount: Int, // Distinct contract windows
    val settledIndependentWindowsCount: Int, // Settled distinct contract windows (Statistically authoritative N)
    val wins: Int,
    val losses: Int,
    val accuracyPercent: Double?,
    val isNearOutcomeObservation: Boolean = false,
    val nearOutcomeDisclaimer: String? = if (isNearOutcomeObservation) "NOT INDEPENDENT PREDICTIVE EVIDENCE — LATE-WINDOW OBSERVATIONS MAY BENEFIT FROM REDUCED TIME FOR PRICE REVERSAL AND MUST NOT BE INTERPRETED AS AN INDEPENDENT PREDICTIVE EDGE." else null
) {
    val gateStatus: ResearchGateStatus
        get() = when {
            settledIndependentWindowsCount < 100 -> ResearchGateStatus.INSUFFICIENT_DATA
            settledIndependentWindowsCount < 300 -> ResearchGateStatus.PRELIMINARY
            else -> ResearchGateStatus.CALIBRATION_READY
        }

    val formattedAccuracy: String
        get() = if (settledIndependentWindowsCount < 100) {
            "INSUFFICIENT (N=$settledIndependentWindowsCount < 100)"
        } else {
            accuracyPercent?.let { String.format(Locale.US, "%.2f%%", it) } ?: "N/A (N=0)"
        }

    val rawAccuracyDisplay: String
        get() = accuracyPercent?.let { String.format(Locale.US, "%.2f%%", it) } ?: "N/A"
}

data class LiveRadarValidationReport(
    // 1. Authoritative Sample Counts (Independent Contract Windows)
    val independentContractWindowsTotalN: Int,
    val independentContractWindowsSettledN: Int,
    val totalObservationsSnapshots: Int,
    val settledObservationsSnapshots: Int,
    val pendingObservationsSnapshots: Int,
    val upObservations: Int,
    val downObservations: Int,
    val noTradeObservations: Int,
    val wins: Int,
    val losses: Int,
    val overallAccuracyPercent: Double?,
    val overallStats: BucketAccuracy,

    // 2. Strong-Signal Hypothesis (Frozen Definition: Raw Score >= 70.0 || Raw Score <= 30.0)
    val strongSignalDefinition: String = "Raw Score >= 70.0 (Strong UP) or Raw Score <= 30.0 (Strong DOWN)",
    val strongSignalStats: BucketAccuracy,
    val strongUpStats: BucketAccuracy,
    val strongDownStats: BucketAccuracy,
    val moderateSignalStats: BucketAccuracy, // 50..70 (UP) or 30..50 (DOWN)
    val moderateUpStats: BucketAccuracy,     // 50 < Score < 70
    val moderateDownStats: BucketAccuracy,   // 30 < Score < 50
    val noTradeSignalStats: BucketAccuracy,

    // 3. Directional Breakdown
    val upDirectionStats: BucketAccuracy,
    val downDirectionStats: BucketAccuracy,

    // 4. Remaining-Time Breakdown (with mandatory near-outcome leakage disclaimer for Late window)
    val earlyWindowStats: BucketAccuracy,   // > 10m remaining (600s - 900s)
    val midWindowStats: BucketAccuracy,     // 5m - 10m remaining (300s - 600s)
    val lateWindowStats: BucketAccuracy,    // < 5m remaining (0s - 300s) [TIME-TO-SETTLEMENT OBSERVATION]

    // 5. Verification Baseline (Strictly Separated)
    val historicalBaselineAccuracy: Double = 46.27,
    val historicalBaselineN: Int = 2576,
    val historicalBaselineDataset: String = "Real 30-Day Coinbase BTC-USD 1m OHLCV (2024-01-01 to 2024-01-31)",
    val historicalBaselineExpectancy: String = "-6.73¢/trade (PF 0.76, Net -$173.28)",
    val baselineSha256Checksum: String = "83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358"
) {
    /**
     * Formats the authoritative audit report separating Section A (Baseline) and Section B (Live Validation).
     */
    fun toFormattedReportText(): String {
        return buildString {
            appendLine("================================================================================")
            appendLine("PHASE 3A: LIVE HEURISTIC RADAR VALIDATION SCORECARD & REPORT")
            appendLine("================================================================================")
            appendLine("STATUS: OBSERVATION ONLY • NO AUTONOMOUS EXECUTION • NO P&L CLAIMS")
            appendLine("FROZEN MODEL: v2.0-quant-multival (EMA 9/21, Momentum, RSI, Strike Buffer)")
            appendLine("CHECKSUM: $baselineSha256Checksum")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("SECTION A: AUTHORITATIVE HISTORICAL BENCHMARK (BASELINE)")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("• Dataset:                $historicalBaselineDataset")
            appendLine("• Sample Size:            N = $historicalBaselineN independent 15m contract windows")
            appendLine("• Baseline Accuracy:      ${String.format(Locale.US, "%.2f", historicalBaselineAccuracy)}% (No predictive edge over random chance)")
            appendLine("• Economic Expectancy:    $historicalBaselineExpectancy")
            appendLine("• Baseline Checksum:      $baselineSha256Checksum")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("SECTION B: LIVE RADAR VALIDATION SCORECARD (FROZEN EVALUATION)")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("• Independent Windows:     N = $independentContractWindowsSettledN settled contract windows (Total distinct windows: $independentContractWindowsTotalN)")
            appendLine("• Total Observational Logs:$totalObservationsSnapshots snapshots (Settled: $settledObservationsSnapshots, Pending: $pendingObservationsSnapshots)")
            appendLine("• Overall Live Accuracy:   ${if (independentContractWindowsSettledN < 100) "INSUFFICIENT (N=$independentContractWindowsSettledN < 100)" else overallAccuracyPercent?.let { String.format(Locale.US, "%.2f%%", it) } ?: "INSUFFICIENT SETTLED DATA"} (Wins: $wins, Losses: $losses)")
            appendLine("• Methodological Rule:     1 Contract Window = 1 Independent Observation. 10s snapshots do NOT inflate N.")
            appendLine()
            appendLine("LIVE SCORECARD TABLE:")
            appendLine(String.format(Locale.US, "%-20s | %-6s | %-9s | %-5s | %-6s | %-12s | %-24s", "Bucket", "N_win", "Snapshots", "Wins", "Losses", "Accuracy", "Gate Status"))
            appendLine("---------------------------------------------------------------------------------------------------")
            val allBuckets = listOf(
                strongUpStats,
                strongDownStats,
                moderateUpStats,
                moderateDownStats,
                noTradeSignalStats,
                earlyWindowStats,
                midWindowStats,
                lateWindowStats,
                overallStats
            )
            for (b in allBuckets) {
                appendLine(String.format(Locale.US, "%-20s | %-6d | %-9d | %-5d | %-6d | %-12s | %-24s",
                    b.bucketName.take(20),
                    b.settledIndependentWindowsCount,
                    b.totalCount,
                    b.wins,
                    b.losses,
                    b.rawAccuracyDisplay,
                    b.gateStatus.label
                ))
            }
            appendLine()
            appendLine("CRITICAL LATE-WINDOW DISCLOSURE:")
            appendLine("• Late Window (<5m): ${lateWindowStats.nearOutcomeDisclaimer}")
            appendLine()
            appendLine("================================================================================")
            appendLine("NOTE: Baseline A and Live Validation B must NEVER be aggregated into a single metric.")
            appendLine("================================================================================")
        }
    }
}

object LiveRadarValidationMetricsCalculator {

    fun calculate(observations: List<LiveObservationEntity>): LiveRadarValidationReport {
        val totalSnapshots = observations.size
        val distinctWindowsTotal = observations.map { it.contractWindowId }.distinct().size
        
        val upList = observations.filter { it.direction == "UP" }
        val downList = observations.filter { it.direction == "DOWN" }
        val noTradeList = observations.filter { it.direction == "NO TRADE" }

        val settled = observations.filter { it.actualOutcome == "WON" || it.actualOutcome == "LOST" }
        val pending = observations.filter { it.actualOutcome == "PENDING" }

        val distinctWindowsSettled = settled.map { it.contractWindowId }.distinct().size

        val wins = settled.count { it.actualOutcome == "WON" }
        val losses = settled.count { it.actualOutcome == "LOST" }
        val overallAccuracy = if (settled.isNotEmpty()) (wins.toDouble() / settled.size) * 100.0 else null

        // Helper to compute bucket accuracy with independent window count
        // For each bucket, we take at most ONE observation per contract window (the earliest/representative snapshot in that bucket)
        // so that accuracy reflects window-level performance
        fun computeBucket(
            name: String,
            subset: List<LiveObservationEntity>,
            isNearOutcome: Boolean = false
        ): BucketAccuracy {
            // Group by contractWindowId to enforce: ONE WINDOW = ONE SAMPLE
            val byWindow = subset.groupBy { it.contractWindowId }
            val distinctTotal = byWindow.size
            
            // For settled windows in this bucket, take the first snapshot outcome as representative of this window's evaluation
            val settledWindows = byWindow.filter { (_, snaps) ->
                snaps.any { it.actualOutcome == "WON" || it.actualOutcome == "LOST" }
            }
            val distinctSettled = settledWindows.size

            var windowWins = 0
            var windowLosses = 0
            for ((_, snaps) in settledWindows) {
                val firstSettled = snaps.firstOrNull { it.actualOutcome == "WON" || it.actualOutcome == "LOST" }
                if (firstSettled?.actualOutcome == "WON") windowWins++
                else if (firstSettled?.actualOutcome == "LOST") windowLosses++
            }

            val subsetSettledSnaps = subset.filter { it.actualOutcome == "WON" || it.actualOutcome == "LOST" }
            val acc = if (distinctSettled > 0) (windowWins.toDouble() / distinctSettled) * 100.0 else null

            return BucketAccuracy(
                bucketName = name,
                totalCount = subset.size,
                settledCount = subsetSettledSnaps.size,
                independentWindowsCount = distinctTotal,
                settledIndependentWindowsCount = distinctSettled,
                wins = windowWins,
                losses = windowLosses,
                accuracyPercent = acc,
                isNearOutcomeObservation = isNearOutcome
            )
        }

        // Strong UP: rawModelScore >= 70.0
        val strongUpList = observations.filter { it.rawModelScore >= 70.0 }
        val strongUpStats = computeBucket("Strong UP (>=70)", strongUpList)

        // Strong DOWN: rawModelScore <= 30.0
        val strongDownList = observations.filter { it.rawModelScore <= 30.0 }
        val strongDownStats = computeBucket("Strong DOWN (<=30)", strongDownList)

        // Composite Strong Signals
        val strongList = observations.filter { it.rawModelScore >= 70.0 || it.rawModelScore <= 30.0 }
        val strongStats = computeBucket("Strong All (>=70/<=30)", strongList)

        // Moderate UP: 50.0 < rawModelScore < 70.0
        val moderateUpList = observations.filter { it.rawModelScore > 50.0 && it.rawModelScore < 70.0 }
        val moderateUpStats = computeBucket("Moderate UP (50-70)", moderateUpList)

        // Moderate DOWN: 30.0 < rawModelScore < 50.0
        val moderateDownList = observations.filter { it.rawModelScore > 30.0 && it.rawModelScore < 50.0 }
        val moderateDownStats = computeBucket("Moderate DOWN (30-50)", moderateDownList)

        // Moderate Signals combined
        val moderateList = observations.filter { (it.rawModelScore in 30.01..<50.0) || (it.rawModelScore in 50.01..<70.0) }
        val modStats = computeBucket("Moderate All (30-70)", moderateList)

        // NO TRADE: 50.0 or stale data
        val noTradeStats = computeBucket("NO TRADE / Neutral", noTradeList)

        // Directional Buckets
        val upStats = computeBucket("UP Direction", upList)
        val downStats = computeBucket("DOWN Direction", downList)

        // Time Buckets (Late is flagged with near-outcome leakage disclaimer)
        val earlyList = observations.filter { it.timeRemainingSeconds > 600 }
        val midList = observations.filter { it.timeRemainingSeconds in 301..600 }
        val lateList = observations.filter { it.timeRemainingSeconds <= 300 }

        val earlyStats = computeBucket("Early (>10m)", earlyList)
        val midStats = computeBucket("Mid (5-10m)", midList)
        val lateStats = computeBucket("Late (<5m)*", lateList, isNearOutcome = true)

        val overallStats = computeBucket("Overall Population", observations)

        return LiveRadarValidationReport(
            independentContractWindowsTotalN = distinctWindowsTotal,
            independentContractWindowsSettledN = distinctWindowsSettled,
            totalObservationsSnapshots = totalSnapshots,
            settledObservationsSnapshots = settled.size,
            pendingObservationsSnapshots = pending.size,
            upObservations = upList.size,
            downObservations = downList.size,
            noTradeObservations = noTradeList.size,
            wins = wins,
            losses = losses,
            overallAccuracyPercent = overallAccuracy,
            overallStats = overallStats,
            strongSignalStats = strongStats,
            strongUpStats = strongUpStats,
            strongDownStats = strongDownStats,
            moderateSignalStats = modStats,
            moderateUpStats = moderateUpStats,
            moderateDownStats = moderateDownStats,
            noTradeSignalStats = noTradeStats,
            upDirectionStats = upStats,
            downDirectionStats = downStats,
            earlyWindowStats = earlyStats,
            midWindowStats = midStats,
            lateWindowStats = lateStats
        )
    }
}


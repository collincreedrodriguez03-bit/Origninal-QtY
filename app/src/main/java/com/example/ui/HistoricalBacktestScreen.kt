package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backtest.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalBacktestScreen(
    viewModel: TradingViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDaysRange by remember { mutableStateOf(7) }
    var selectedFriction by remember { mutableStateOf(ExecutionFrictionScenario.BASELINE) }
    var includeGeminiTrack by remember { mutableStateOf(false) }
    var useSyntheticBenchmarkMode by remember { mutableStateOf(false) }

    var isRunningBacktest by remember { mutableStateOf(false) }
    var backtestProgress by remember { mutableStateOf(0) }
    var backtestStatusText by remember { mutableStateOf("") }
    var backtestResult by remember { mutableStateOf<BacktestRunResult?>(null) }
    var selectedFilterTrack by remember { mutableStateOf("ALL") }

    val darkBackground = Color(0xFF0D0E12)
    val cardBackground = Color(0xFF14151D)
    val neonGreen = Color(0xFF00FF66)
    val neonRed = Color(0xFFFF3366)
    val cyanAccent = Color(0xFF00E5FF)
    val kalshiOrange = Color(0xFFFF9900)
    val purpleAccent = Color(0xFF9D00FF)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HISTORICAL BACKTESTER (P0-C)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Walk-Forward Replay • 5 Independent Tracks • Strict No-Lookahead",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Mandatory Legal & Methodological Disclosures
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1308)),
                    border = BorderStroke(1.dp, kalshiOrange.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = kalshiOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = "RESEARCH INTEGRITY & METHODOLOGY DISCLOSURES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = kalshiOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "• 1-minute OHLC historical data does not reveal the exact intraminute price path or exact moment the market crossed the strike.\n• All backtest executions represent SIMULATED EXECUTION — NOT HISTORICAL FILLS.\n• Settlement Convention: Kalshi Rule 5.1 (Above Strike) — YES wins if S_15 > S_0; NO wins if S_15 <= S_0 (FLAT settles NO).\n• Gemini historical results are labeled RETROSPECTIVE — CURRENT MODEL VERSION ONLY.\n• Confidence tiers (INSUFFICIENT / PRELIMINARY / CALIBRATION READY) indicate sample size readiness only, not proven edge.",
                            fontSize = 10.sp,
                            color = Color(0xFFE0D0B0),
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Backtest Parameter Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, Color(0xFF2A2B3D))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "BACKTEST CONFIGURATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = cyanAccent,
                            fontFamily = FontFamily.Monospace
                        )

                        // Data Source Mode Selection
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Dataset Mode:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { useSyntheticBenchmarkMode = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, if (!useSyntheticBenchmarkMode) neonGreen else Color(0xFF333344)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!useSyntheticBenchmarkMode) neonGreen.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = "Coinbase REST (Research)",
                                        fontSize = 10.sp,
                                        color = if (!useSyntheticBenchmarkMode) neonGreen else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                OutlinedButton(
                                    onClick = { useSyntheticBenchmarkMode = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, if (useSyntheticBenchmarkMode) kalshiOrange else Color(0xFF333344)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (useSyntheticBenchmarkMode) kalshiOrange.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = "Synthetic GBM (Dev Test)",
                                        fontSize = 10.sp,
                                        color = if (useSyntheticBenchmarkMode) kalshiOrange else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Date Range Options
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Lookback Window:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1 to "24 Hours", 3 to "3 Days", 7 to "7 Days", 14 to "14 Days", 30 to "30 Days").forEach { (days, label) ->
                                    val isSel = selectedDaysRange == days
                                    OutlinedButton(
                                        onClick = { selectedDaysRange = days },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, if (isSel) cyanAccent else Color(0xFF333344)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSel) cyanAccent.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = if (isSel) cyanAccent else Color.LightGray,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Execution Friction Scenario Options
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Execution Cost Scenario:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            ExecutionFrictionScenario.values().forEach { scenario ->
                                val isSel = selectedFriction == scenario
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFF1E2235) else Color(0xFF161722))
                                        .border(1.dp, if (isSel) purpleAccent else Color(0xFF2A2B3D), RoundedCornerShape(8.dp))
                                        .clickable { selectedFriction = scenario }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = scenario.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) purpleAccent else Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Total Drag: ${String.format("%.1f", scenario.totalFrictionCents)}¢ per contract fill",
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    RadioButton(
                                        selected = isSel,
                                        onClick = { selectedFriction = scenario },
                                        colors = RadioButtonDefaults.colors(selectedColor = purpleAccent)
                                    )
                                }
                            }
                        }

                        // Run Backtest Trigger Button
                        Button(
                            onClick = {
                                isRunningBacktest = true
                                backtestProgress = 0
                                backtestStatusText = "Initializing backtest engine..."
                                coroutineScope.launch {
                                    try {
                                        val now = System.currentTimeMillis()
                                        val startMs = now - (selectedDaysRange * 24 * 60 * 60 * 1000L)
                                        val (candles, metadata) = if (useSyntheticBenchmarkMode) {
                                            HistoricalDatasetManager.generateSyntheticBenchmarkDataset(
                                                startMs = startMs,
                                                endMs = now
                                            )
                                        } else {
                                            HistoricalDatasetManager.fetchCoinbaseCandles(
                                                startMs = startMs,
                                                endMs = now,
                                                onProgress = { p, msg ->
                                                    backtestProgress = (p * 0.4).toInt()
                                                    backtestStatusText = msg
                                                }
                                            )
                                        }

                                        val result = HistoricalBacktestEngine.executeBacktest(
                                            dataset = candles,
                                            metadata = metadata,
                                            frictionScenario = selectedFriction,
                                            runGeminiTrack = includeGeminiTrack,
                                            onProgress = { p, msg ->
                                                backtestProgress = 40 + (p * 0.6).toInt()
                                                backtestStatusText = msg
                                            }
                                        )
                                        backtestResult = result
                                        snackbarHostState.showSnackbar("Backtest completed: ${metadata.totalCandlesCount} 1m candles analyzed.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Backtest Error: ${e.message}")
                                    } finally {
                                        isRunningBacktest = false
                                    }
                                }
                            },
                            enabled = !isRunningBacktest,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (useSyntheticBenchmarkMode) kalshiOrange else cyanAccent)
                        ) {
                            if (isRunningBacktest) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$backtestProgress% — $backtestStatusText",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (useSyntheticBenchmarkMode) "EXECUTE SYNTHETIC TEST RUN" else "EXECUTE AUTHENTIC RESEARCH BACKTEST",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Results Overview & Checksum Card (If results ready)
            backtestResult?.let { result ->
                if (result.isSyntheticRun) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1111)),
                            border = BorderStroke(1.dp, neonRed)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = neonRed, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "SYNTHETIC TEST DATA — NOT REAL MARKET DATA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = neonRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "This run utilized offline deterministic benchmark candles. It is strictly segregated and cannot contribute to research metrics or sample size N.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFCCCC),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.dp, if (result.isSyntheticRun) kalshiOrange.copy(alpha = 0.6f) else neonGreen.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DATASET AUDIT METADATA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1F2937)
                                ) {
                                    Text(
                                        text = result.datasetMetadata.source,
                                        fontSize = 9.sp,
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Metrics Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("1-MIN CANDLES", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.datasetMetadata.totalCandlesCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("DATA GAPS", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.datasetMetadata.missingCandlesCount} missing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (result.datasetMetadata.missingCandlesCount > 0) kalshiOrange else neonGreen, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("FRICTION DRAG", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.frictionScenario.totalFrictionCents}¢/fill", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = purpleAccent, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // SHA-256 Checksum Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F1015), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF262838), RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(result.datasetMetadata.datasetSha256Checksum))
                                        coroutineScope.launch { snackbarHostState.showSnackbar("SHA-256 Checksum copied to clipboard!") }
                                    }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHA-256 DATASET CHECKSUM (Tap to Copy):", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = result.datasetMetadata.datasetSha256Checksum,
                                    fontSize = 10.sp,
                                    color = cyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 4. Five Independent Tracks Summary Cards
                item {
                    Text(
                        text = "FIVE INDEPENDENT EVALUATION TRACKS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val tracks = listOf(
                    result.trackA_Random,
                    result.trackB_Persistence,
                    result.trackC_Momentum,
                    result.trackD_Quantitative,
                    result.trackE_Gemini
                )

                items(tracks) { track ->
                    TrackPerformanceCard(track = track)
                }

                // 5. Calibration Reliability Section (if Track D is CALIBRATION_READY)
                if (result.trackD_Quantitative.dataConfidenceTier == DataConfidenceTier.CALIBRATION_READY && result.trackD_Quantitative.calibrationBins.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            border = BorderStroke(1.dp, neonGreen.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "EMPIRICAL CALIBRATION RELIABILITY (N ≥ 300)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Compares model predicted probability bucket against realized outcome win frequency.",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )

                                result.trackD_Quantitative.calibrationBins.forEach { bin ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(bin.binRangeLabel, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = "Expected: ${bin.expectedProbPercent.toInt()}% | Realized: ${String.format("%.1f", bin.realizedAccuracyPercent)}% (${bin.actualWinsInBin}/${bin.totalPredictionsInBin})",
                                            fontSize = 10.sp,
                                            color = if (bin.realizedAccuracyPercent >= bin.expectedProbPercent - 5.0) neonGreen else kalshiOrange,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Granular Trade-by-Trade Window Inspection
                item {
                    Text(
                        text = "WALK-FORWARD TRADE LOG (SAMPLE REVIEWS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val sampleTrades = result.trackD_Quantitative.predictionRecords.takeLast(15).reversed()
                items(sampleTrades) { trade ->
                    TradeAuditRecordCard(trade = trade)
                }
            }
        }
    }
}

@Composable
fun TrackPerformanceCard(track: TrackPerformanceSummary) {
    val cardBackground = Color(0xFF14151D)
    val neonGreen = Color(0xFF00FF66)
    val neonRed = Color(0xFFFF3366)
    val cyanAccent = Color(0xFF00E5FF)
    val kalshiOrange = Color(0xFFFF9900)

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            1.dp,
            when (track.dataConfidenceTier) {
                DataConfidenceTier.INSUFFICIENT -> Color(0xFF333344)
                DataConfidenceTier.PRELIMINARY -> kalshiOrange.copy(alpha = 0.5f)
                DataConfidenceTier.CALIBRATION_READY -> neonGreen.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = track.trackName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    track.retrospectiveBadge?.let { badge ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2E1A38)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                color = Color(0xFFE088FF),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    track.syntheticBadge?.let { badge ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF331414)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                color = neonRed,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Confidence Tier Badge
                when (track.dataConfidenceTier) {
                    DataConfidenceTier.INSUFFICIENT -> {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E28)) {
                            Text(
                                text = "INSUFFICIENT (N=${track.totalSettledCountN}/100)",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    DataConfidenceTier.PRELIMINARY -> {
                        Surface(shape = RoundedCornerShape(12.dp), color = kalshiOrange.copy(alpha = 0.15f)) {
                            Text(
                                text = "PRELIMINARY (N=${track.totalSettledCountN})",
                                fontSize = 9.sp,
                                color = kalshiOrange,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    DataConfidenceTier.CALIBRATION_READY -> {
                        Surface(shape = RoundedCornerShape(12.dp), color = neonGreen.copy(alpha = 0.15f)) {
                            Text(
                                text = "CALIBRATION READY (N=${track.totalSettledCountN})",
                                fontSize = 9.sp,
                                color = neonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Performance Metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ACCURACY RATE", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = track.accuracyPercent?.let { "${String.format("%.1f", it)}%" } ?: "INSUFFICIENT DATA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.accuracyPercent != null) neonGreen else Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text("WON / LOST / FLAT", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${track.wonCount}W / ${track.lostCount}L / ${track.noTradeCount}F",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("SIMULATED P&L", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${if (track.totalSimulatedPnlDollars >= 0) "+" else ""}$${String.format("%.2f", track.totalSimulatedPnlDollars)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.totalSimulatedPnlDollars >= 0) neonGreen else neonRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun TradeAuditRecordCard(trade: BacktestPredictionRecord) {
    val darkCard = Color(0xFF101117)
    val neonGreen = Color(0xFF00FF66)
    val neonRed = Color(0xFFFF3366)
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = darkCard),
        border = BorderStroke(1.dp, Color(0xFF1F212E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${dateFormat.format(Date(trade.windowStartTimestamp))} UTC",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "S0: $${String.format("%,.1f", trade.strikeReferencePrice)} → S15: $${String.format("%,.1f", trade.settlementPrice)}",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = trade.predictedDirection,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trade.predictedDirection == "BULLISH") neonGreen else if (trade.predictedDirection == "BEARISH") neonRed else Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Cost: ${String.format("%.1f", trade.simulatedCostCents)}¢",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (trade.actualOutcome == "WON") neonGreen.copy(alpha = 0.15f) else neonRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = trade.actualOutcome,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trade.actualOutcome == "WON") neonGreen else neonRed,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "${if (trade.simulatedNetProfitCents >= 0) "+" else ""}${String.format("%.1f", trade.simulatedNetProfitCents)}¢",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trade.simulatedNetProfitCents >= 0) neonGreen else neonRed,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

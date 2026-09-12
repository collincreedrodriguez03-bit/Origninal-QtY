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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backtest.*
import com.example.ui.theme.QtYColors
import com.example.ui.components.QtYHeader
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

    val darkBackground = QtYColors.Background
    val cardBackground = QtYColors.Surface
    val cardBorder = QtYColors.BorderDefault
    val neonGreen = QtYColors.BullishGreen
    val neonRed = QtYColors.BearishRed
    val cyanAccent = QtYColors.PrimaryCyan
    val kalshiOrange = QtYColors.WarningAmber
    val purpleAccent = QtYColors.SecondaryPurple

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QtYHeader(
                title = "HISTORICAL BACKTESTER (P0-C)",
                subtitle = "Walk-Forward Replay • 5 Independent Tracks",
                onOpenLiveRadar = onBack
            )
        },
        containerColor = darkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .testTag("backtest_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Mandatory Methodological Disclosures
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("backtest_disclosure_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141008)),
                    border = BorderStroke(1.dp, kalshiOrange.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = kalshiOrange, modifier = Modifier.size(14.dp))
                            Text(
                                text = "RESEARCH INTEGRITY & METHODOLOGY DISCLOSURES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = kalshiOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "• 1-minute OHLC historical data does not reveal intraminute price paths.\n• All backtest executions represent SIMULATED EXECUTION — NOT HISTORICAL FILLS.\n• Settlement: Kalshi Rule 5.1 (Above Strike) — FLAT settles NO.",
                            fontSize = 9.sp,
                            color = Color(0xFFD0C0A0),
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Backtest Parameter Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("backtest_config_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "BACKTEST CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = cyanAccent,
                            fontFamily = FontFamily.Monospace
                        )

                        // Data Source Mode Selection
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Dataset Mode:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { useSyntheticBenchmarkMode = false },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, if (!useSyntheticBenchmarkMode) neonGreen else cardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!useSyntheticBenchmarkMode) neonGreen.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = "Coinbase REST (Research)",
                                        fontSize = 9.sp,
                                        color = if (!useSyntheticBenchmarkMode) neonGreen else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                OutlinedButton(
                                    onClick = { useSyntheticBenchmarkMode = true },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, if (useSyntheticBenchmarkMode) kalshiOrange else cardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (useSyntheticBenchmarkMode) kalshiOrange.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = "Synthetic GBM (Dev Test)",
                                        fontSize = 9.sp,
                                        color = if (useSyntheticBenchmarkMode) kalshiOrange else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Date Range Options
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Lookback Window:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1 to "24H", 3 to "3D", 7 to "7D", 14 to "14D", 30 to "30D").forEach { (days, label) ->
                                    val isSel = selectedDaysRange == days
                                    OutlinedButton(
                                        onClick = { selectedDaysRange = days },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                        border = BorderStroke(1.dp, if (isSel) cyanAccent else cardBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSel) cyanAccent.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = if (isSel) cyanAccent else Color.LightGray,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Execution Friction Scenario Options
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Execution Cost Scenario:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            ExecutionFrictionScenario.values().forEach { scenario ->
                                val isSel = selectedFriction == scenario
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF161A26) else Color(0xFF0E1017))
                                        .border(1.dp, if (isSel) purpleAccent else cardBorder, RoundedCornerShape(6.dp))
                                        .clickable { selectedFriction = scenario }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = scenario.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) purpleAccent else Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Drag: ${String.format("%.1f", scenario.totalFrictionCents)}¢ per contract",
                                            fontSize = 8.sp,
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
                                        snackbarHostState.showSnackbar("Backtest completed successfully.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Backtest Error: ${e.message}")
                                    } finally {
                                        isRunningBacktest = false
                                    }
                                }
                            },
                            enabled = !isRunningBacktest,
                            modifier = Modifier.fillMaxWidth().testTag("run_backtest_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (useSyntheticBenchmarkMode) kalshiOrange else cyanAccent)
                        ) {
                            if (isRunningBacktest) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$backtestProgress% — $backtestStatusText",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (useSyntheticBenchmarkMode) "EXECUTE SYNTHETIC TEST RUN" else "EXECUTE RESEARCH BACKTEST",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF260E0E)),
                            border = BorderStroke(1.dp, neonRed)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = neonRed, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = "SYNTHETIC TEST DATASET ACTIVE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = neonRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Utilizes offline benchmark candles. Segregated from production research metrics.",
                                        fontSize = 9.sp,
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
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.dp, if (result.isSyntheticRun) kalshiOrange.copy(alpha = 0.5f) else neonGreen.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DATASET AUDIT METADATA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF161A26)
                                ) {
                                    Text(
                                        text = result.datasetMetadata.source,
                                        fontSize = 8.sp,
                                        color = cyanAccent,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Metrics Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("1-MIN CANDLES", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.datasetMetadata.totalCandlesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("DATA GAPS", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.datasetMetadata.missingCandlesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (result.datasetMetadata.missingCandlesCount > 0) kalshiOrange else neonGreen, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("FRICTION DRAG", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text("${result.frictionScenario.totalFrictionCents}¢", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = purpleAccent, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // SHA-256 Checksum Box (Terminal Style)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF090B10), RoundedCornerShape(6.dp))
                                    .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(result.datasetMetadata.datasetSha256Checksum))
                                        coroutineScope.launch { snackbarHostState.showSnackbar("SHA-256 Checksum copied!") }
                                    }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHA-256 CHECKSUM (Tap to Copy):", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                                }
                                Text(
                                    text = result.datasetMetadata.datasetSha256Checksum,
                                    fontSize = 9.sp,
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
                        fontSize = 11.sp,
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
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            border = BorderStroke(1.dp, neonGreen.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "EMPIRICAL CALIBRATION RELIABILITY (N ≥ 300)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Model predicted probability vs. realized outcome win frequency.",
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )

                                result.trackD_Quantitative.calibrationBins.forEach { bin ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(bin.binRangeLabel, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = "Exp: ${bin.expectedProbPercent.toInt()}% | Real: ${String.format("%.1f", bin.realizedAccuracyPercent)}% (${bin.actualWinsInBin}/${bin.totalPredictionsInBin})",
                                            fontSize = 9.sp,
                                            color = if (bin.realizedAccuracyPercent >= bin.expectedProbPercent - 5.0) neonGreen else kalshiOrange,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Granular Trade-by-Trade Window Inspection (Terminal Log Feed)
                item {
                    Text(
                        text = "WALK-FORWARD TRADE LOG (TERMINAL FEED)",
                        fontSize = 11.sp,
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
    val cardBackground = QtYColors.Surface
    val cardBorder = QtYColors.BorderDefault
    val neonGreen = QtYColors.BullishGreen
    val neonRed = QtYColors.BearishRed
    val cyanAccent = QtYColors.PrimaryCyan
    val kalshiOrange = QtYColors.WarningAmber

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            1.dp,
            when (track.dataConfidenceTier) {
                DataConfidenceTier.INSUFFICIENT -> cardBorder
                DataConfidenceTier.PRELIMINARY -> kalshiOrange.copy(alpha = 0.5f)
                DataConfidenceTier.CALIBRATION_READY -> neonGreen.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = track.trackName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    track.retrospectiveBadge?.let { badge ->
                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF261230)) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                color = Color(0xFFE088FF),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    track.syntheticBadge?.let { badge ->
                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF2E1010)) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                color = neonRed,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Confidence Tier Badge
                when (track.dataConfidenceTier) {
                    DataConfidenceTier.INSUFFICIENT -> {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF161822)) {
                            Text(
                                text = "INSUFFICIENT (N=${track.totalSettledCountN}/100)",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    DataConfidenceTier.PRELIMINARY -> {
                        Surface(shape = RoundedCornerShape(10.dp), color = kalshiOrange.copy(alpha = 0.15f)) {
                            Text(
                                text = "PRELIMINARY (N=${track.totalSettledCountN})",
                                fontSize = 8.sp,
                                color = kalshiOrange,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    DataConfidenceTier.CALIBRATION_READY -> {
                        Surface(shape = RoundedCornerShape(10.dp), color = neonGreen.copy(alpha = 0.15f)) {
                            Text(
                                text = "CALIBRATION READY (N=${track.totalSettledCountN})",
                                fontSize = 8.sp,
                                color = neonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Compact Performance Metrics Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ACCURACY RATE", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = track.accuracyPercent?.let { "${String.format("%.1f", it)}%" } ?: "N/A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.accuracyPercent != null) neonGreen else Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text("WON / LOST / FLAT", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${track.wonCount}W / ${track.lostCount}L / ${track.noTradeCount}F",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("SIMULATED P&L", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${if (track.totalSimulatedPnlDollars >= 0) "+" else ""}$${String.format("%.2f", track.totalSimulatedPnlDollars)}",
                        fontSize = 12.sp,
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
    val terminalCard = Color(0xFF0A0C10)
    val neonGreen = QtYColors.BullishGreen
    val neonRed = QtYColors.BearishRed
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = terminalCard),
        border = BorderStroke(1.dp, QtYColors.BorderDefault)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${dateFormat.format(Date(trade.windowStartTimestamp))} UTC",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "S0: $${String.format("%,.1f", trade.strikeReferencePrice)} → S15: $${String.format("%,.1f", trade.settlementPrice)}",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = trade.predictedDirection,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trade.predictedDirection == "BULLISH") neonGreen else if (trade.predictedDirection == "BEARISH") neonRed else Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Cost: ${String.format("%.1f", trade.simulatedCostCents)}¢",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (trade.actualOutcome == "WON") neonGreen.copy(alpha = 0.15f) else neonRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = trade.actualOutcome,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trade.actualOutcome == "WON") neonGreen else neonRed,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "${if (trade.simulatedNetProfitCents >= 0) "+" else ""}${String.format("%.1f", trade.simulatedNetProfitCents)}¢",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trade.simulatedNetProfitCents >= 0) neonGreen else neonRed,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

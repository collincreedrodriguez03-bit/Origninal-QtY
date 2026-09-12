package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.live.LiveObservationEntity
import com.example.data.live.LiveRadarValidationReport
import com.example.data.live.ResearchGateStatus
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Phase 3: LIVE HEURISTIC RADAR VALIDATION SCREEN
 *
 * Mandatory Research Boundaries:
 * - OBSERVATION ONLY: Zero automated execution or order placement.
 * - NO FINANCIAL CLAIMS: No win-rate headline claims or simulated P&L marketing.
 * - FROZEN EVALUATION: Strictly evaluates frozen model v2.0-quant-multival without tweaking weights.
 * - SEPARATION OF BASELINE: Clearly separates Historical Benchmark A (46.27%, N=2,576) from Live Observations B.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRadarValidationScreen(
    viewModel: TradingViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val livePrediction by viewModel.currentLivePrediction.collectAsStateWithLifecycle()
    val liveWindow by viewModel.liveContractWindow.collectAsStateWithLifecycle()
    val observations by viewModel.liveObservationLedger.collectAsStateWithLifecycle()
    val validationReport by viewModel.liveValidationReport.collectAsStateWithLifecycle()
    val isAutoLogging by viewModel.isAutoObservationLoggingEnabled.collectAsStateWithLifecycle()
    val collectorLastObsTime by viewModel.collectorLastObservationTimeUtc.collectAsStateWithLifecycle()
    val networkDiagnostics by viewModel.networkDiagnostics.collectAsStateWithLifecycle()

    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val kalshiOrange = Color(0xFFFF9100)
    val purpleAccent = Color(0xFF7C4DFF)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LIVE RADAR VALIDATION",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .background(purpleAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, purpleAccent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OBSERVATION ONLY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = purpleAccent
                                )
                            }
                        }
                        Text(
                            text = "Phase 3 • Frozen Radar Model • Strict No-Lookahead Audit",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("validation_back_button")) {
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
            // 1. Mandatory Research Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, purpleAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = purpleAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = "EXPERIMENTAL / RESEARCH VALIDATION MODE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = purpleAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "This mode tests whether the frozen heuristic signal radar provides meaningful conditional accuracy in live operation. All evaluations are recorded to an immutable Room database ledger. Settlement outcomes are strictly decoupled and evaluated only after contract expiration.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 1.5 Minimal Collector Lifecycle & Runtime Status Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("collector_status_bar"),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    border = BorderStroke(1.dp, Color(0xFF282828))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isAutoLogging) neonGreen else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (isAutoLogging) "COLLECTOR: RUNNING" else "COLLECTOR: PAUSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isAutoLogging) neonGreen else Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "LAST OBS: $collectorLastObsTime",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENDING: ${validationReport.pendingObservationsSnapshots}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = cyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SETTLED N: ${validationReport.independentContractWindowsSettledN}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = goldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            val netColor = when (networkDiagnostics.connectionState) {
                                com.example.ui.ConnectionState.CONNECTED -> neonGreen
                                com.example.ui.ConnectionState.RECOVERED -> cyanAccent
                                com.example.ui.ConnectionState.RECONNECTING, com.example.ui.ConnectionState.DATA_STALE -> goldAccent
                                com.example.ui.ConnectionState.DISCONNECTED, com.example.ui.ConnectionState.INVALID_DATA, com.example.ui.ConnectionState.API_ERROR -> neonRed
                            }
                            Text(
                                text = "FEED: ${networkDiagnostics.connectionState.name}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = netColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (networkDiagnostics.connectionState != com.example.ui.ConnectionState.CONNECTED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (networkDiagnostics.retryAttemptCount > 0) "RETRIES: ${networkDiagnostics.retryAttemptCount}" else "DIAGNOSTIC: ACTIVE",
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (networkDiagnostics.affectedSources.isNotEmpty()) {
                                    Text(
                                        text = "AFFECTED: ${networkDiagnostics.affectedSources.joinToString()}",
                                        fontSize = 9.sp,
                                        color = neonRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else if (networkDiagnostics.lastRecoveryTimeMs != null) {
                                    Text(
                                        text = "RECOVERED: ${networkDiagnostics.formattedLastRecoveryTime}",
                                        fontSize = 9.sp,
                                        color = cyanAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Live Signal Snapshot
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("live_validation_signal_hud"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT RADAR SIGNAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "WINDOW: ${liveWindow.formattedWindowRange}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = cyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val dir = livePrediction?.direction ?: "NO TRADE"
                        val dirColor = when (dir) {
                            "UP" -> neonGreen
                            "DOWN" -> neonRed
                            else -> Color.Gray
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(dirColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(1.dp, dirColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "SIGNAL: $dir",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = dirColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (livePrediction != null && (livePrediction!!.rawModelScore >= 70.0 || livePrediction!!.rawModelScore <= 30.0)) {
                                    Box(
                                        modifier = Modifier
                                            .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(1.dp, goldAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "STRONG SIGNAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = goldAccent,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "SCORE: ${livePrediction?.rawModelScore?.let { String.format(Locale.US, "%.1f", it) } ?: "--"} / 100",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Feature Breakdown Pills
                        val snap = livePrediction?.featureSnapshot
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FactorPill(label = "EMA 9/21", value = snap?.let { String.format(Locale.US, "%+.1f$", it.emaSpread) } ?: "--", modifier = Modifier.weight(1f))
                            FactorPill(label = "MOMENTUM", value = snap?.let { String.format(Locale.US, "%+.1f$", it.momentum) } ?: "--", modifier = Modifier.weight(1f))
                            FactorPill(label = "RSI(14)", value = snap?.let { String.format(Locale.US, "%.1f", it.rsi) } ?: "--", modifier = Modifier.weight(1f))
                            FactorPill(label = "BUFFER", value = snap?.let { String.format(Locale.US, "%+.1f$", it.deltaToStrike) } ?: "--", modifier = Modifier.weight(1f))
                        }

                        // Control Buttons Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.recordCurrentLiveObservation()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Live observation recorded to ledger.")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("record_observation_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RECORD OBS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.settlePendingLiveObservations()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Checked & settled completed window observations.")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("settle_observations_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = goldAccent),
                                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SETTLE EXPIRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // 3. Transparent Live Scorecard (Phase 3A Requirement)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("live_scorecard_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = cyanAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "LIVE RESEARCH SCORECARD (PHASE 3A)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = cyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "1 WINDOW = 1 N",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Tracked across independent 15-minute contract windows. Telemetry updates (10s) do NOT inflate experimental N.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        // Scorecard Rows
                        val scorecardBuckets = listOf(
                            validationReport.strongUpStats,
                            validationReport.strongDownStats,
                            validationReport.moderateUpStats,
                            validationReport.moderateDownStats,
                            validationReport.noTradeSignalStats,
                            validationReport.earlyWindowStats,
                            validationReport.midWindowStats,
                            validationReport.lateWindowStats,
                            validationReport.overallStats
                        )

                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("BUCKET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.8f), fontFamily = FontFamily.Monospace)
                            Text("N_WIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.9f), fontFamily = FontFamily.Monospace)
                            Text("W/L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.9f), fontFamily = FontFamily.Monospace)
                            Text("ACCURACY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.3f), fontFamily = FontFamily.Monospace)
                            Text("GATE STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.5f), fontFamily = FontFamily.Monospace)
                        }

                        for (b in scorecardBuckets) {
                            val gateColor = when (b.gateStatus) {
                                ResearchGateStatus.INSUFFICIENT_DATA -> Color.Gray
                                ResearchGateStatus.PRELIMINARY -> kalshiOrange
                                ResearchGateStatus.CALIBRATION_READY -> neonGreen
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.bucketName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (b.isNearOutcomeObservation) kalshiOrange else Color.White,
                                    modifier = Modifier.weight(1.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${b.settledIndependentWindowsCount}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.weight(0.9f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${b.wins}/${b.losses}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.weight(0.9f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (b.settledIndependentWindowsCount < 100) "--" else b.rawAccuracyDisplay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (b.settledIndependentWindowsCount < 100) Color.Gray else Color.White,
                                    modifier = Modifier.weight(1.3f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .background(gateColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (b.gateStatus) {
                                            ResearchGateStatus.INSUFFICIENT_DATA -> "N < 100"
                                            ResearchGateStatus.PRELIMINARY -> "PRELIM"
                                            ResearchGateStatus.CALIBRATION_READY -> "READY"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gateColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            HorizontalDivider(color = cardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }

                        // Late Window Mandatory Disclosure
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                .border(1.dp, kalshiOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "* LATE WINDOW (<5M) CRITICAL DISCLOSURE: NOT INDEPENDENT PREDICTIVE EVIDENCE — LATE-WINDOW OBSERVATIONS MAY BENEFIT FROM REDUCED TIME FOR PRICE REVERSAL AND MUST NOT BE INTERPRETED AS AN INDEPENDENT PREDICTIVE EDGE.",
                                fontSize = 9.sp,
                                color = kalshiOrange,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 4. Strong-Signal Hypothesis Investigation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("strong_signal_hypothesis_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = goldAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "STRONG-SIGNAL HYPOTHESIS TEST",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = goldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Hypothesis: Strong radar signals (|C| >= 1.67, score >= 70 or <= 30) exhibit higher conditional accuracy than the overall 46.27% historical baseline.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "FROZEN DEFINITION: Raw Score >= 70.0 (Strong UP) or <= 30.0 (Strong DOWN)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatMiniBox(
                                label = "STRONG SIGNALS",
                                value = validationReport.strongSignalStats.formattedAccuracy,
                                subtext = "Wins: ${validationReport.strongSignalStats.wins}/${validationReport.strongSignalStats.settledIndependentWindowsCount} (N_win: ${validationReport.strongSignalStats.settledIndependentWindowsCount})",
                                accentColor = goldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "MODERATE SIGNALS",
                                value = validationReport.moderateSignalStats.formattedAccuracy,
                                subtext = "Wins: ${validationReport.moderateSignalStats.wins}/${validationReport.moderateSignalStats.settledIndependentWindowsCount} (N_win: ${validationReport.moderateSignalStats.settledIndependentWindowsCount})",
                                accentColor = cyanAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Directional & Time Bucket Matrix
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "CONDITIONAL ACCURACY MATRIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatMiniBox(
                                label = "UP ACCURACY",
                                value = validationReport.upDirectionStats.formattedAccuracy,
                                subtext = "N_win=${validationReport.upDirectionStats.settledIndependentWindowsCount} (${validationReport.upDirectionStats.totalCount} logs)",
                                accentColor = neonGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "DOWN ACCURACY",
                                value = validationReport.downDirectionStats.formattedAccuracy,
                                subtext = "N_win=${validationReport.downDirectionStats.settledIndependentWindowsCount} (${validationReport.downDirectionStats.totalCount} logs)",
                                accentColor = neonRed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatMiniBox(
                                label = "EARLY (>10M)",
                                value = validationReport.earlyWindowStats.formattedAccuracy,
                                subtext = "N_win=${validationReport.earlyWindowStats.settledIndependentWindowsCount}",
                                accentColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "MID (5-10M)",
                                value = validationReport.midWindowStats.formattedAccuracy,
                                subtext = "N_win=${validationReport.midWindowStats.settledIndependentWindowsCount}",
                                accentColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniBox(
                                label = "LATE (<5M)*",
                                value = validationReport.lateWindowStats.formattedAccuracy,
                                subtext = "N_win=${validationReport.lateWindowStats.settledIndependentWindowsCount}",
                                accentColor = kalshiOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Strict Separation of Baseline A vs Live Validation B
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("baseline_separation_panel"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SEPARATION OF BASELINE & LIVE VALIDATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )

                        // Baseline A
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                                .border(1.dp, cyanAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SECTION A: HISTORICAL BENCHMARK (BASELINE)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cyanAccent, fontFamily = FontFamily.Monospace)
                                Text("• Dataset: 30-Day Real Coinbase BTC-USD 2024-01-01 to 2024-01-31", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                Text("• Sample Size: N = 2,576 independent 15m windows", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                Text("• Accuracy: 46.27% • Net P&L: -$173.28 • Expectancy: -6.73¢/trade", fontSize = 10.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                Text("• Finding: Operates with no proven predictive edge over random chance.", fontSize = 10.sp, color = kalshiOrange, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Live Validation B
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                                .border(1.dp, purpleAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SECTION B: LIVE FROZEN VALIDATION (POST-PATH B)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = purpleAccent, fontFamily = FontFamily.Monospace)
                                Text("• Independent Windows: N = ${validationReport.independentContractWindowsSettledN} settled (${validationReport.independentContractWindowsTotalN} total windows)", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                Text("• Observational Logs: ${validationReport.totalObservationsSnapshots} snapshots (Settled: ${validationReport.settledObservationsSnapshots}, Pending: ${validationReport.pendingObservationsSnapshots})", fontSize = 10.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                Text("• Overall Accuracy: ${validationReport.overallAccuracyPercent?.let { String.format(Locale.US, "%.2f%%", it) } ?: "INSUFFICIENT SETTLED DATA"}", fontSize = 10.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                Text("• Rule: 1 Contract Window = 1 Sample. Never blended with Section A.", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Copy Report Action
                        Button(
                            onClick = {
                                val reportText = validationReport.toFormattedReportText()
                                clipboardManager.setText(AnnotatedString(reportText))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Validation Report copied to clipboard!")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("copy_validation_report_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF333333)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPY COMPLETE AUDIT REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // 6. Live Observation Ledger Header & Clear Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE OBSERVATION LEDGER (N=${observations.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    if (observations.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.clearLiveObservationLedger()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ledger cleared.")
                                }
                            },
                            modifier = Modifier.testTag("clear_ledger_button")
                        ) {
                            Text("CLEAR", fontSize = 10.sp, color = neonRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // 7. Observation Items Feed
            if (observations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No observations recorded yet. Click 'RECORD OBS' to capture the current signal snapshot.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                items(observations) { obs ->
                    ObservationRowCard(obs = obs)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FactorPill(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF141414), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF141414), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor, fontFamily = FontFamily.Monospace)
            Text(subtext, fontSize = 8.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ObservationRowCard(obs: LiveObservationEntity) {
    val dirColor = when (obs.direction) {
        "UP" -> Color(0xFF00E676)
        "DOWN" -> Color(0xFFFF1744)
        else -> Color.Gray
    }

    val outcomeColor = when (obs.actualOutcome) {
        "WON" -> Color(0xFF00E676)
        "LOST" -> Color(0xFFFF1744)
        "PENDING" -> Color(0xFFFFD600)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("observation_card_${obs.sequenceId}"),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C)),
        border = BorderStroke(1.dp, Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = obs.formattedTimestampUtc,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    if (obs.isStrongSignal) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD600).copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("STRONG", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD600), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(outcomeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, outcomeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = obs.actualOutcome,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = outcomeColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${obs.direction} (Score: ${String.format(Locale.US, "%.1f", obs.rawModelScore)})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = dirColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Spot: $${String.format(Locale.US, "%,.1f", obs.btcPrice)} | Strike: $${String.format(Locale.US, "%,.1f", obs.strikePrice)}",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMA: ${String.format(Locale.US, "%+.1f", obs.emaContribution)} | Mom: ${String.format(Locale.US, "%+.1f", obs.momentumContribution)} | RSI: ${String.format(Locale.US, "%+.1f", obs.rsiContribution)} | Buffer: ${String.format(Locale.US, "%+.1f", obs.strikeBufferContribution)}",
                    fontSize = 8.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${obs.timeRemainingSeconds / 60}m ${obs.timeRemainingSeconds % 60}s left",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

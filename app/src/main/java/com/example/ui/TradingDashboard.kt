package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.live.LiveContractWindow
import com.example.data.live.LivePredictionLogRecord
import com.example.data.live.LiveContributingFactor
import com.example.data.live.NetworkDiagnostics
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDashboard(viewModel: TradingViewModel) {
    val btcPrice by viewModel.btcPrice.collectAsStateWithLifecycle()
    val priceChange24h by viewModel.priceChange24h.collectAsStateWithLifecycle()
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()
    val activePrediction by viewModel.activePrediction.collectAsStateWithLifecycle()
    val isPredicting by viewModel.isPredicting.collectAsStateWithLifecycle()
    val overallAccuracyRate by viewModel.overallAccuracyRate.collectAsStateWithLifecycle()
    val settledPredictionsCount by viewModel.settledPredictionsCount.collectAsStateWithLifecycle()
    val dataConfidenceTier by viewModel.dataConfidenceTier.collectAsStateWithLifecycle()
    val platformTradeOptions by viewModel.platformTradeOptions.collectAsStateWithLifecycle()
    val selectedPlatformFilter by viewModel.selectedPlatformFilter.collectAsStateWithLifecycle()
    val selectedGraphTimeframe by viewModel.selectedGraphTimeframe.collectAsStateWithLifecycle()

    // MTF Trend Confirmation, Volume Delta, and Kalshi 10-Contract Tracker
    val mtfTrend by viewModel.mtfTrend.collectAsStateWithLifecycle()
    val volumeDeltaHistory by viewModel.volumeDeltaHistory.collectAsStateWithLifecycle()
    val currentVolumeDelta by viewModel.currentVolumeDelta.collectAsStateWithLifecycle()
    val kalshiCycles by viewModel.kalshiCycles.collectAsStateWithLifecycle()
    val kalshiTrendAnalysis by viewModel.kalshiTrendAnalysis.collectAsStateWithLifecycle()
    val kalshiActiveContract by viewModel.kalshiActiveContract.collectAsStateWithLifecycle()
    val confidenceAnalysis by viewModel.confidenceAnalysis.collectAsStateWithLifecycle()

    // Technical indicators
    val rsi by viewModel.rsi.collectAsStateWithLifecycle()
    val rsiHistory by viewModel.rsiHistory.collectAsStateWithLifecycle()
    val momentum by viewModel.momentum.collectAsStateWithLifecycle()
    val momentumHistory by viewModel.momentumHistory.collectAsStateWithLifecycle()
    val ema9 by viewModel.ema9.collectAsStateWithLifecycle()
    val ema21 by viewModel.ema21.collectAsStateWithLifecycle()
    val volatility by viewModel.volatility.collectAsStateWithLifecycle()
    val marketRegime by viewModel.marketRegime.collectAsStateWithLifecycle()

    // Settings
    val selectedHorizon by viewModel.selectedHorizon.collectAsStateWithLifecycle()
    val predictionHorizon by viewModel.predictionHorizon.collectAsStateWithLifecycle()
    val minConfidenceFilter by viewModel.minConfidenceFilter.collectAsStateWithLifecycle()
    val refreshIntervalSeconds by viewModel.refreshIntervalSeconds.collectAsStateWithLifecycle()
    val autoForecastEnabled by viewModel.autoForecastEnabled.collectAsStateWithLifecycle()
    val forecastCycleCountdown by viewModel.forecastCycleCountdown.collectAsStateWithLifecycle()
    val paperBotState by viewModel.paperBotState.collectAsStateWithLifecycle()

    // Phase 2 Live Prediction Flows
    val currentLivePrediction by viewModel.currentLivePrediction.collectAsStateWithLifecycle()
    val livePredictionHistory by viewModel.livePredictionHistory.collectAsStateWithLifecycle()
    val liveContractWindow by viewModel.liveContractWindow.collectAsStateWithLifecycle()
    val live10sCountdown by viewModel.live10sCountdown.collectAsStateWithLifecycle()
    val lastPriceFetchTimeMs by viewModel.lastPriceFetchTimeMs.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    // Multi-Market Data Feeds (Coinbase, Kraken, Kalshi)
    val coinbaseQuote by viewModel.coinbaseQuote.collectAsStateWithLifecycle()
    val krakenQuote by viewModel.krakenQuote.collectAsStateWithLifecycle()
    val spotSpread by viewModel.spotSpread.collectAsStateWithLifecycle()
    val kalshiQuote by viewModel.kalshiQuote.collectAsStateWithLifecycle()
    val validationReport by viewModel.liveValidationReport.collectAsStateWithLifecycle()
    val isAutoLogging by viewModel.isAutoObservationLoggingEnabled.collectAsStateWithLifecycle()
    val networkDiagnostics by viewModel.networkDiagnostics.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showLiveValidationScreen by remember { mutableStateOf(false) }

    // Pure Pitch Black Palette with High-Contrast Vibrant Section Colors
    val terminalDark = Color(0xFF000000)
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val kalshiOrange = Color(0xFFFF9100)
    val purpleAccent = Color(0xFF7C4DFF)

    if (showLiveValidationScreen) {
        LiveRadarValidationScreen(
            viewModel = viewModel,
            onBack = { showLiveValidationScreen = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(cyanAccent.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, cyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "Logo",
                                    tint = cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "BTC QUANT FORECAST",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = "Instant Look & Bet Engine",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLiveValidationScreen = true },
                            modifier = Modifier.testTag("open_live_validation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Live Radar Validation",
                                tint = purpleAccent
                            )
                        }
                        // Explicit Data-Confidence Tier Badge
                    when (dataConfidenceTier) {
                        DataConfidenceTier.INSUFFICIENT -> {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "INSUFFICIENT DATA (N=$settledPredictionsCount/100)",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        DataConfidenceTier.PRELIMINARY -> {
                            val accText = overallAccuracyRate?.let { "${String.format("%.1f", it)}%" } ?: "CALC"
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(kalshiOrange.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .border(1.dp, kalshiOrange.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = kalshiOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "PRELIMINARY: $accText (N=$settledPredictionsCount)",
                                        color = kalshiOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        DataConfidenceTier.CALIBRATION_READY -> {
                            val accText = overallAccuracyRate?.let { "${String.format("%.1f", it)}%" } ?: "CALC"
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(neonGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .border(1.dp, neonGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = neonGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "CALIBRATION READY: $accText (N=$settledPredictionsCount)",
                                        color = neonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = terminalDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = terminalDark,
                modifier = Modifier
                    .border(BorderStroke(1.dp, cardBorder))
                    .testTag("navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "Predict") },
                    label = { Text("PREDICT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cyanAccent,
                        selectedTextColor = cyanAccent,
                        indicatorColor = Color(0xFF141414),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_predict").testTag("tab_terminal")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                    label = { Text("ANALYSIS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = purpleAccent,
                        selectedTextColor = purpleAccent,
                        indicatorColor = Color(0xFF141414),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_analysis").testTag("tab_confidence")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Market & History") },
                    label = { Text("MARKET/HISTORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = goldAccent,
                        selectedTextColor = goldAccent,
                        indicatorColor = Color(0xFF141414),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_market_history").testTag("tab_graphs").testTag("tab_backtest")
                )
            }
        },
        containerColor = terminalDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(terminalDark)
        ) {
            when (selectedTab) {
                0 -> TerminalScreen(
                    btcPrice = btcPrice,
                    priceChange24h = priceChange24h,
                    priceHistory = priceHistory,
                    prediction = activePrediction,
                    isPredicting = isPredicting,
                    marketRegime = marketRegime,
                    accuracyRate = overallAccuracyRate,
                    autoForecastEnabled = autoForecastEnabled,
                    forecastCycleCountdown = forecastCycleCountdown,
                    minConfidenceFilter = minConfidenceFilter,
                    selectedHorizon = selectedHorizon,
                    mtfTrend = mtfTrend,
                    volumeDeltaHistory = volumeDeltaHistory,
                    currentVolumeDelta = currentVolumeDelta,
                    kalshiCycles = kalshiCycles,
                    kalshiTrendAnalysis = kalshiTrendAnalysis,
                    kalshiActiveContract = kalshiActiveContract,
                    livePrediction = currentLivePrediction,
                    liveHistory = livePredictionHistory,
                    liveWindow = liveContractWindow,
                    live10sCountdown = live10sCountdown,
                    connectionState = connectionState,
                    lastPriceFetchTimeMs = lastPriceFetchTimeMs,
                    coinbaseQuote = coinbaseQuote,
                    krakenQuote = krakenQuote,
                    spotSpread = spotSpread,
                    kalshiQuote = kalshiQuote,
                    isAutoLogging = isAutoLogging,
                    pendingObsCount = validationReport.pendingObservationsSnapshots,
                    settledN = validationReport.independentContractWindowsSettledN,
                    networkDiagnostics = networkDiagnostics,
                    onTriggerPredict = { viewModel.recalculateForecast() },
                    onTrigger10sPredict = { viewModel.run10SecondLiveEvaluation() },
                    onToggleAutoForecast = { viewModel.toggleAutoForecast() },
                    onSelectConfidence = { viewModel.setMinConfidenceFilter(it) },
                    onSelectHorizon = { viewModel.setPredictionHorizon(it) },
                    onSyncKalshiStrike = { viewModel.syncKalshiTargetWithSpot(it) },
                    onSetCustomKalshiStrike = { viewModel.setCustomKalshiStrike(it) },
                    onResetToStrike = { viewModel.resetToStrike() },
                    onToggleKalshiLock = { viewModel.toggleKalshiAutoLock() }
                )
                1 -> AdvancedAnalysisScreen(
                    btcPrice = btcPrice,
                    livePrediction = currentLivePrediction,
                    liveWindow = liveContractWindow,
                    mtfTrend = mtfTrend,
                    rsi = rsi,
                    rsiHistory = rsiHistory,
                    momentum = momentum,
                    momentumHistory = momentumHistory,
                    ema9 = ema9,
                    ema21 = ema21,
                    volatility = volatility,
                    marketRegime = marketRegime,
                    confidenceAnalysis = confidenceAnalysis,
                    selectedHorizon = selectedHorizon,
                    forecastCycleCountdown = forecastCycleCountdown,
                    settledPredictionsCount = settledPredictionsCount,
                    dataConfidenceTier = dataConfidenceTier,
                    onTriggerRecalculate = { viewModel.recalculateForecast() },
                    onSelectHorizon = { viewModel.setPredictionHorizon(it) }
                )
                2 -> MarketAndHistoryScreen(
                    viewModel = viewModel,
                    btcPrice = btcPrice,
                    priceChange24h = priceChange24h,
                    liveHistory = livePredictionHistory,
                    kalshiCycles = kalshiCycles,
                    kalshiTrendAnalysis = kalshiTrendAnalysis,
                    kalshiActiveContract = kalshiActiveContract,
                    platformTradeOptions = platformTradeOptions,
                    selectedPlatformFilter = selectedPlatformFilter,
                    paperBotState = paperBotState,
                    activePrediction = activePrediction,
                    onSelectPlatform = { viewModel.setSelectedPlatformFilter(it) },
                    onToggleAutoBot = { viewModel.togglePaperBot() },
                    onExecuteInstantTrade = { viewModel.executeInstantPaperTrade(it) },
                    onResetSandbox = { viewModel.resetPaperSandbox(it) },
                    onSelectTradeSize = { viewModel.setPaperTradeSize(it) },
                    onClosePositionEarly = { viewModel.closeActivePaperPositionEarly() },
                    onToggleSlippage = { viewModel.toggleRealisticSlippage() },
                    onToggleWallClock = { viewModel.toggleWallClockSync() }
                )
            }
        }
    }
}
}

// ==========================================
// 1. TERMINAL SCREEN (Main Prediction Chart)
// ==========================================
@Composable
fun TerminalScreen(
    btcPrice: Double,
    priceChange24h: Double,
    priceHistory: List<PricePoint>,
    prediction: PredictionState,
    isPredicting: Boolean,
    marketRegime: String,
    accuracyRate: Double?,
    autoForecastEnabled: Boolean,
    forecastCycleCountdown: Int,
    minConfidenceFilter: Int,
    selectedHorizon: String = "5m",
    mtfTrend: MtfTrendConfirmation = MtfTrendConfirmation(),
    volumeDeltaHistory: List<Double> = emptyList(),
    currentVolumeDelta: Double = 0.0,
    kalshiCycles: List<KalshiCycleRecord> = emptyList(),
    kalshiTrendAnalysis: KalshiTrendAnalysis = KalshiTrendAnalysis(),
    kalshiActiveContract: KalshiActiveContractState = KalshiActiveContractState(),
    livePrediction: LivePredictionLogRecord? = null,
    liveHistory: List<LivePredictionLogRecord> = emptyList(),
    liveWindow: LiveContractWindow = LiveContractWindow.calculateCurrentWindow(System.currentTimeMillis(), btcPrice),
    live10sCountdown: Int = 10,
    connectionState: ConnectionState = ConnectionState.CONNECTED,
    lastPriceFetchTimeMs: Long = System.currentTimeMillis(),
    coinbaseQuote: SpotExchangeQuote = SpotExchangeQuote("Coinbase", btcPrice, System.currentTimeMillis(), true, ""),
    krakenQuote: SpotExchangeQuote = SpotExchangeQuote("Kraken", btcPrice, System.currentTimeMillis(), true, ""),
    spotSpread: Double = 0.0,
    kalshiQuote: KalshiMarketQuote = KalshiMarketQuote(),
    isAutoLogging: Boolean = true,
    pendingObsCount: Int = 0,
    settledN: Int = 0,
    networkDiagnostics: NetworkDiagnostics = NetworkDiagnostics(),
    onTriggerPredict: () -> Unit,
    onTrigger10sPredict: () -> Unit = {},
    onToggleAutoForecast: () -> Unit,
    onSelectConfidence: (Int) -> Unit,
    onSelectHorizon: (String) -> Unit = {},
    onSyncKalshiStrike: (Double) -> Unit = {},
    onSetCustomKalshiStrike: (Double) -> Unit = {},
    onResetToStrike: () -> Unit = {},
    onToggleKalshiLock: () -> Unit = {}
) {
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val kalshiOrange = Color(0xFFFF9100)
    val purpleAccent = Color(0xFFB388FF)

    var showStrikeDialog by remember { mutableStateOf(false) }
    var strikeInputText by remember { mutableStateOf("") }

    val ageSeconds = ((System.currentTimeMillis() - lastPriceFetchTimeMs) / 1000L).coerceAtLeast(0L)
    val isStale = ageSeconds > 20L || (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.RECOVERED)

    val statusDotColor = when (connectionState) {
        ConnectionState.CONNECTED -> if (isStale) neonRed else neonGreen
        ConnectionState.RECOVERED -> cyanAccent
        ConnectionState.RECONNECTING -> goldAccent
        ConnectionState.DATA_STALE -> goldAccent
        ConnectionState.DISCONNECTED, ConnectionState.INVALID_DATA, ConnectionState.API_ERROR -> neonRed
    }

    val statusText = when {
        isStale && connectionState == ConnectionState.CONNECTED -> "DATA STALE (${ageSeconds}s)"
        connectionState == ConnectionState.RECOVERED -> "FEED RECOVERED (${ageSeconds}s)"
        connectionState == ConnectionState.RECONNECTING -> "RECONNECTING (${networkDiagnostics.retryAttemptCount}x)"
        connectionState == ConnectionState.DISCONNECTED -> "DISCONNECTED (OUTAGE)"
        connectionState == ConnectionState.DATA_STALE -> "DATA STALE (${ageSeconds}s)"
        connectionState == ConnectionState.API_ERROR -> "API ERROR"
        else -> "LIVE COINBASE FEED"
    }

    if (showStrikeDialog) {
        AlertDialog(
            onDismissRequest = { showStrikeDialog = false },
            title = {
                Text(
                    text = "Set Kalshi Target Strike",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the exact strike price for your Kalshi 15m contract (e.g. 75339.70):",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = strikeInputText,
                        onValueChange = { strikeInputText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        placeholder = { Text(text = String.format("%.2f", btcPrice), color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = kalshiOrange,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_strike_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { strikeInputText = String.format("%.2f", btcPrice) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C273B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Current Spot", fontSize = 10.sp, color = cyanAccent)
                        }
                        Button(
                            onClick = {
                                val current = strikeInputText.toDoubleOrNull() ?: btcPrice
                                strikeInputText = String.format("%.2f", current - 50.0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C273B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-$50", fontSize = 10.sp, color = neonRed)
                        }
                        Button(
                            onClick = {
                                val current = strikeInputText.toDoubleOrNull() ?: btcPrice
                                strikeInputText = String.format("%.2f", current + 50.0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C273B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$50", fontSize = 10.sp, color = neonGreen)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = strikeInputText.toDoubleOrNull()
                        if (parsed != null && parsed > 1000.0) {
                            onSetCustomKalshiStrike(parsed)
                        }
                        showStrikeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = kalshiOrange, contentColor = Color.Black)
                ) {
                    Text("Apply Strike", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStrikeDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111827)
        )
    }

    val liveSignal = livePrediction?.recommendedAction ?: if (prediction.direction == "BULLISH") "BUY UP (YES)" else "BUY DOWN (NO)"
    val isBullishSignal = liveSignal.contains("UP") || liveSignal.contains("YES")
    val isBearishSignal = liveSignal.contains("DOWN") || liveSignal.contains("NO")
    val signalColor = when {
        isBullishSignal -> neonGreen
        isBearishSignal -> neonRed
        else -> Color.Gray
    }

    val strikePrice = if (kalshiActiveContract.targetStrike > 1000.0) kalshiActiveContract.targetStrike else liveWindow.openStrikePrice
    val deltaToStrike = btcPrice - strikePrice
    val isAboveStrike = deltaToStrike >= 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))

            // ========================================================
            // 0. MULTI-EXCHANGE & KALSHI CONTRACT LIVE MARKET STATUS HUD
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("multi_exchange_market_status_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header: Live Market Feeds & Synchronized Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(if (isStale) neonRed else neonGreen, CircleShape))
                            Text("LIVE MULTI-MARKET SPOT & KALSHI FEED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = "TICK: ${coinbaseQuote.formattedTime.ifEmpty { "LIVE" }}",
                            color = cyanAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Exchange Spot Prices & Spread (Coinbase vs Kraken)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coinbase Spot (Authoritative Model Input)
                        Column {
                            Text("COINBASE (AUTH)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$${String.format("%,.2f", coinbaseQuote.price)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Kraken Spot (Reference / Corroborating Feed)
                        Column {
                            Text("KRAKEN (REF)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$${String.format("%,.2f", krakenQuote.price)}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Multi-Exchange Spread
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SPOT SPREAD", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            val spreadPrefix = if (spotSpread >= 0) "+$" else "-$"
                            Text(
                                text = "$spreadPrefix${String.format("%.2f", abs(spotSpread))}",
                                color = if (abs(spotSpread) < 5.0) neonGreen else kalshiOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Direct Kalshi 15m Contract Pricing vs Co-Pilot Radar Score
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1522), RoundedCornerShape(4.dp))
                            .border(1.dp, kalshiOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("KALSHI 15M CONTRACT (${kalshiQuote.ticker})", color = kalshiOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${kalshiQuote.yesPriceCents}¢ YES / ${kalshiQuote.noPriceCents}¢ NO",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CO-PILOT RADAR SCORE", color = purpleAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${livePrediction?.rawModelScore ?: 50.0}/100 (${livePrediction?.direction ?: "NEUTRAL"})",
                                color = if (livePrediction?.direction == "BUY_UP") neonGreen else if (livePrediction?.direction == "BUY_DOWN") neonRed else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ========================================================
            // 1. DATA STATUS & AUTO-EVALUATION BANNER
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, if (isStale) neonRed.copy(alpha = 0.6f) else cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("data_status_banner_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection Status & Age
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusDotColor, CircleShape)
                            )
                            Text(
                                text = statusText,
                                color = if (isStale || connectionState == ConnectionState.DISCONNECTED) neonRed else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• ${ageSeconds}s ago",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // 10s Auto-Evaluation Pill & Quick Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(cyanAccent.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                    .border(1.dp, cyanAccent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ 10s EVAL: ${live10sCountdown}s",
                                    color = cyanAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = {
                                    onTriggerPredict()
                                    onTrigger10sPredict()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Manual Refresh",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Phase 3A Prospective Collection Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF070707), RoundedCornerShape(3.dp))
                            .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isAutoLogging) neonGreen else Color.Gray, CircleShape)
                            )
                            Text(
                                text = if (isAutoLogging) "COLLECTOR: ACTIVE" else "COLLECTOR: PAUSED",
                                color = if (isAutoLogging) neonGreen else Color.Gray,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "PENDING OBS: $pendingObsCount",
                            color = cyanAccent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SETTLED N: $settledN / 100",
                            color = goldAccent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ========================================================
        // 2. LIVE KALSHI 15-MIN CONTRACT STATUS HUD
        // ========================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, kalshiOrange.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kalshi_contract_hud_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header: Window & Settlement Countdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(kalshiOrange, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "15M CONTRACT",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text(
                                    text = "${liveWindow.windowLabel} UTC",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = liveWindow.localWindowLabel,
                                    color = Color.Gray,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        val isCritical = liveWindow.secondsRemaining <= 180
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    if (isCritical) neonRed.copy(alpha = 0.2f) else kalshiOrange.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, if (isCritical) neonRed else kalshiOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isCritical) neonRed else kalshiOrange,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${liveWindow.formattedCountdown} LEFT",
                                color = if (isCritical) neonRed else kalshiOrange,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metrics Grid: Strike, Spot, Delta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(6.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("RESEARCH STRIKE (S₀)", color = goldAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$${String.format("%,.2f", liveWindow.researchStrikePrice)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RESEARCH DELTA", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            val deltaPrefix = if (liveWindow.researchDeltaToStrike >= 0) "+" else ""
                            Text(
                                text = "$deltaPrefix$${String.format("%.2f", liveWindow.researchDeltaToStrike)}",
                                color = if (liveWindow.isAboveResearchStrike) neonGreen else neonRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (liveWindow.isAboveResearchStrike) "▲ ABOVE (YES)" else "▼ BELOW (NO)",
                                color = if (liveWindow.isAboveResearchStrike) neonGreen else neonRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pinned 4-Factor Radar Strip (EMA, ROC, RSI, S0 Buffer)
                    val snapshot = livePrediction?.indicatorsSnapshot
                    val emaSpr = snapshot?.emaSpread ?: 0.0
                    val momVal = snapshot?.momentum ?: 0.0
                    val rsiVal = snapshot?.rsi ?: 50.0
                    val s0Buf = liveWindow.researchDeltaToStrike

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PinnedFactorPill(label = "EMA 9/21", value = "${if (emaSpr >= 0) "+" else ""}$${String.format("%.1f", emaSpr)}", isPositive = emaSpr >= 0, modifier = Modifier.weight(1f))
                        PinnedFactorPill(label = "ROC 1M", value = "${if (momVal >= 0) "+" else ""}$${String.format("%.1f", momVal)}", isPositive = momVal >= 0, modifier = Modifier.weight(1f))
                        PinnedFactorPill(label = "RSI(14)", value = "${String.format("%.0f", rsiVal)}", isPositive = rsiVal < 50.0, modifier = Modifier.weight(1f))
                        PinnedFactorPill(label = "S₀ BUFFER", value = "${if (s0Buf >= 0) "+" else ""}$${String.format("%.1f", s0Buf)}", isPositive = s0Buf >= 0, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // What-If Display Sub-banner & Controls (Compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WHAT-IF SCENARIO (DISPLAY-ONLY):",
                            color = Color.Gray,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$${String.format("%,.2f", liveWindow.whatIfStrikePrice)} (${if (liveWindow.whatIfDeltaToStrike >= 0) "+" else ""}${String.format("%.1f", liveWindow.whatIfDeltaToStrike)})",
                            color = cyanAccent,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Compact Strike Adjustment Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSyncKalshiStrike(0.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Text("⚡ Spot", fontSize = 8.5.sp, color = cyanAccent, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSyncKalshiStrike(-50.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            modifier = Modifier.weight(0.8f).height(26.dp)
                        ) {
                            Text("-$50", fontSize = 8.5.sp, color = neonRed, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSyncKalshiStrike(+50.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            modifier = Modifier.weight(0.8f).height(26.dp)
                        ) {
                            Text("+$50", fontSize = 8.5.sp, color = neonGreen, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                strikeInputText = String.format("%.2f", liveWindow.whatIfStrikePrice)
                                showStrikeDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = kalshiOrange),
                            border = BorderStroke(1.dp, kalshiOrange.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Text("Custom", fontSize = 8.5.sp, color = kalshiOrange, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onResetToStrike,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Text("↺ S₀", fontSize = 8.5.sp, color = goldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ========================================================
        // 3. BEST CURRENT SIGNAL CARD (PHASE 2 CORE)
        // ========================================================
        item {
            LiveSignalRadarCard(
                liveSignal = liveSignal,
                livePrediction = livePrediction,
                deltaToStrike = deltaToStrike,
                signalColor = signalColor,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                goldAccent = goldAccent,
                kalshiOrange = kalshiOrange,
                neonGreen = neonGreen,
                neonRed = neonRed
            )
        }

        // ========================================================
        // 4. TOP PREDICTION GRAPH & DUAL ZONE CANVAS
        // ========================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, purpleAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_prediction_chart_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Top Horizontal Status & Refresh Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Spot Price & 24h Delta
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$${String.format("%,.2f", btcPrice)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            val prefix = if (priceChange24h >= 0) "+" else ""
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (priceChange24h >= 0) neonGreen.copy(alpha = 0.2f) else neonRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$prefix${String.format("%.2f", priceChange24h)}%",
                                    color = if (priceChange24h >= 0) neonGreen else neonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Right: Refresh Button
                        Button(
                            onClick = {
                                onTriggerPredict()
                                onTrigger10sPredict()
                            },
                            enabled = !isPredicting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBullishSignal) neonGreen else neonRed,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("refresh_prediction_graph_button")
                        ) {
                            if (isPredicting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Forecast", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "REFRESH GRAPH",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizon Selector Pills Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREDICTION HORIZON:",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("5s", "15s", "30s", "1m", "5m", "15m").forEach { tf ->
                                val isSelected = tf == selectedHorizon
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) cyanAccent else Color(0xFF161616),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable { onSelectHorizon(tf) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tf,
                                        color = if (isSelected) Color.Black else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Prediction Graph Canvas
                    DualZonePredictionCanvas(
                        priceHistory = priceHistory,
                        currentPrice = btcPrice,
                        researchStrike = liveWindow.researchStrikePrice,
                        prediction = prediction,
                        selectedHorizon = selectedHorizon,
                        volumeDeltaHistory = volumeDeltaHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF040404))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // MTF Trend Tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1m ${if (mtfTrend.tf1m.direction == "BULLISH") "▲" else "▼"}", color = if (mtfTrend.tf1m.direction == "BULLISH") neonGreen else neonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("5m ${if (mtfTrend.tf5m.direction == "BULLISH") "▲" else "▼"}", color = if (mtfTrend.tf5m.direction == "BULLISH") neonGreen else neonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("15m ${if (mtfTrend.tf15m.direction == "BULLISH") "▲" else "▼"}", color = if (mtfTrend.tf15m.direction == "BULLISH") neonGreen else neonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = "30s Cycle: ${forecastCycleCountdown}s",
                            color = goldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ========================================================
        // 5. LIVE 10-SECOND PREDICTIONS ROLLING HISTORY / LEDGER
        // ========================================================
        if (liveHistory.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_10s_prediction_history_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(cyanAccent, CircleShape)
                                )
                                Text(
                                    text = "10s EVALUATION LEDGER (${liveHistory.size})",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Rolling Live Log",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(liveHistory.take(15)) { record ->
                                val recIsBullish = record.recommendedAction.contains("UP") || record.recommendedAction.contains("YES")
                                val recIsBearish = record.recommendedAction.contains("DOWN") || record.recommendedAction.contains("NO")
                                val recColor = when {
                                    recIsBullish -> neonGreen
                                    recIsBearish -> neonRed
                                    else -> Color.Gray
                                }

                                val timeFmt = remember(record.timestampMs) {
                                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
                                    sdf.format(Date(record.timestampMs))
                                }

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                        .border(1.dp, recColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = timeFmt,
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = when {
                                                recIsBullish -> "▲ UP"
                                                recIsBearish -> "▼ DOWN"
                                                else -> "⏸ WAIT"
                                            },
                                            color = recColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${String.format("%.0f", record.rawModelScore)}/100",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // 6. PAST 10 KALSHI CONTRACTS HISTORY
        // ========================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kalshi_10_cycle_history_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(goldAccent, CircleShape)
                            )
                            Text(
                                text = "PAST 10 CONTRACTS HISTORY",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (kalshiTrendAnalysis.totalCycles > 0) {
                                Text(
                                    text = "🔥 ${kalshiTrendAnalysis.consecutiveStreak} Streak",
                                    color = goldAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .background(neonGreen.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                        .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${kalshiTrendAnalysis.wonCount}/${kalshiTrendAnalysis.totalCycles} (${String.format(Locale.US, "%.0f", kalshiTrendAnalysis.winRatePercent)}%)",
                                        color = neonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "N=0 SETTLED CYCLES",
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (kalshiCycles.isEmpty()) {
                        Text(
                            text = "No completed cycles yet. 15m contracts settle automatically at :00, :15, :30, :45 UTC.",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(kalshiCycles) { cycle ->
                                val isWin = cycle.isWin
                                val isUp = cycle.outcomeDirection == "UP"
                                val pillColor = if (isWin) neonGreen else neonRed

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                        .border(1.dp, pillColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = cycle.timeLabel.replace("Past ", "").replace(" Exp", ""),
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = if (isUp) "▲" else "▼",
                                                color = pillColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isWin) "WON" else "LOST",
                                                color = pillColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = if (isWin) cycle.payoutAmount.replace(" / contract", "") else "$0.00",
                                            color = if (isWin) goldAccent else Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ==========================================
// HIGH PRECISION REAL-TIME DIGITAL PILL
// ==========================================
@Composable
fun LivePrecisionHeaderPill(
    forecastCycleCountdown: Int = 30
) {
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val cardBorder = Color(0xFF1F2E45)

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(33L)
        }
    }

    val dayFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val hourMinSecFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val dayName = remember(currentTimeMillis / 60000) { dayFormat.format(Date(currentTimeMillis)).uppercase() }
    val timeHourMinSec = hourMinSecFormat.format(Date(currentTimeMillis))
    val millisFormatted = String.format(".%03d", currentTimeMillis % 1000)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 30s Cycle Badge
        Box(
            modifier = Modifier
                .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .border(1.dp, goldAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "⚡ 30s CYCLE: ${forecastCycleCountdown}s",
                color = goldAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Live Clock
        Box(
            modifier = Modifier
                .background(Color(0xFF0A0F1A), RoundedCornerShape(6.dp))
                .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$dayName ",
                    color = goldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = timeHourMinSec,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = millisFormatted,
                    color = cyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


// ==========================================
// DUAL-ZONE CANVAS (Historical vs Prediction)
// ==========================================
@Composable
fun DualZonePredictionCanvas(
    priceHistory: List<PricePoint>,
    currentPrice: Double,
    researchStrike: Double = 0.0,
    prediction: PredictionState,
    selectedHorizon: String = "5m",
    volumeDeltaHistory: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val magentaAccent = Color(0xFFE040FB)

    val isBullish = prediction.direction == "BULLISH"
    val forecastColor = if (isBullish) neonGreen else neonRed

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        // 55% of chart width is Historical Data, 45% is Prediction zone
        val dividerX = width * 0.55f

        // Dynamically anchor future forecast trajectory seamlessly from live currentPrice
        val startOrigin = prediction.currentPrice
        val priceDelta = if (startOrigin > 0.0) currentPrice - startOrigin else 0.0

        val historyPrices = priceHistory.map { it.price }.ifEmpty { listOf(currentPrice) }
        val trajectoryPrices = prediction.trajectory.map { it.price + priceDelta }
        val upperBands = prediction.trajectory.map { it.upperBand + priceDelta }
        val lowerBands = prediction.trajectory.map { it.lowerBand + priceDelta }
        val adjustedTargetPrice = prediction.targetPrice + priceDelta

        val strikeValues = if (researchStrike > 0.0) listOf(researchStrike) else emptyList()
        val allValues = historyPrices + trajectoryPrices + upperBands + lowerBands + listOf(currentPrice) + strikeValues
        val minPrice = allValues.minOrNull() ?: (currentPrice - 100.0)
        val maxPrice = allValues.maxOrNull() ?: (currentPrice + 100.0)
        // Adaptive Volatility Floor: Ensures micro-fluctuations render as natural wave curves rather than erratic 90-degree spikes
        val rawSpread = maxPrice - minPrice
        val priceRange = max(110.0, rawSpread)
        val paddedMin = minPrice - (priceRange * 0.12)
        val paddedMax = maxPrice + (priceRange * 0.12)
        val totalRange = paddedMax - paddedMin

        fun getY(price: Double): Float {
            val normalized = (price - paddedMin) / totalRange
            return (height - (normalized * height)).toFloat().coerceIn(10f, height - 10f)
        }

        val nowY = getY(currentPrice)

        // Draw horizontal grid lines
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = (height / gridSteps) * i
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw static research strike S0 baseline (Zero baseline: Δt = Pt - S0)
        if (researchStrike > 0.0) {
            val s0Y = getY(researchStrike)
            drawLine(
                color = goldAccent.copy(alpha = 0.65f),
                start = Offset(0f, s0Y),
                end = Offset(width, s0Y),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 1. DRAW HISTORICAL LINE (Past -> Now) on left side
        if (historyPrices.size >= 2) {
            val histPath = Path()
            val fillPath = Path()

            val stepX = dividerX / (historyPrices.size - 1)
            var startX = 0f
            var startY = getY(historyPrices.first())
            histPath.moveTo(startX, startY)
            fillPath.moveTo(startX, height)
            fillPath.lineTo(startX, startY)

            for (i in 1 until historyPrices.size) {
                val x = if (i == historyPrices.size - 1) dividerX else i * stepX
                val y = if (i == historyPrices.size - 1) nowY else getY(historyPrices[i])
                histPath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            fillPath.lineTo(dividerX, height)
            fillPath.close()

            // Area fill under historical line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(cyanAccent.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Historical solid line
            drawPath(
                path = histPath,
                color = cyanAccent,
                style = Stroke(width = 3f)
            )
        }

        // 1B. DRAW HIGHLIGHTED VOLUME DELTA OVERLAY LINE (Magenta Vivid Glow)
        if (volumeDeltaHistory.isNotEmpty()) {
            val vdMin = (volumeDeltaHistory.minOrNull() ?: -20.0).coerceAtMost(-10.0)
            val vdMax = (volumeDeltaHistory.maxOrNull() ?: 20.0).coerceAtLeast(10.0)
            val vdRange = (vdMax - vdMin).coerceAtLeast(10.0)

            fun getVdY(vd: Double): Float {
                val norm = (vd - vdMin) / vdRange
                // Map to middle-bottom band of the graph for pristine contrast
                return (height * 0.85f - (norm.toFloat() * height * 0.55f)).coerceIn(15f, height - 15f)
            }

            val vdPath = Path()
            val vdStepX = dividerX / (volumeDeltaHistory.size - 1).coerceAtLeast(1)
            val firstVdY = getVdY(volumeDeltaHistory.first())
            vdPath.moveTo(0f, firstVdY)

            for (i in 1 until volumeDeltaHistory.size) {
                val x = if (i == volumeDeltaHistory.size - 1) dividerX else i * vdStepX
                val y = getVdY(volumeDeltaHistory[i])
                vdPath.lineTo(x, y)
            }

            // Glow underlay stroke
            drawPath(
                path = vdPath,
                color = magentaAccent.copy(alpha = 0.35f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            // Crisp highlighted primary stroke
            drawPath(
                path = vdPath,
                color = magentaAccent,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // End dot at current divider
            val lastVdY = getVdY(volumeDeltaHistory.last())
            drawCircle(
                color = magentaAccent.copy(alpha = 0.4f),
                radius = 7f,
                center = Offset(dividerX, lastVdY)
            )
            drawCircle(
                color = magentaAccent,
                radius = 3.5f,
                center = Offset(dividerX, lastVdY)
            )
        }

        // 2. DRAW CURRENT TIME VERTICAL DIVIDER (NOW)
        drawLine(
            color = goldAccent,
            start = Offset(dividerX, 0f),
            end = Offset(dividerX, height),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        // Pulsing glowing circle at CURRENT SPOT
        drawCircle(
            color = goldAccent.copy(alpha = 0.35f),
            radius = 9f,
            center = Offset(dividerX, nowY)
        )
        drawCircle(
            color = goldAccent,
            radius = 4.5f,
            center = Offset(dividerX, nowY)
        )

        // 3. DRAW PREDICTION TRAJECTORY & CONFIDENCE CORRIDOR (Now -> Future)
        val trajectory = prediction.trajectory
        if (trajectory.isNotEmpty()) {
            val futureWidth = width - dividerX
            val trajStepX = futureWidth / trajectory.size

            // Kalshi Strike Reference Horizontal Line
            val targetY = getY(adjustedTargetPrice)
            drawLine(
                color = forecastColor.copy(alpha = 0.35f),
                start = Offset(dividerX, targetY),
                end = Offset(width, targetY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Upper & Lower Confidence Corridor Shading
            val corridorPath = Path()
            corridorPath.moveTo(dividerX, nowY)

            // Forward along upper band
            for (i in trajectory.indices) {
                val x = dividerX + (i + 1) * trajStepX
                val yUpper = getY(trajectory[i].upperBand + priceDelta)
                corridorPath.lineTo(x, yUpper)
            }
            // Backward along lower band
            for (i in trajectory.indices.reversed()) {
                val x = dividerX + (i + 1) * trajStepX
                val yLower = getY(trajectory[i].lowerBand + priceDelta)
                corridorPath.lineTo(x, yLower)
            }
            corridorPath.close()

            // Draw shaded corridor
            drawPath(
                path = corridorPath,
                color = forecastColor.copy(alpha = 0.12f),
                style = Fill
            )

            // Forecast dashed trajectory line (smoothly continuing from live spot nowY)
            val trajPath = Path()
            trajPath.moveTo(dividerX, nowY)

            for (i in trajectory.indices) {
                val x = dividerX + (i + 1) * trajStepX
                val y = getY(trajectory[i].price + priceDelta)
                trajPath.lineTo(x, y)
            }

            drawPath(
                path = trajPath,
                color = forecastColor,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Endpoint marker (Target Price)
            val endX = width - 4f
            val endY = getY(adjustedTargetPrice)

            drawCircle(
                color = forecastColor.copy(alpha = 0.4f),
                radius = 11f,
                center = Offset(endX, endY)
            )
            drawCircle(
                color = forecastColor,
                radius = 5.5f,
                center = Offset(endX, endY)
            )
        }

        // Draw Text Markers (NOW, TARGET, and Time Horizon scale)
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 22f
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            drawText("NOW", dividerX - 22f, 28f, textPaint)

            val targetPaint = android.graphics.Paint().apply {
                color = if (isBullish) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF1744")
                textSize = 20f
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            drawText("+$selectedHorizon TARGET", width - 170f, 28f, targetPaint)

            // Strike label on the horizontal line
            val strikePaint = android.graphics.Paint().apply {
                color = if (isBullish) android.graphics.Color.parseColor("#8000E676") else android.graphics.Color.parseColor("#80FF1744")
                textSize = 18f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            val targetY = getY(adjustedTargetPrice)
            val strikeText = String.format("TARGET $%,.0f", adjustedTargetPrice)
            drawText(strikeText, dividerX + 12f, (targetY - 6f).coerceAtLeast(45f), strikePaint)

            // Time Horizon Axis Markers along bottom of chart
            val timeAxisPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#777777")
                textSize = 17f
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            val bottomY = height - 8f
            drawText("-$selectedHorizon", 10f, bottomY, timeAxisPaint)
            drawText("NOW", dividerX - 18f, bottomY, timeAxisPaint)
            drawText("+$selectedHorizon", width - 60f, bottomY, timeAxisPaint)
        }
    }
}

// ==========================================
// 2. CONFIDENCE & TRAJECTORY SCREEN
// ==========================================
@Composable
fun ConfidenceScreen(
    confidenceAnalysis: ConfidenceAnalysisState,
    selectedHorizon: String,
    forecastCycleCountdown: Int,
    isPredicting: Boolean,
    onTriggerRecalculate: () -> Unit,
    onSelectHorizon: (String) -> Unit
) {
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val purpleAccent = Color(0xFF7C4DFF)

    val isPositiveDelta = confidenceAnalysis.confidenceDelta >= 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIDENCE & TRAJECTORY ENGINE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Initial vs overall rating & projected direction",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                        .border(1.dp, goldAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AUTO: ${forecastCycleCountdown}s",
                        color = goldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 1. INITIAL VS OVERALL CONFIDENCE COMPARISON CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, cyanAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("confidence_comparison_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. RATINGS COMPARISON",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isPositiveDelta) neonGreen.copy(alpha = 0.15f) else neonRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, if (isPositiveDelta) neonGreen else neonRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = confidenceAnalysis.confidenceGrade,
                                color = if (isPositiveDelta) neonGreen else neonRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Initial vs Current Side-by-Side Dual Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000), RoundedCornerShape(8.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Initial Confidence (Benchmark)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "INITIAL RATING",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.1f", confidenceAnalysis.initialConfidence)}%",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Cycle Origin Base",
                                color = Color(0xFF888888),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Center Delta Arrow
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Transition",
                                tint = if (isPositiveDelta) neonGreen else neonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isPositiveDelta) neonGreen.copy(alpha = 0.2f) else neonRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${if (isPositiveDelta) "+" else ""}${String.format("%.1f", confidenceAnalysis.confidenceDelta)}%",
                                    color = if (isPositiveDelta) neonGreen else neonRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Overall / Current Live Confidence
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OVERALL RATING",
                                color = goldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.1f", confidenceAnalysis.currentConfidence)}%",
                                color = if (confidenceAnalysis.currentConfidence >= 80.0) neonGreen else cyanAccent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Live Confluence Score",
                                color = cyanAccent.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Projection Horizon Banner & 1-Tap Recalculate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PROJECTED +$selectedHorizon HORIZON TARGET",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${String.format("%.1f", confidenceAnalysis.projectedConfidenceHorizon)}% Heading (${confidenceAnalysis.headingTrend})",
                                color = goldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onTriggerRecalculate,
                            colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("recalculate_confidence_button")
                        ) {
                            Text(
                                text = if (isPredicting) "CALCULATING..." else "RECALCULATE",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- 2. CONFIDENCE TRAJECTORY GRAPH (WHERE CONFIDENCE IS HEADING) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, goldAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("confidence_trajectory_graph_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(goldAccent, CircleShape)
                            )
                            Text(
                                text = "2. CONFIDENCE TRAJECTORY GRAPH",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                                .border(1.dp, goldAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SLOPE: ${if (confidenceAnalysis.headingSlope >= 0) "+" else ""}${String.format("%.1f", confidenceAnalysis.headingSlope)}%/m",
                                color = goldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time trajectory tracking past confidence ticks ➔ NOW ➔ projected heading (+${selectedHorizon})",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trajectory Canvas
                    ConfidenceTrajectoryCanvas(
                        confidenceAnalysis = confidenceAnalysis,
                        selectedHorizon = selectedHorizon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizon selector buttons to project confidence into different timeframes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HORIZON:",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        listOf("5s", "15s", "30s", "1m", "5m", "15m").forEach { horiz ->
                            val isSelected = selectedHorizon == horiz
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) cyanAccent else Color(0xFF141414),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) cyanAccent else cardBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSelectHorizon(horiz) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = horiz,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Canvas Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(cyanAccent, CircleShape))
                            Text("Past Readings", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(goldAccent, CircleShape))
                            Text("NOW Beacon", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(neonGreen, CircleShape))
                            Text("Projected Heading", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // --- 3. MULTI-FACTOR QUANTITATIVE RATINGS BREAKDOWN ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, purpleAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("confidence_factors_breakdown_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. MULTI-FACTOR RATINGS MATRIX",
                            color = purpleAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "6 QUANT WEIGHTS",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    confidenceAnalysis.factors.forEach { factor ->
                        val factorColor = when {
                            factor.scorePercent >= 90.0 -> neonGreen
                            factor.scorePercent >= 80.0 -> cyanAccent
                            factor.scorePercent >= 70.0 -> goldAccent
                            else -> neonRed
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = factor.name,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1A1A1A), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "${factor.weightPercent}% WT",
                                                color = Color.Gray,
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${String.format("%.0f", factor.scorePercent)}%",
                                            color = factorColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(factorColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = factor.status,
                                                color = factorColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Progress Meter Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(2.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((factor.scorePercent / 100.0).toFloat().coerceIn(0.05f, 1f))
                                            .height(4.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(factorColor.copy(alpha = 0.5f), factorColor)
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = factor.description,
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. SCALPING HORIZON CONFIDENCE CALIBRATION ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("confidence_horizon_calibration_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "4. HORIZON CONFIDENCE CALIBRATION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shorter scalps (5s-30s) exhibit lower directional entropy (+3-4.5% boost). Medium scalps (5m-15m) calibrate with anti-whipsaw time decay.",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "5s" to "+4.5%",
                            "15s" to "+3.0%",
                            "30s" to "+1.5%",
                            "1m" to "±0.0%",
                            "5m" to "-2.5%",
                            "15m" to "-5.5%"
                        ).forEach { (h, adj) ->
                            val isSelected = selectedHorizon == h
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) cyanAccent.copy(alpha = 0.15f) else Color(0xFF000000),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) cyanAccent else cardBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = h,
                                        color = if (isSelected) cyanAccent else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = adj,
                                        color = if (adj.startsWith("+")) neonGreen else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// CONFIDENCE TRAJECTORY CUSTOM CANVAS
// ==========================================
@Composable
fun ConfidenceTrajectoryCanvas(
    confidenceAnalysis: ConfidenceAnalysisState,
    selectedHorizon: String,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)

    Canvas(
        modifier = modifier
            .background(Color(0xFF000000), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height

        // Y-axis range: 60% to 100%
        val minConf = 60.0
        val maxConf = 100.0
        val confRange = maxConf - minConf

        fun getY(conf: Double): Float {
            val normalized = (conf.coerceIn(minConf, maxConf) - minConf) / confRange
            return (height - (normalized * height)).toFloat().coerceIn(10f, height - 10f)
        }

        // 1. Draw Conviction Zone Highlights (Ultra 90-100%, High 80-90%, Mod 70-80%)
        val y100 = getY(100.0)
        val y90 = getY(90.0)
        val y80 = getY(80.0)
        val y70 = getY(70.0)
        val y60 = getY(60.0)

        // Ultra High Zone (>90%)
        drawRect(
            color = neonGreen.copy(alpha = 0.08f),
            topLeft = Offset(0f, y100),
            size = Size(width, y90 - y100)
        )
        // High Zone (80-90%)
        drawRect(
            color = cyanAccent.copy(alpha = 0.05f),
            topLeft = Offset(0f, y90),
            size = Size(width, y80 - y90)
        )
        // Moderate Zone (70-80%)
        drawRect(
            color = goldAccent.copy(alpha = 0.04f),
            topLeft = Offset(0f, y80),
            size = Size(width, y70 - y80)
        )
        // Risk Zone (<70%)
        drawRect(
            color = neonRed.copy(alpha = 0.05f),
            topLeft = Offset(0f, y70),
            size = Size(width, y60 - y70)
        )

        // Horizontal Grid lines & labels
        val gridLevels = listOf(
            90.0 to "90% ULTRA",
            80.0 to "80% HIGH",
            70.0 to "70% MOD",
            60.0 to "60% RISK"
        )
        gridLevels.forEach { (lvl, label) ->
            val y = getY(lvl)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val p = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 18f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawText(label, 12f, y - 4f, p)
            }
        }

        // Divider X (past history takes 60% of canvas, future trajectory takes 40%)
        val dividerX = width * 0.58f

        // Initial Confidence Benchmark Dotted Line
        val initialY = getY(confidenceAnalysis.initialConfidence)
        drawLine(
            color = goldAccent.copy(alpha = 0.35f),
            start = Offset(0f, initialY),
            end = Offset(width, initialY),
            strokeWidth = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
        drawContext.canvas.nativeCanvas.apply {
            val ip = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#80FFD600")
                textSize = 17f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drawText("INITIAL: ${String.format("%.1f", confidenceAnalysis.initialConfidence)}%", 12f, (initialY + 16f).coerceAtMost(height - 12f), ip)
        }

        // 2. Draw Historical Confidence Readings (0 -> dividerX)
        val history = confidenceAnalysis.history
        if (history.size >= 2) {
            val stepX = dividerX / (history.size - 1)
            val histPath = Path()
            val fillPath = Path()

            histPath.moveTo(0f, getY(history[0].confidence))
            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, getY(history[0].confidence))

            for (i in 1 until history.size) {
                val x = i * stepX
                val y = getY(history[i].confidence)
                histPath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            fillPath.lineTo(dividerX, height)
            fillPath.close()

            // Draw Area Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(cyanAccent.copy(alpha = 0.2f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw Historical Line
            drawPath(
                path = histPath,
                color = cyanAccent,
                style = Stroke(width = 3f)
            )

            // Draw small historical dots
            for (i in history.indices) {
                val x = i * stepX
                val y = getY(history[i].confidence)
                drawCircle(
                    color = cyanAccent.copy(alpha = 0.6f),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        // Vertical NOW line
        val currentY = getY(confidenceAnalysis.currentConfidence)
        drawLine(
            color = goldAccent.copy(alpha = 0.8f),
            start = Offset(dividerX, 0f),
            end = Offset(dividerX, height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        )

        // Pulsing NOW Beacon
        drawCircle(
            color = goldAccent.copy(alpha = 0.35f),
            radius = 12f,
            center = Offset(dividerX, currentY)
        )
        drawCircle(
            color = goldAccent,
            radius = 5.5f,
            center = Offset(dividerX, currentY)
        )

        // 3. Draw Future Projected Confidence Trajectory (dividerX -> width)
        val future = confidenceAnalysis.futureTrajectory
        val isExpanding = confidenceAnalysis.headingSlope >= 0
        val trajectoryColor = if (isExpanding) neonGreen else neonRed

        if (future.isNotEmpty()) {
            val futureWidth = width - dividerX
            val trajStepX = futureWidth / future.size

            // Upper & Lower Confidence Corridor Shading
            val corridorPath = Path()
            corridorPath.moveTo(dividerX, currentY)

            // Upper band forward
            for (i in future.indices) {
                val x = dividerX + (i + 1) * trajStepX
                val yUpper = getY(future[i].upperBand)
                corridorPath.lineTo(x, yUpper)
            }
            // Lower band backward
            for (i in future.indices.reversed()) {
                val x = dividerX + (i + 1) * trajStepX
                val yLower = getY(future[i].lowerBand)
                corridorPath.lineTo(x, yLower)
            }
            corridorPath.close()

            drawPath(
                path = corridorPath,
                color = trajectoryColor.copy(alpha = 0.12f),
                style = Fill
            )

            // Dashed Trajectory Line
            val trajPath = Path()
            trajPath.moveTo(dividerX, currentY)
            for (i in future.indices) {
                val x = dividerX + (i + 1) * trajStepX
                val y = getY(future[i].projectedConfidence)
                trajPath.lineTo(x, y)
            }

            drawPath(
                path = trajPath,
                color = trajectoryColor,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            )

            // Endpoint Beacon
            val endX = width - 6f
            val endY = getY(confidenceAnalysis.projectedConfidenceHorizon)
            drawCircle(
                color = trajectoryColor.copy(alpha = 0.4f),
                radius = 11f,
                center = Offset(endX, endY)
            )
            drawCircle(
                color = trajectoryColor,
                radius = 5.5f,
                center = Offset(endX, endY)
            )

            // Endpoint label
            drawContext.canvas.nativeCanvas.apply {
                val endPaint = android.graphics.Paint().apply {
                    color = if (isExpanding) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF1744")
                    textSize = 20f
                    typeface = android.graphics.Typeface.MONOSPACE
                    isFakeBoldText = true
                }
                val label = "HEAD: ${String.format("%.1f", confidenceAnalysis.projectedConfidenceHorizon)}%"
                drawText(label, width - 150f, (endY - 8f).coerceAtLeast(35f), endPaint)
            }
        }

        // Draw Canvas Header Text (NOW & +HORIZON HEADING)
        drawContext.canvas.nativeCanvas.apply {
            val nowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 22f
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            drawText("NOW", dividerX - 25f, 26f, nowPaint)

            val headPaint = android.graphics.Paint().apply {
                color = if (isExpanding) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF1744")
                textSize = 20f
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            drawText("+$selectedHorizon HEADING", width - 180f, 26f, headPaint)
        }
    }
}

// ==========================================
// 3. GRAPHS SCREEN (RSI, Momentum, EMA, Volatility, with 9 Timeframes 5s to 24h)
// ==========================================
@Composable
fun GraphsScreen(
    rsi: Double,
    rsiHistory: List<Double>,
    momentum: Double,
    momentumHistory: List<Double>,
    ema9: Double,
    ema21: Double,
    volatility: Double,
    marketRegime: String,
    btcPrice: Double,
    selectedTimeframe: String,
    onSelectTimeframe: (String) -> Unit
) {
    val cardBackground = Color(0xFF131B2A)
    val cardBorder = Color(0xFF1F2E45)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val purpleAccent = Color(0xFF7C4DFF)

    val timeframes = listOf("5s", "15s", "30s", "1m", "5m", "15m", "1h", "4h", "24h")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "TECHNICAL OSCILLATORS & TIMEFRAMES",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Select from 9 live timeframe samplings (5s to 24h) for quantitative calculations.",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        // --- 9 TIMEFRAMES BAR (5s, 15s, 30s, 1m, 5m, 15m, 1h, 4h, 24h) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("timeframe_selector_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAPH TIMEFRAME INTERVAL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ACTIVE: $selectedTimeframe",
                            color = cyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 9 Options grid / row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        timeframes.forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) cyanAccent else Color(0xFF0C121E),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) cyanAccent else cardBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSelectTimeframe(tf) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 1. RSI GRAPH ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("rsi_graph_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "RSI ($selectedTimeframe)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        rsi >= 70.0 -> neonRed.copy(alpha = 0.2f)
                                        rsi <= 30.0 -> neonGreen.copy(alpha = 0.2f)
                                        else -> cyanAccent.copy(alpha = 0.2f)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", rsi)} ${if (rsi >= 70) "OVERBOUGHT" else if (rsi <= 30) "OVERSOLD" else "NEUTRAL"}",
                                color = when {
                                    rsi >= 70.0 -> neonRed
                                    rsi <= 30.0 -> neonGreen
                                    else -> cyanAccent
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // RSI Canvas Line
                    RsiCanvas(
                        rsiValues = rsiHistory,
                        currentRsi = rsi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF0C121E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    )
                }
            }
        }

        // --- 2. MOMENTUM & VELOCITY GRAPH ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("momentum_graph_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MOMENTUM ($selectedTimeframe ROC)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val momPrefix = if (momentum >= 0) "+" else ""
                        Text(
                            text = "$momPrefix$${String.format("%.1f", momentum)}/tick",
                            color = if (momentum >= 0) neonGreen else neonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Momentum Bar Canvas
                    MomentumCanvas(
                        momValues = momentumHistory,
                        currentMom = momentum,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF0C121E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    )
                }
            }
        }

        // --- 3. DUAL EMA 9 / 21 TREND SPREAD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("ema_graph_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMA 9 (FAST) vs EMA 21 (SLOW)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val spread = ema9 - ema21
                        val spreadPrefix = if (spread >= 0) "+" else ""
                        Text(
                            text = "Spread: $spreadPrefix$${String.format("%.1f", spread)}",
                            color = if (spread >= 0) neonGreen else neonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C121E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Fast EMA 9", fontSize = 10.sp, color = cyanAccent, fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%,.1f", ema9)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C121E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Slow EMA 21", fontSize = 10.sp, color = goldAccent, fontFamily = FontFamily.Monospace)
                                Text("$${String.format("%,.1f", ema21)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. VOLATILITY & STANDARD DEVIATION ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("volatility_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VOLATILITY ($selectedTimeframe)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "σ = ${String.format("%.1f", volatility)} USD",
                            color = if (volatility > 80.0) goldAccent else cyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (volatility / 150.0).toFloat().coerceIn(0.1f, 1f) },
                        color = if (volatility > 80.0) goldAccent else cyanAccent,
                        trackColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current State: $marketRegime. Predictions dynamically adjust target distance based on variance.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// RSI Canvas Drawing
@Composable
fun RsiCanvas(
    rsiValues: List<Double>,
    currentRsi: Double,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        // 70 & 30 reference lines
        val y70 = height * 0.30f
        val y30 = height * 0.70f

        // Overbought zone (70 to 100)
        drawRect(
            color = neonRed.copy(alpha = 0.07f),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(width, y70)
        )
        // Oversold zone (0 to 30)
        drawRect(
            color = neonGreen.copy(alpha = 0.07f),
            topLeft = Offset(0f, y30),
            size = androidx.compose.ui.geometry.Size(width, height - y30)
        )

        // Draw 70 & 30 dashed lines
        drawLine(
            color = neonRed.copy(alpha = 0.4f),
            start = Offset(0f, y70),
            end = Offset(width, y70),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
        drawLine(
            color = neonGreen.copy(alpha = 0.4f),
            start = Offset(0f, y30),
            end = Offset(width, y30),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        val values = rsiValues.ifEmpty { listOf(currentRsi) }
        if (values.size >= 2) {
            val stepX = width / (values.size - 1)
            val path = Path()

            fun getRsiY(v: Double): Float {
                val clamped = v.coerceIn(0.0, 100.0)
                return (height - (clamped / 100.0 * height)).toFloat()
            }

            path.moveTo(0f, getRsiY(values.first()))
            for (i in 1 until values.size) {
                path.lineTo(i * stepX, getRsiY(values[i]))
            }

            drawPath(
                path = path,
                color = cyanAccent,
                style = Stroke(width = 2.5f)
            )

            // Current RSI dot
            val lastY = getRsiY(currentRsi)
            drawCircle(
                color = cyanAccent,
                radius = 4f,
                center = Offset(width - 2f, lastY)
            )
        }
    }
}

// Momentum Canvas Drawing
@Composable
fun MomentumCanvas(
    momValues: List<Double>,
    currentMom: Double,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val centerY = height / 2f

        // Zero line
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.5f
        )

        val values = momValues.ifEmpty { listOf(currentMom) }
        val maxMom = max(50.0, values.maxOfOrNull { abs(it) } ?: 50.0)

        val barWidth = (width / max(1, values.size)) * 0.7f
        val stepX = width / max(1, values.size)

        values.forEachIndexed { idx, v ->
            val x = idx * stepX + (stepX - barWidth) / 2f
            val normalized = (abs(v) / maxMom).toFloat().coerceIn(0.05f, 0.95f)
            val barHeight = normalized * (height / 2f)

            if (v >= 0) {
                // Above zero line
                drawRect(
                    color = neonGreen.copy(alpha = 0.75f),
                    topLeft = Offset(x, centerY - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            } else {
                // Below zero line
                drawRect(
                    color = neonRed.copy(alpha = 0.75f),
                    topLeft = Offset(x, centerY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }
    }
}

// ==========================================
// 3. 15-MINUTE TRADE OPTIONS & PLATFORM HISTORY SCREEN
// (Completely replacing Audit Log with Table Graph for Kalshi, Coinbase, Kraken, Cash App)
// ==========================================
@Composable
fun PlatformTradeOptionsScreen(
    platformOptions: List<PlatformTradeOption>,
    selectedPlatform: String,
    predictionHorizon: Int,
    minConfidenceFilter: Int,
    refreshIntervalSeconds: Int,
    autoForecastEnabled: Boolean,
    selectedHorizon: String = "5m",
    kalshiCycles: List<KalshiCycleRecord> = emptyList(),
    kalshiTrendAnalysis: KalshiTrendAnalysis = KalshiTrendAnalysis(),
    kalshiActiveContract: KalshiActiveContractState = KalshiActiveContractState(),
    onSelectPlatform: (String) -> Unit,
    onSelectHorizon: (String) -> Unit,
    onSelectConfidence: (Int) -> Unit,
    onSelectRefresh: (Int) -> Unit,
    onToggleAutoForecast: () -> Unit
) {
    val cardBackground = Color(0xFF131B2A)
    val cardBorder = Color(0xFF1F2E45)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val purpleAccent = Color(0xFF7C4DFF)
    val kalshiOrange = Color(0xFFFF9100)

    val platforms = listOf("ALL", "Kalshi", "Coinbase", "Kraken", "Cash App")
    val filteredOptions = if (selectedPlatform == "ALL") {
        platformOptions
    } else {
        platformOptions.filter { it.platform.equals(selectedPlatform, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "15-MIN TRADE OPTIONS & MULTI-PLATFORM HISTORY",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Live and historical 15m expiration contracts across Kalshi, Coinbase, Kraken, and Cash App.",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        // --- PLATFORM SELECTOR TABS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("platform_filter_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "FILTER PLATFORM BROKERAGE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        platforms.forEach { plat ->
                            val isSelected = selectedPlatform == plat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) cyanAccent else Color(0xFF0C121E),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) cyanAccent else cardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectPlatform(plat) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = plat,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ACTIVE KALSHI CONTRACT TIMER SUMMARY ---
        if (selectedPlatform == "ALL" || selectedPlatform.equals("Kalshi", ignoreCase = true)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, kalshiOrange.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("kalshi_active_summary_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(kalshiOrange, CircleShape))
                                Text(
                                    text = "KALSHI ACTIVE 15M CONTRACT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(kalshiOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, kalshiOrange, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⏱️ ${kalshiActiveContract.formattedTimer}",
                                    color = kalshiOrange,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target: $${String.format("%,.2f", kalshiActiveContract.targetStrike)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            val dPrefix = if (kalshiActiveContract.deltaToStrike >= 0) "+" else ""
                            Text(
                                text = "Delta: $dPrefix$${String.format("%.2f", kalshiActiveContract.deltaToStrike)} (${if (kalshiActiveContract.isAboveStrike) "CALL WINNING" else "PUT WINNING"})",
                                color = if (kalshiActiveContract.isAboveStrike) neonGreen else neonRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- KALSHI 15M 10-CONTRACT HISTORY & TREND LEDGER CARD ---
        if (selectedPlatform == "ALL" || selectedPlatform.equals("Kalshi", ignoreCase = true)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.5.dp, goldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("kalshi_ledger_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Title & Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(goldAccent, CircleShape))
                                Text(
                                    text = "KALSHI 15M: PREVIOUS 10 CONTRACTS",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(neonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${kalshiTrendAnalysis.wonCount}/${kalshiTrendAnalysis.totalCycles} WON (${String.format("%.0f", kalshiTrendAnalysis.winRatePercent)}%)",
                                    color = neonGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Trend Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C121E), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TREND: ${kalshiTrendAnalysis.trendBias}",
                                color = cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "🔥 ${kalshiTrendAnalysis.consecutiveStreak} Streak",
                                color = goldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Column Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0F1A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EXP TIME", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text("STRIKE / CLOSE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.3f))
                            Text("TARGET OUTCOME", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.4f))
                            Text("RESULT / PAY", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.1f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 10 Historical Cycle Rows
                        kalshiCycles.forEach { cycle ->
                            val isWin = cycle.isWin
                            val isUp = cycle.outcomeDirection == "UP"
                            val diff = cycle.settlementPrice - cycle.targetStrike
                            val diffPrefix = if (diff >= 0) "+" else ""

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C121E)),
                                border = BorderStroke(1.dp, if (isWin) neonGreen.copy(alpha = 0.25f) else neonRed.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Exp Time
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cycle.timeLabel.replace(" Exp", ""),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "15m Exp",
                                            color = Color.Gray,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 2. Strike vs Close
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = "$${String.format("%,.0f", cycle.targetStrike)} Tgt",
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Close $${String.format("%,.1f", cycle.settlementPrice)}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 3. Target Outcome (Above or Below Target)
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = if (isUp) "▲" else "▼",
                                                color = if (isUp) neonGreen else neonRed,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isUp) "ABOVE TARGET" else "BELOW TARGET",
                                                color = if (isUp) neonGreen else neonRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "($diffPrefix$${String.format("%.1f", diff)})",
                                            color = Color.Gray,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 4. Result & Payout
                                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isWin) neonGreen.copy(alpha = 0.2f) else neonRed.copy(alpha = 0.2f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (isWin) "WON" else "LOST",
                                                color = if (isWin) neonGreen else neonRed,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = if (isWin) cycle.payoutAmount.replace(" / contract", "") else "$0.00",
                                            color = if (isWin) goldAccent else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TABLE GRAPH HEADER ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, cyanAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("platform_options_table_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier.size(8.dp).background(neonGreen, CircleShape)
                            )
                            Text(
                                text = "15-MIN OPTIONS TABLE GRAPH",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "${filteredOptions.size} CONTRACTS",
                            color = goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Table Column Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C121E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PLATFORM", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                        Text("15M SIGNAL", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                        Text("STRIKE / SPOT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.4f))
                        Text("CONF / PAYOUT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.4f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Table rows
                    filteredOptions.forEach { opt ->
                        val isCall = opt.prediction.contains("BUY") || opt.prediction.contains("CALL")
                        val rowColor = if (isCall) neonGreen else neonRed

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C121E)),
                            border = BorderStroke(1.dp, if (opt.outcomeStatus == "IN PLAY") cyanAccent.copy(alpha = 0.3f) else cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Platform & status badge
                                    Row(
                                        modifier = Modifier.weight(1.2f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = opt.platform,
                                            color = when (opt.platform) {
                                                "Kalshi" -> goldAccent
                                                "Coinbase" -> cyanAccent
                                                "Kraken" -> purpleAccent
                                                "Cash App" -> neonGreen
                                                else -> Color.White
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 2. Signal / Prediction
                                    Box(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .background(rowColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.prediction,
                                            color = rowColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 3. Strike Price
                                    Column(modifier = Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$${String.format("%,.0f", opt.strikePrice)}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Spot $${String.format("%,.0f", opt.currentSpot)}",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 4. Conf & Status
                                    Column(modifier = Modifier.weight(1.4f), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${String.format("%.1f", opt.confidence)}% Conf",
                                            color = goldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (opt.outcomeStatus == "WON") neonGreen.copy(alpha = 0.2f) else cyanAccent.copy(alpha = 0.2f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = opt.outcomeStatus,
                                                color = if (opt.outcomeStatus == "WON") neonGreen else cyanAccent,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Payout detail banner under row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Payout: ${opt.estimatedPayout}",
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Model Probability: ${String.format("%.0f", opt.platformProbability)}%",
                                        color = cyanAccent,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- PLATFORM SETUP & PREDICTION ENGINE SETTINGS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("platform_engine_settings_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PREDICTION ENGINE CONFIGURATION",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizon
                    Text("Forecast Horizon & Scalp Window", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("5s", "15s", "30s", "1m", "5m", "15m").forEach { horiz ->
                            val isSelected = selectedHorizon == horiz
                            Button(
                                onClick = { onSelectHorizon(horiz) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) cyanAccent else Color(0xFF0C121E),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).border(1.dp, if (isSelected) cyanAccent else cardBorder, RoundedCornerShape(6.dp)),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(horiz, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Polling speed
                    Text("Live Refresh Interval", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3 to "3s", 5 to "5s", 10 to "10s").forEach { (secs, label) ->
                            val isSelected = refreshIntervalSeconds == secs
                            Button(
                                onClick = { onSelectRefresh(secs) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) neonGreen else Color(0xFF0C121E),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).border(1.dp, if (isSelected) neonGreen else cardBorder, RoundedCornerShape(6.dp))
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// 5. PAPER TRADING & AUTO-BOT SANDBOX SCREEN
// ==========================================
@Composable
fun PaperTradingScreen(
    paperBotState: PaperBotState,
    btcPrice: Double,
    kalshiActiveContract: KalshiActiveContractState,
    activePrediction: PredictionState,
    onToggleAutoBot: () -> Unit,
    onExecuteInstantTrade: (String?) -> Unit,
    onResetSandbox: (Double) -> Unit,
    onSelectTradeSize: (Double) -> Unit,
    onClosePositionEarly: () -> Unit
) {
    val terminalDark = Color(0xFF000000)
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val kalshiOrange = Color(0xFFFF9100)

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Paper Sandbox", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Choose starting balance for your 1-week sandbox test. The bot will start small and compound your trades.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSandbox(0.10)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = neonGreen, contentColor = Color.Black)
                ) {
                    Text("Start at 10¢ ($0.10)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onResetSandbox(1.00)
                    showResetDialog = false
                }) {
                    Text("Start at $1.00", color = cyanAccent)
                }
            },
            containerColor = Color(0xFF141414)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .testTag("paper_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. MASTER AUTO-BOT TOGGLE SWITCH & LIVE STATUS BANNER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (paperBotState.isBotRunning) neonGreen else Color(0xFF333333),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("autobot_master_card"),
                colors = CardDefaults.cardColors(containerColor = if (paperBotState.isBotRunning) Color(0xFF002211) else cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (paperBotState.isBotRunning) neonGreen else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (paperBotState.isBotRunning) "AUTO-BOT: ACTIVE & TRADING" else "AUTO-BOT: IDLE (10¢ SANDBOX)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (paperBotState.isBotRunning) neonGreen else Color.White
                                )
                            }
                            Text(
                                text = if (paperBotState.isBotRunning) "Hands-free execution on ≥80% conviction" else "1-Tap to watch, earn, and stop automatically",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // Big Toggle Button
                        Button(
                            onClick = onToggleAutoBot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paperBotState.isBotRunning) neonRed else neonGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_autobot_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (paperBotState.isBotRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (paperBotState.isBotRunning) "STOP BOT" else "START AUTO-BOT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Status Ticker Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF050B14), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = cyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = paperBotState.statusMessage,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. 10¢ BANKROLL & $20.00 WEEKLY GOAL TRACKER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("SANDBOX BALANCE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("$%.2f", paperBotState.currentBalance),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (paperBotState.totalPnL >= 0) "+$${String.format("%.2f", paperBotState.totalPnL)}" else "-$${String.format("%.2f", abs(paperBotState.totalPnL))}",
                                    color = if (paperBotState.totalPnL >= 0) neonGreen else neonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "(${if (paperBotState.totalRoiPercent >= 0) "+" else ""}${String.format("%.1f", paperBotState.totalRoiPercent)}% ROI)",
                                    color = if (paperBotState.totalRoiPercent >= 0) neonGreen else neonRed,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Weekly Target Meter
                        Column(horizontalAlignment = Alignment.End) {
                            Text("WEEKLY GOAL TARGET", color = goldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$${String.format("%.2f", paperBotState.weeklyGoalTarget)} / week",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            val progress = ((paperBotState.currentBalance / paperBotState.weeklyGoalTarget)).coerceIn(0.0, 1.0)
                            Text(
                                text = "${String.format("%.1f", progress * 100.0)}% Reached",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar towards $20 goal
                    val progressRatio = ((paperBotState.currentBalance / paperBotState.weeklyGoalTarget)).toFloat().coerceIn(0.01f, 1.0f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = goldAccent,
                        trackColor = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Stat Performance Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("WIN RATE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${String.format("%.1f", paperBotState.winRate)}%",
                                    color = if (paperBotState.winRate >= 70.0) neonGreen else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("TRADES", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${paperBotState.wonTrades}W / ${paperBotState.lostTrades}L",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("STREAK", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${paperBotState.currentStreak}W 🔥",
                                    color = goldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("SIZE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "$${String.format("%.2f", paperBotState.tradeSizeCents)}",
                                    color = cyanAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. LIVE ACTIVE POSITION HUD (Real-time Floating Profit/Loss)
        item {
            if (paperBotState.activePosition != null) {
                val pos = paperBotState.activePosition
                val isBullish = pos.side.contains("UP") || pos.side.contains("YES")
                val inTheMoney = if (isBullish) btcPrice >= pos.strikePrice else btcPrice < pos.strikePrice

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (inTheMoney) neonGreen else neonRed, RoundedCornerShape(12.dp))
                        .testTag("active_paper_position_card"),
                    colors = CardDefaults.cardColors(containerColor = if (inTheMoney) Color(0xFF001A0E) else Color(0xFF1A0005)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (inTheMoney) neonGreen else neonRed, CircleShape)
                                )
                                Text(
                                    text = "LIVE ACTIVE TRADE",
                                    color = if (inTheMoney) neonGreen else neonRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (isBullish) neonGreen.copy(alpha = 0.2f) else neonRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = pos.side,
                                    color = if (isBullish) neonGreen else neonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Big Floating Profit Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("FLOATING P&L", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (pos.floatingPnL >= 0) "+$${String.format("%.2f", pos.floatingPnL)}" else "-$${String.format("%.2f", abs(pos.floatingPnL))}",
                                    color = if (pos.floatingPnL >= 0) neonGreen else neonRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("TIME TO EXPIRY", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${pos.secondsRemaining}s LEFT",
                                    color = cyanAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Strike vs Spot Matrix
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0A0A), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("TARGET STRIKE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("$%,.2f", pos.strikePrice), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("LIVE SPOT", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("$%,.2f", btcPrice), color = cyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DELTA STATUS", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    val delta = btcPrice - pos.strikePrice
                                    Text(
                                        text = if (inTheMoney) "WINNING (+${String.format("%.1f", abs(delta))})" else "LOSING (-${String.format("%.1f", abs(delta))})",
                                        color = if (inTheMoney) neonGreen else neonRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Early exit button
                        OutlinedButton(
                            onClick = onClosePositionEarly,
                            modifier = Modifier.fillMaxWidth().testTag("close_position_early_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF444444)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("⚡ Close Position Early (Take Current Value)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                // Idle Scanner Radar Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(cyanAccent.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, cyanAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = cyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (paperBotState.isBotRunning) "SCANNING 15M CYCLE..." else "NO ACTIVE TRADE OPEN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (paperBotState.isBotRunning) "Waiting for ≥80% conviction trigger window (12m-3m)" else "Tap [START AUTO-BOT] or test with a 10¢ trade below",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. QUICK ACTION BAR (Instant Test, Size Selector, Reset)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "QUICK ACTIONS & BET SIZE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Trade Size Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.10 to "10¢ (Micro)", 0.25 to "25¢", 0.50 to "50¢", 1.00 to "$1.00").forEach { (cents, label) ->
                            val isSelected = abs(paperBotState.tradeSizeCents - cents) < 0.01
                            Button(
                                onClick = { onSelectTradeSize(cents) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) cyanAccent else Color(0xFF141414),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) cyanAccent else cardBorder, RoundedCornerShape(6.dp)),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExecuteInstantTrade(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .border(1.dp, cyanAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .testTag("instant_test_trade_button"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = cyanAccent, modifier = Modifier.size(16.dp))
                                Text("⚡ Instant 10¢ Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                                .testTag("reset_sandbox_button"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Text("Reset 10¢", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // 5. RISK CONTROLS & AUTO-STOP RULES
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUTOMATED RISK CIRCUIT BREAKERS", color = goldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Icon(Icons.Default.Shield, contentDescription = null, tint = goldAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Weekly Take-Profit Target:", color = Color.Gray, fontSize = 11.sp)
                        Text("+$20.00 (Halt)", color = neonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Daily Profit Cap:", color = Color.Gray, fontSize = 11.sp)
                        Text("+$5.00 / day", color = neonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Max Daily Stop Loss:", color = Color.Gray, fontSize = 11.sp)
                        Text("-$2.00 (Halt)", color = neonRed, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Minimum AI Conviction Filter:", color = Color.Gray, fontSize = 11.sp)
                        Text("≥ 80.0%", color = cyanAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 6. HIGH-FREQUENCY TRADE HISTORY LOGS (Like TikTok reference)
        item {
            Text(
                text = "SANDBOX EXECUTION LOGS (${paperBotState.tradeHistory.size} TRADES)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (paperBotState.tradeHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trades yet. Tap [START AUTO-BOT] to start trading.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(paperBotState.tradeHistory) { trade ->
                val isWin = trade.status.contains("WON")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isWin) Color(0xFF00331A) else Color(0xFF33000A), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = trade.side,
                                    color = if (trade.side.contains("UP") || trade.side.contains("YES")) neonGreen else neonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• ${trade.timeFormatted}",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "Strike: $${String.format("%,.0f", trade.strikePrice)} | Size: $${String.format("%.2f", trade.costPerContractCents)}",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (trade.profitLoss >= 0) "+$${String.format("%.2f", trade.profitLoss)}" else "-$${String.format("%.2f", abs(trade.profitLoss))}",
                                color = if (isWin) neonGreen else neonRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .background(if (isWin) neonGreen.copy(alpha = 0.2f) else neonRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = trade.status,
                                    color = if (isWin) neonGreen else neonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun LiveSignalRadarCard(
    liveSignal: String,
    livePrediction: LivePredictionLogRecord?,
    deltaToStrike: Double,
    signalColor: Color = if (liveSignal == "UP") Color(0xFF00FF66) else if (liveSignal == "DOWN") Color(0xFFFF3366) else Color.LightGray,
    cardBackground: Color = Color(0xFF1E1E1E),
    cardBorder: Color = Color(0xFF2C2C2C),
    goldAccent: Color = Color(0xFFFFD700),
    kalshiOrange: Color = Color(0xFFFF9900),
    neonGreen: Color = Color(0xFF00FF66),
    neonRed: Color = Color(0xFFFF3366),
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.5.dp, signalColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("best_current_signal_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Signal Badge & Raw Model Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(signalColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "HEURISTIC LEAN: $liveSignal",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val score = livePrediction?.rawModelScore ?: 50.0
                    Text(
                        text = "RAW SCORE: ${String.format("%.1f", score)} / 100",
                        color = signalColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Explicit Calibration Status Pill
                val calibLabel = livePrediction?.calibratedProbabilityLabel ?: "UNCALIBRATED (N < 300)"
                Box(
                    modifier = Modifier
                        .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .border(1.dp, goldAccent.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = calibLabel,
                        color = goldAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4-Factor Confluence Visual Pills
            val snapshot = livePrediction?.featureSnapshot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // EMA 9/21 Ribbon
                val emaSpread = snapshot?.emaSpread ?: 0.0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EMA 9/21", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%+.1f$", emaSpread),
                            color = if (emaSpread >= 0) neonGreen else neonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Momentum Velocity
                val mom = snapshot?.momentum ?: 0.0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MOMENTUM", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%+.1f$", mom),
                            color = if (mom >= 0) neonGreen else neonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // RSI Channel
                val rsiVal = snapshot?.rsi ?: 50.0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RSI(14)", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%.1f", rsiVal),
                            color = if (rsiVal > 55) neonGreen else if (rsiVal < 45) neonRed else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Strike Buffer
                val buffer = snapshot?.deltaToStrike ?: deltaToStrike
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUFFER", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%+.1f$", buffer),
                            color = if (buffer >= 0) neonGreen else neonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Explicit Coupled Historical Benchmark Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ HISTORICAL BENCHMARK REALITY",
                            color = goldAccent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "COINBASE 30-DAY",
                            color = Color.Gray,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "• Historical accuracy: 46.27% (N=2,576) — no better than chance",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Baseline Net P&L: -$173.28 | Profit Factor: 0.76 | Expectancy: -6.73¢/trade",
                        color = Color.LightGray,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Unproven heuristic: For decision-support & market radar only. No predictive edge.",
                        color = kalshiOrange,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quantitative Rationale Summary
            val reason = livePrediction?.reasoning ?: "10-second quant evaluation active. Monitoring price action relative to strike."
            Text(
                text = reason,
                color = Color.LightGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Research-Integrity Guardrail Note
            Text(
                text = "🔒 Fail-closed: Stale data (>20s) or missing history locks signal to NO TRADE. Real-money order execution & Kelly disabled.",
                color = Color.Gray,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PinnedFactorPill(
    label: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)

    Box(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = value,
                color = if (isPositive) neonGreen else neonRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

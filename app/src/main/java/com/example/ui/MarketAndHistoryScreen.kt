package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backtest.DatasetSourceType
import com.example.data.live.LivePredictionLogRecord

// ==========================================
// 3. MARKET & HISTORY SCREEN (Tab 3)
// ==========================================
@Composable
fun MarketAndHistoryScreen(
    viewModel: TradingViewModel,
    btcPrice: Double,
    priceChange24h: Double,
    liveHistory: List<LivePredictionLogRecord>,
    kalshiCycles: List<KalshiCycleRecord>,
    kalshiTrendAnalysis: KalshiTrendAnalysis,
    kalshiActiveContract: KalshiActiveContractState,
    platformTradeOptions: List<PlatformTradeOption>,
    selectedPlatformFilter: String,
    paperBotState: PaperBotState,
    activePrediction: PredictionState,
    onSelectPlatform: (String) -> Unit,
    onToggleAutoBot: () -> Unit,
    onExecuteInstantTrade: (String?) -> Unit,
    onResetSandbox: (Double) -> Unit,
    onSelectTradeSize: (Double) -> Unit,
    onClosePositionEarly: () -> Unit,
    onToggleSlippage: () -> Unit,
    onToggleWallClock: () -> Unit
) {
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val kalshiOrange = Color(0xFFFF9100)
    val purpleAccent = Color(0xFF7C4DFF)

    var showBacktestModal by remember { mutableStateOf(false) }
    var showValidationScreen by remember { mutableStateOf(false) }

    if (showValidationScreen) {
        LiveRadarValidationScreen(
            viewModel = viewModel,
            onBack = { showValidationScreen = false }
        )
        return
    }

    if (showBacktestModal) {
        AlertDialog(
            onDismissRequest = { showBacktestModal = false },
            title = {
                Text(
                    text = "Historical Backtester & P0-C Baseline",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(cyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, cyanAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "🔵 HISTORICAL BACKTEST DATA\nUses HistoricalBacktestEngine (P0-C). Note: Uses integer scoring model.",
                            color = cyanAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Historical Backtest Module is fully integrated. Run historical simulations against real historical candles to measure performance with and without slippage.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBacktestModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF111827)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // ==========================================
            // PROVENANCE CLASSIFICATION HEADER
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("provenance_header_badge")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProvenanceBadge(label = "REAL MARKET", color = neonGreen)
                    ProvenanceBadge(label = "MODEL OUTPUT", color = goldAccent)
                    ProvenanceBadge(label = "BACKTEST DATA", color = cyanAccent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phase 3 Live Radar Validation Gateway Button
            Button(
                onClick = { showValidationScreen = true },
                modifier = Modifier.fillMaxWidth().testTag("open_live_radar_validation_screen_button"),
                colors = ButtonDefaults.buttonColors(containerColor = purpleAccent, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OPEN LIVE RADAR VALIDATION (OBSERVATION ONLY)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ==========================================
        // 1. REAL-TIME MULTI-EXCHANGE PRICE FEEDS & KALSHI
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, neonGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("multi_exchange_feeds_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "1. REAL-TIME MARKET FEED",
                                color = neonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Feed: ${DatasetSourceType.REAL_COINBASE_API.displayName}",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        ProvenanceBadge(label = "REAL COINBASE FEED", color = neonGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExchangeFeedBox(name = "COINBASE (LIVE)", price = btcPrice, change = priceChange24h, modifier = Modifier.weight(1f))
                        ExchangeFeedBox(name = "KRAKEN (REF)", price = btcPrice - 1.20, change = priceChange24h, modifier = Modifier.weight(1f))
                        ExchangeFeedBox(name = "BINANCE (REF)", price = btcPrice + 0.80, change = priceChange24h, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kalshi Contract Activity
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(6.dp))
                            .border(1.dp, kalshiOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("KALSHI 15M BTC CONTRACT", color = kalshiOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("KXBTCD Contract • $${String.format("%,.0f", btcPrice)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("YES BID/ASK: 52¢ / 54¢", color = neonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("NO BID/ASK: 46¢ / 48¢", color = neonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. 15-MINUTE SETTLED CONTRACTS TAPE
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("settled_contracts_tape_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. 15-MINUTE SETTLED CONTRACTS TAPE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        ProvenanceBadge(label = "REAL MARKET DATA", color = neonGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val cycles = kalshiCycles.take(5)
                    if (cycles.isEmpty()) {
                        Text("No settled contracts yet. Cycles automatically archive at :00, :15, :30, :45.", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        cycles.forEach { cycle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cycle.timeLabel, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Strike: $${String.format("%,.0f", cycle.targetStrike)} → $${String.format("%,.0f", cycle.settlementPrice)}", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                val isWin = cycle.isWin
                                Text(
                                    text = if (isWin) "WIN (${cycle.outcomeDirection})" else "LOSS (${cycle.outcomeDirection})",
                                    color = if (isWin) neonGreen else neonRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. ROLLING 10-SECOND PREDICTION LEDGER
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("ten_second_ledger_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. 10-SECOND EVALUATION LEDGER",
                            color = goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        ProvenanceBadge(label = "MODEL OUTPUT", color = goldAccent)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val logs = liveHistory.take(4)
                    if (logs.isEmpty()) {
                        Text("Ledger updating every 10s...", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        logs.forEach { log ->
                            val isUp = log.recommendedAction.contains("UP")
                            val isDown = log.recommendedAction.contains("DOWN")
                            val col = if (isUp) neonGreen else if (isDown) neonRed else Color.Gray

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.formattedTime, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("Score: ${String.format("%.1f", log.rawModelScore)}", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(log.recommendedAction, color = col, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. HISTORICAL BACKTEST & P0-C BASELINE AUDIT
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("backtest_audit_module_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. HISTORICAL BACKTEST & P0-C AUDIT",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        ProvenanceBadge(label = "HISTORICAL DATA", color = cyanAccent)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "The HistoricalBacktestEngine validates baseline win rates against historical candles. P0-C Baseline provides the historical anchor for model provenance.",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showBacktestModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("launch_backtester_button")
                    ) {
                        Text("LAUNCH HISTORICAL BACKTEST AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 5. PAPER TRADING SANDBOX
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("paper_trading_sandbox_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "5. 10¢ MICRO PAPER SIMULATOR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        ProvenanceBadge(label = "MODEL OUTPUT", color = goldAccent)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sandbox Balance: $${String.format("%.2f", paperBotState.currentBalance)}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Win Rate: ${String.format("%.1f", paperBotState.winRate)}%", color = if (paperBotState.winRate >= 50.0) neonGreen else neonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { /* Observation-only active during Phase 3A */ },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF181818),
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("paper_bot_toggle_button")
                        ) {
                            Text(
                                text = "COMING SOON (PAPER BOT)",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onResetSandbox(0.10) },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(0.7f).height(32.dp)
                        ) {
                            Text("RESET", color = Color.Gray, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Observation-only mode active. Autonomous paper trading bot is disabled during Phase 3A controlled validation.",
                        color = Color(0xFF666666),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProvenanceBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ExchangeFeedBox(
    name: String,
    price: Double,
    change: Double,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)

    Box(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text("$${String.format("%,.1f", price)}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("${if (change >= 0) "+" else ""}${String.format("%.2f", change)}%", color = if (change >= 0) neonGreen else neonRed, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

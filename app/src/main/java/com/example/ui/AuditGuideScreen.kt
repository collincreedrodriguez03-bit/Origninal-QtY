package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max

@Composable
fun AuditGuideScreen(
    paperBotState: PaperBotState,
    btcPrice: Double,
    onToggleSlippage: () -> Unit,
    onToggleWallClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val terminalDark = Color(0xFF0D0D0E)
    val cardDark = Color(0xFF141416)
    val cardBorder = Color(0xFF232429)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF3D71)
    val cyanAccent = Color(0xFF00E5FF)
    val amberAccent = Color(0xFFFFB300)
    val purpleAccent = Color(0xFFB388FF)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(terminalDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Executive Guidebook Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_guide_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, cyanAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(cyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Audit Guidebook",
                                    tint = cyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "1-WEEK SANDBOX AUDIT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = "Automated PnL, Drawdown & Sizing Guide",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = neonGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, neonGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(neonGreen)
                                )
                                Text(
                                    text = "100% HANDS-FREE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = neonGreen
                                )
                            }
                        }
                    }

                    Text(
                        text = "This screen acts as your automated audit ledger and institutional guidebook. All drawdown math, Kelly position sizing, and Kalshi spread tracking are computed automatically without requiring manual spreadsheet data entry.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B8),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 2. Automated Maximum Drawdown & Risk Meter
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_drawdown_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Risk Shield",
                                tint = amberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AUTOMATED DRAWDOWN & RISK AUDIT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    // Key Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Max Drawdown
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1C20),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("MAX DRAWDOWN", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                val dd = paperBotState.maxDrawdownDollars
                                val ddPct = paperBotState.maxDrawdownPercent
                                Text(
                                    text = "$${String.format("%.2f", dd)} (${String.format("%.1f", ddPct)}%)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (ddPct > 20.0) neonRed else neonGreen
                                )
                                Text(
                                    text = if (ddPct <= 10.0) "Optimal (<15%)" else "Caution",
                                    fontSize = 10.sp,
                                    color = if (ddPct <= 10.0) neonGreen else amberAccent
                                )
                            }
                        }

                        // Peak Balance
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1C20),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("HIGH WATERMARK", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "$${String.format("%.2f", paperBotState.peakBalance)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = cyanAccent
                                )
                                Text(
                                    text = "Peak Bankroll",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Profit Factor
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1C20),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("PROFIT FACTOR", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${String.format("%.2f", paperBotState.profitFactor)}x",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = purpleAccent
                                )
                                Text(
                                    text = "Wins / Losses",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Spread Drag & Realistic Kalshi Slippage Indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1B1C20),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Kalshi Spread",
                                        tint = cyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Kalshi 2¢ Spread & Fee Drag",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Total Drag Incurred: -$${String.format("%.2f", paperBotState.totalSlippagePaid)} (Accounts for realistic orderbook fills)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Switch(
                                checked = paperBotState.realisticSlippageEnabled,
                                onCheckedChange = { onToggleSlippage() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = neonGreen,
                                    checkedTrackColor = neonGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF2A2B30)
                                ),
                                modifier = Modifier.testTag("toggle_slippage_switch")
                            )
                        }
                    }
                }
            }
        }

        // 3. Automated Kelly Criterion Position Sizing Guide (Hard-Disabled)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_kelly_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, purpleAccent.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = "Kelly Sizing",
                                tint = purpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "KELLY SIZING: HARD-DISABLED",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2A1520)
                        ) {
                            Text(
                                text = "LOCKED (0.0)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = neonRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("KELLY POSITION FRACTION", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "0.0% (Hard Disabled)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("RISK POLICY", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "FLAT 10¢ / 1-CONTRACT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = cyanAccent
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF191A22)
                    ) {
                        Text(
                            text = "🛡 Risk Architecture: Automated Kelly leverage remains hard-disabled across all sample sizes (N<100, 100≤N<300, and N≥300) to protect against estimation error, regime shifts, and tail-risk ruin. The bot operates exclusively under fixed micro risk.",
                            fontSize = 11.sp,
                            color = Color(0xFFC0C0D0),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 4. Real-Time Kalshi 15-Minute Clock Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_clock_sync_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Clock Sync",
                                tint = cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "REAL KALSHI 15M CLOCK SYNC",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Switch(
                            checked = paperBotState.syncedWallClockMode,
                            onCheckedChange = { onToggleWallClock() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cyanAccent,
                                checkedTrackColor = cyanAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF2A2B30)
                            ),
                            modifier = Modifier.testTag("toggle_wall_clock_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NEXT SETTLEMENT", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text(
                                text = paperBotState.wallClockTargetFormatted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("CYCLE COUNTDOWN", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text(
                                text = paperBotState.wallClockFormattedTimer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = amberAccent
                            )
                        }
                    }

                    Text(
                        text = if (paperBotState.syncedWallClockMode) {
                            "⏱ Synced to real-world Kalshi contract settlement windows (:00, :15, :30, :45 past the hour). Trades enter in the high-conviction 12m–3m window."
                        } else {
                            "⚡ Fast-Forward Sandbox Speed active (35s testing intervals)."
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // 5. Automated 7-Day Performance Ledger
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_ledger_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "7-Day Ledger",
                                tint = neonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AUTOMATED 7-DAY LEDGER",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Auto-Computed",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1C20), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DAY", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                        Text("TRADES", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.9f))
                        Text("WIN %", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.9f))
                        Text("NET PNL", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
                        Text("STATUS", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
                    }

                    // Table Rows
                    paperBotState.dailyLedger.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = "${row.tradesCount} (${row.winCount}W/${row.lossCount}L)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFD0D0D8),
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = "${String.format("%.0f", row.winRate)}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (row.winRate >= 70.0) neonGreen else amberAccent,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = if (row.netPnL >= 0) "+$${String.format("%.2f", row.netPnL)}" else "-$${String.format("%.2f", abs(row.netPnL))}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (row.netPnL >= 0) neonGreen else neonRed,
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = row.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when (row.status) {
                                    "TARGET HIT" -> neonGreen
                                    "ON TRACK" -> cyanAccent
                                    else -> Color.Gray
                                },
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End
                            )
                        }
                        Divider(color = Color(0xFF1E1F24), thickness = 0.5.dp)
                    }
                }
            }
        }

        // 6. The 7-Day Sandbox Master Rules Guidebook
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_master_rules_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Master Rules",
                            tint = neonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "7-DAY SANDBOX MASTER RULES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    val rules = listOf(
                        "1. The 50-Trade Sample Minimum" to "Never risk real capital until the sandbox completes at least 50 automated trades across multiple market regimes.",
                        "2. The 68% Win Rate Benchmark" to "Factoring in 2¢ Kalshi spread drag, the system must sustain >= 68% win rate to maintain positive statistical expectancy.",
                        "3. The 30% Peak Drawdown Halt" to "If total drawdown from peak balance ever exceeds 30%, pause trading and inspect BTC volatility conditions.",
                        "4. Strict $20.00 Weekly Profit Lock" to "When weekly profit reaches +$20.00, the auto-bot locks gains to avoid late-cycle giveback."
                    )

                    rules.forEach { (ruleTitle, ruleDesc) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF191A20),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23242A))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = ruleTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = ruleDesc,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A0AC),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

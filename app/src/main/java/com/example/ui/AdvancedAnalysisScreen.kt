package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.live.LiveContractWindow
import com.example.data.live.LivePredictionLogRecord
import kotlin.math.abs
import kotlin.math.max

// ==========================================
// 2. ADVANCED ANALYSIS SCREEN (Tab 2)
// ==========================================
@Composable
fun AdvancedAnalysisScreen(
    btcPrice: Double,
    livePrediction: LivePredictionLogRecord?,
    liveWindow: LiveContractWindow,
    mtfTrend: MtfTrendConfirmation,
    rsi: Double,
    rsiHistory: List<Double>,
    momentum: Double,
    momentumHistory: List<Double>,
    ema9: Double,
    ema21: Double,
    volatility: Double,
    marketRegime: String,
    confidenceAnalysis: ConfidenceAnalysisState,
    selectedHorizon: String,
    forecastCycleCountdown: Int,
    settledPredictionsCount: Int,
    dataConfidenceTier: DataConfidenceTier,
    onTriggerRecalculate: () -> Unit,
    onSelectHorizon: (String) -> Unit
) {
    val cardBackground = Color(0xFF0C0C0C)
    val cardBorder = Color(0xFF222222)
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)
    val purpleAccent = Color(0xFFB388FF)

    val snapshot = livePrediction?.featureSnapshot
    val isBullish = livePrediction?.recommendedAction?.contains("UP") == true
    val isBearish = livePrediction?.recommendedAction?.contains("DOWN") == true
    val signalColor = when {
        isBullish -> neonGreen
        isBearish -> neonRed
        else -> Color.Gray
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QUANTITATIVE ANALYSIS ENGINE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = "4-Factor Confluence • Deadbands • Calibration",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                        .border(1.dp, purpleAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CYCLE: ${forecastCycleCountdown}s",
                        color = purpleAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }

        // ==========================================
        // 1. 4-FACTOR CONFLUENCE BREAKDOWN CARD (COMPACT GRID)
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, purpleAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("four_factor_confluence_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. 4-FACTOR CONFLUENCE BREAKDOWN",
                            color = purpleAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        val rawScore = livePrediction?.rawModelScore ?: 50.0
                        Text(
                            text = "SCORE: ${String.format("%.1f", rawScore)} / 100",
                            color = if (rawScore >= 55.0) neonGreen else if (rawScore <= 45.0) neonRed else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val emaSpread = snapshot?.emaSpread ?: (ema9 - ema21)
                    val momVal = snapshot?.momentum ?: momentum
                    val rsiVal = snapshot?.rsi ?: rsi
                    val deltaBuffer = liveWindow.researchDeltaToStrike

                    // High-density 2x2 Factor Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactFactorBox(
                            title = "EMA 9/21 RIBBON",
                            metric = "${if (emaSpread >= 0) "+" else ""}$${String.format("%.2f", emaSpread)}",
                            subText = if (emaSpread >= 0) "BULL TREND" else "BEAR TREND",
                            weight = "W: 1.50",
                            contribution = String.format("%+.2f", (emaSpread / 30.0).coerceIn(-1.5, 1.5)),
                            isPositive = emaSpread >= 0,
                            modifier = Modifier.weight(1f)
                        )
                        CompactFactorBox(
                            title = "1-MIN ROC VELOCITY",
                            metric = "${if (momVal >= 0) "+" else ""}$${String.format("%.2f", momVal)}/m",
                            subText = if (momVal >= 0) "UP VELOCITY" else "DOWN VELOCITY",
                            weight = "W: 1.20",
                            contribution = String.format("%+.2f", (momVal / 40.0).coerceIn(-1.2, 1.2)),
                            isPositive = momVal >= 0,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val rsiBias = when {
                            rsiVal < 35.0 -> "OVERSOLD"
                            rsiVal > 65.0 -> "OVERBOUGHT"
                            else -> "NEUTRAL"
                        }
                        CompactFactorBox(
                            title = "RSI (14) CHANNEL",
                            metric = "${String.format("%.1f", rsiVal)}/100",
                            subText = rsiBias,
                            weight = "W: 1.00",
                            contribution = String.format("%+.2f", ((50.0 - rsiVal) / 25.0).coerceIn(-1.0, 1.0)),
                            isPositive = rsiVal < 50.0,
                            modifier = Modifier.weight(1f)
                        )
                        CompactFactorBox(
                            title = "S₀ STRIKE BUFFER",
                            metric = "${if (deltaBuffer >= 0) "+" else ""}$${String.format("%.2f", deltaBuffer)}",
                            subText = if (deltaBuffer >= 0) "ABOVE S₀" else "BELOW S₀",
                            weight = "W: 1.20",
                            contribution = String.format("%+.2f", (deltaBuffer / 50.0).coerceIn(-1.2, 1.2)),
                            isPositive = deltaBuffer >= 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. DEADBAND MECHANICS & DECISION LOGIC
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("deadband_mechanics_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "2. DEADBAND & THRESHOLD LOGIC",
                        color = cyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Confluence Range: [-4.90, +4.90]\n" +
                               "• Deadband / NO TRADE Zone: [-0.50, +0.50]\n" +
                               "• BUY UP (YES) Threshold: Score ≥ +0.50\n" +
                               "• BUY DOWN (NO) Threshold: Score ≤ -0.50\n" +
                               "• Fail-Closed Safety: If confluence lacks statistical significance or data is stale, signal strictly locks to NO TRADE.",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // ==========================================
        // 3. QUANTITATIVE NARRATIVE RATIONALE
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("narrative_reason_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. QUANTITATIVE REASONING RATIONALE",
                            color = goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(signalColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, signalColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = livePrediction?.recommendedAction ?: "NO TRADE",
                                color = signalColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = livePrediction?.reasoning ?: "Engine evaluating live 10-second data against immutable research strike S₀.",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // ==========================================
        // 4. MULTI-TIMEFRAME TREND CONFIRMATION MATRIX
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("mtf_matrix_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "4. MULTI-TIMEFRAME (MTF) TREND MATRIX (${mtfTrend.summary})",
                        color = cyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MtfTimeframePill(title = "1M MICRO", trend = mtfTrend.tf1m.direction, delta = mtfTrend.tf1m.trendDelta, modifier = Modifier.weight(1f))
                        MtfTimeframePill(title = "5M LOCAL", trend = mtfTrend.tf5m.direction, delta = mtfTrend.tf5m.trendDelta, modifier = Modifier.weight(1f))
                        MtfTimeframePill(title = "15M HORIZON", trend = mtfTrend.tf15m.direction, delta = mtfTrend.tf15m.trendDelta, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ==========================================
        // 5. OSCILLATOR CHARTS (RSI & MOMENTUM)
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("rsi_graph_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "5. RSI (14) OSCILLATOR TRAJECTORY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "RSI: ${String.format("%.1f", rsi)}",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    RsiCanvasAnalysis(
                        rsiValues = rsiHistory,
                        currentRsi = rsi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Color(0xFF000000), RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("momentum_graph_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "6. 1-MIN MOMENTUM VELOCITY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${if (momentum >= 0) "+" else ""}$${String.format("%.1f", momentum)}/min",
                            color = if (momentum >= 0) neonGreen else neonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MomentumCanvasAnalysis(
                        momValues = momentumHistory,
                        currentMom = momentum,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF000000), RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    )
                }
            }
        }

        // ==========================================
        // 6. CONFIDENCE TRAJECTORY & RECALCULATE
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("confidence_comparison_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7. CONFIDENCE CONVICTION RATING",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = onTriggerRecalculate,
                            colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("recalculate_confidence_button").height(26.dp)
                        ) {
                            Text("RECALCULATE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ConfidenceCanvasAnalysis(
                        confidenceAnalysis = confidenceAnalysis,
                        selectedHorizon = selectedHorizon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // ==========================================
        // 7. EMPIRICAL CALIBRATION & DATA READINESS
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("empirical_calibration_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "8. EMPIRICAL CALIBRATION STATE",
                            color = goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, goldAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = dataConfidenceTier.name,
                                color = goldAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Settled Sample Progress: $settledPredictionsCount / 300 required for statistical calibration.\n" +
                               "Until N ≥ 300, probabilities remain uncalibrated raw heuristic scores.",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (settledPredictionsCount / 300f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = goldAccent,
                        trackColor = Color(0xFF1F2937),
                    )
                }
            }
        }

        // ==========================================
        // 8. SCIENTIFIC PARAMETER PROVENANCE TABLE
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("parameter_provenance_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "9. SCIENTIFIC PARAMETER PROVENANCE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Parameters active in LivePredictionEngine (Track D):\n" +
                               "• EMA Fast Period: 9 | EMA Slow Period: 21 (Weight: 1.50)\n" +
                               "• Momentum Period: 1m ROC (Weight: 1.20)\n" +
                               "• RSI Period: 14 (Weight: 1.00)\n" +
                               "• Strike Distance S₀: locked at :00,:15,:30,:45 (Weight: 1.20)\n" +
                               "• Status: HEURISTIC / UNVALIDATED pending forward walk-forward validation.",
                        color = Color.Gray,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // ==========================================
        // 9. SYSTEM & RESEARCH INTEGRITY (AUDIT GUIDE)
        // ==========================================
        item {
            var showAuditDetails by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("system_research_integrity_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10. SYSTEM & RESEARCH INTEGRITY",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = { showAuditDetails = !showAuditDetails },
                            colors = ButtonDefaults.buttonColors(containerColor = if (showAuditDetails) cyanAccent else Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp).testTag("toggle_audit_guide_button")
                        ) {
                            Text(
                                text = if (showAuditDetails) "COLLAPSE" else "EXPAND AUDIT",
                                color = if (showAuditDetails) Color.Black else cyanAccent,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Frozen Historical Benchmark (Section A): 46.27% Accuracy (N=2,576), Net P&L: -$173.28\n" +
                               "• Live Validation Radar (Section B): Forward walk-forward tracking on real Coinbase feed.\n" +
                               "• Absolute Boundary: Section A & B are never blended, averaged, or pooled.",
                        color = Color.LightGray,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )

                    if (showAuditDetails) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF070707), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "RESEARCH AUDIT SPECIFICATION & TENETS:",
                                    color = goldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "1. SHA-256 Benchmark Hash: 83a1adb653dbb86861ed30d76ec8964aebc2654bcb1efb29798e22d610f43358\n" +
                                           "2. Observation-Only Mandate: System does not execute trades, place orders, or claim predictive alpha.\n" +
                                           "3. Strike Isolation Rule: S₀ is fixed at window open (:00,:15,:30,:45 UTC); What-If controls cannot alter research inputs.\n" +
                                           "4. Kalshi Rule 5.1 Parity: Above Strike (YES if S₁₅ > S₀, NO if S₁₅ ≤ S₀, FLAT resolves NO).\n" +
                                           "5. Phase 3A Strong-Signal Hypothesis: Evaluates |C| ≥ 1.67 across Early (0-5m), Mid (5-10m), Late (10-15m) buckets.\n" +
                                           "6. Statistical Gating: N < 100 (Insufficient), N 100–299 (Preliminary), N ≥ 300 (Calibration-Ready).\n" +
                                           "7. Telemetry vs Samples: 10s live UI ticks are telemetry; each 15m contract yields exactly 1 independent outcome sample.",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
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

@Composable
fun CompactFactorBox(
    title: String,
    metric: String,
    subText: String,
    weight: String,
    contribution: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)

    Box(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = weight,
                    color = Color(0xFF777777),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (isPositive) neonGreen.copy(alpha = 0.15f) else neonRed.copy(alpha = 0.15f),
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = contribution,
                        color = if (isPositive) neonGreen else neonRed,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subText,
                color = if (isPositive) neonGreen else neonRed,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FactorRow(
    title: String,
    metric: String,
    status: String,
    weight: String,
    contribution: String,
    isPositive: Boolean
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(metric, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("• $status", color = if (isPositive) neonGreen else neonRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Text(weight, color = Color(0xFF666666), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        }

        Box(
            modifier = Modifier
                .background(if (isPositive) neonGreen.copy(alpha = 0.15f) else neonRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, if (isPositive) neonGreen.copy(alpha = 0.4f) else neonRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = contribution,
                color = if (isPositive) neonGreen else neonRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MtfTimeframePill(
    title: String,
    trend: String,
    delta: Double,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val neonRed = Color(0xFFFF1744)
    val isBull = trend.contains("BULL")

    Box(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(6.dp))
            .border(1.dp, if (isBull) neonGreen.copy(alpha = 0.4f) else neonRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = if (isBull) "▲ BULL" else "▼ BEAR",
                color = if (isBull) neonGreen else neonRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text("${if (delta >= 0) "+" else ""}$${String.format("%.1f", delta)}", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ConfidenceCanvasAnalysis(
    confidenceAnalysis: ConfidenceAnalysisState,
    selectedHorizon: String,
    modifier: Modifier = Modifier
) {
    val neonGreen = Color(0xFF00E676)
    val cyanAccent = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFD600)

    Canvas(
        modifier = modifier
            .background(Color(0xFF000000), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val minConf = 60.0
        val maxConf = 100.0
        val confRange = maxConf - minConf

        fun getY(conf: Double): Float {
            val normalized = (conf.coerceIn(minConf, maxConf) - minConf) / confRange
            return (height - (normalized * height)).toFloat().coerceIn(10f, height - 10f)
        }

        // Draw Conviction Zone Highlights
        val y90 = getY(90.0)
        val y80 = getY(80.0)
        val y70 = getY(70.0)

        drawRect(
            color = neonGreen.copy(alpha = 0.08f),
            topLeft = Offset(0f, 0f),
            size = Size(width, y90)
        )
        drawRect(
            color = cyanAccent.copy(alpha = 0.05f),
            topLeft = Offset(0f, y90),
            size = Size(width, y80 - y90)
        )
        drawRect(
            color = goldAccent.copy(alpha = 0.04f),
            topLeft = Offset(0f, y80),
            size = Size(width, y70 - y80)
        )

        val dividerX = width * 0.6f
        val history = confidenceAnalysis.history
        if (history.size >= 2) {
            val stepX = dividerX / (history.size - 1)
            val histPath = Path()
            histPath.moveTo(0f, getY(history[0].confidence))
            for (i in 1 until history.size) {
                histPath.lineTo(i * stepX, getY(history[i].confidence))
            }
            drawPath(path = histPath, color = cyanAccent, style = Stroke(width = 2.5f))
        }

        val curY = getY(confidenceAnalysis.currentConfidence)
        drawCircle(color = goldAccent, radius = 5f, center = Offset(dividerX, curY))

        val projY = getY(confidenceAnalysis.projectedConfidenceHorizon)
        drawLine(
            color = goldAccent,
            start = Offset(dividerX, curY),
            end = Offset(width - 8f, projY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        )
        drawCircle(color = neonGreen, radius = 4f, center = Offset(width - 8f, projY))
    }
}

@Composable
fun RsiCanvasAnalysis(
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

        val y70 = height * 0.3f
        val y30 = height * 0.7f

        drawRect(color = neonRed.copy(alpha = 0.08f), topLeft = Offset(0f, 0f), size = Size(width, y70))
        drawRect(color = neonGreen.copy(alpha = 0.08f), topLeft = Offset(0f, y30), size = Size(width, height - y30))

        drawLine(color = neonRed.copy(alpha = 0.4f), start = Offset(0f, y70), end = Offset(width, y70), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
        drawLine(color = neonGreen.copy(alpha = 0.4f), start = Offset(0f, y30), end = Offset(width, y30), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

        val values = rsiValues.ifEmpty { listOf(50.0, currentRsi) }
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

            drawPath(path = path, color = cyanAccent, style = Stroke(width = 2.5f))

            val lastY = getRsiY(currentRsi)
            drawCircle(color = cyanAccent, radius = 4f, center = Offset(width - 2f, lastY))
        }
    }
}

@Composable
fun MomentumCanvasAnalysis(
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

        drawLine(color = Color.White.copy(alpha = 0.2f), start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 1.5f)

        val values = momValues.ifEmpty { listOf(currentMom) }
        val maxMom = max(50.0, values.maxOfOrNull { abs(it) } ?: 50.0)

        val barWidth = (width / max(1, values.size)) * 0.7f
        val stepX = width / max(1, values.size)

        values.forEachIndexed { idx, v ->
            val x = idx * stepX + (stepX - barWidth) / 2f
            val normalized = (abs(v) / maxMom).toFloat().coerceIn(0.05f, 0.95f)
            val barHeight = normalized * (height / 2f)

            if (v >= 0) {
                drawRect(color = neonGreen.copy(alpha = 0.75f), topLeft = Offset(x, centerY - barHeight), size = Size(barWidth, barHeight))
            } else {
                drawRect(color = neonRed.copy(alpha = 0.75f), topLeft = Offset(x, centerY), size = Size(barWidth, barHeight))
            }
        }
    }
}

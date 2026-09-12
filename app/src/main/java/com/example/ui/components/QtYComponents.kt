package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QtYColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun QtYPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = QtYColors.Surface,
    borderColor: Color = QtYColors.BorderDefault,
    cornerRadius: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        content = {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content
            )
        }
    )
}

@Composable
fun QtYOutlinedPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = QtYColors.PrimaryCyan.copy(alpha = 0.4f),
    cornerRadius: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = QtYColors.SurfaceVariant.copy(alpha = 0.5f),
        content = {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content
            )
        }
    )
}

@Composable
fun QtYStatusDot(
    color: Color = QtYColors.LiveGreen,
    size: Dp = 8.dp,
    pulse: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pulse) color.copy(alpha = alpha) else color)
    )
}

@Composable
fun QtYSectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = QtYColors.PrimaryCyan,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 14.dp)
                    .background(QtYColors.PrimaryCyan, RoundedCornerShape(2.dp))
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QtYColors.TextPrimary
                    )
                    if (badgeText != null) {
                        QtYStatusBadge(text = badgeText, color = badgeColor)
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = QtYColors.TextSecondary
                    )
                }
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun QtYMetric(
    label: String,
    value: String,
    valueColor: Color = QtYColors.TextPrimary,
    subValue: String? = null,
    subValueColor: Color = QtYColors.TextSecondary
) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = QtYColors.TextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
        if (subValue != null) {
            Text(
                text = subValue,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = subValueColor
            )
        }
    }
}

@Composable
fun QtYDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        thickness = 1.dp,
        color = QtYColors.Divider
    )
}

@Composable
fun QtYGlowBorder(
    modifier: Modifier = Modifier,
    glowColor: Color = QtYColors.PrimaryCyan,
    cornerRadius: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.15f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .border(1.dp, glowColor.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
fun QtYStatusBadge(
    text: String,
    color: Color = QtYColors.PrimaryCyan
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun QtYBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple(0, "Prediction", Icons.Default.Timeline),
        Triple(1, "Engine Room", Icons.Default.Memory),
        Triple(2, "Backtest", Icons.Default.History)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = QtYColors.Surface,
        border = BorderStroke(1.dp, QtYColors.BorderDefault)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, title, icon) ->
                val selected = selectedTab == index
                val itemColor = if (selected) QtYColors.PrimaryCyan else QtYColors.TextSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(QtYColors.PrimaryCyan.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, QtYColors.PrimaryCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = QtYColors.PrimaryCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = itemColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = title.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = itemColor
                    )
                }
            }
        }
    }
}

@Composable
fun QtYHeader(
    title: String = "BTC PREDICTION ENGINE",
    subtitle: String = "Quant Telemetry • Live Coinbase Feed",
    cycleInfo: String = "",
    onOpenLiveRadar: () -> Unit = {}
) {
    val currentTimeUtc = remember {
        SimpleDateFormat("MMM dd, yyyy HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = QtYColors.Surface,
        border = BorderStroke(1.dp, QtYColors.BorderDefault)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Heartbeat / Logo Box
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(QtYColors.PrimaryCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, QtYColors.PrimaryCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "QtY Logo",
                        tint = QtYColors.PrimaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "QtY",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = QtYColors.PrimaryCyan
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QtYStatusBadge(text = "LIVE", color = QtYColors.LiveGreen)
                Text(
                    text = currentTimeUtc,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = QtYColors.TextSecondary
                )
                IconButton(
                    onClick = onOpenLiveRadar,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = QtYColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

package com.aegis.vault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/** A tab in the bottom bar. */
data class TabItem(val label: String, val icon: AegisIcon)

/**
 * The bottom tab bar from L4/L10-L14: floating 56dp pill bar, translucent white,
 * blue active tint with the light-blue highlight pill behind the active icon.
 */
@Composable
fun AegisTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(colors.tabBar)
            .padding(horizontal = 6.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(width = 48.dp, height = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(colors.highlightPill),
                        )
                    }
                    AegisIconGraphic(
                        icon = tab.icon,
                        contentDescription = null,
                        tint = if (selected) colors.blue else colors.textSecondary,
                        size = 22.dp,
                    )
                }
                Box(Modifier.size(2.dp))
                AegisText(
                    text = tab.label,
                    style = AegisType.micro,
                    color = if (selected) colors.blue else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The floating action button: 52dp gradient circle with the plus icon, floating
 * above the tab bar (resolves the mockups' stray purple dock FAB to the app's
 * blue gradient palette).
 */
@Composable
fun AegisFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Add item",
) {
    val colors = AegisTheme.colors
    PressableSurface(
        onClick = onClick,
        modifier = modifier.size(52.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd))),
        )
        AegisIconGraphic(
            icon = AegisIcon.Plus,
            contentDescription = contentDescription,
            tint = Color.White,
            size = 22.dp,
        )
    }
}

/** The Aegis logo: gradient squircle with the white key icon. */
@Composable
fun AegisLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    val colors = AegisTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(size * 0.25f))
            .clip(RoundedCornerShape(size * 0.25f))
            .background(Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd))),
        contentAlignment = Alignment.Center,
    ) {
        AegisIconGraphic(
            icon = AegisIcon.KeyRound,
            contentDescription = "Aegis",
            tint = Color.White,
            size = size * 0.42f,
        )
    }
}

/**
 * The strength segment bar from L2 (4 segments) and L10 (5 segments).
 * Color ramps red → amber → blue → green as the level rises.
 */
@Composable
fun StrengthMeter(
    level: Int,
    modifier: Modifier = Modifier,
    segmentCount: Int = 5,
) {
    val colors = AegisTheme.colors
    val filled = level.coerceIn(0, segmentCount)
    val fillColor = when {
        filled <= 1 -> colors.red
        filled == 2 -> colors.amber
        filled <= 3 -> colors.blue
        else -> colors.green
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(segmentCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < filled) fillColor else colors.track),
            )
        }
    }
}

/** Security score ring from L11: 118px ring, big number, caption underneath. */
@Composable
fun ScoreRing(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 118.dp,
) {
    val colors = AegisTheme.colors
    val clamped = score.coerceIn(0, 100)
    val ringColor = when {
        clamped >= 80 -> colors.green
        clamped >= 60 -> colors.amber
        else -> colors.red
    }
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = colors.track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped / 100f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AegisText(
                text = clamped.toString(),
                style = AegisType.titleCompact.copy(fontSize = AegisType.titleCompact.fontSize),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            AegisText(
                text = label,
                style = AegisType.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The empty-state block from L14 (notes, and reusable per category). */
@Composable
fun EmptyState(
    icon: AegisIcon,
    accent: Color,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    buttonLabel: String? = null,
    onButton: (() -> Unit)? = null,
    hint: String? = null,
) {
    val colors = AegisTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.tintFor(accent)),
            contentAlignment = Alignment.Center,
        ) {
            AegisIconGraphic(icon = icon, contentDescription = null, tint = accent, size = 40.dp)
        }
        Box(Modifier.size(24.dp))
        AegisText(
            text = title,
            style = AegisType.titleCompact,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.size(10.dp))
        AegisText(
            text = text,
            style = AegisType.subhead,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (buttonLabel != null && onButton != null) {
            Box(Modifier.size(28.dp))
            PrimaryButton(
                text = buttonLabel,
                onClick = onButton,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hint != null) {
            Box(Modifier.size(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AegisIconGraphic(
                    icon = AegisIcon.Lock,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    size = 14.dp,
                )
                Box(Modifier.size(8.dp))
                AegisText(
                    text = hint,
                    style = AegisType.caption,
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The password strength helper copy shared by L2's meter. */
fun strengthLabelFor(level: Int): String = when (level) {
    0, 1 -> "Weak"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Strong"
    else -> "Excellent"
}

package com.aegis.vault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The iOS switch from the mockups: 51x31 track with a 27px knob, green when on,
 * #EDEDF0 when off. Animated knob travel, no ripple.
 */
@Composable
fun IosToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    val trackColor = if (checked) colors.green else colors.chip
    val knobX by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "toggle-knob",
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) }
            .semantics { contentDescription = if (checked) "On" else "Off" },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((2.dp.toPx() + knobX * 20.dp.toPx()).toInt(), 0) }
                .size(27.dp)
                .shadow(elevation = 1.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * The generator length slider: 6px track, blue fill, 24px shadowed knob, with
 * tap-to-seek and drag. [value] is the current length in characters.
 */
@Composable
fun IosSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    val fraction = ((value - range.first).toFloat() / (range.last - range.first)).coerceIn(0f, 1f)
    var trackWidth by remember { mutableFloatStateOf(0f) }

    fun updateFromPosition(x: Float) {
        if (trackWidth <= 0f) return
        val raw = (x / trackWidth).coerceIn(0f, 1f)
        onValueChange(range.first + Math.round(raw * (range.last - range.first)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .semantics { contentDescription = "Length $value" },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track + fill.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.track),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.blue),
        )
        // Knob.
        Box(
            modifier = Modifier
                .offset { IntOffset(((trackWidth * fraction) - 12.dp.toPx()).toInt().coerceAtLeast(0), 0) }
                .size(24.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
        // Touch area overlaying the whole width.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { trackWidth = it.width.toFloat() }
                .pointerInput(range) {
                    detectTapGestures(
                        onPress = { offset ->
                            updateFromPosition(offset.x)
                        },
                    )
                }
                .pointerInput(range) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> updateFromPosition(offset.x) },
                        onDragEnd = { },
                        onDragCancel = { },
                    ) { change, _ ->
                        updateFromPosition(change.position.x)
                    }
                },
        )
    }
}

/**
 * iOS segmented control: light gray container, white selected segment with a
 * soft shadow. Used for Random/Memorable (L10) and filter modes.
 */
@Composable
fun SegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (colors.isDark) colors.cardElevated else colors.track)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            PressableSurface(
                onClick = { onSegmentSelected(index) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) colors.card else Color.Transparent,
            ) {
                AegisText(
                    text = label,
                    style = AegisType.footnoteStrong,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Rounded filter chip (All / Weak / Reused / No 2FA in L5, All/Logins/Notes/Cards in L13). */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    val background = if (selected) colors.blue else colors.card
    val textColor = if (selected) Color.White else colors.textSecondary
    PressableSurface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(17.dp),
        color = background,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            AegisText(
                text = label,
                style = AegisType.footnote,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * The green check circle marking a met master-password requirement in L2.
 * Shows an empty gray ring when [met] is false.
 */
@Composable
fun RequirementCheck(met: Boolean, modifier: Modifier = Modifier) {
    val colors = AegisTheme.colors
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (met) colors.green else Color.Transparent)
            .border(
                width = if (met) 0.dp else 1.5.dp,
                color = if (met) Color.Transparent else colors.chip,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (met) {
            AegisIconGraphic(
                icon = AegisIcon.Check,
                contentDescription = "Met",
                tint = Color.White,
                size = 12.dp,
            )
        }
    }
}

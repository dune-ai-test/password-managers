package com.aegis.vault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * A surface that gives iOS-style press feedback: content dims to 60% alpha while
 * held, with no ripple. Used for every tappable row and card in the app.
 */
@Composable
fun PressableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    color: Color = AegisTheme.colors.card,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.6f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "press-alpha",
    )
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

/** The primary call-to-action button: 52dp tall, 14dp radius, filled system blue. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    gradient: Boolean = false,
) {
    val colors = AegisTheme.colors
    val shape = RoundedCornerShape(14.dp)
    if (gradient) {
        PressableSurface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(height),
            shape = shape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd)),
                        shape = shape,
                    ),
            )
            AegisText(
                text = text,
                style = AegisType.button,
                color = Color.White,
                maxLines = 1,
            )
        }
    } else {
        PressableSurface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(height),
            shape = shape,
            color = colors.blue.copy(alpha = if (enabled) 1f else 0.4f),
        ) {
            AegisText(
                text = text,
                style = AegisType.button,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

/** Secondary button: light gray fill, blue label (the "Copy" pattern in L10). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AegisIcon? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val colors = AegisTheme.colors
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        color = colors.chip.copy(alpha = if (enabled) 1f else 0.5f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (icon != null) {
                AegisIconGraphic(
                    icon = icon,
                    contentDescription = null,
                    tint = colors.blue,
                    size = 20.dp,
                )
                Box(Modifier.size(10.dp))
            }
            AegisText(
                text = text,
                style = AegisType.bodyStrong,
                color = colors.blue,
                maxLines = 1,
            )
        }
    }
}

/** Quiet text-only link ("I already have a vault", "Forgot master password?"). */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AegisTheme.colors.blue,
    style: TextStyle = AegisType.subhead,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        AegisText(text = text, style = style, color = color, maxLines = 1)
    }
}

/** The circular icon button used in nav bars (back chevron, history, sort...). */
@Composable
fun IconButton38(
    icon: AegisIcon,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AegisTheme.colors.textPrimary,
    background: Color = AegisTheme.colors.card,
    buttonSize: Dp = 38.dp,
    iconSize: Dp = 18.dp,
) {
    PressableSurface(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = RoundedCornerShape(19.dp),
        color = background,
    ) {
        AegisIconGraphic(
            icon = icon,
            contentDescription = contentDescription,
            tint = tint,
            size = iconSize,
        )
    }
}

/** Filled width convenience used by screens for full-width buttons. */
fun Modifier.fullWidth(): Modifier = this.fillMaxWidth()

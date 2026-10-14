package com.aegis.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The standard rounded surface every card in the mockups uses.
 */
@Composable
fun AegisCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    color: Color = AegisTheme.colors.card,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(color)
            .padding(contentPadding),
    ) {
        content()
    }
}

/**
 * Full-screen background. The app draws with the real Android status bar, so this
 * only keeps content out of the system-bar cutouts.
 */
@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AegisTheme.colors.background),
    ) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            content()
        }
    }
}

/** "Recently used" + "See all" style section header. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionColor: Color = AegisTheme.colors.blue,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AegisText(
            text = title,
            style = AegisType.captionStrong,
            color = colors.textSecondary,
        )
        if (actionLabel != null && onAction != null) {
            TextLink(
                text = actionLabel,
                onClick = onAction,
                color = actionColor,
                style = AegisType.footnote,
            )
        }
    }
}

/** 1px hairline in the mockup separator color. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, color: Color = AegisTheme.colors.separator) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** Small rounded pill: the "Offline" chip in L4, tags in L9. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = AegisTheme.colors.chip,
    contentColor: Color = AegisTheme.colors.textSecondary,
    textStyle: TextStyle = AegisType.caption,
    icon: AegisIcon? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            AegisIconGraphic(
                icon = icon,
                contentDescription = null,
                tint = contentColor,
                size = 12.dp,
            )
            Box(Modifier.padding(horizontal = 3.dp))
        }
        AegisText(text = text, style = textStyle, color = contentColor, maxLines = 1)
    }
}

/** The "Local-only vault · 248 KB on this device" strip from L4. */
@Composable
fun StorageStrip(
    storageText: String,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AegisIconGraphic(
                icon = AegisIcon.HardDrive,
                contentDescription = null,
                tint = colors.textSecondary,
                size = 16.dp,
            )
            Box(Modifier.padding(horizontal = 8.dp))
            AegisText(
                text = storageText,
                style = AegisType.caption,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        Pill(text = "Offline")
    }
}

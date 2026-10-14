package com.aegis.vault.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The app's single text composable — a thin wrapper over foundation's [BasicText]
 * that applies the Aegis palette by default. (No Material: `material3.Text` is not
 * a dependency of this project.)
 */
@Composable
fun AegisText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AegisType.body,
    color: Color = AegisTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    lineHeight: TextUnit? = null,
    letterSpacing: TextUnit? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val effectiveStyle = style.copy(
        color = color,
        fontSize = style.fontSize,
        letterSpacing = letterSpacing ?: style.letterSpacing,
    ).let { base ->
        if (lineHeight != null) base.copy(lineHeight = lineHeight) else base
    }.let { base ->
        if (textAlign != null) base.copy(textAlign = textAlign) else base
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = effectiveStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        maxLines = maxLines,
        minLines = 1,
    )
}

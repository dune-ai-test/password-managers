package com.aegis.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The app's confirmation dialog — a plain foundation overlay card. No Material
 * dialogs are used anywhere in this app.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    val colors = AegisTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.shadow.copy(alpha = 0.45f))
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        AegisCard(radius = 20.dp, contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AegisText(text = title, style = AegisType.bodyStrong)
                AegisText(
                    text = message,
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(
                        text = dismissLabel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                    )
                    if (destructive) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.red)
                                .clickable(onClick = onConfirm),
                            contentAlignment = Alignment.Center,
                        ) {
                            AegisText(
                                text = confirmLabel,
                                style = AegisType.bodyStrong,
                                color = Color.White,
                            )
                        }
                    } else {
                        PrimaryButton(
                            text = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            height = 44.dp,
                        )
                    }
                }
            }
        }
    }
}

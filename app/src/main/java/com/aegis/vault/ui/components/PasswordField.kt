package com.aegis.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The white rounded password input from L2/L3: lock glyph, masked value with
 * 3dp letter spacing (like the mock), and a show/hide eye toggle.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Master password",
    showPassword: Boolean = false,
    onToggleShow: () -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val colors = AegisTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card),
        singleLine = true,
        textStyle = if (showPassword) {
            AegisType.body
        } else {
            AegisType.body.copy(letterSpacing = 3.sp)
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        cursorBrush = SolidColor(colors.blue),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AegisIconGraphic(
                    icon = AegisIcon.Lock,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    size = 20.dp,
                )
                Box(Modifier.size(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        AegisText(
                            text = placeholder,
                            style = if (showPassword) AegisType.body else AegisType.body.copy(letterSpacing = 0.sp),
                            color = colors.textTertiary,
                        )
                    }
                    innerTextField()
                }
                Box(Modifier.size(10.dp))
                AegisIconGraphic(
                    icon = AegisIcon.Eye,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint = colors.textSecondary,
                    size = 20.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleShow,
                        )
                        .padding(4.dp),
                )
            }
        },
    )
}

package com.aegis.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * The vault list row from L4/L5/L11: 64dp white card, 14dp gap, 40px letter
 * badge, title + subtitle, trailing chevron.
 */
@Composable
fun ItemRow(
    title: String,
    subtitle: String,
    badgeLetter: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
) {
    val colors = AegisTheme.colors
    PressableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 11.dp, end = 14.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterBadge(letter = badgeLetter, color = badgeColor, size = 40.dp)
            Box(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                AegisText(text = title, style = AegisType.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.size(2.dp))
                AegisText(text = subtitle, style = AegisType.footnote, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showChevron) {
                Box(Modifier.size(10.dp))
                AegisIconGraphic(
                    icon = AegisIcon.ChevronRight,
                    contentDescription = null,
                    tint = AegisTheme.colors.textTertiary,
                    size = 14.dp,
                )
            }
        }
    }
}

/**
 * The category tile from L4 (Logins/Cards/Notes/Identities): 88dp, squircle icon
 * badge, name and item count.
 */
@Composable
fun CategoryTile(
    name: String,
    count: String,
    icon: AegisIcon,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    PressableSurface(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.tintFor(accent)),
                contentAlignment = Alignment.Center,
            ) {
                AegisIconGraphic(icon = icon, contentDescription = null, tint = accent, size = 18.dp)
            }
            Box(Modifier.size(6.dp))
            AegisText(text = name, style = AegisType.footnote, color = colors.textPrimary, maxLines = 1)
            Box(Modifier.size(2.dp))
            AegisText(text = count, style = AegisType.caption, color = colors.textSecondary, maxLines = 1)
        }
    }
}

/** Action rows inside the detail screens' action cards (L6/L8/L9). */
@Composable
fun ActionRow(
    icon: AegisIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AegisTheme.colors.blue,
    destructive: Boolean = false,
) {
    val colors = AegisTheme.colors
    val labelColor = if (destructive) colors.red else colors.textPrimary
    PressableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AegisIconGraphic(icon = icon, contentDescription = null, tint = tint, size = 19.dp)
            Box(Modifier.size(14.dp))
            AegisText(text = label, style = AegisType.body, color = labelColor, maxLines = 1)
        }
    }
}

/**
 * Settings rows from L12: 34px squircle icon badge, label, then a value, toggle
 * or chevron. Height 52, grouped 16px cards.
 */
@Composable
fun SettingsRow(
    icon: AegisIcon,
    accent: Color,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    toggleState: Boolean? = null,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AegisTheme.colors
    val rowModifier = modifier
        .fillMaxWidth()
        .height(52.dp)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
            } else {
                Modifier
            },
        )
    Row(
        modifier = rowModifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.tintFor(accent)),
            contentAlignment = Alignment.Center,
        ) {
            AegisIconGraphic(icon = icon, contentDescription = null, tint = accent, size = 17.dp)
        }
        Box(Modifier.size(12.dp))
        AegisText(text = label, style = AegisType.body, modifier = Modifier.weight(1f), maxLines = 1)
        if (value != null) {
            AegisText(text = value, style = AegisType.subhead, color = colors.textSecondary, maxLines = 1)
            Box(Modifier.size(6.dp))
        }
        when {
            toggleState != null && onToggle != null -> IosToggle(
                checked = toggleState,
                onCheckedChange = onToggle,
            )
            onClick != null -> AegisIconGraphic(
                icon = AegisIcon.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                size = 18.dp,
            )
        }
    }
}

/** Label + value rows in the credential/detail cards (L6/L8). */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 112.dp,
    onClick: (() -> Unit)? = null,
    trailingIcon: AegisIcon? = null,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AegisText(
            text = label,
            style = AegisType.body,
            color = colors.textSecondary,
            modifier = Modifier.width(labelWidth),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.size(12.dp))
        AegisText(
            text = value,
            style = AegisType.body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Box(Modifier.size(10.dp))
            AegisIconGraphic(icon = trailingIcon, contentDescription = null, tint = colors.blue, size = 18.dp)
        }
    }
}

/**
 * The top navigation bar from L5/L6/L7/L8/L9: optional back button, centered
 * title, optional trailing action text, optional leading icon action (L5 sort).
 */
@Composable
fun TopNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    leadingAction: (() -> Unit)? = null,
    leadingIcon: AegisIcon = AegisIcon.SlidersHorizontal,
    leadingContentDescription: String? = null,
    trailingAction: (() -> Unit)? = null,
    trailingLabel: String? = null,
    trailingIcon: AegisIcon? = null,
) {
    val colors = AegisTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        if (onBack != null) {
            IconButton38(
                icon = AegisIcon.ChevronLeft,
                onClick = onBack,
                contentDescription = backContentDescription ?: "Back",
                modifier = Modifier.align(Alignment.CenterStart),
            )
        } else if (leadingAction != null) {
            IconButton38(
                icon = leadingIcon,
                onClick = leadingAction,
                contentDescription = leadingContentDescription,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        AegisText(
            text = title,
            style = AegisType.navTitle,
            modifier = Modifier.align(Alignment.Center),
            maxLines = 1,
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (trailingIcon != null && trailingAction != null) {
                IconButton38(
                    icon = trailingIcon,
                    onClick = trailingAction,
                    contentDescription = null,
                )
            }
            if (trailingLabel != null && trailingAction != null) {
                TextLink(
                    text = trailingLabel,
                    onClick = trailingAction,
                    color = colors.blue,
                    style = AegisType.subhead.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                )
            }
        }
    }
}

/** Letter badge with deterministic squircle shape from the mockups (40px, r12). */
@Composable
fun LetterBadge(
    letter: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    textStyle: TextStyle = AegisType.bodyStrong.copy(fontSize = 17.sp),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        AegisText(
            text = letter.take(1).uppercase(),
            style = textStyle,
            color = Color.White,
            maxLines = 1,
        )
    }
}

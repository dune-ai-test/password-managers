package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.security.PasswordGenerator
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.IconButton38
import com.aegis.vault.ui.components.IosSlider
import com.aegis.vault.ui.components.IosToggle
import com.aegis.vault.ui.components.PressableSurface
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SecondaryButton
import com.aegis.vault.ui.components.SegmentedControl
import com.aegis.vault.ui.components.StrengthMeter
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/** Label shown next to the strength meter (L2/L10). */
internal fun strengthLabelFor(level: Int): String = when (level) {
    0, 1 -> "Weak"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Strong"
    else -> "Excellent"
}

/**
 * L10 — the password generator: display card, length slider, character options,
 * random/memorable modes, and the (in-memory) history of recent generations.
 */
@Composable
fun GeneratorScreen(locator: ServiceLocator) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val generator = remember { PasswordGenerator() }
    val scanner = locator.securityScanner

    var length by remember { mutableIntStateOf(20) }
    var uppercase by remember { mutableStateOf(true) }
    var lowercase by remember { mutableStateOf(true) }
    var numbers by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var avoidLookAlikes by remember { mutableStateOf(false) }
    var memorable by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    var password by remember {
        mutableStateOf(
            generator.generate(
                PasswordGenerator.Options(length = length),
            ),
        )
    }

    val options = PasswordGenerator.Options(
        length = length,
        uppercase = uppercase,
        lowercase = lowercase,
        numbers = numbers,
        symbols = symbols,
        avoidLookAlikes = avoidLookAlikes,
        memorable = memorable,
    )
    val level = scanner.strengthLevel(password)

    fun regenerate() {
        password = generator.generate(options)
        history.add(0, password)
        if (history.size > 10) history.removeAt(history.lastIndex)
        copied = false
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Nav bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.size(38.dp))
                AegisText("Generator", style = AegisType.navTitle)
                IconButton38(
                    icon = AegisIcon.RotateCcw,
                    onClick = { showHistory = !showHistory },
                    contentDescription = "History",
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showHistory) {
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                        Column {
                            AegisText("Recent generations", style = AegisType.bodyStrong)
                            Box(Modifier.size(8.dp))
                            if (history.isEmpty()) {
                                AegisText(
                                    "Nothing generated yet this session.",
                                    style = AegisType.footnote,
                                    color = colors.textSecondary,
                                )
                            } else {
                                history.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AegisText(
                                            text = entry,
                                            style = AegisType.footnote,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                        AegisIconGraphic(
                                            icon = AegisIcon.Copy,
                                            contentDescription = "Copy",
                                            tint = colors.blue,
                                            size = 16.dp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null,
                                                ) {
                                                    if (copyToClipboard(context, "Password", entry)) {
                                                        copied = true
                                                    }
                                                }
                                                .padding(4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Display card.
                AegisCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp, contentPadding = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AegisText(
                            text = password,
                            style = AegisType.passwordDisplay,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            AegisText("Strength", style = AegisType.footnote, color = colors.textSecondary)
                            AegisText(
                                text = strengthLabelFor(level),
                                style = AegisType.footnoteStrong,
                                color = when {
                                    level <= 1 -> colors.red
                                    level == 2 -> colors.amber
                                    level <= 3 -> colors.blue
                                    else -> colors.green
                                },
                            )
                        }
                        StrengthMeter(level = level, segmentCount = 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            PressableSurface(
                                onClick = { regenerate() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = colors.blue,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    AegisIconGraphic(
                                        icon = AegisIcon.RefreshCw,
                                        contentDescription = null,
                                        tint = colors.onAccent,
                                        size = 20.dp,
                                    )
                                    Box(Modifier.size(10.dp))
                                    AegisText(
                                        text = "Regenerate",
                                        style = AegisType.bodyStrong,
                                        color = colors.onAccent,
                                    )
                                }
                            }
                            SecondaryButton(
                                text = if (copied) "Copied!" else "Copy",
                                onClick = {
                                    if (copyToClipboard(context, "Password", password)) {
                                        copied = true
                                    }
                                },
                                icon = AegisIcon.Copy,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Length card.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            AegisText("Length", style = AegisType.body)
                            AegisText(
                                text = length.toString(),
                                style = AegisType.bodyStrong,
                            )
                        }
                        IosSlider(
                            value = length,
                            onValueChange = {
                                length = it
                                copied = false
                            },
                            range = 8..40,
                        )
                    }
                }

                // Options card.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    Column {
                        OptionToggle("Uppercase (A–Z)", uppercase) { uppercase = it; copied = false }
                        OptionToggle("Lowercase (a–z)", lowercase) { lowercase = it; copied = false }
                        OptionToggle("Numbers (0–9)", numbers) { numbers = it; copied = false }
                        OptionToggle("Symbols (!@#)", symbols) { symbols = it; copied = false }
                        OptionToggle("Avoid look-alikes", avoidLookAlikes) { avoidLookAlikes = it; copied = false }
                    }
                }

                // Mode.
                SegmentedControl(
                    segments = listOf("Random", "Memorable"),
                    selectedIndex = if (memorable) 1 else 0,
                    onSegmentSelected = { index ->
                        memorable = index == 1
                        copied = false
                    },
                )

                PrimaryButton(
                    text = "Generate new password",
                    onClick = { regenerate() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OptionToggle(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AegisText(label, style = AegisType.body)
        IosToggle(checked = checked, onCheckedChange = onChange)
    }
}

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.crypto.VaultCrypto
import com.aegis.vault.data.model.CardItem
import com.aegis.vault.data.model.IdentityItem
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.data.model.NoteItem
import com.aegis.vault.data.security.PasswordGenerator
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.PressableSurface
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SecondaryButton
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L7 — new/edit item. One screen for all four item types, with the mock's type
 * selector, per-type form rows, the inline generator (login), and the suggestion
 * card. Editing loads the item; an empty id means "create".
 */
@Composable
fun ItemEditorScreen(
    locator: ServiceLocator,
    itemType: ItemType,
    itemId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository
    val generator = remember { PasswordGenerator() }

    val isEditing = itemId.isNotBlank()
    val existingLogin = remember(itemId) { if (isEditing) repo.login(itemId) else null }
    val existingCard = remember(itemId) { if (isEditing) repo.card(itemId) else null }
    val existingNote = remember(itemId) { if (isEditing) repo.note(itemId) else null }
    val existingIdentity = remember(itemId) { if (isEditing) repo.identity(itemId) else null }

    var selectedType by remember { mutableStateOf(itemType) }
    var title by remember { mutableStateOf(existingLogin?.title ?: existingCard?.title ?: existingNote?.title ?: existingIdentity?.title.orEmpty()) }
    var username by remember { mutableStateOf(existingLogin?.username.orEmpty()) }
    var password by remember { mutableStateOf(existingLogin?.password.orEmpty()) }
    var website by remember { mutableStateOf(existingLogin?.website.orEmpty()) }
    var totpSecret by remember { mutableStateOf(existingLogin?.totpSecret.orEmpty()) }
    var notes by remember { mutableStateOf(existingLogin?.notes ?: existingNote?.body.orEmpty()) }
    var bankName by remember { mutableStateOf(existingCard?.bankName.orEmpty()) }
    var network by remember { mutableStateOf(existingCard?.network.orEmpty()) }
    var cardNumber by remember { mutableStateOf(existingCard?.number.orEmpty()) }
    var expiry by remember { mutableStateOf(existingCard?.expiry.orEmpty()) }
    var securityCode by remember { mutableStateOf(existingCard?.securityCode.orEmpty()) }
    var cardholder by remember { mutableStateOf(existingCard?.cardholder.orEmpty()) }
    var billingAddress by remember { mutableStateOf(existingCard?.billingAddress.orEmpty()) }
    var tags by remember { mutableStateOf(existingNote?.tags?.joinToString(", ").orEmpty()) }
    var firstName by remember { mutableStateOf(existingIdentity?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(existingIdentity?.lastName.orEmpty()) }
    var email by remember { mutableStateOf(existingIdentity?.email.orEmpty()) }
    var phone by remember { mutableStateOf(existingIdentity?.phone.orEmpty()) }
    var address by remember { mutableStateOf(existingIdentity?.address.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val suggested = remember { generator.generate(PasswordGenerator.Options(length = 16)) }

    fun save() {
        when (selectedType) {
            ItemType.Login -> {
                if (title.isBlank() || username.isBlank() || password.isBlank()) {
                    error = "Title, username and password are required"
                    return
                }
                val item = LoginItem(
                    id = existingLogin?.id ?: VaultCrypto.randomUuid(),
                    title = title.trim(),
                    username = username.trim(),
                    password = password,
                    website = website.trim(),
                    notes = notes.trim(),
                    totpSecret = totpSecret.trim(),
                    isDemo = false,
                    createdAt = existingLogin?.createdAt ?: 0L,
                )
                repo.saveLogin(item, originalPassword = existingLogin?.password?.takeIf { it != password })
            }

            ItemType.Card -> {
                if (title.isBlank() || cardNumber.isBlank()) {
                    error = "Title and card number are required"
                    return
                }
                val item = CardItem(
                    id = existingCard?.id ?: VaultCrypto.randomUuid(),
                    title = title.trim(),
                    bankName = bankName.trim(),
                    network = network.trim(),
                    cardholder = cardholder.trim(),
                    number = cardNumber.trim(),
                    expiry = expiry.trim(),
                    securityCode = securityCode.trim(),
                    billingAddress = billingAddress.trim(),
                    notes = notes.trim(),
                    isDemo = false,
                    createdAt = existingCard?.createdAt ?: 0L,
                )
                repo.saveCard(item)
            }

            ItemType.Note -> {
                if (title.isBlank()) {
                    error = "A title is required"
                    return
                }
                val item = NoteItem(
                    id = existingNote?.id ?: VaultCrypto.randomUuid(),
                    title = title.trim(),
                    body = notes.trim(),
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    isDemo = false,
                    createdAt = existingNote?.createdAt ?: 0L,
                )
                repo.saveNote(item)
            }

            ItemType.Identity -> {
                if (title.isBlank()) {
                    error = "A title is required"
                    return
                }
                val item = IdentityItem(
                    id = existingIdentity?.id ?: VaultCrypto.randomUuid(),
                    title = title.trim(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    notes = notes.trim(),
                    isDemo = false,
                    createdAt = existingIdentity?.createdAt ?: 0L,
                )
                repo.saveIdentity(item)
            }
        }
        onSaved()
    }

    val navTitle = if (isEditing) "Edit ${selectedType.name.lowercase()}" else "New ${selectedType.name.lowercase()}"

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(
                title = navTitle.replaceFirstChar { it.uppercase() },
                onBack = onCancel,
                backContentDescription = "Cancel",
                trailingLabel = "Save",
                trailingAction = { save() },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Type selector.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ItemType.entries.forEach { type ->
                        TypeTile(
                            label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            icon = when (type) {
                                ItemType.Login -> AegisIcon.KeyRound
                                ItemType.Card -> AegisIcon.CreditCard
                                ItemType.Note -> AegisIcon.FileText
                                ItemType.Identity -> AegisIcon.User
                            },
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Form.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    when (selectedType) {
                        ItemType.Login -> {
                            FormRow("Title", title, { title = it }, placeholder = "github.com")
                            FormRow("Username", username, { username = it }, placeholder = "alex@meridian.io")
                            PasswordFormRow(
                                label = "Password",
                                value = password,
                                onValueChange = { password = it },
                                onGenerate = {
                                    password = generator.generate(PasswordGenerator.Options(length = 20))
                                },
                            )
                            FormRow("Website", website, { website = it }, placeholder = "https://github.com")
                            FormRow("Authenticator secret", totpSecret, { totpSecret = it }, placeholder = "Optional TOTP secret")
                        }

                        ItemType.Card -> {
                            FormRow("Title", title, { title = it }, placeholder = "Chase Sapphire")
                            FormRow("Bank", bankName, { bankName = it }, placeholder = "Chase")
                            FormRow("Network", network, { network = it }, placeholder = "VISA")
                            FormRow("Number", cardNumber, { cardNumber = it }, placeholder = "4242 4242 4242 4242", keyboardType = KeyboardType.Number)
                            FormRow("Expires", expiry, { expiry = it }, placeholder = "09/28")
                            FormRow("Security code", securityCode, { securityCode = it }, placeholder = "482", keyboardType = KeyboardType.NumberPassword)
                            FormRow("Cardholder", cardholder, { cardholder = it }, placeholder = "ALEX MORGAN")
                            FormRow("Billing address", billingAddress, { billingAddress = it }, placeholder = "24 Harbour St")
                        }

                        ItemType.Note -> {
                            FormRow("Title", title, { title = it }, placeholder = "Passport")
                            MultilineFormRow("Note", notes, { notes = it }, placeholder = "Body of the note…")
                            FormRow("Tags", tags, { tags = it }, placeholder = "Documents, Personal")
                        }

                        ItemType.Identity -> {
                            FormRow("Title", title, { title = it }, placeholder = "Alex Morgan")
                            FormRow("First name", firstName, { firstName = it })
                            FormRow("Last name", lastName, { lastName = it })
                            FormRow("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                            FormRow("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
                            FormRow("Address", address, { address = it })
                        }
                    }
                }

                // Suggestion card (logins).
                if (selectedType == ItemType.Login && !isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.tintFor(colors.blue))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AegisIconGraphic(
                            icon = AegisIcon.RefreshCw,
                            contentDescription = null,
                            tint = colors.blue,
                            size = 16.dp,
                        )
                        AegisText(
                            text = "Suggested: $suggested",
                            style = AegisType.footnote,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        AegisIconGraphic(
                            icon = AegisIcon.Copy,
                            contentDescription = "Use suggestion",
                            tint = colors.blue,
                            size = 16.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    copyToClipboard(context, "Suggested password", suggested)
                                }
                                .padding(4.dp),
                        )
                    }
                }

                error?.let {
                    AegisText(it, style = AegisType.footnote, color = colors.red)
                }

                PrimaryButton(
                    text = if (isEditing) "Save changes" else "Save ${selectedType.name.lowercase()}",
                    onClick = { save() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TypeTile(
    label: String,
    icon: AegisIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    val accent = when (label.lowercase()) {
        "login" -> colors.blue
        "card" -> colors.purple
        "note" -> colors.green
        else -> colors.amber
    }
    PressableSurface(
        onClick = onClick,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.tintFor(accent) else colors.card,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) accent else colors.chip),
                contentAlignment = Alignment.Center,
            ) {
                AegisIconGraphic(
                    icon = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else colors.textSecondary,
                    size = 17.dp,
                )
            }
            Box(Modifier.size(6.dp))
            AegisText(
                text = label,
                style = AegisType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = if (selected) accent else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FormRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = AegisTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        AegisText(label, style = AegisType.caption, color = colors.textSecondary)
        Box(Modifier.size(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = AegisType.body,
            cursorBrush = SolidColor(colors.blue),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        AegisText(placeholder, style = AegisType.body, color = colors.textTertiary)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun PasswordFormRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    val colors = AegisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AegisText(label, style = AegisType.caption, color = colors.textSecondary)
            Box(Modifier.size(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = AegisType.body,
                cursorBrush = SolidColor(colors.blue),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            AegisText("••••••••", style = AegisType.body, color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
        }
        SecondaryButton(
            text = "Generate",
            onClick = onGenerate,
            icon = AegisIcon.RefreshCw,
            height = 38.dp,
        )
    }
}

@Composable
private fun MultilineFormRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = AegisTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        AegisText(label, style = AegisType.caption, color = colors.textSecondary)
        Box(Modifier.size(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            textStyle = AegisType.body,
            cursorBrush = SolidColor(colors.blue),
            singleLine = false,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        AegisText(placeholder, style = AegisType.body, color = colors.textTertiary)
                    }
                    inner()
                }
            },
        )
    }
}

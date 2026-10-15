package com.aegis.vault.data.seed

import com.aegis.vault.data.crypto.VaultCrypto
import com.aegis.vault.data.model.CardItem
import com.aegis.vault.data.model.IdentityItem
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.data.model.NoteItem

/**
 * The demo vault inserted on first launch — the same items the mockup screens
 * show (github.com, mail.google.com, Chase Sapphire, the Passport note...).
 *
 * Everything here is fabricated sample data: no real credentials, no real people.
 * The "reused" and "breached" logins are intentional so the Security screen has
 * something honest to show; the Settings "Delete sample data" action clears every
 * item with [LoginItem.isDemo] set.
 */
object SeedData {

    private val day = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()

    /** Shared across five accounts on purpose — powers the "Reused" attention row. */
    private const val SHARED_PASSWORD = "Summer21!"

    private fun login(
        title: String,
        username: String,
        password: String,
        website: String,
        notes: String = "",
        totpSecret: String = "",
        modifiedDaysAgo: Int = 0,
        createdDaysAgo: Int = 365,
    ) = LoginItem(
        id = VaultCrypto.randomUuid(),
        title = title,
        username = username,
        password = password,
        website = website,
        notes = notes,
        totpSecret = totpSecret,
        isDemo = true,
        createdAt = now - createdDaysAgo * day,
        lastModified = now - modifiedDaysAgo * day,
    )

    val logins: List<LoginItem> = listOf(
        login(
            title = "github.com",
            username = "alex@meridian.io",
            password = SHARED_PASSWORD,
            website = "https://github.com",
            notes = "Recovery codes saved in Safe #4. Team account — ask Dana before resetting.",
            totpSecret = "JBSWY3DPEHPK3PXP",
            modifiedDaysAgo = 14,
            createdDaysAgo = 900,
        ),
        login(
            title = "mail.google.com",
            username = "alex@meridian.io",
            password = SHARED_PASSWORD,
            website = "https://mail.google.com",
            modifiedDaysAgo = 40,
            createdDaysAgo = 800,
        ),
        login(
            title = "figma.com",
            username = "alex@meridian.io",
            password = SHARED_PASSWORD,
            website = "https://figma.com",
            modifiedDaysAgo = 2,
            createdDaysAgo = 500,
        ),
        login(
            title = "figma.com (work)",
            username = "alex@meridian.io · work",
            password = "7fKd92#LzAx!Pq3",
            website = "https://figma.com",
            notes = "Organization-wide billing, keep on the work profile.",
            modifiedDaysAgo = 20,
            createdDaysAgo = 220,
        ),
        login(
            title = "chase.com",
            username = "amorgan_92",
            password = "Tr0ut!Run-2023-Vault",
            website = "https://chase.com",
            notes = "Bank account — unique password, 2FA via SMS.",
            // Untouched for years, like the mockup's "Unchanged for 3 years" row.
            modifiedDaysAgo = 1100,
            createdDaysAgo = 1900,
        ),
        login(
            title = "netflix.com",
            username = "alex@meridian.io",
            password = SHARED_PASSWORD,
            website = "https://netflix.com",
            modifiedDaysAgo = 300,
            createdDaysAgo = 1200,
        ),
        login(
            title = "notion.so",
            username = "alex@meridian.io",
            password = SHARED_PASSWORD,
            website = "https://notion.so",
            modifiedDaysAgo = 60,
            createdDaysAgo = 700,
        ),
        login(
            title = "amazon.com",
            username = "amorgan_92",
            password = "W3lc0me-Back#88",
            website = "https://amazon.com",
            modifiedDaysAgo = 25,
            createdDaysAgo = 1500,
        ),
        login(
            title = "spotify.com",
            username = "alex@meridian.io",
            password = "P!ano-F0rt3-2024",
            website = "https://spotify.com",
            notes = "Family plan owner.",
            modifiedDaysAgo = 90,
            createdDaysAgo = 600,
        ),
        login(
            title = "discord.com",
            username = "xalexm",
            password = "Kx9!zrT4vQb#Lm",
            website = "https://discord.com",
            modifiedDaysAgo = 8,
            createdDaysAgo = 400,
        ),
        login(
            title = "linkedin.com",
            username = "alex.morgan@work.io",
            password = "letmein2024",
            website = "https://linkedin.com",
            notes = "Password is old and weak — replace when you get a chance.",
            modifiedDaysAgo = 500,
            createdDaysAgo = 1500,
        ),
        login(
            title = "dropbox.com",
            username = "alex@meridian.io",
            password = "dragon123",
            website = "https://dropbox.com",
            modifiedDaysAgo = 700,
            createdDaysAgo = 1600,
        ),
    )

    val cards: List<CardItem> = listOf(
        CardItem(
            id = VaultCrypto.randomUuid(),
            title = "Chase Sapphire",
            bankName = "Chase",
            network = "VISA",
            cardholder = "ALEX MORGAN",
            number = "4242424242424242",
            expiry = "09/28",
            securityCode = "482",
            billingAddress = "24 Harbour St",
            notes = "Primary travel card.",
            isDemo = true,
            createdAt = now - 800 * day,
            lastModified = now - 3 * day,
        ),
        CardItem(
            id = VaultCrypto.randomUuid(),
            title = "Figma Pro",
            bankName = "Monzo",
            network = "Mastercard",
            cardholder = "ALEX MORGAN",
            number = "5555555555554444",
            expiry = "02/27",
            securityCode = "119",
            billingAddress = "24 Harbour St",
            notes = "Design subscription auto-pay.",
            isDemo = true,
            createdAt = now - 300 * day,
            lastModified = now - 10 * day,
        ),
    )

    val notes: List<NoteItem> = listOf(
        NoteItem(
            id = VaultCrypto.randomUuid(),
            title = "Passport",
            body = "Passport no. 558392014. Issued 12 Mar 2019, expires 11 Mar 2029.\n\nThe digital scan lives in Files ▸ ID. Keep the physical booklet in the office drawer — we'll need it for the visa application in June.",
            tags = listOf("Documents", "Personal", "Travel"),
            isDemo = true,
            createdAt = now - 120 * day,
            lastModified = now - 2 * day,
        ),
        NoteItem(
            id = VaultCrypto.randomUuid(),
            title = "Wi-Fi",
            body = "Home network — Harbor-2.4G / parking-octopus-71. Guest network shares the same password. Router admin at 192.168.1.1.",
            tags = listOf("Home"),
            isDemo = true,
            createdAt = now - 200 * day,
            lastModified = now - 200 * day,
        ),
    )

    val identities: List<IdentityItem> = listOf(
        IdentityItem(
            id = VaultCrypto.randomUuid(),
            title = "Alex Morgan",
            firstName = "Alex",
            lastName = "Morgan",
            email = "alex@meridian.io",
            phone = "+44 7700 900123",
            address = "24 Harbour St, Edinburgh EH6 6QA",
            notes = "Used for most sign-ups.",
            isDemo = true,
            createdAt = now - 600 * day,
            lastModified = now - 30 * day,
        ),
    )

    /** Password history entries for the demo GitHub login (previous + older). */
    val passwordHistory: List<Pair<String, String>> = listOf(
        "GitHub" to "8lFz!9Qwe-2022",
        "GitHub" to "g1tHubb-2020!",
    )
}

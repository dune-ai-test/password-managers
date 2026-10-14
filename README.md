# Aegis

An offline, iOS-style password manager for Android. Every credential lives in a
single SQLCipher-encrypted database on the device; the app holds no
`INTERNET` permission and cannot phone home.

This repository is the Android app for the design mockups in `ui/` (14 HTML
screens). Every screen in this app maps 1:1 to one of those files:

| Mockup | Screen |
| --- | --- |
| `l1-onboarding.html` | Onboarding (fresh install) |
| `l2-create-master-password.html` | Create master password |
| `l3-unlock.html` | Unlock (password + biometric) |
| `l4-passwords.html` | Vault tab (categories, recents, attention) |
| `l5-logins-category.html` | Category list with filters + sort |
| `l6-login-detail.html` | Login detail, actions, password history |
| `l7-new-item.html` | New / edit item (login, card, note, identity) |
| `l8-card-detail.html` | Card detail with the visual credit card |
| `l9-note-detail.html` | Secure note detail |
| `l10-generator.html` | Password generator |
| `l11-security.html` | Security score, alerts, weak passwords |
| `l12-settings.html` | Settings |
| `l13-search.html` | Search |
| `l14-empty-state.html` | Empty states per category |

## Implementation

- **Jetpack Compose, no Material.** The whole design system is custom:
  `ui/theme` (light + dark palettes, Inter via downloadable fonts), `ui/icons`
  (the mockups' Lucide icons recreated from their exact vector path data), and
  `ui/components` (toggle, slider, segmented control, tab bar, FAB, rows…).
- **Encrypted vault.** SQLCipher database opened with a PBKDF2-derived
  passphrase (210k iterations, per-vault salt). Biometric unlock wraps the
  passphrase behind a Keystore key that requires fresh authentication.
- **Security scoring** is computed on-device: weak, reused, missing 2FA and
  breaches (matched against a bundled offline catalog).
- **Encrypted backups** (`Import & export`) are PBKDF2 + AES-256-GCM files you
  create and share yourself.

## Building the nightly APK

`.github/workflows/nightly.yml` builds `app-debug.apk` on every push to `main`
and attaches it to the `nightly` **prerelease** — no artifacts are uploaded, the
APK lands directly on the release page (marked prerelease, never "Latest").

```bash
git init && git add -A && git commit -m "Aegis initial"
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

Then download the debug APK from the release page.

Versions are a verified-compatible set: Kotlin 2.1.21, AGP 8.7.2, Gradle 8.9,
Compose BOM 2025.04.01, minSdk 26 / targetSdk 35.

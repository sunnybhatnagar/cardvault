<p align="center">
  <img src="https://img.shields.io/badge/Card_Vault-Encrypted_Card_Storage-00FFFF?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHJlY3QgeD0iMSIgeT0iNCIgcng9IjIiIHJ5PSIyIiB3aWR0aD0iMjIiIGhlaWdodD0iMTYiLz48bGluZSB4MT0iMSIgeTE9IjEwIiB4Mj0iMjMiIHkyPSIxMCIvPjwvc3ZnPg==&logoColor=white" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Material_3-00BBBB?style=flat-square&logo=materialdesign&logoColor=white" />
  <img src="https://img.shields.io/badge/SQLCipher-003B57?style=flat-square&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/ML_Kit-FF6D00?style=flat-square&logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/minSdk-28-4CAF50?style=flat-square" />
  <img src="https://img.shields.io/badge/targetSdk-34-4CAF50?style=flat-square" />
</p>

<p align="center">
  <img src="https://img.shields.io/github/last-commit/sunnybhatnagar/cardvault?style=flat-square&label=Last%20Commit" />
  <img src="https://img.shields.io/github/repo-size/sunnybhatnagar/cardvault?style=flat-square" />
  <img src="https://img.shields.io/badge/license-MIT-yellow?style=flat-square" />
</p>

<p align="center">
  <b>AES-256 encrypted</b> · <b>biometric lock</b> · <b>on-device OCR</b> · <b>offline-first</b>
</p>

---

Card Vault keeps your credit and debit cards safely encrypted on your phone. No accounts, no cloud, no tracking.

## Features

| | |
|---|---|
| 🔒 **AES-256 Encryption** | Every card photo encrypted with AES-256-GCM. Database secured with SQLCipher. Keys in Android Keystore. |
| 📸 **OCR Scanning** | Take a photo — card number, expiry, and issuer auto-fill via on-device ML Kit. No data leaves your phone. |
| 🧬 **Biometric Lock** | Fingerprint, face, or PIN unlock. Configurable auto-lock. Screenshots blocked on card details. |
| 💳 **Card Flip** | Tap to flip between front and back card photos with 3D animation. |
| 📂 **Categories** | Organize cards into custom groups. Search by name, issuer, or number. |
| ☁️ **Backup & Restore** | Encrypted local backups. Optional Google Drive sync. JSON export with AES encryption. |
| 📅 **Expiry Alerts** | Push notification 30 days before a card expires. |
| 🌙 **Dark & Light Themes** | Toggle between dark and light mode. All screens react instantly — no restart needed. |
| 🔐 **Privacy First** | No analytics, no tracking, no accounts, no internet required. |

## Screenshots

<p align="center">
  <img src="screenshots/home_grid.png" width="200" />
  <img src="screenshots/home_list.png" width="200" />
  <img src="screenshots/add_card.png" width="200" />
  <img src="screenshots/card_detail.png" width="200" />
</p>

<p align="center">
  <img src="screenshots/settings.png" width="200" />
  <img src="screenshots/categories.png" width="200" />
  <img src="screenshots/dark_theme.png" width="200" />
  <img src="screenshots/onboarding.png" width="200" />
</p>

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9 |
| **UI** | Jetpack Compose + Material 3 |
| **Database** | Room + SQLCipher (AES-256 encrypted) |
| **Encryption** | Android Security Crypto (EncryptedFile, EncryptedSharedPreferences) |
| **Auth** | Android Biometric API (fingerprint, face, PIN) |
| **OCR** | Google ML Kit Text Recognition (on-device) |
| **Image Loading** | Coil |
| **Backup** | Google Drive API (optional) |
| **Analytics** | None |
| **minSdk / targetSdk** | 28 / 34 |

## Security

- All data encrypted at rest (AES-256-GCM for images, SQLCipher for database)
- Encryption keys stored in Android Keystore (hardware-backed on supported devices)
- `FLAG_SECURE` prevents screenshots on card details
- Biometric authentication on launch with configurable timeout
- Notifications hidden on lock screen (`VISIBILITY_PRIVATE`)
- Log output redacts potential credit card numbers
- Root detection at startup
- Network security config blocks cleartext HTTP
- Automatic backup disabled

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed with your own keystore)
./gradlew assembleRelease
```

## License

MIT

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://github.com/sunnybhatnagar">Sunny Bhatnagar</a></sub>
</p>

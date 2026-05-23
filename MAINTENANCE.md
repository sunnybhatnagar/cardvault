# Card Vault — Maintenance Guide

## Keystore & Signing

Your signing key is `cardvault.keystore` in the project root. It is excluded from git.

**Store password:** `cardvault`
**Key alias:** `cardvault`
**Key password:** `cardvault`

> **If you lose this keystore, you cannot push updates to existing installs.**
> Users would need to uninstall first (losing all data).

**Backup locations:**
- Project root: `~/Development/CardVault/cardvault.keystore`
- Password manager (SunnyHomeNas entry)
- Recommended: Google Drive / Dropbox / USB stick

## Building a Release

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd ~/Development/CardVault
./gradlew assembleRelease
```

Output APKs are in `app/build/outputs/apk/release/`:
- `app-arm64-v8a-release.apk` (10MB) — for most modern phones
- `app-universal-release.apk` (20MB) — works everywhere

## Publishing a New Version

1. **Bump version** in `app/build.gradle.kts`:
   - `versionCode`: increment by 1 each release
   - `versionName`: semantic version (e.g., "1.2")

2. **Update changelog** in `SettingsScreen.kt` (the `showChangelog` dialog)

3. **Build release APK** (see above)

4. **Create GitHub Release:**
   ```bash
   gh release create v1.x \
     --title "Card Vault v1.x" \
     --notes "Release notes here" \
     app/build/outputs/apk/release/app-arm64-v8a-release.apk#CardVault-v1.x.apk
   ```

5. **Push all changes** to `main`

## Before Making Schema Changes

1. Room schema auto-exports to `app/schemas/com.sunnyb.cardvault.data.db.AppDatabase/`
2. Write a migration in the `AppDatabase` companion object
3. Add a test in `app/src/androidTest/java/.../MigrationTest.kt`
4. **Never change an existing entity field** without a migration — Room crashes

## Database

- Encrypted with SQLCipher using AES-256 key
- Key stored in EncryptedSharedPreferences (backed by Android Keystore)
- Migration test at `MigrationTest.kt` — run with `./gradlew connectedCheck`

## Image Storage

- Card images are resized to max 1080px width, JPEG quality 80, before encrypting
- Encrypted with AES-256-GCM via `EncryptedFile`
- Temp camera files cleaned automatically from cache dir after save

## Security Checklist

- [ ] All activities not exported unless required
- [ ] FLAG_SECURE applied globally in MainActivity
- [ ] Biometric + DEVICE_CREDENTIAL fallback configured
- [ ] Network: cleartext blocked, system CAs only
- [ ] No clipboard data exposure (no copy-to-clipboard exists)
- [ ] Keystore backed up in at least 2 locations
- [ ] Privacy policy URL resolves (raw.githubusercontent.com/.../PRIVACY.md)

## Common Issues

| Symptom | Fix |
|---------|-----|
| "App not installed" on update | Keystore mismatch or versionCode not incremented |
| Room crash after update | Missing migration for schema change |
| Biometric not showing | Device may lack biometric hardware; uses PIN/pattern fallback |

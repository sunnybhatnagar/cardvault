# Card Vault Privacy Policy

**Last updated:** May 23, 2026

## Summary

Card Vault does not collect, store, or transmit any personal data. All information stays on your device.

## Data Storage

- Card information (numbers, expiry, CVV, photos) is stored **only on your device**
- The database is encrypted with **AES-256 via SQLCipher**
- Card images are encrypted with **AES-256-GCM**
- Encryption keys are stored in the **Android Keystore**
- **No data is sent to any server**

## Permissions

| Permission | Purpose |
|-----------|---------|
| Camera | Taking photos of credit/debit cards |
| Notifications | Alerting when a card is nearing expiry |
| Biometric | Unlocking the app (fingerprint / face / PIN) |

Photos are only captured when you explicitly tap the capture button. They are never uploaded anywhere.

## Third-Party Services

This app uses **Google ML Kit** for on-device text recognition (OCR). All processing happens on your device — no image data leaves your phone.

Optional **Google Drive** backup uses the Drive API only if you explicitly sign in and trigger a backup. You control when and if this happens.

## Data Deletion

Deleting the app removes all stored data. You can also delete individual cards or categories within the app at any time.

## Changes

If this policy changes, the "Last updated" date at the top will be updated.

## Contact

Developer: Sunny Bhatnagar  
GitHub: [github.com/sunnybhatnagar](https://github.com/sunnybhatnagar)

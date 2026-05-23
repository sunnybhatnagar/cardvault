# Cardholder Name, Variant, and Product Fields

## Summary
Add three new fields to the Card model: cardholder name (full name from OCR + manual), variant/network (auto-detected from BIN + dropdown), and product name (OCR + manual entry). All display on card views and detail screens.

## Fields

| Field | Type | Auto-detect | Manual | DB Column |
|-------|------|-------------|--------|-----------|
| Cardholder Name | String | OCR from front scan (all-caps text blocks after removing known patterns) | Text field | `cardholderName TEXT` |
| Variant | String | BIN detection from card number prefix | ExposedDropdownMenu | `variant TEXT` |
| Product | String | OCR from front scan (text adjacent to bank name) | Text field | `product TEXT` |

## Variant Options (for dropdown + BIN detection)

| Network | BIN Prefixes (detection order matters — check specific first) |
|---------|--------------------------------------------------------------|
| Visa | `4` |
| Mastercard | `51-55`, `2221-2720` |
| American Express | `34`, `37` |
| RuPay | `60`, `65`, `81`, `82`, `50` (check before Mastercard/Maestro for Indian cards) |
| Diners Club | `300-305`, `36`, `38`, `39` |
| JCB | `3528-3589` |
| Maestro | `5018`, `5020`, `5038`, `56-69` (remaining 5x/6x after RuPay/MC) |

## Scanner Changes (`CardScanner.kt`)

- `ScannedCardInfo` gets `cardholderName: String?` and `product: String?`
- New `parseCardholderName(texts)`:
  1. After extracting card number and expiry from combined text
  2. Remove known bank keywords (ICICI, HDFC, AXIS, SBI, YES, KOTAK, etc.)
  3. Remove dates, URLs, CVV-like patterns, "VALID THRU", "CARDMEMBER" etc.
  4. Filter remaining text blocks to all-caps, 3-30 chars
  5. Return longest remaining block as cardholder name
- New `detectVariant(cardNumber)`: BIN matching against the table above
- New `parseProduct(texts, issuer)`: look for text blocks adjacent to/matching the bank name on the front (e.g. "Emeralde" next to "ICICI Bank")
- Existing `parseIssuer` stays unchanged (it handles bank name detection)

## Database Migration

- `Card.kt`: add `cardholderName: String = ""`, `variant: String = ""`, `product: String = ""`
- `AppDatabase.kt`: version 1→2, add migration:
  ```sql
  ALTER TABLE card ADD COLUMN cardholderName TEXT NOT NULL DEFAULT ''
  ALTER TABLE card ADD COLUMN variant TEXT NOT NULL DEFAULT ''
  ALTER TABLE card ADD COLUMN product TEXT NOT NULL DEFAULT ''
  ```
- `DatabaseFactory.kt`: add `.addMigrations(MIGRATION_1_2)` to builder
- Export schema for version 2

## ViewModel Changes (`AddCardViewModel.kt`)

- `AddCardUiState`: add `cardholderName: String`, `variant: String`, `product: String`, `variantOptions: List<String>`
- New update functions: `updateCardholderName`, `updateVariant`, `updateProduct`
- `scanCardImage()`: populate new fields from `ScannedCardInfo`, only fill blanks
- `saveCard()`: pass new fields through to Card entity

## UI Changes

### AddCardScreen (step 3 — Card Details)
- Cardholder name text field (between Issuer and Card Number)
- Variant dropdown (after Card Number, before Expiry)
- Product text field (after Variant, before Expiry)
- Reorder: Nickname → Issuer → Product → Cardholder Name → Card Number → Variant → Expiry → CVV → Category

### CardFrontView
- Add cardholder name below card number (18sp, all caps, monospace)
- Add variant/network badge (small chip, top-right near card logo area)
- Remove the emoji card icon, replace with variant badge text

### CardDetailScreen
- Add InfoRow for Cardholder Name (below expiry)
- Add InfoRow for Variant (below cardholder name)
- Add InfoRow for Product (below variant)

### CardTile / CardListItem
- Add small variant badge (e.g. "Visa" chip) next to the issuer label
- No change to cardholder name or product (would clutter compact view)

## Notification & Export
- Backup/restore handles new fields automatically (Room serializes all columns)
- No change to notification text needed

## Implementation Order
1. Card entity + DB migration
2. CardScanner updates (name detection, variant detection, product detection)
3. AddCardViewModel state + update functions
4. AddCardScreen UI (step 3 fields)
5. CardFrontView (name + variant badge)
6. CardDetailScreen (new InfoRows)
7. CardTile / CardListItem (variant badge)

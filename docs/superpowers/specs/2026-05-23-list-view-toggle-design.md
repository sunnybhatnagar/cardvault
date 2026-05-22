# List/Grid View Toggle — Design Spec

## Overview
Add a toggle between the existing 2-column grid view and a new list view on the Home screen. The toggle sits between the search bar and the card list, also showing the card count.

## Toggle Area
- A horizontal segmented control (chip group) below the search bar
- Left label shows card count (e.g., "3 cards")
- Right side: two segments — "Grid" (selected by default) and "List"
- Selected segment has a subtle background highlight (primary color at ~13% alpha)
- No persistence — resets to grid on app restart (simple state, follows existing patterns)

## List Item (`CardListItem`)
- Height: ~64dp, compact horizontal card
- Background: same issuer-based gradient as grid tiles (Chase, Amex, Default)
- Left edge: 4dp accent strip in primary/secondary color for visual distinction
- Content (one row):
  - Issuer name (top-left, muted)
  - Last 4 digits (top-right, muted)
  - Nickname (bottom-left, bold white)
  - Expiry (bottom-right, muted)
- Entire row is clickable, navigates to CardDetail
- RoundedCorners(12.dp) shape
- "Add Card" item at list end: dashed-border box with "+ Add Card" text

## Loading State
- `ShimmerCardListItem`: matching ~64dp height shimmer placeholder, shown 4 times during loading

## Implementation Changes

### New files
1. `ui/components/CardListItem.kt` — list item composable
2. `ui/components/ShimmerCardListItem.kt` — shimmer for list loading

### Modified files
1. `ui/screens/HomeScreen.kt` — add `ViewMode` enum, segmented toggle, conditional render (LazyVerticalGrid vs LazyColumn)
2. `viewmodel/HomeViewModel.kt` — add `_viewMode` MutableStateFlow<ViewMode>

## File Details

### CardListItem.kt
```kotlin
@Composable
fun CardListItem(card: Card, onClick: () -> Unit, modifier: Modifier = Modifier)
```
- Same gradient logic as CardTile (`when card.issuer`)
- Row layout: accent strip (4dp) | Column(issuer + last4, nickname + expiry) | trailing
- RoundedCornerShape(12.dp), height 64dp, horizontal padding 16dp

### ShimmerCardListItem.kt
- Matches CardListItem dimensions (64dp height)
- Uses same shimmer animation pattern as ShimmerCardTile
- 4 shimmer rows shown during loading

### HomeScreen.kt
- Add enum `ViewMode { GRID, LIST }` (in file or separate)
- Add segmented toggle composable between search bar and card list
- Replace single `LazyVerticalGrid` with conditional:
  - Grid: `LazyVerticalGrid(GridCells.Fixed(2))` — existing code
  - List: `LazyColumn` wrapping `CardListItem` + "Add Card" item
- Shimmer: conditional `LazyVerticalGrid` vs `LazyColumn` with `ShimmerCardListItem`

### HomeViewModel.kt
- Add `MutableStateFlow<ViewMode>(ViewMode.GRID)` with public `StateFlow`
- Add `fun toggleViewMode(mode: ViewMode)` setter

## Not in scope
- Persisting view preference across restarts
- Animation when toggling between views
- Search results view mode (follows same toggle)

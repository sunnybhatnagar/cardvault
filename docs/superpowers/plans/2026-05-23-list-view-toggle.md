# List/Grid View Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a toggle on the Home screen to switch between the existing 2-column grid view and a new compact list view.

**Architecture:** A `ViewMode` enum stored as `MutableStateFlow` in `HomeViewModel`. The `HomeScreen` reads this and conditionally renders `LazyVerticalGrid` (grid) or `LazyColumn` (list). Two new composables: `CardListItem` (horizontal card row) and `ShimmerCardListItem` (loading placeholder for list mode).

**Tech Stack:** Kotlin, Jetpack Compose, Material3

---

### Task 1: Create CardListItem composable

**Files:**
- Create: `app/src/main/java/com/sunnyb/cardvault/ui/components/CardListItem.kt`

- [ ] **Step 1: Write CardListItem.kt**

```kotlin
package com.sunnyb.cardvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.ui.theme.*

@Composable
fun CardListItem(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = when (card.issuer?.lowercase()) {
        "chase" -> GradientChase
        "amex", "american express" -> GradientAmex
        else -> GradientDefault
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(start = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.issuer ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "•••• ${card.cardNumber.takeLast(4)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.nickname,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = card.expiry,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/components/CardListItem.kt
git commit -m "feat: add CardListItem composable for list view"
```

---

### Task 2: Create ShimmerCardListItem composable

**Files:**
- Create: `app/src/main/java/com/sunnyb/cardvault/ui/components/ShimmerCardListItem.kt`

- [ ] **Step 1: Write ShimmerCardListItem.kt**

```kotlin
package com.sunnyb.cardvault.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerCardListItem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = shimmerAlpha),
            MaterialTheme.colorScheme.surface.copy(alpha = shimmerAlpha * 1.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = shimmerAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = shimmerBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.5f))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.5f))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.5f))
                )
            }
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.5f))
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/components/ShimmerCardListItem.kt
git commit -m "feat: add ShimmerCardListItem composable for list loading state"
```

---

### Task 3: Add viewMode to HomeViewModel

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/viewmodel/HomeViewModel.kt`

- [ ] **Step 1: Add ViewMode enum and state to HomeViewModel**

Insert after the existing `_error` state:

```kotlin
enum class ViewMode { GRID, LIST }

private val _viewMode = MutableStateFlow(ViewMode.GRID)
val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

fun setViewMode(mode: ViewMode) {
    _viewMode.value = mode
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/viewmodel/HomeViewModel.kt
git commit -m "feat: add ViewMode enum and state to HomeViewModel"
```

---

### Task 4: Update HomeScreen with toggle and conditional rendering

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Update imports — add LazyColumn, items (LazyColumn), CardListItem, ShimmerCardListItem, ViewMode**

Replace the existing imports with:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
```

Note: we alias `items` imports to avoid name collision between LazyVerticalGrid.items and LazyColumn.items.

- [ ] **Step 2: Add the toggle area between search bar and card list**

After the `OutlinedTextField` (search bar) block and before the `if (isLoading)` block, add a view count and toggle row:

```kotlin
Spacer(Modifier.height(4.dp))
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "${cards.size} card${if (cards.size != 1) "s" else ""}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val modes = ViewMode.entries
        modes.forEach { mode ->
            val isSelected = viewMode.value == mode
            Text(
                text = mode.name,
                modifier = Modifier
                    .clip(RoundedCornerShape(if (mode == ViewMode.GRID) 8.dp else 8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { viewModel.setViewMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 3: Conditionally render Grid or List for the card content**

Replace the `else { LazyVerticalGrid(...) }` block with:

```kotlin
} else if (viewMode.value == ViewMode.GRID) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
    ) {
        gridItems(cards, key = { it.id }) { card ->
            CardTile(
                card = card,
                onClick = { onCardClick(card.id) }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onAddCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+", fontSize = 32.sp, color = MaterialTheme.colorScheme.outline)
                    Text("Add Card", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
} else {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        listItems(cards, key = { it.id }) { card ->
            CardListItem(
                card = card,
                onClick = { onCardClick(card.id) }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+ Add Card",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
```

- [ ] **Step 4: Add shimmer for list loading state**

Replace the shimmer loading block to handle both modes. Currently it renders `ShimmerCardTile()`. Update the `if (isLoading)` block to:

```kotlin
if (isLoading) {
    if (viewMode.value == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) {
            items(6) { ShimmerCardTile() }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) {
            items(6) { ShimmerCardListItem() }
        }
    }
```

- [ ] **Step 5: Add missing imports to HomeScreen.kt**

Make sure all newly needed imports are present:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnyb.cardvault.ui.components.CardListItem
import com.sunnyb.cardvault.ui.components.ShimmerCardListItem
import com.sunnyb.cardvault.viewmodel.ViewMode
```

- [ ] **Step 6: Add `viewMode` state collection and "No cards" empty state fix**

At the top of HomeScreen, after `val error`, add:
```kotlin
val viewMode by viewModel.viewMode.collectAsState()
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/screens/HomeScreen.kt
git commit -m "feat: add grid/list toggle to HomeScreen with conditional rendering"
```

---

### Task 5: Build and verify

- [ ] **Step 1: Build the project**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Push to feature branch**

```bash
git checkout -b feat/list-view-toggle
git push origin feat/list-view-toggle
```

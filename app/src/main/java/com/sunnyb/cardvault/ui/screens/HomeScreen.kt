package com.sunnyb.cardvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunnyb.cardvault.ui.components.CardTile
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.viewmodel.HomeViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCardClick: (Long) -> Unit,
    onAddCard: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp),
            placeholder = { Text("Search cards...", color = MaterialTheme.colorScheme.outline) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (cards.isEmpty() && searchQuery.isBlank()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CreditCard, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No cards yet", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap the button below to add your first card",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = onAddCard,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Add Card")
                    }
                }
            }
        } else if (cards.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No cards match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cards, key = { it.id }) { card ->
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
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onAddCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "+",
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Add Card",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

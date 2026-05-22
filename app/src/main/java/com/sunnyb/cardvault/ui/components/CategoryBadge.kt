package com.sunnyb.cardvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sunnyb.cardvault.ui.theme.NeonCyan

@Composable
fun CategoryBadge(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NeonCyan.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

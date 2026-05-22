package com.sunnyb.cardvault.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnyb.cardvault.ui.theme.*

@Composable
fun LockScreen(
    onUnlockClick: () -> Unit
) {
    var animTriggered by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animTriggered) 0.4f else 1f,
        animationSpec = tween(durationMillis = 2000),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        animTriggered = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(onClick = onUnlockClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "◈",
                fontSize = 52.sp,
                color = NeonCyan,
                modifier = Modifier.alpha(0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Card Vault",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                textAlign = TextAlign.Center
            )
            Text(
                text = "encrypted · private · yours",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    fontSize = 28.sp,
                    modifier = Modifier.alpha(alpha)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Touch to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

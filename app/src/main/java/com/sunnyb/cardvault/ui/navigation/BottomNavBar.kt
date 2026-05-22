package com.sunnyb.cardvault.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sunnyb.cardvault.ui.theme.NeonCyan

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: String
) {
    data object Home : BottomNavItem("home", "Home", "🏠")
    data object Categories : BottomNavItem("categories", "Categories", "📁")
    data object Settings : BottomNavItem("settings", "Settings", "⚙️")
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Categories,
    BottomNavItem.Settings
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Text(
                        text = item.icon,
                        fontSize = 20.sp
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

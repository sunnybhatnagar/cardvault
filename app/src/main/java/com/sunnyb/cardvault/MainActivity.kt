package com.sunnyb.cardvault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunnyb.cardvault.security.BiometricAuth
import com.sunnyb.cardvault.ui.navigation.BottomNavBar
import com.sunnyb.cardvault.ui.navigation.NavGraph
import com.sunnyb.cardvault.ui.navigation.Routes
import com.sunnyb.cardvault.ui.screens.LockScreen
import com.sunnyb.cardvault.ui.theme.CardVaultTheme
import com.sunnyb.cardvault.ui.theme.DarkBackground

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuth: BiometricAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        biometricAuth = BiometricAuth(this)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (::biometricAuth.isInitialized) {
                        app.sessionManager.onBackground()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (::biometricAuth.isInitialized) {
                        app.sessionManager.onForeground()
                    }
                }
                else -> {}
            }
        })

        setContent {
            CardVaultTheme {
                val isLocked by app.sessionManager.isLocked.collectAsState()

                if (isLocked) {
                    LockScreen(
                        onUnlockClick = {
                            if (biometricAuth.canAuthenticate()) {
                                biometricAuth.authenticate()
                            } else {
                                app.initializeDatabase()
                                app.sessionManager.onAuthenticated()
                            }
                        }
                    )

                    LaunchedEffect(Unit) {
                        biometricAuth.resultFlow.collect { result ->
                            if (result is BiometricAuth.AuthResult.Success) {
                                app.initializeDatabase()
                                app.sessionManager.onAuthenticated()
                            }
                        }
                    }
                } else {
                    MainApp()
                }
            }
        }
    }

    private val app: CardVaultApp
        get() = application as CardVaultApp
}

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(Routes.HOME, Routes.CATEGORIES, Routes.SETTINGS)
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}

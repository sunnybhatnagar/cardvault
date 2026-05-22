package com.sunnyb.cardvault

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunnyb.cardvault.security.BiometricAuth
import com.sunnyb.cardvault.ui.navigation.BottomNavBar
import com.sunnyb.cardvault.ui.navigation.NavGraph
import com.sunnyb.cardvault.ui.navigation.Routes
import com.sunnyb.cardvault.ui.screens.LockScreen
import com.sunnyb.cardvault.ui.screens.OnboardingScreen
import com.sunnyb.cardvault.ui.theme.CardVaultTheme
import com.sunnyb.cardvault.ui.theme.DarkBackground
import com.sunnyb.cardvault.ui.theme.ThemeMode
import com.sunnyb.cardvault.util.ExpiryChecker
import com.sunnyb.cardvault.util.NotificationHelper
import kotlinx.coroutines.flow.first

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuth: BiometricAuth
    private var notificationPermRequested = false

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePrefs = getSharedPreferences("cardvault_theme", MODE_PRIVATE)

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
            val onboardingPrefs = getSharedPreferences("cardvault_onboarding", MODE_PRIVATE)
            var showOnboarding by remember {
                mutableStateOf(!onboardingPrefs.getBoolean("done", false))
            }

            CardVaultTheme(themeMode = app.themeMode) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            onboardingPrefs.edit().putBoolean("done", true).apply()
                            showOnboarding = false
                        }
                    )
                    return@CardVaultTheme
                }

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
                            when (result) {
                                is BiometricAuth.AuthResult.Success -> {
                                    app.initializeDatabase()
                                    app.sessionManager.onAuthenticated()
                                }
                                is BiometricAuth.AuthResult.Error -> {
                                    // Error already shown by system dialog; user can retry
                                }
                                is BiometricAuth.AuthResult.Cancelled -> {
                                    // User dismissed — keep lock screen
                                }
                            }
                        }
                    }
                } else {
                    MainApp()

                    if (!notificationPermRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        LaunchedEffect(Unit) {
                            notificationPermRequested = true
                            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
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

    LaunchedEffect(Unit) {
        val repo = CardVaultApp.instance.cardRepository
        val cards = repo.allCards.first()
        val expiring = ExpiryChecker.check(cards)
        if (expiring.isNotEmpty()) {
            val names = expiring.joinToString(", ") { "${it.nickname} (${it.expiry})" }
            NotificationHelper.showExpiryNotification(
                CardVaultApp.instance,
                "Cards Expiring Soon",
                names
            )
        }
    }

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
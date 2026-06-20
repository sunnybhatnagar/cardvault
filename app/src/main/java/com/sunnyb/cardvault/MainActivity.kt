package com.sunnyb.cardvault

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.edit
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.security.BiometricAuth
import com.sunnyb.cardvault.ui.navigation.BottomNavBar
import com.sunnyb.cardvault.ui.navigation.NavGraph
import com.sunnyb.cardvault.ui.navigation.Routes
import com.sunnyb.cardvault.ui.screens.LockScreen
import com.sunnyb.cardvault.ui.screens.OnboardingScreen
import com.sunnyb.cardvault.ui.theme.CardVaultTheme
import com.sunnyb.cardvault.ui.theme.DarkBackground
import com.sunnyb.cardvault.ui.theme.ThemeMode
import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.security.SessionManager
import com.sunnyb.cardvault.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var cardDao: CardDao

    private lateinit var biometricAuth: BiometricAuth
    private var notificationPermRequested = false

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        biometricAuth = BiometricAuth(this)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (::biometricAuth.isInitialized) {
                        sessionManager.onBackground()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (::biometricAuth.isInitialized) {
                        sessionManager.onForeground()
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

            CardVaultTheme(themeMode = (application as? CardVaultApp)?.themeMode ?: ThemeMode.DARK) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            onboardingPrefs.edit { putBoolean("done", true) }
                            showOnboarding = false
                        }
                    )
                    return@CardVaultTheme
                }

                val isLocked by sessionManager.isLocked.collectAsState()

                if (isLocked) {
                    LockScreen(
                        onUnlockClick = {
                            if (biometricAuth.canAuthenticate()) {
                                biometricAuth.authenticate()
                            } else {
                                sessionManager.onAuthenticated()
                            }
                        }
                    )

                    LaunchedEffect(Unit) {
                        biometricAuth.resultFlow.collect { result ->
                            when (result) {
                                is BiometricAuth.AuthResult.Success -> {
                                    sessionManager.onAuthenticated()
                                }
                                is BiometricAuth.AuthResult.Error -> {
                                }
                                is BiometricAuth.AuthResult.Cancelled -> {
                                }
                            }
                        }
                    }
                } else {
                    MainApp(cardDao)

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
}

@Composable
private fun MainApp(cardDao: CardDao) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        val cards = cardDao.getAllCards().first()
        val expiring = checkExpiringCards(cards)
        if (expiring.isNotEmpty()) {
            val names = expiring.joinToString(", ") { "${it.nickname} (${it.expiry})" }
            NotificationHelper.showExpiryNotification(
                navController.context,
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

private data class ExpiringCard(
    val nickname: String,
    val expiry: String,
    val daysUntilExpiry: Int
)

private fun checkExpiringCards(cards: List<Card>): List<ExpiringCard> {
    val now = Calendar.getInstance()
    val deadline = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
    return cards.mapNotNull { card ->
        val expiryDate = parseExpiry(card.expiry) ?: return@mapNotNull null
        if (expiryDate.before(deadline) && expiryDate.after(now)) {
            val daysUntil = ((expiryDate.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            ExpiringCard(card.nickname, card.expiry, daysUntil)
        } else null
    }
}

private fun parseExpiry(expiry: String): Calendar? {
    val parts = expiry.split("/")
    if (parts.size != 2) return null
    val month = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val year = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val fullYear = if (year < 100) 2000 + year else year
    return Calendar.getInstance().apply {
        set(fullYear, month - 1, 1)
        add(Calendar.MONTH, 1)
        add(Calendar.DAY_OF_YEAR, -1)
    }
}

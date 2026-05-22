package com.sunnyb.cardvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sunnyb.cardvault.ui.screens.*

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val CARD_DETAIL = "card_detail/{cardId}"
    const val ADD_CARD = "add_card"

    fun cardDetail(cardId: Long) = "card_detail/$cardId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onCardClick = { cardId ->
                    navController.navigate(Routes.cardDetail(cardId))
                },
                onAddCard = {
                    navController.navigate(Routes.ADD_CARD)
                }
            )
        }

        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                onCategoryClick = { /* navigate home filtered */ }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }

        composable(
            route = Routes.CARD_DETAIL,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_CARD) {
            AddCardScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }
    }
}

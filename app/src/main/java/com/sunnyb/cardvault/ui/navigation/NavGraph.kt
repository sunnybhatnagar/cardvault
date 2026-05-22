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
    const val EDIT_CARD = "edit_card/{cardId}"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val HELP = "help"
    const val ABOUT = "about"

    fun cardDetail(cardId: Long) = "card_detail/$cardId"
    fun editCard(cardId: Long) = "edit_card/$cardId"
    fun categoryDetail(categoryId: Long) = "category_detail/$categoryId"
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
                onCategoryClick = { categoryId ->
                    navController.navigate(Routes.categoryDetail(categoryId))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onHelpClick = { navController.navigate(Routes.HELP) },
                onAboutClick = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.HELP) {
            HelpScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CARD_DETAIL,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) {
            CardDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Routes.editCard(id))
                }
            )
        }

        composable(Routes.ADD_CARD) {
            AddCardScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_CARD,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            AddCardScreen(
                editCardId = cardId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CATEGORY_DETAIL,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
            CategoryDetailScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onCardClick = { cardId ->
                    navController.navigate(Routes.cardDetail(cardId))
                }
            )
        }
    }
}
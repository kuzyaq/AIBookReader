package com.example.aibookreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aibookreader.presentation.screens.addbook.AddBookScreen
import com.example.aibookreader.presentation.screens.home.HomeScreen
import com.example.aibookreader.presentation.screens.reader.ReaderScreen


sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddBook : Screen("add_book")

    object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: Int) = "reader/$bookId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onBookClick = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                },
                onAddBookClick = {
                    navController.navigate(Screen.AddBook.route)
                }
            )
        }

        composable(Screen.AddBook.route) {
            AddBookScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,  // Передаем bookId
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
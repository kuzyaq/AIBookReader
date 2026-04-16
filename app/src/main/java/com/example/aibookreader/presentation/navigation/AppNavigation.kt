package com.example.aibookreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aibookreader.presentation.screens.addbook.AddBookScreen
import com.example.aibookreader.presentation.screens.auth.login.LoginScreen
import com.example.aibookreader.presentation.screens.auth.register.RegisterScreen
import com.example.aibookreader.presentation.screens.home.HomeScreen
import com.example.aibookreader.presentation.screens.reader.ReadiumReaderActivity
import com.example.aibookreader.presentation.screens.splash.SplashScreen


sealed class Screen(val route: String) {

    object Splash : Screen("splash")

    object Login : Screen("login")
    object Register : Screen("register")

    object Home : Screen("home")
    object AddBook : Screen("add_book")
    object Profile : Screen("profile")

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
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            val context = LocalContext.current
            HomeScreen(
                onBookClick = { bookId ->
                    context.startActivity(ReadiumReaderActivity.createIntent(context, bookId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AddBook.route) {
            AddBookScreen()
        }
    }
}

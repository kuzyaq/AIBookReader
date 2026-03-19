package com.example.aibookreader.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.aibookreader.presentation.screens.addbook.AddBookScreen
import com.example.aibookreader.presentation.screens.home.components.BookList
import com.example.aibookreader.presentation.screens.home.components.EmptyLibraryScreen
import com.example.aibookreader.presentation.screens.home.components.HomeBottomBar
import com.example.aibookreader.presentation.screens.home.components.HomeTopBar
import com.example.aibookreader.presentation.screens.home.components.LoadingScreen
import com.example.aibookreader.presentation.screens.home.components.TAB_ADD
import com.example.aibookreader.presentation.screens.home.components.TAB_LIBRARY
import com.example.aibookreader.presentation.screens.home.components.TAB_PROFILE
import com.example.aibookreader.presentation.screens.profile.ProfileScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(TAB_LIBRARY) }


    Scaffold(
        topBar = {
            when (selectedTab) {
                TAB_LIBRARY -> HomeTopBar(title = "Моя библиотека", icon = Icons.Filled.MenuBook)
                TAB_ADD -> HomeTopBar(title = "Добавить книгу", icon = Icons.Filled.AddCircle)
                TAB_PROFILE -> HomeTopBar(title = "Профиль", icon = Icons.Filled.AccountCircle)
            }
        },
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.padding(paddingValues),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * direction } + fadeOut())
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                TAB_LIBRARY -> LibraryTabContent(
                    state = state,
                    onBookClick = onBookClick,
                    onDeleteBook = { viewModel.deleteBook(it) }
                )

                TAB_ADD -> AddBookScreen()
                TAB_PROFILE -> ProfileScreen(onLogout = { })
            }
        }
    }
}

// Содержимое вкладки Библиотека
@Composable
private fun LibraryTabContent(
    state: HomeUiState,
    onBookClick: (Int) -> Unit,
    onDeleteBook: (Int) -> Unit
) {
    when {
        state.isLoading -> LoadingScreen()
        state.books.isEmpty() -> EmptyLibraryScreen()
        else -> BookList(
            books = state.books,
            onBookClick = onBookClick,
            onDelete = onDeleteBook
        )
    }
}
package com.example.aibookreader.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aibookreader.presentation.screens.addbook.AddBookScreen
import com.example.aibookreader.presentation.theme.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.aibookreader.presentation.screens.home.components.BookList
import com.example.aibookreader.presentation.screens.home.components.EmptyLibraryScreen
import com.example.aibookreader.presentation.screens.home.components.HomeBottomBar
import com.example.aibookreader.presentation.screens.home.components.HomeTopBar
import com.example.aibookreader.presentation.screens.home.components.LoadingScreen
import com.example.aibookreader.presentation.screens.home.components.TAB_ADD
import com.example.aibookreader.presentation.screens.home.components.TAB_LIBRARY
import com.example.aibookreader.presentation.screens.home.components.TAB_PROFILE
import com.example.aibookreader.presentation.screens.profile.ProfileScreen


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemeManagerEntryPoint {
    fun themeManager(): ThemeManager
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (Int) -> Unit,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(TAB_LIBRARY) }
    val context = LocalContext.current
    val themeManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ThemeManagerEntryPoint::class.java
        ).themeManager()
    }


    Scaffold(
        topBar = {
            when (selectedTab) {
                TAB_LIBRARY -> HomeTopBar(
                    title = "Моя библиотека",
                    icon = Icons.Filled.MenuBook,
                    isSearchActive = state.isSearchActive,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onToggleSearch = { viewModel.toggleSearch() }
                )
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
                TAB_PROFILE -> ProfileScreen(
                    onLogout = onLogout,
                    themeManager = themeManager
                )
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
        state.books.isEmpty() && state.searchQuery.isNotBlank() -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Ничего не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Попробуйте изменить запрос",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
        state.books.isEmpty() -> EmptyLibraryScreen()
        else -> BookList(
            books = state.books,
            onBookClick = onBookClick,
            onDelete = onDeleteBook
        )
    }
}
package com.example.aibookreader.presentation.screens.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String) {
        val trimmed = email.trim()
        when {
            trimmed.isEmpty() -> {
                _uiState.update { it.copy(error = "Введите email") }
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> {
                _uiState.update { it.copy(error = "Некорректный email") }
                return
            }
            password.length < 8 -> {
                _uiState.update { it.copy(error = "Пароль не менее 8 символов") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            authRepository.register(trimmed, password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: "Не удалось зарегистрироваться")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(success = false) }
    }
}

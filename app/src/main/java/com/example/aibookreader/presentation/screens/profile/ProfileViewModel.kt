package com.example.aibookreader.presentation.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.data.local.profile.ProfileAvatarStorage
import com.example.aibookreader.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileAvatarStorage: ProfileAvatarStorage
) : ViewModel() {

    val userState = authRepository.currentUser

    val localAvatarPath = authRepository.currentUser
        .flatMapLatest { user ->
            val id = user?.id
            if (id == null) flowOf(null)
            else profileAvatarStorage.localAvatarPathFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onAvatarPicked(uri: Uri) {
        val id = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            profileAvatarStorage.saveAvatarFromUri(id, uri)
        }
    }

    fun updateDisplayName(name: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(name)
            result.fold(
                onSuccess = { onResult(null) },
                onFailure = { onResult(it) }
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}

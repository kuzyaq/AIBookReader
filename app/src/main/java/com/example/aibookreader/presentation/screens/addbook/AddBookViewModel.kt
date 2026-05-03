package com.example.aibookreader.presentation.screens.addbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.aibookreader.data.remote.library.LibraryApiService
import com.example.aibookreader.data.sync.LibrarySyncScheduler
import com.example.aibookreader.data.worker.DownloadRemoteBookScheduler
import com.example.aibookreader.data.worker.ImportBookScheduler
import com.example.aibookreader.domain.repository.AuthRepository
import com.example.aibookreader.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImportEvent {
    data object Started : ImportEvent()
    data object Success : ImportEvent()
    data class Error(val message: String) : ImportEvent()
}

data class RemoteLibraryBookUi(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val onDevice: Boolean
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val importBookScheduler: ImportBookScheduler,
    private val workManager: WorkManager,
    private val authRepository: AuthRepository,
    private val libraryApi: LibraryApiService,
    private val bookRepository: BookRepository,
    private val downloadRemoteBookScheduler: DownloadRemoteBookScheduler,
    private val librarySyncScheduler: LibrarySyncScheduler
) : ViewModel() {

    private val _importEvents = MutableSharedFlow<ImportEvent>()
    val importEvents: SharedFlow<ImportEvent> = _importEvents.asSharedFlow()

    private val _remoteBooks = MutableStateFlow<List<RemoteLibraryBookUi>>(emptyList())
    val remoteBooks = _remoteBooks.asStateFlow()

    val isLoggedIn = authRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    refreshCloudLibrary()
                } else {
                    _remoteBooks.value = emptyList()
                }
            }
        }
    }

    fun refreshCloudLibrary() {
        viewModelScope.launch {
            if (authRepository.currentUser.value == null) return@launch
            try {
                librarySyncScheduler.requestImmediateSync()
                val list = libraryApi.listBooks().filter { it.status == "ready" }
                val localRemotes = bookRepository.getAllBooks().first()
                    .mapNotNull { it.remoteBookId }
                    .toSet()
                _remoteBooks.value = list.map { dto ->
                    RemoteLibraryBookUi(
                        id = dto.id,
                        title = dto.title,
                        author = dto.author,
                        format = dto.format,
                        onDevice = localRemotes.contains(dto.id)
                    )
                }
            } catch (_: Exception) {
                _remoteBooks.value = emptyList()
            }
        }
    }

    fun importBook(filePath: String) {
        importBookScheduler.importBook(filePath)

        viewModelScope.launch {
            _importEvents.emit(ImportEvent.Started)
        }

        val workName = "import_$filePath"
        workManager.getWorkInfosForUniqueWorkLiveData(workName)
            .observeForever { workInfos ->
                val info = workInfos?.firstOrNull() ?: return@observeForever
                viewModelScope.launch {
                    when (info.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _importEvents.emit(ImportEvent.Success)
                            refreshCloudLibrary()
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString("error") ?: "Неизвестная ошибка"
                            _importEvents.emit(ImportEvent.Error(error))
                        }
                        else -> {}
                    }
                }
            }
    }

    fun downloadFromCloud(remoteId: String, format: String) {
        val ext = if (format.equals("PDF", ignoreCase = true)) "pdf" else "epub"
        downloadRemoteBookScheduler.enqueue(remoteId, ext)
        viewModelScope.launch {
            _importEvents.emit(ImportEvent.Started)
        }
    }
}

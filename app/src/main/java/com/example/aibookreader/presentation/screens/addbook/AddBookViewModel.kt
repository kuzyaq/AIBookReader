package com.example.aibookreader.presentation.screens.addbook

import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.aibookreader.data.worker.ImportBookScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class ImportEvent {
    object Started : ImportEvent()
    object Success : ImportEvent()
    data class Error(val message: String) : ImportEvent()
}

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val importBookScheduler: ImportBookScheduler,
    private val workManager: WorkManager
) : ViewModel() {

    private val _importEvents = MutableSharedFlow<ImportEvent>()
    val importEvents: SharedFlow<ImportEvent> = _importEvents.asSharedFlow()

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
                        WorkInfo.State.SUCCEEDED -> _importEvents.emit(ImportEvent.Success)
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString("error") ?: "Неизвестная ошибка"
                            _importEvents.emit(ImportEvent.Error(error))
                        }
                        else -> {}
                    }
                }
            }
    }
}
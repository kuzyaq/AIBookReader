package com.example.aibookreader.presentation.screens.addbook

import androidx.lifecycle.ViewModel
import com.example.aibookreader.data.worker.ImportBookScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val importBookScheduler: ImportBookScheduler
): ViewModel(){

    fun importBook(filePath: String){
        importBookScheduler.importBook(filePath)
    }
}
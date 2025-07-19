package com.example.pdfconverter.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StateViewModel: ViewModel(){


    //for pdf remember of pdfPickerScreen
    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri: StateFlow<Uri?> = _selectedPdfUri

    private val _selectedPdfFileName = MutableStateFlow<String?>(null)
    val selectedPdfFileName: StateFlow<String?> = _selectedPdfFileName

    //for pdf remember of pdfMergingScreen
    private val _selectedPdfUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedPdfUris: StateFlow<List<Uri>> = _selectedPdfUris


    //fun of pdf merging
    fun removePdfUri(uri: Uri) {
        _selectedPdfUris.value = _selectedPdfUris.value.toMutableList().apply { remove(uri) }
    }

    fun addPdfUris(newUris: List<Uri>) {
        val updated = (_selectedPdfUris.value + newUris).distinct()
        _selectedPdfUris.value = updated
    }

    fun clearSelectedPdfUris() {
        _selectedPdfUris.value = emptyList()
    }


    //fun of pdf pickerScreen and pdf splitScreen
    fun setSelectedPdf(uri: Uri, name: String) {
        _selectedPdfUri.value = uri
        _selectedPdfFileName.value = name
    }

    fun clearSelectedPdf() {
        _selectedPdfUri.value = null
        _selectedPdfFileName.value = null
    }


}
package com.example.pdfconverter.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfconverter.utills.PdfUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PdfConverterViewModel : ViewModel() {

    private val _splitResultFiles = MutableStateFlow<List<String>>(emptyList())
    val splitResultFiles: StateFlow<List<String>> get() = _splitResultFiles

    private val _isSplitting = MutableStateFlow(false)
    val isSplitting: StateFlow<Boolean> get() = _isSplitting

    private val _isMerging = MutableStateFlow(false)
    val isMerging: StateFlow<Boolean> get() = _isMerging

    private val _mergedPdfUri = MutableStateFlow<Uri?>(null)
    val mergedPdfUri: StateFlow<Uri?> get() = _mergedPdfUri

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToText(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val text = PdfUtil.extractTextFromPdf(context, uri)
            PdfUtil.saveTextToDownloads(context, "$fileName.txt", text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToWord(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val text = PdfUtil.extractTextFromPdf(context, uri)
            PdfUtil.saveTextAsWordToDownloads(context, "$fileName.docx", text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToPpt(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val pages = PdfUtil.extractTextFromPdf(context, uri) // List<List<PdfElement>>

            // Flatten and extract text from PdfElement
            val contentList = pages.map { pageElements ->
                pageElements.joinToString("\n") { it.toString() } // Convert each page to string
            }

            PdfUtil.savePdfAsPptToDownloads(context, "$fileName.pptx", contentList)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToRtf(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val text = PdfUtil.extractTextFromPdf(context, uri)
            PdfUtil.saveTextAsRtfToDownloads(context, "$fileName.rtf", text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToDoc(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val text = PdfUtil.extractTextFromPdf(context, uri)
            PdfUtil.saveTextAsDocToDownloads(context, "$fileName.doc", text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToHtml(context: Context, uri: Uri, fileName: String) {
        launchIO {
            val pages = PdfUtil.extractTextFromPdf(context, uri)
            PdfUtil.saveHtmlToDownloads(context, fileName, pages)
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun mergeSelectedPdfs(context: Context, uris: List<Uri>, fileName: String) {
        viewModelScope.launch {
            _isMerging.value = true
            val mergedUri = withContext(Dispatchers.IO) {
                PdfUtil.mergePdfsWithIText(context, uris, fileName)
            }
            _mergedPdfUri.value = mergedUri
            _isMerging.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun splitPdfIntoPages(context: Context, uri: Uri, baseFileName: String) {
        viewModelScope.launch {
            _isSplitting.value = true
            val resultFiles = withContext(Dispatchers.IO) {
                PdfUtil.splitPdfPagesWithIText(context, uri, baseFileName)
            }
            _splitResultFiles.value = resultFiles
            _isSplitting.value = false
        }
    }

    fun clearSplitResult() {
        _splitResultFiles.value = emptyList()
        _isSplitting.value = false
    }

    // Helper function to avoid repeat launch(Dispatchers.IO)
    private fun launchIO(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
}

package com.example.pdfconverter.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfconverter.utills.PdfUtil
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfConverterViewModel : ViewModel() {

    private val _splitResultFiles = MutableStateFlow<List<String>>(emptyList())
    val splitResultFiles: StateFlow<List<String>> = _splitResultFiles

    private val _isSplitting = MutableStateFlow(false)
    val isSplitting: StateFlow<Boolean> = _isSplitting

    private val _isMerging = MutableStateFlow(false)
    val isMerging: StateFlow<Boolean> = _isMerging

    private val _mergedPdfUri = MutableStateFlow<Uri?>(null)
    val mergedPdfUri: StateFlow<Uri?> = _mergedPdfUri

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToText(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                PdfUtil.extractTextFromPdf(context, uri)
            }
            withContext(Dispatchers.IO) {
                PdfUtil.saveTextToDownloads(context, "$fileName.txt", text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToWord(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                PdfUtil.extractTextFromPdf(context, uri)
            }
            withContext(Dispatchers.IO) {
                PdfUtil.saveTextAsWordToDownloads(context, "$fileName.docx", text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToPpt(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val pageContents = withContext(Dispatchers.IO) {
                val reader = PdfReader(context.contentResolver.openInputStream(uri)!!)
                val pages = reader.numberOfPages
                val contentList = mutableListOf<String>()
                for (i in 1..pages) {
                    val text = PdfTextExtractor.getTextFromPage(reader, i)
                    contentList.add(text)
                }
                reader.close()
                contentList
            }
            withContext(Dispatchers.IO) {
                PdfUtil.savePdfAsPptToDownloads(context, "$fileName.pptx", pageContents)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToRtf(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                PdfUtil.extractTextFromPdf(context, uri)
            }
            withContext(Dispatchers.IO) {
                PdfUtil.saveTextAsRtfToDownloads(context, "$fileName.rtf", text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToDoc(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                PdfUtil.extractTextFromPdf(context, uri)
            }
            withContext(Dispatchers.IO) {
                PdfUtil.saveTextAsDocToDownloads(context, "$fileName.doc", text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun convertPdfToHtml(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val htmlContent = withContext(Dispatchers.IO) {
                val text = PdfUtil.extractTextFromPdf(context, uri)
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>$fileName</title>
                </head>
                <body>
                    <pre>${text.replace("<", "&lt;").replace(">", "&gt;")}</pre>
                </body>
                </html>
                """.trimIndent()
            }
            withContext(Dispatchers.IO) {
                PdfUtil.saveHtmlToDownloads(context, "$fileName.html", htmlContent)
            }
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
}

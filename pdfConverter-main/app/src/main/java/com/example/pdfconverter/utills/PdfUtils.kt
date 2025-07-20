package com.example.pdfconverter.utills

import androidx.annotation.RequiresApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.OutputStream


object PdfUtil {

    //for getting pdf data
    fun extractTextFromPdf(context: Context, uri: Uri): String {

        //Get the ContentResolver so you can read from uri.
        val resolver = context.contentResolver

        return resolver.openInputStream(uri)?.use { input ->
            //PdfReader (from iText) reads the PDF structure from the input stream.
            val reader = PdfReader(input)
           /*Get total number of pages in the PDF.*/
            val pages = reader.numberOfPages
           /* Initialize a StringBuilder to efficiently build the final text output.*/
            val sb = StringBuilder()

            for (i in 1..pages) {
                sb.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n\n")
            }

            reader.close()
           /*Convert the StringBuilder to a full String and return it.*/
            sb.toString()
        } ?: "Failed to read file"
    }

    //for txt
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextToDownloads(context: Context, fileName: String, content: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)

            if (itemUri != null) {
                resolver.openOutputStream(itemUri).use { stream: OutputStream? ->
                    stream?.write(content.toByteArray())
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    //for word docx
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextAsWordToDownloads(context: Context, fileName: String, content: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)

            itemUri?.let { uri ->
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        // Build DOCX file
                        val document = org.apache.poi.xwpf.usermodel.XWPFDocument()
                        val para = document.createParagraph()
                        val run = para.createRun()
                        run.setText(content)

                        document.write(outputStream)
                        document.close()
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                Toast.makeText(context, "Saved as $fileName", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(context, "Failed to save Word file", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }


    //for ppt
    @RequiresApi(Build.VERSION_CODES.Q)
    fun savePdfAsPptToDownloads(context: Context, fileName: String, content: List<String>) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)

            itemUri?.let { uri ->
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        val ppt = org.apache.poi.xslf.usermodel.XMLSlideShow()
                        val slideMaster = ppt.slideMasters[0]

                        for (pageText in content) {
                            val slide = ppt.createSlide(
                                slideMaster.getLayout(org.apache.poi.xslf.usermodel.SlideLayout.TITLE_AND_CONTENT)
                            )
                            val shapes = slide.shapes
                            if (shapes.isNotEmpty()) {
                                val shape = shapes[0]
                                if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                                    shape.clearText()
                                    shape.setText(pageText.take(2000)) // avoid overloading
                                }
                            }
                        }

                        ppt.write(outputStream)
                        ppt.close()
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            // Handle silently or log if needed
        }
    }



    //for rtf
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextAsRtfToDownloads(context: Context, fileName: String, content: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/rtf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)

            if (itemUri != null) {
                resolver.openOutputStream(itemUri).use { stream ->
                    if (stream != null) {
                        val rtfContent = "{\\rtf1\\ansi\\deff0\\n${content.replace("\n", "\\line ")} }"
                        stream.write(rtfContent.toByteArray())
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    //for doc
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextAsDocToDownloads(context: Context, fileName: String, content: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/msword")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)

            itemUri?.let { uri ->
                resolver.openOutputStream(uri).use { stream ->
                    if (stream != null) {
                        // Create a blank document from a dummy InputStream
                        val emptyDocStream = ByteArray(512) // minimum header size for DOC
                        val doc = org.apache.poi.hwpf.HWPFDocument(emptyDocStream.inputStream())
                        val range = doc.range
                        range.insertAfter(content)

                        doc.write(stream)
                        doc.close()
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            // Error handled silently, no toast
        }
    }


    //for html
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveHtmlToDownloads(context: Context, fileName: String, htmlContent: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Downloads.DISPLAY_NAME,
                if (fileName.endsWith(".html")) fileName else "$fileName.html"
            )
            put(MediaStore.Downloads.MIME_TYPE, "text/html")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, contentValues)

        itemUri?.let { uri ->
            resolver.openOutputStream(uri).use { stream: OutputStream? ->
                stream?.write(htmlContent.toByteArray(Charsets.UTF_8))
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }


    //for merging pdfs
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun mergePdfsWithIText(context: Context, uris: List<Uri>, outputFileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val outputUri = resolver.insert(downloadsUri, values)

            outputUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val document = Document()
                    val copy = PdfCopy(document, outputStream)
                    document.open()

                    for (pdfUri in uris) {
                        val inputStream = resolver.openInputStream(pdfUri)
                        val reader = PdfReader(inputStream)
                        for (i in 1..reader.numberOfPages) {
                            val page = copy.getImportedPage(reader, i)
                            copy.addPage(page)
                        }
                        reader.close()
                    }

                    document.close()
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
                }

                uri
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create merged file", Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }


    //for splitting
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun splitPdfPagesWithIText(context: Context, uri: Uri, baseFileName: String): List<String> = withContext(Dispatchers.IO) {
        val fileNames = mutableListOf<String>()
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri)
        val reader = PdfReader(inputStream)
        val totalPages = reader.numberOfPages

        for (i in 1..totalPages) {
            val fileName = "${baseFileName}_page_$i.pdf"
            val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val outputUri = resolver.insert(downloadsUri, values)

            outputUri?.let { output ->
                resolver.openOutputStream(output)?.use { outputStream ->
                    val document = Document()
                    val copy = PdfCopy(document, outputStream)
                    document.open()
                    val page = copy.getImportedPage(reader, i)
                    copy.addPage(page)
                    document.close()
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(output, values, null, null)
                fileNames.add(fileName)
            }
        }

        reader.close()
        return@withContext fileNames
    }


}





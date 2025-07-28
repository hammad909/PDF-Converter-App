package com.example.pdfconverter.utills

import androidx.annotation.RequiresApi
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import android.content.Context
import android.net.Uri
import com.example.pdfconverter.dataClasses.PdfElement
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy
import com.itextpdf.kernel.utils.PdfMerger
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import kotlin.math.abs


object PdfUtil {

    //for getting pdf data
// Modified extractTextFromPdf to use groupTextByLines
    fun extractTextFromPdf(context: Context, uri: Uri): List<List<PdfElement>> {
        val resolver = context.contentResolver
        val pages = mutableListOf<List<PdfElement>>()

        resolver.openInputStream(uri)?.use { input ->
            val reader = PdfReader(input)
            val pdfDoc = PdfDocument(reader)

            for (pageIndex in 1..pdfDoc.numberOfPages) {
                val pdfPage = pdfDoc.getPage(pageIndex)
                val pageElements = mutableListOf<PdfElement>()

                // ✅ TEXT: Extract raw text using SimpleTextExtractionStrategy
                val strategy = SimpleTextExtractionStrategy()
                val rawText = PdfTextExtractor.getTextFromPage(pdfPage, strategy)

                // Split text into lines
                val lines = rawText.lines()
                var lineY = 800f // start from top (you can calibrate this based on page height)

                for (line in lines) {
                    if (line.isNotBlank()) {
                        pageElements.add(
                            PdfElement(
                                type = "text",
                                content = line.trim(),
                                x = 40f, // generic left padding
                                y = lineY,
                                fontSize = 12f // you can optionally tweak this or ignore
                            )
                        )
                    }
                    lineY -= 18f // line spacing
                }

                // ✅ IMAGE: Extract images using PdfCanvasProcessor
                val listener = object : IEventListener {
                    override fun eventOccurred(data: IEventData?, type: EventType?) {
                        if (type == EventType.RENDER_IMAGE) {
                            val imageRenderInfo = data as ImageRenderInfo
                            val image = imageRenderInfo.image
                            val imageBytes = image?.imageBytes
                            val matrix = imageRenderInfo.imageCtm

                            if (imageBytes != null) {
                                val x = matrix[6]
                                val y = matrix[7]
                                val width = matrix[0]
                                val height = matrix[4]

                                pageElements.add(
                                    PdfElement(
                                        type = "image",
                                        x = x,
                                        y = y,
                                        width = width,
                                        height = height,
                                        imageBytes = imageBytes
                                    )
                                )
                            }

                        }
                    }

                    override fun getSupportedEvents() = setOf(EventType.RENDER_IMAGE)
                }

                val processor = PdfCanvasProcessor(listener)
                processor.processPageContent(pdfPage)

                // ✅ Sort everything by Y descending (top to bottom)
                val sortedPage = pageElements.sortedByDescending { it.y }
                pages.add(sortedPage)
            }

            pdfDoc.close()
            reader.close()
        }

        return pages
    }


    // Your saveTextToDownloads function (no changes needed here, as it uses the same PdfElement list structure)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextToDownloads(context: Context, fileName: String, pages: List<List<PdfElement>>) {
        val plainText = buildString {
            pages.forEachIndexed { index, page ->
                append("Page ${index + 1}\n")
                page.forEach { element ->
                    when (element.type) {
                        "text" -> appendLine(element.content)
                        "image" -> appendLine("[Image]")
                    }
                }
                appendLine("\n-----------------------------------\n")
            }
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(
                MediaStore.Downloads.DISPLAY_NAME,
                if (fileName.endsWith(".txt")) fileName else "$fileName.txt"
            )
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)

        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                output.write(plainText.toByteArray(Charsets.UTF_8))
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }


    // Your saveToWordDocx function (adjusted to use new PdfElement fields for precision)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextAsWordToDownloads(context: Context, fileName: String, pages: List<List<PdfElement>>) {
        val document = XWPFDocument()

        pages.forEachIndexed { pageIndex, pageElements ->
            if (pageIndex > 0) {
                val paragraph = document.createParagraph()
                paragraph.setPageBreak(true)
            }

            var lastY = Float.MAX_VALUE // To track vertical spacing for paragraphs
            var lastFontSize = 10f // Default for first line

            pageElements.forEach { element ->
                when (element.type) {
                    "text" -> {
                        val content = element.content ?: ""
                        val x = element.x
                        val y = element.y
                        val fontSize = element.fontSize ?: 10f
                        val fontFamily = element.fontFamily
                        val isBold = element.isBold
                        val isItalic = element.isItalic

                        val paragraph = document.createParagraph()

                        // Calculate and apply vertical spacing more accurately
                        if (lastY != Float.MAX_VALUE) {
                            val verticalSpace = abs(lastY - y) // Distance between baselines
                            // A rough line height can be approximated from font size * line multiplier.
                            // If vertical space is significantly more than typical line height, add spacing.
                            // Fine-tune this threshold (e.g., 1.2 to 2.0 times the font size)
                            val assumedLineHeight = lastFontSize * 1.2f // Adjust multiplier for typical line height
                            if (verticalSpace > assumedLineHeight) {
                                // Convert points to twips (1/20th of a point).
                                // This calculates the extra space needed.
                                val extraVerticalSpaceTwips = ((verticalSpace - assumedLineHeight) * 20).toInt()
                                if (extraVerticalSpaceTwips > 0) {
                                    paragraph.spacingBefore = extraVerticalSpaceTwips
                                }
                            }
                        }

                        val run = paragraph.createRun()
                        run.setText(content)
                        run.setFontSize(fontSize.toDouble())

                        // Apply font family, bold, italic
                        fontFamily?.let { run.setFontFamily(it) }
                        run.isBold = isBold
                        run.isItalic = isItalic

                        // Apply left indentation to mimic X position
                        // Convert PDF points to DOCX twips (1 point = 20 twips)
                        // This creates a left indent for the entire paragraph.
                        if (x > 0) {
                            paragraph.indentationLeft = (x * 20).toInt()
                        }

                        lastY = y
                        lastFontSize = fontSize
                    }
                    "image" -> {
                        val imageBytes = element.imageBytes
                        val width = element.width ?: 0f
                        val height = element.height ?: 0f
                        val x = element.x // X position of image
                        val y = element.y // Y position of image

                        if (imageBytes != null && width > 0 && height > 0) {
                            try {
                                val pictureData = document.addPictureData(imageBytes, XWPFDocument.PICTURE_TYPE_PNG)
                                val paragraph = document.createParagraph()
                                paragraph.alignment = ParagraphAlignment.LEFT

                                // Apply left indentation for images as well, similar to text
                                if (x > 0) {
                                    paragraph.indentationLeft = (x * 20).toInt()
                                }

                                val run = paragraph.createRun()

                                // Convert points (PDF unit) to EMUs (English Metric Units - used by POI)
                                // 1 point = 12700 EMUs
                                val imgWidthEMU = (width * 12700).toInt()
                                val imgHeightEMU = (height * 12700).toInt()

                                run.addPicture(ByteArrayInputStream(imageBytes), XWPFDocument.PICTURE_TYPE_PNG, "image.png", imgWidthEMU, imgHeightEMU)

                                lastY = y // Update lastY with image's Y for subsequent elements
                                lastFontSize = 10f // Reset or estimate for images, as they don't have font size
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Log or handle image insertion error gracefully
                            }
                        }
                    }
                }
            }
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(
                MediaStore.Downloads.DISPLAY_NAME,
                if (fileName.endsWith(".docx")) fileName else "$fileName.docx"
            )
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)

        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                document.write(output)
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }


    //for ppt
    @RequiresApi(Build.VERSION_CODES.Q)
    fun savePdfAsPptToDownloads(context: Context, fileName: String, pageContents: List<String>) {
        val ppt = XMLSlideShow()

        for (content in pageContents) {
            val slide = ppt.createSlide()
            val shape = slide.createTextBox()
            shape.text = content // Add text to the shape
            // Don't set `anchor` because Rectangle isn't available
        }

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.pptx")
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { stream ->
                ppt.write(stream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }

    //for rtf
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveTextAsRtfToDownloads(context: Context, fileName: String, content: List<List<PdfElement>>): Boolean {
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
                        val rtfContent = buildString {
                            append("{\\rtf1\\ansi\\deff0\n")
                            content.forEach { page ->
                                page.forEach { element ->
                                    if (element.type == "text" && !element.content.isNullOrBlank()) {
                                        append(element.content.replace("\n", "\\line "))
                                        append("\\line\n")
                                    }
                                }
                                append("\\page\n") // Optional: separate pages in RTF
                            }
                            append("}")
                        }
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
     fun saveTextAsDocToDownloads(context: Context, fileName: String, content: List<List<PdfElement>>) {
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
                    // Create an empty HWPF document using POI
                    val doc = HWPFDocument(ByteArrayInputStream(ByteArray(512)))
                    val range = doc.range

                    val extractedText = buildString {
                        content.forEach { page ->
                            page.forEach { element ->
                                if (element.type == "text" && !element.content.isNullOrBlank()) {
                                    append(element.content)
                                    append("\n")
                                }
                            }
                            append("\n") // Page break (optional)
                        }
                    }

                    range.insertAfter(extractedText.toString())
                    doc.write(stream)
                    doc.close()
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    } catch (e: Exception) {
        e.printStackTrace() // Optional: Log error for debugging
    }
}


    //for html
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveHtmlToDownloads(context: Context, fileName: String, pages: List<List<PdfElement>>) {
        val htmlContent = buildString {
            append("<html><body>")

            for (page in pages) {
                val sortedPage = page.sortedWith(
                    compareByDescending<PdfElement> { it.y }.thenBy { it.x }
                )

                for (element in sortedPage) {
                    when (element.type) {
                        "text" -> {
                            append("<p style='font-size:14px;'>${element.content}</p>")
                        }

                        "image" -> {
                            val base64Image = Base64.encodeToString(
                                element.imageBytes,
                                Base64.NO_WRAP
                            )
                            append(
                                "<img src='data:image/png;base64,$base64Image' " +
                                        "width='${element.width}' height='${element.height}' style='margin:8px 0;' /><br/>"
                            )
                        }
                    }
                }

                append("<hr style='border:1px dashed #ccc; margin:20px 0;'/>") // Page break
            }

            append("</body></html>")
        }

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
            resolver.openOutputStream(uri).use { stream ->
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
                    val pdfWriter = PdfWriter(outputStream)
                    val mergedPdfDoc = PdfDocument(pdfWriter)
                    val merger = PdfMerger(mergedPdfDoc)

                    for (pdfUri in uris) {
                        resolver.openInputStream(pdfUri)?.use { inputStream ->
                            val reader = PdfReader(inputStream)
                            val pdfDoc = PdfDocument(reader)
                            merger.merge(pdfDoc, 1, pdfDoc.numberOfPages)
                            pdfDoc.close()
                        }
                    }

                    mergedPdfDoc.close()
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                uri
            }
        }
    }

    //for splitting
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun splitPdfPagesWithIText(context: Context, uri: Uri, baseFileName: String): List<String> = withContext(Dispatchers.IO) {
        val fileNames = mutableListOf<String>()
        val resolver = context.contentResolver

        resolver.openInputStream(uri)?.use { inputStream ->
            val reader = PdfReader(inputStream)
            val sourcePdf = PdfDocument(reader)
            val totalPages = sourcePdf.numberOfPages

            for (i in 1..totalPages) {
                val fileName = "${baseFileName}_page_$i.pdf"
                val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val outputUri = resolver.insert(downloadsUri, values)

                outputUri?.let { outUri ->
                    resolver.openOutputStream(outUri)?.use { outputStream ->
                        val writer = PdfWriter(outputStream)
                        val newPdf = PdfDocument(writer)
                        sourcePdf.copyPagesTo(i, i, newPdf)
                        newPdf.close()
                    }

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(outUri, values, null, null)

                    fileNames.add(fileName)
                }
            }

            sourcePdf.close()
        }

        return@withContext fileNames
    }


}





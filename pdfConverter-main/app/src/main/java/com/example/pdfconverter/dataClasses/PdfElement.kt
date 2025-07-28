package com.example.pdfconverter.dataClasses

data class PdfElement(
    val type: String,
    val content: String? = null,
    val imageBytes: ByteArray? = null,
    val x: Float,
    val y: Float,
    val width: Float? = null,
    val height: Float? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val charSpacing: Float? = null,
    val wordSpacing: Float? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PdfElement

        if (x != other.x) return false
        if (y != other.y) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (fontSize != other.fontSize) return false
        if (fontFamily != other.fontFamily) return false
        if (isBold != other.isBold) return false
        if (isItalic != other.isItalic) return false
        if (charSpacing != other.charSpacing) return false
        if (wordSpacing != other.wordSpacing) return false
        if (type != other.type) return false
        if (content != other.content) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + (width?.hashCode() ?: 0)
        result = 31 * result + (height?.hashCode() ?: 0)
        result = 31 * result + (fontSize?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0) // Include new fields
        result = 31 * result + isBold.hashCode()             // Include new fields
        result = 31 * result + isItalic.hashCode()           // Include new fields
        result = 31 * result + (charSpacing?.hashCode() ?: 0) // Include new fields
        result = 31 * result + (wordSpacing?.hashCode() ?: 0) // Include new fields
        result = 31 * result + type.hashCode()
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}
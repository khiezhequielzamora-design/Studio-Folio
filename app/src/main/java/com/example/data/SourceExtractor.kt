package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ExtractedSourceResult(
    val title: String,
    val sourceType: SourceType,
    val mimeType: String,
    val content: String
)

class SourceExtractor(
    private val context: Context,
    private val aiService: StudyAiService
) {
    companion object {
        private const val MAX_PDF_BYTES = 14 * 1024 * 1024 // Stay safely under Gemini 20MB inline limit
        private const val MAX_IMAGE_DIM = 1600
    }

    suspend fun extractFromDocumentUri(uri: Uri): ExtractedSourceResult {
        val resolver = context.contentResolver
        val fileName = resolveFileName(uri)
        val mimeType = resolver.getType(uri) ?: inferMimeType(fileName)
        val lowerName = fileName.lowercase()

        return when {
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> {
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Unable to open PDF file.")
                }
                if (bytes.size > MAX_PDF_BYTES) {
                    throw IllegalStateException("PDF exceeds 14 MB limit for direct study extraction.")
                }
                val extracted = aiService.extractTextFromPdf(bytes, fileName)
                ExtractedSourceResult(
                    title = fileName,
                    sourceType = SourceType.PDF,
                    mimeType = "application/pdf",
                    content = extracted
                )
            }

            mimeType.contains("presentationml") || lowerName.endsWith(".pptx") -> {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { extractPptxText(it) }
                        ?: throw IllegalStateException("Unable to open PowerPoint file.")
                }
                if (text.isBlank()) {
                    throw IllegalStateException("No readable slide text found in $fileName.")
                }
                ExtractedSourceResult(
                    title = fileName,
                    sourceType = SourceType.SLIDES,
                    mimeType = mimeType,
                    content = text
                )
            }

            mimeType.contains("wordprocessingml") || lowerName.endsWith(".docx") -> {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { extractDocxText(it) }
                        ?: throw IllegalStateException("Unable to open Word document.")
                }
                if (text.isBlank()) {
                    throw IllegalStateException("No readable text found in $fileName.")
                }
                ExtractedSourceResult(
                    title = fileName,
                    sourceType = SourceType.DOC,
                    mimeType = mimeType,
                    content = text
                )
            }

            mimeType.startsWith("image/") -> {
                extractFromImageUri(uri, fileName, mimeType)
            }

            else -> {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Unable to read text from file.")
                }
                val clean = text.trim()
                if (clean.isEmpty()) {
                    throw IllegalStateException("The selected file appears to be empty.")
                }
                ExtractedSourceResult(
                    title = fileName,
                    sourceType = SourceType.DOC,
                    mimeType = mimeType,
                    content = clean.take(78000)
                )
            }
        }
    }

    suspend fun extractFromImageUri(
        uri: Uri,
        overrideName: String? = null,
        overrideMime: String? = null
    ): ExtractedSourceResult {
        val resolver = context.contentResolver
        val fileName = overrideName ?: resolveFileName(uri)
        val mimeType = overrideMime ?: resolver.getType(uri) ?: "image/jpeg"

        val bitmap = withContext(Dispatchers.IO) {
            decodeScaledBitmap(uri)
                ?: throw IllegalStateException("Could not decode the selected image.")
        }
        val extracted = try {
            aiService.extractTextFromImage(bitmap, fileName)
        } finally {
            bitmap.recycle()
        }
        return ExtractedSourceResult(
            title = fileName,
            sourceType = SourceType.IMAGE,
            mimeType = mimeType,
            content = extracted
        )
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Study Material"
    }

    private fun inferMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            else -> "text/plain"
        }
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_DIM ||
            bounds.outHeight / sampleSize > MAX_IMAGE_DIM
        ) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun extractPptxText(inputStream: InputStream): String {
        val slides = mutableListOf<Pair<Int, String>>()
        val slideRegex = Regex("ppt/slides/slide(\\d+)\\.xml")

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val match = slideRegex.matchEntire(entry.name)
                if (match != null) {
                    val slideNum = match.groupValues[1].toIntOrNull() ?: 0
                    val xmlBytes = zip.readBytes()
                    val slideText = parseXmlTextTags(xmlBytes, textTag = "t", paragraphTag = "p")
                    if (slideText.isNotBlank()) {
                        slides.add(slideNum to slideText)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return slides.sortedBy { it.first }
            .joinToString("\n\n") { (num, text) -> "## Slide $num\n$text" }
            .take(78000)
    }

    private fun extractDocxText(inputStream: InputStream): String {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlBytes = zip.readBytes()
                    return parseXmlTextTags(xmlBytes, textTag = "t", paragraphTag = "p")
                        .take(78000)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ""
    }

    private fun parseXmlTextTags(
        xmlBytes: ByteArray,
        textTag: String,
        paragraphTag: String
    ): String {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        val output = StringBuilder()
        val currentParagraph = StringBuilder()
        var insideTextTag = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == textTag) {
                        insideTextTag = true
                    }
                }

                XmlPullParser.TEXT -> {
                    if (insideTextTag) {
                        currentParagraph.append(parser.text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        textTag -> insideTextTag = false
                        paragraphTag -> {
                            val line = currentParagraph.toString().trim()
                            if (line.isNotEmpty()) {
                                output.appendLine(line)
                            }
                            currentParagraph.clear()
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        val trailing = currentParagraph.toString().trim()
        if (trailing.isNotEmpty()) output.appendLine(trailing)
        return output.toString().trim()
    }
}

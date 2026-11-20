package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONObject

enum class NotebookColorToken(val key: String, val label: String) {
    TEAL("teal", "Forest Teal"),
    TERRACOTTA("terracotta", "Terracotta"),
    AMBER("amber", "Warm Amber"),
    INDIGO("indigo", "Slate Indigo"),
    SAGE("sage", "Botanical Sage");

    companion object {
        fun fromKey(raw: String?): NotebookColorToken =
            entries.firstOrNull { it.key == raw } ?: TEAL
    }
}

enum class SourceType(val key: String, val badge: String) {
    PDF("PDF", "PDF"),
    DOC("DOC", "DOCX"),
    SLIDES("SLIDES", "PPTX"),
    NOTE("NOTE", "NOTE"),
    IMAGE("IMAGE", "IMAGE");

    companion object {
        fun fromKey(raw: String?): SourceType =
            entries.firstOrNull { it.key == raw } ?: NOTE
    }
}

enum class ArtifactType(val key: String, val label: String) {
    SUMMARY("SUMMARY", "Summary Brief"),
    STUDY_GUIDE("STUDY_GUIDE", "Study Guide"),
    FLASHCARDS("FLASHCARDS", "Flashcards"),
    QUIZ("QUIZ", "Practice Exam"),
    AUDIO_OVERVIEW("AUDIO_OVERVIEW", "Audio Discussion");

    companion object {
        fun fromKey(raw: String?): ArtifactType =
            entries.firstOrNull { it.key == raw } ?: STUDY_GUIDE
    }
}

enum class ChatRole(val key: String) {
    USER("user"),
    ASSISTANT("assistant");

    companion object {
        fun fromKey(raw: String?): ChatRole =
            entries.firstOrNull { it.key == raw } ?: ASSISTANT
    }
}

@Keep
@IgnoreExtraProperties
data class Notebook(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val colorToken: String = NotebookColorToken.TEAL.key,
    val sourceCount: Long = 0L,
    val summaryOverview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudySource(
    @DocumentId val id: String = "",
    val userId: String = "",
    val notebookId: String = "",
    val title: String = "",
    val sourceType: String = SourceType.NOTE.key,
    val mimeType: String = "text/plain",
    val content: String = "",
    val excerpt: String = "",
    val wordCount: Long = 0L,
    val included: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyArtifact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val notebookId: String = "",
    val artifactType: String = ArtifactType.STUDY_GUIDE.key,
    val title: String = "",
    val payloadJson: String = "",
    val sourceTitles: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val notebookId: String = "",
    val role: String = ChatRole.USER.key,
    val text: String = "",
    val citations: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

// Parsed domain models for interactive Studio Artifacts
data class FlashcardItem(
    val front: String,
    val back: String,
    val sourceHint: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceHint: String
)

data class AudioTurn(
    val speaker: String,
    val line: String
)

data class AudioOverviewData(
    val episodeTitle: String,
    val summary: String,
    val turns: List<AudioTurn>
)

object ArtifactJsonParser {
    fun parseFlashcards(payloadJson: String): List<FlashcardItem> = try {
        val root = JSONObject(payloadJson)
        val array = root.getJSONArray("cards")
        (0 until array.length()).map { idx ->
            val obj = array.getJSONObject(idx)
            FlashcardItem(
                front = obj.optString("front", "Question"),
                back = obj.optString("back", "Answer"),
                sourceHint = obj.optString("sourceHint", "")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun parseQuiz(payloadJson: String): List<QuizQuestion> = try {
        val root = JSONObject(payloadJson)
        val array = root.getJSONArray("questions")
        (0 until array.length()).mapNotNull { idx ->
            val obj = array.getJSONObject(idx)
            val optsArr = obj.getJSONArray("options")
            val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
            if (opts.size < 2) return@mapNotNull null
            QuizQuestion(
                question = obj.optString("question", ""),
                options = opts,
                correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex),
                explanation = obj.optString("explanation", ""),
                sourceHint = obj.optString("sourceHint", "")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun parseAudioOverview(payloadJson: String): AudioOverviewData? = try {
        val root = JSONObject(payloadJson)
        val turnsArr = root.getJSONArray("dialogue")
        val turns = (0 until turnsArr.length()).map { idx ->
            val obj = turnsArr.getJSONObject(idx)
            AudioTurn(
                speaker = obj.optString("speaker", if (idx % 2 == 0) "Alex" else "Maya"),
                line = obj.optString("line", "")
            )
        }.filter { it.line.isNotBlank() }
        AudioOverviewData(
            episodeTitle = root.optString("episodeTitle", "Audio Lesson Overview"),
            summary = root.optString("summary", ""),
            turns = turns
        )
    } catch (_: Exception) {
        null
    }
}

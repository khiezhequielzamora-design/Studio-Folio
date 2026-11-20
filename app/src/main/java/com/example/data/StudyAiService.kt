package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ArtifactType
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.StudySource
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "StudyAiService"
private const val AI_TIMEOUT_MS = 60_000L
private const val MODEL_NAME = "gemini-3.8-flash"

class StudyAiService {

    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are StudyFolio, an accurate, supportive academic study assistant. " +
                        "Always ground your explanations in the student's uploaded sources. " +
                        "Cite source titles in square brackets like [Source Title] when referencing facts. " +
                        "If the sources do not cover a question, clearly say so before offering general context."
                )
            }
        )
    }

    private val flashcardsModel by lazy {
        val cardSchema = Schema.obj(
            properties = mapOf(
                "front" to Schema.string(description = "Clear question, concept, or term on the front of the card"),
                "back" to Schema.string(description = "Concise, accurate answer or explanation on the back"),
                "sourceHint" to Schema.string(description = "Title of the source document this card comes from")
            )
        )
        val deckSchema = Schema.obj(
            properties = mapOf(
                "deckTitle" to Schema.string(description = "Descriptive title for this flashcard deck"),
                "cards" to Schema.array(
                    items = cardSchema,
                    description = "8 to 12 high-yield study flashcards grounded in the sources"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = deckSchema
            }
        )
    }

    private val quizModel by lazy {
        val questionSchema = Schema.obj(
            properties = mapOf(
                "question" to Schema.string(description = "Exam-style multiple choice question grounded in the sources"),
                "options" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 4 plausible answer choices"
                ),
                "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option"),
                "explanation" to Schema.string(description = "Brief explanation of why the answer is correct"),
                "sourceHint" to Schema.string(description = "Title of the source document tested")
            )
        )
        val quizSchema = Schema.obj(
            properties = mapOf(
                "quizTitle" to Schema.string(description = "Title of the practice exam"),
                "questions" to Schema.array(
                    items = questionSchema,
                    description = "6 to 8 multiple choice practice questions"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val audioOverviewModel by lazy {
        val turnSchema = Schema.obj(
            properties = mapOf(
                "speaker" to Schema.enumeration(
                    values = listOf("Alex", "Maya"),
                    description = "Speaker name alternating naturally between Alex and Maya"
                ),
                "line" to Schema.string(description = "Natural conversational spoken line (1-3 sentences)")
            )
        )
        val audioSchema = Schema.obj(
            properties = mapOf(
                "episodeTitle" to Schema.string(description = "Catchy podcast episode title summarizing the lesson"),
                "summary" to Schema.string(description = "2-sentence overview of what Alex and Maya discuss"),
                "dialogue" to Schema.array(
                    items = turnSchema,
                    description = "10 to 14 engaging back-and-forth conversational turns between Alex and Maya"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = audioSchema
            }
        )
    }

    suspend fun extractTextFromPdf(pdfBytes: ByteArray, fileName: String): String {
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = generalModel.generateContent(
                    content {
                        inlineData(pdfBytes, "application/pdf")
                        text(
                            "Extract and organize the full educational content from this PDF document ('$fileName') " +
                                "for a student's study notebook. Preserve headings, key definitions, formulas, " +
                                "bullet points, examples, and important conclusions accurately without omitting details."
                        )
                    }
                )
                response.text?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("Could not extract readable text from this PDF.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "PDF extraction timed out", e)
            throw IllegalStateException("Reading PDF timed out after 60 seconds. Try a smaller file.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction failed", e)
            throw e
        }
    }

    suspend fun extractTextFromImage(bitmap: Bitmap, fileName: String): String {
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = generalModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Transcribe and explain all study content visible in this image ('$fileName'). " +
                                "Include handwritten or printed text, equations, labels, and a clear explanation " +
                                "of any diagrams, charts, or tables shown."
                        )
                    }
                )
                response.text?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("No readable study content was detected in the image.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image extraction timed out", e)
            throw IllegalStateException("Analyzing image timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image extraction failed", e)
            throw e
        }
    }

    suspend fun generateNotebookOverview(sources: List<StudySource>): String {
        val contextBlock = buildSourcesContext(sources)
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = generalModel.generateContent(
                    """
                    Based on the student's uploaded study sources below, write a crisp 3-4 sentence overview 
                    of what this notebook covers, followed by 3 bullet-pointed "Core Takeaways".
                    
                    $contextBlock
                    """.trimIndent()
                )
                response.text?.trim().orEmpty()
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Overview generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Overview generation failed", e)
            throw e
        }
    }

    fun askNotebookStream(
        question: String,
        sources: List<StudySource>,
        recentHistory: List<ChatMessage>
    ): Flow<String> = flow {
        val contextBlock = buildSourcesContext(sources)
        val historySnippet = recentHistory.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.role == ChatRole.USER.key) "Student" else "Assistant"
            "$speaker: ${msg.text.take(600)}"
        }
        val prompt = buildString {
            appendLine("STUDY SOURCES:")
            appendLine(contextBlock)
            if (historySnippet.isNotBlank()) {
                appendLine("\nRECENT CONVERSATION:")
                appendLine(historySnippet)
            }
            appendLine("\nSTUDENT QUESTION:")
            appendLine(question)
            appendLine(
                "\nAnswer clearly and thoroughly for a student. Cite relevant sources inline using " +
                    "[Source Title]. Use bullet points or step-by-step explanations for difficult concepts."
            )
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming chat timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming chat failed", e)
            throw e
        }
    }

    suspend fun generateStudioArtifact(
        artifactType: ArtifactType,
        notebookTitle: String,
        sources: List<StudySource>
    ): Pair<String, String> {
        val contextBlock = buildSourcesContext(sources)
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                when (artifactType) {
                    ArtifactType.SUMMARY -> {
                        val response = generalModel.generateContent(
                            """
                            Create an Executive Summary Brief for the notebook "$notebookTitle" using ONLY the sources below.
                            Organize with:
                            - High-Level Thesis (2-3 sentences)
                            - Key Concepts & Findings (clear bullet points with [Source Title] citations)
                            - Quick Synthesis (how the ideas connect)
                            
                            $contextBlock
                            """.trimIndent()
                        )
                        val text = response.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalStateException("Summary generation returned empty text.")
                        "Summary Brief: $notebookTitle" to text
                    }

                    ArtifactType.STUDY_GUIDE -> {
                        val response = generalModel.generateContent(
                            """
                            Build a comprehensive Student Study Guide for "$notebookTitle" grounded in the sources below.
                            Include clear sections:
                            1. Core Concepts & Explanations
                            2. Key Vocabulary & Definitions
                            3. Important Relationships, Steps, or Formulas
                            4. Common Misconceptions to Avoid
                            5. Self-Test Short Answer Prompts (with brief answer hints)
                            Cite sources inline with [Source Title].
                            
                            $contextBlock
                            """.trimIndent()
                        )
                        val text = response.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalStateException("Study guide generation returned empty text.")
                        "Study Guide: $notebookTitle" to text
                    }

                    ArtifactType.FLASHCARDS -> {
                        val response = flashcardsModel.generateContent(
                            """
                            Create 10 high-yield flashcards for studying "$notebookTitle" based strictly on the sources below.
                            Cover key definitions, cause-and-effect concepts, and important examples.
                            
                            $contextBlock
                            """.trimIndent()
                        )
                        val json = response.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalStateException("Flashcard generation returned empty output.")
                        "Flashcard Deck: $notebookTitle" to json
                    }

                    ArtifactType.QUIZ -> {
                        val response = quizModel.generateContent(
                            """
                            Create a 6-question multiple-choice practice exam for "$notebookTitle" grounded strictly in the sources below.
                            Ensure each question tests real comprehension (not trivial recall), has 4 distinct options,
                            and includes a clear explanation referencing the source material.
                            
                            $contextBlock
                            """.trimIndent()
                        )
                        val json = response.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalStateException("Quiz generation returned empty output.")
                        "Practice Exam: $notebookTitle" to json
                    }

                    ArtifactType.AUDIO_OVERVIEW -> {
                        val response = audioOverviewModel.generateContent(
                            """
                            Write an engaging, friendly two-host educational podcast script ("Alex" and "Maya") 
                            explaining the key lessons in "$notebookTitle" based on the sources below.
                            Alex guides the big-picture questions and analogies; Maya unpacks the technical details 
                            and real-world examples. Keep each spoken turn natural, vivid, and easy to follow aloud (12 turns).
                            
                            $contextBlock
                            """.trimIndent()
                        )
                        val json = response.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalStateException("Audio discussion generation returned empty output.")
                        "Audio Discussion: $notebookTitle" to json
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Artifact generation timed out for $artifactType", e)
            throw IllegalStateException("${artifactType.label} timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Artifact generation failed for $artifactType", e)
            throw e
        }
    }

    fun extractCitedTitles(responseText: String, sources: List<StudySource>): List<String> {
        return sources.map { it.title }.filter { title ->
            responseText.contains("[$title]", ignoreCase = true) ||
                responseText.contains(title, ignoreCase = true)
        }.distinct().take(10)
    }

    private fun buildSourcesContext(sources: List<StudySource>): String {
        if (sources.isEmpty()) return "(No active sources provided.)"
        val perSourceBudget = (55_000 / sources.size).coerceIn(2_000, 18_000)
        return sources.joinToString("\n\n---\n\n") { source ->
            val snippet = if (source.content.length > perSourceBudget) {
                source.content.take(perSourceBudget) + "\n[...truncated for length]"
            } else {
                source.content
            }
            "[Source: ${source.title} (${source.sourceType})]\n$snippet"
        }
    }
}

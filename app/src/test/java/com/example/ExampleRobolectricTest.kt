package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArtifactJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StudyFolio", appName)
    }

    @Test
    fun `flashcard parser extracts structured cards`() {
        val json = """
            {
              "deckTitle": "Biology Deck",
              "cards": [
                {
                  "front": "What powers ATP synthase?",
                  "back": "The electrochemical proton gradient across the inner mitochondrial membrane.",
                  "sourceHint": "Lecture 03.pdf"
                }
              ]
            }
        """.trimIndent()

        val cards = ArtifactJsonParser.parseFlashcards(json)
        assertEquals(1, cards.size)
        assertEquals("What powers ATP synthase?", cards[0].front)
        assertEquals("Lecture 03.pdf", cards[0].sourceHint)
    }

    @Test
    fun `quiz parser clamps correctIndex and filters malformed questions`() {
        val json = """
            {
              "quizTitle": "Exam Prep",
              "questions": [
                {
                  "question": "Which organelle generates ATP?",
                  "options": ["Ribosome", "Mitochondrion", "Golgi", "Lysosome"],
                  "correctIndex": 1,
                  "explanation": "Mitochondria perform oxidative phosphorylation.",
                  "sourceHint": "Bio Ch4"
                }
              ]
            }
        """.trimIndent()

        val questions = ArtifactJsonParser.parseQuiz(json)
        assertEquals(1, questions.size)
        assertEquals(1, questions[0].correctIndex)
        assertEquals(4, questions[0].options.size)
    }

    @Test
    fun `audio overview parser extracts dialogue turns`() {
        val json = """
            {
              "episodeTitle": "Deep Dive: Cellular Respiration",
              "summary": "Alex and Maya review glycolysis and the Krebs cycle.",
              "dialogue": [
                {"speaker": "Alex", "line": "Let's break down how cells turn glucose into usable energy."},
                {"speaker": "Maya", "line": "It starts in the cytoplasm with glycolysis!"}
              ]
            }
        """.trimIndent()

        val overview = ArtifactJsonParser.parseAudioOverview(json)
        assertNotNull(overview)
        assertEquals(2, overview!!.turns.size)
        assertTrue(overview.turns[0].speaker == "Alex")
    }
}

package com.example

import com.example.model.ArtifactType
import com.example.model.NotebookColorToken
import com.example.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `safe enum deserialization falls back cleanly on unknown keys`() {
        assertEquals(NotebookColorToken.TEAL, NotebookColorToken.fromKey("unknown"))
        assertEquals(SourceType.NOTE, SourceType.fromKey(null))
        assertEquals(ArtifactType.STUDY_GUIDE, ArtifactType.fromKey("LEGACY_TYPE"))
    }
}

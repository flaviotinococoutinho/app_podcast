package com.example

import com.example.engine.TextChunker
import com.example.engine.TextOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun textOptimizer_removesMannerismsAndFormats() {
        val rawText = "Olá, tipo assim, vamos testar isso aqui, né? Temos 100% de certeza com IA."
        val cleaned = TextOptimizer.optimizeForAudio(rawText, "pt-BR")

        assertFalse(cleaned.contains("tipo assim"))
        assertFalse(cleaned.contains("né?"))
        assertTrue(cleaned.contains("por cento"))
        assertTrue(cleaned.contains("I.A."))
    }

    @Test
    fun textChunker_splitsLongTextCorrectly() {
        val longText = (1..600).joinToString(" ") { "palavra" }
        val chunks = TextChunker.splitIntoSessionParts(longText, "pt-BR")

        assertTrue(chunks.size > 1)
        assertEquals(1, chunks.first().partIndex)
        assertTrue(chunks.first().title.contains("Parte 1"))
    }
}

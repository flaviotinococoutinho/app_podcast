package com.example.engine

data class TextSessionChunk(
    val partIndex: Int,
    val title: String,
    val text: String,
    val wordCount: Int,
    val estimatedDurationSeconds: Int
)

object TextChunker {
    private const val WORDS_PER_MINUTE = 135
    private const val TARGET_WORDS_PER_CHUNK = 220
    private const val MAX_WORDS_PER_CHUNK = 320

    /**
     * Splits long text into sequential session parts.
     * Respects paragraph and sentence boundaries.
     */
    fun splitIntoSessionParts(text: String, languageCode: String): List<TextSessionChunk> {
        val clean = text.trim()
        if (clean.isEmpty()) return emptyList()

        val words = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size <= MAX_WORDS_PER_CHUNK) {
            val title = generatePartTitle(1, 1, languageCode)
            val duration = calculateDuration(words.size)
            return listOf(
                TextSessionChunk(
                    partIndex = 1,
                    title = title,
                    text = clean,
                    wordCount = words.size,
                    estimatedDurationSeconds = duration
                )
            )
        }

        // Split by paragraphs first
        val rawParagraphs = clean.split(Regex("""\n\s*\n+""")).map { it.trim() }.filter { it.isNotEmpty() }
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()
        var currentWordCount = 0

        for (para in rawParagraphs) {
            val paraWords = para.split(Regex("""\s+""")).count { it.isNotBlank() }

            if (paraWords > MAX_WORDS_PER_CHUNK) {
                // Large paragraph - split by sentences
                val sentences = para.split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }
                for (sentence in sentences) {
                    val sWords = sentence.split(Regex("""\s+""")).count { it.isNotBlank() }
                    if (currentWordCount + sWords > TARGET_WORDS_PER_CHUNK && currentWordCount > 0) {
                        chunks.add(currentChunk.toString().trim())
                        currentChunk.clear()
                        currentWordCount = 0
                    }
                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                    currentChunk.append(sentence)
                    currentWordCount += sWords
                }
            } else {
                if (currentWordCount + paraWords > TARGET_WORDS_PER_CHUNK && currentWordCount > 0) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                    currentWordCount = 0
                }
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(para)
                currentWordCount += paraWords
            }
        }

        if (currentChunk.isNotBlank()) {
            chunks.add(currentChunk.toString().trim())
        }

        val totalParts = chunks.size
        return chunks.mapIndexed { index, chunkText ->
            val pWords = chunkText.split(Regex("""\s+""")).count { it.isNotBlank() }
            val partNum = index + 1
            TextSessionChunk(
                partIndex = partNum,
                title = generatePartTitle(partNum, totalParts, languageCode),
                text = chunkText,
                wordCount = pWords,
                estimatedDurationSeconds = calculateDuration(pWords)
            )
        }
    }

    fun deriveEpisodeTitle(text: String, defaultTitle: String = "Podcast Episódio"): String {
        val firstLine = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return defaultTitle
        val clean = firstLine.replace(Regex("""^#+\s*"""), "").take(50).trim()
        return if (clean.length > 5) clean else defaultTitle
    }

    private fun calculateDuration(wordCount: Int): Int {
        val minutes = wordCount.toFloat() / WORDS_PER_MINUTE
        return (minutes * 60).toInt().coerceAtLeast(5)
    }

    private fun generatePartTitle(partIndex: Int, totalParts: Int, languageCode: String): String {
        val isPt = languageCode.lowercase().startsWith("pt")
        val isEs = languageCode.lowercase().startsWith("es")
        val isEn = languageCode.lowercase().startsWith("en")

        return when {
            totalParts == 1 -> {
                if (isPt) "Episódio Completo" else if (isEs) "Episodio Completo" else "Full Episode"
            }
            partIndex == 1 -> {
                if (isPt) "Parte 1: Abertura & Introdução"
                else if (isEs) "Parte 1: Apertura e Introducción"
                else "Part 1: Intro & Context"
            }
            partIndex == totalParts -> {
                if (isPt) "Parte $partIndex: Conclusão & Considerações"
                else if (isEs) "Parte $partIndex: Conclusión y Cierre"
                else "Part $partIndex: Conclusion & Outro"
            }
            partIndex == 2 && totalParts == 3 -> {
                if (isPt) "Parte 2: Desenvolvimento Central"
                else if (isEs) "Parte 2: Desarrollo Central"
                else "Part 2: Core Analysis"
            }
            else -> {
                if (isPt) "Parte $partIndex: Aprofundamento"
                else if (isEs) "Parte $partIndex: Profundización"
                else "Part $partIndex: Deep Dive"
            }
        }
    }
}

package com.example.engine

import android.util.Log

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val ttsLocale: java.util.Locale
)

object LanguageCatalog {
    val languages = listOf(
        SupportedLanguage("pt-BR", "Português", "🇧🇷", java.util.Locale("pt", "BR")),
        SupportedLanguage("en-US", "English", "🇺🇸", java.util.Locale.US),
        SupportedLanguage("es-ES", "Español", "🇪🇸", java.util.Locale("es", "ES")),
        SupportedLanguage("fr-FR", "Français", "🇫🇷", java.util.Locale.FRANCE),
        SupportedLanguage("de-DE", "Deutsch", "🇩🇪", java.util.Locale.GERMANY),
        SupportedLanguage("it-IT", "Italiano", "🇮🇹", java.util.Locale.ITALY)
    )

    fun getByCode(code: String): SupportedLanguage {
        return languages.find { it.code.equals(code, ignoreCase = true) } ?: languages.first()
    }
}

object TextOptimizer {
    private const val TAG = "TextOptimizer"

    /**
     * Cleans text to optimize it for human audio consumption:
     * - Removes oral ticks and crutches ("né", "tipo assim", "you know", etc.)
     * - Removes formatting syntax, URLs, citation tags ([1], [fonte])
     * - Formats acronyms and numbers for natural pronunciation
     * - Replaces bullet points with conversational transitions
     * - Adds natural breath cadence
     */
    fun optimizeForAudio(rawText: String, languageCode: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Strip raw URLs and citations
        text = text.replace(Regex("""https?://\S+"""), "no link de referência")
        text = text.replace(Regex("""\[\d+\]"""), "") // removes [1], [2]
        text = text.replace(Regex("""\[(?:fonte|source|ref)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\(ver\s+nota[^\)]*\)""", RegexOption.IGNORE_CASE), "")

        // 2. Clean markdown formatting
        text = text.replace(Regex("""\*\*(.*?)\*\*"""), "$1") // bold
        text = text.replace(Regex("""\*(.*?)\*"""), "$1")     // italics
        text = text.replace(Regex("""_{1,2}(.*?)_{1,2}"""), "$1")
        text = text.replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "") // headings
        text = text.replace(Regex("""```[\s\S]*?```"""), "código citado no documento.") // code blocks
        text = text.replace(Regex("""`([^`]+)`"""), "$1")

        // 3. Convert bullet points to spoken narration transitions
        text = convertBulletPointsToSpeech(text, languageCode)

        // 4. Expand symbols and currency for natural pronunciation
        text = expandSymbolsAndAcronyms(text, languageCode)

        // 5. Remove linguistic mannerisms and crutches based on language
        text = removeMannerisms(text, languageCode)

        // 6. Cadence and breath punctuation
        text = enhanceAudioCadence(text)

        // 7. Clean excessive whitespaces
        text = text.replace(Regex("""[ \t]+"""), " ")
        text = text.replace(Regex("""\n\s*\n\s*\n+"""), "\n\n")
        text = text.trim()

        return text
    }

    private fun removeMannerisms(text: String, languageCode: String): String {
        var result = text
        when (languageCode.lowercase().take(2)) {
            "pt" -> {
                val crutches = listOf(
                    Regex("""\b(tipo assim|tipo|tá ligado|tá entendendo|sabe como é|quer dizer|ou seja)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(né|viu)\??""", RegexOption.IGNORE_CASE),
                    Regex("""\b(aí então|então tipo|aí tipo)\b\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(enfim|basicamente|na verdade|como se diz)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(como eu ia dizendo|digamos assim)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(sabe\?|entende\?)\b""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
            "en" -> {
                val crutches = listOf(
                    Regex("""\b(you know|like|kind of|sort of|basically|actually|literally)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(I mean|you see|to be honest|at the end of the day)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(um|uh|er|ah)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(right\?|okay\?)\b""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
            "es" -> {
                val crutches = listOf(
                    Regex("""\b(o sea|tipo|es decir|en plan|¿sabes\?|¿me entiendes\?)\b[,]?\s*""", RegexOption.IGNORE_CASE),
                    Regex("""\b(bueno|viste|de hecho|la verdad)\b[,]?\s*""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
            "fr" -> {
                val crutches = listOf(
                    Regex("""\b(en fait|genre|du coup|voilà|tu vois|en gros)\b[,]?\s*""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
            "de" -> {
                val crutches = listOf(
                    Regex("""\b(quasi|sozusagen|halt|irgendwie|weißt du|eigentlich)\b[,]?\s*""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
            "it" -> {
                val crutches = listOf(
                    Regex("""\b(cioè|tipo|praticamente|diciamo|nel senso|sai)\b[,]?\s*""", RegexOption.IGNORE_CASE)
                )
                for (pattern in crutches) {
                    result = result.replace(pattern, " ")
                }
            }
        }
        return result
    }

    private fun convertBulletPointsToSpeech(text: String, languageCode: String): String {
        val lines = text.lines()
        val formatted = StringBuilder()
        var inList = false
        var itemIndex = 0

        val connectorsPt = listOf("Primeiro,", "Segundo,", "Além disso,", "Outro ponto importante é que", "Por fim,")
        val connectorsEn = listOf("First,", "Second,", "In addition,", "Another important point is that", "Finally,")
        val connectorsEs = listOf("En primer lugar,", "En segundo lugar,", "Además,", "Otro punto clave es que", "Por último,")

        val connectors = when (languageCode.lowercase().take(2)) {
            "en" -> connectorsEn
            "es" -> connectorsEs
            else -> connectorsPt
        }

        for (line in lines) {
            val trimmed = line.trim()
            val isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") ||
                    Regex("""^\d+\.\s+""").containsMatchIn(trimmed)

            if (isBullet) {
                val cleanContent = trimmed
                    .replaceFirst(Regex("""^[-*•]\s+"""), "")
                    .replaceFirst(Regex("""^\d+\.\s+"""), "")

                val connector = connectors.getOrElse(itemIndex) { "Além disso," }
                formatted.append(if (inList) " " else "\n").append("$connector $cleanContent")
                inList = true
                itemIndex++
            } else {
                if (inList) {
                    formatted.append("\n\n")
                    inList = false
                    itemIndex = 0
                }
                if (trimmed.isNotEmpty()) {
                    formatted.append(line).append("\n")
                }
            }
        }
        return formatted.toString().trim()
    }

    private fun expandSymbolsAndAcronyms(text: String, languageCode: String): String {
        var result = text
        when (languageCode.lowercase().take(2)) {
            "pt" -> {
                result = result.replace(Regex("""(\d+)\s*%"""), "$1 por cento")
                result = result.replace(Regex("""R\$\s*(\d+)"""), "$1 reais")
                result = result.replace(Regex("""\$\s*(\d+)"""), "$1 dólares")
                result = result.replace(Regex("""€\s*(\d+)"""), "$1 euros")
                result = result.replace(Regex("""\bex:\s*""", RegexOption.IGNORE_CASE), "por exemplo, ")
                result = result.replace(Regex("""\betc\.?""", RegexOption.IGNORE_CASE), "e outros.")
                result = result.replace(Regex("""\bIA\b"""), "I.A.")
                result = result.replace(Regex("""\bEUA\b"""), "Estados Unidos")
                result = result.replace(Regex("""\bvs\.?\b""", RegexOption.IGNORE_CASE), "versus")
            }
            "en" -> {
                result = result.replace(Regex("""(\d+)\s*%"""), "$1 percent")
                result = result.replace(Regex("""\$\s*(\d+)"""), "$1 dollars")
                result = result.replace(Regex("""€\s*(\d+)"""), "$1 euros")
                result = result.replace(Regex("""\be\.?g\.?,?\s*""", RegexOption.IGNORE_CASE), "for example, ")
                result = result.replace(Regex("""\bi\.?e\.?,?\s*""", RegexOption.IGNORE_CASE), "that is, ")
                result = result.replace(Regex("""\betc\.?""", RegexOption.IGNORE_CASE), "and so forth.")
                result = result.replace(Regex("""\bAI\b"""), "A.I.")
                result = result.replace(Regex("""\bUSA\b"""), "United States")
                result = result.replace(Regex("""\bvs\.?\b""", RegexOption.IGNORE_CASE), "versus")
            }
            "es" -> {
                result = result.replace(Regex("""(\d+)\s*%"""), "$1 por ciento")
                result = result.replace(Regex("""\$\s*(\d+)"""), "$1 dólares")
                result = result.replace(Regex("""€\s*(\d+)"""), "$1 euros")
                result = result.replace(Regex("""\bej:\s*""", RegexOption.IGNORE_CASE), "por ejemplo, ")
                result = result.replace(Regex("""\betc\.?""", RegexOption.IGNORE_CASE), "y demás.")
                result = result.replace(Regex("""\bIA\b"""), "I.A.")
                result = result.replace(Regex("""\bEE\.UU\.?\b"""), "Estados Unidos")
            }
        }
        return result
    }

    private fun enhanceAudioCadence(text: String): String {
        var result = text
        // Ensure sentences have clean spacing after punctuation
        result = result.replace(Regex("""([.!?])([A-Za-zÀ-ÿ])"""), "$1 $2")
        // Soften repeated punctuation
        result = result.replace(Regex("""!{2,}"""), "!")
        result = result.replace(Regex("""\?{2,}"""), "?")
        result = result.replace(Regex("""\.{4,}"""), "...")
        // Ensure proper breath pauses before long clauses
        result = result.replace(Regex("""\s*;\s*"""), ", ")
        return result
    }
}

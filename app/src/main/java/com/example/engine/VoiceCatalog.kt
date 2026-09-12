package com.example.engine

data class VoiceProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val isGeminiAI: Boolean,
    val defaultPitch: Float = 0.76f,
    val defaultSpeechRate: Float = 0.94f
)

object VoiceCatalog {
    val voices = listOf(
        VoiceProfile(
            id = "Fenrir",
            name = "Fenrir (Gemini AI)",
            subtitle = "Grave & Omnipresente",
            description = "Barítono imponente, autoritário e profundo, ideal para podcasts investigativos e narrações sérias.",
            isGeminiAI = true
        ),
        VoiceProfile(
            id = "Charon",
            name = "Charon (Gemini AI)",
            subtitle = "Cinematográfico & Baixo",
            description = "Tom baixo, cadência calma e presença envolvente estilo documentário.",
            isGeminiAI = true
        ),
        VoiceProfile(
            id = "Orus",
            name = "Orus (Gemini AI)",
            subtitle = "Aveludado & Ressonante",
            description = "Voz masculina aveludada com ressonância profunda, perfeita para ensaios e reflexões.",
            isGeminiAI = true
        ),
        VoiceProfile(
            id = "NativeDeep",
            name = "Barítono Studio (Local)",
            subtitle = "Síntese Grave Offline",
            description = "Sintetizador nativo do Android com pitch grave (0.76x) e cadência firme. Funciona offline sem limites de cota.",
            isGeminiAI = false,
            defaultPitch = 0.76f,
            defaultSpeechRate = 0.94f
        )
    )

    fun getById(id: String): VoiceProfile {
        return voices.find { it.id.equals(id, ignoreCase = true) } ?: voices.first()
    }
}

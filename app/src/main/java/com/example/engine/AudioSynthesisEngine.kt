package com.example.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

data class AudioSynthesisResult(
    val filePath: String,
    val durationMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class AudioSynthesisEngine(private val context: Context) {
    private val tag = "AudioSynthesisEngine"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var nativeTts: TextToSpeech? = null
    private val ttsInitDeferred = CompletableDeferred<Boolean>()

    init {
        nativeTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitDeferred.complete(true)
            } else {
                Log.e(tag, "Native TextToSpeech initialization failed: $status")
                ttsInitDeferred.complete(false)
            }
        }
    }

    private fun getAudioDirectory(): File {
        val dir = File(context.filesDir, "audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Synthesizes text into an audio file (.mp3 / .wav) with a deep masculine voice.
     * Uses Gemini TTS if selected and API key is valid; otherwise falls back gracefully to Native Deep Voice.
     */
    suspend fun synthesizeText(
        text: String,
        voiceProfile: VoiceProfile,
        languageCode: String,
        sessionId: Long,
        partIndex: Int
    ): AudioSynthesisResult = withContext(Dispatchers.IO) {
        val audioDir = getAudioDirectory()
        val outputFile = File(audioDir, "podcast_session_${sessionId}_part_${partIndex}.wav")

        if (voiceProfile.isGeminiAI) {
            val geminiKey = BuildConfig.GEMINI_API_KEY
            if (geminiKey.isNotBlank() && !geminiKey.contains("MY_GEMINI_API_KEY")) {
                val geminiResult = synthesizeWithGeminiTTS(text, voiceProfile.id, outputFile)
                if (geminiResult.isSuccess) {
                    return@withContext geminiResult
                }
                Log.w(tag, "Gemini TTS failed: ${geminiResult.errorMessage}. Falling back to Native Deep Voice.")
            } else {
                Log.i(tag, "Gemini API key not configured. Using high-fidelity Native Deep Voice synthesizer.")
            }
        }

        // Native Deep Voice synthesis fallback
        synthesizeWithNativeTTS(text, voiceProfile, languageCode, outputFile)
    }

    private suspend fun synthesizeWithGeminiTTS(
        text: String,
        voiceName: String,
        outputFile: File
    ): AudioSynthesisResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"

            // Construct Gemini TTS JSON payload
            val promptText = "Narrate in a deep, omnipresent, realistic masculine podcast voice: $text"
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                return@withContext AudioSynthesisResult(
                    filePath = "",
                    durationMs = 0,
                    isSuccess = false,
                    errorMessage = "HTTP ${response.code}: $errorBody"
                )
            }

            val responseString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseString)

            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var audioDataB64: String? = null
            var mimeType = "audio/wav"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    if (inlineData != null) {
                        audioDataB64 = inlineData.optString("data")
                        mimeType = inlineData.optString("mimeType", "audio/wav")
                        break
                    }
                }
            }

            if (audioDataB64.isNullOrEmpty()) {
                return@withContext AudioSynthesisResult(
                    filePath = "",
                    durationMs = 0,
                    isSuccess = false,
                    errorMessage = "Nenhum dado de áudio retornado pelo modelo Gemini."
                )
            }

            val rawAudioBytes = Base64.decode(audioDataB64, Base64.DEFAULT)

            // If raw PCM, add RIFF WAV header
            val finalBytes = if (mimeType.contains("pcm", ignoreCase = true) || !hasWavHeader(rawAudioBytes)) {
                createWavFileBytes(rawAudioBytes, sampleRate = 24000, channels = 1, bitsPerSample = 16)
            } else {
                rawAudioBytes
            }

            FileOutputStream(outputFile).use { it.write(finalBytes) }

            val durationMs = computeAudioDuration(outputFile)
            AudioSynthesisResult(
                filePath = outputFile.absolutePath,
                durationMs = durationMs,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception during Gemini TTS synthesis", e)
            AudioSynthesisResult(
                filePath = "",
                durationMs = 0,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: e.message
            )
        }
    }

    private suspend fun synthesizeWithNativeTTS(
        text: String,
        voiceProfile: VoiceProfile,
        languageCode: String,
        outputFile: File
    ): AudioSynthesisResult = withContext(Dispatchers.IO) {
        val isReady = ttsInitDeferred.await()
        val tts = nativeTts
        if (!isReady || tts == null) {
            return@withContext AudioSynthesisResult(
                filePath = "",
                durationMs = 0,
                isSuccess = false,
                errorMessage = "Motor de síntese de voz indisponível."
            )
        }

        val supportedLang = LanguageCatalog.getByCode(languageCode)
        tts.language = supportedLang.ttsLocale

        // Deep masculine resonance tuning
        tts.setPitch(voiceProfile.defaultPitch)
        tts.setSpeechRate(voiceProfile.defaultSpeechRate)

        val completionDeferred = CompletableDeferred<Boolean>()
        val utteranceId = "utterance_${System.currentTimeMillis()}_${outputFile.nameWithoutExtension}"

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    completionDeferred.complete(true)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    completionDeferred.complete(false)
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    Log.e(tag, "TTS Error code: $errorCode for utterance: $id")
                    completionDeferred.complete(false)
                }
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        val status = tts.synthesizeToFile(text, params, outputFile, utteranceId)
        if (status != TextToSpeech.SUCCESS) {
            return@withContext AudioSynthesisResult(
                filePath = "",
                durationMs = 0,
                isSuccess = false,
                errorMessage = "Falha ao iniciar sintetização de arquivo de áudio."
            )
        }

        val completed = completionDeferred.await()
        if (completed && outputFile.exists() && outputFile.length() > 0) {
            val duration = computeAudioDuration(outputFile)
            AudioSynthesisResult(
                filePath = outputFile.absolutePath,
                durationMs = duration,
                isSuccess = true
            )
        } else {
            AudioSynthesisResult(
                filePath = "",
                durationMs = 0,
                isSuccess = false,
                errorMessage = "Arquivo de áudio não pôde ser gravado pelo sintetizador."
            )
        }
    }

    /**
     * Stitches multiple session part WAV files into a single master podcast episode file.
     */
    suspend fun mergeSessionParts(partFiles: List<File>, sessionId: Long): File? = withContext(Dispatchers.IO) {
        val validFiles = partFiles.filter { it.exists() && it.length() > 44 }
        if (validFiles.isEmpty()) return@withContext null
        if (validFiles.size == 1) return@withContext validFiles.first()

        val masterFile = File(getAudioDirectory(), "podcast_session_${sessionId}_full_master.wav")

        try {
            // Merge raw PCM bytes from WAV files (skipping 44-byte headers)
            val pcmOutStream = java.io.ByteArrayOutputStream()
            for (file in validFiles) {
                val bytes = file.readBytes()
                val offset = if (hasWavHeader(bytes)) 44 else 0
                val length = (bytes.size - offset).coerceAtLeast(0)
                if (length > 0) {
                    pcmOutStream.write(bytes, offset, length)
                    // Insert 600ms silence pause between session parts
                    val silenceBytes = ByteArray(24000 * 2 * 6 / 10) // 0.6s at 24kHz 16-bit mono
                    pcmOutStream.write(silenceBytes)
                }
            }

            val fullPcm = pcmOutStream.toByteArray()
            val masterWav = createWavFileBytes(fullPcm, sampleRate = 24000, channels = 1, bitsPerSample = 16)
            FileOutputStream(masterFile).use { it.write(masterWav) }
            masterFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to merge session parts", e)
            validFiles.firstOrNull()
        }
    }

    private fun hasWavHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'A'.code.toByte() &&
                bytes[10] == 'V'.code.toByte() &&
                bytes[11] == 'E'.code.toByte()
    }

    private fun createWavFileBytes(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        // RIFF chunk
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt sub-chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // SubChunk1Size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat (1 = PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // data sub-chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        val dataLen = pcmData.size
        header[40] = (dataLen and 0xff).toByte()
        header[41] = ((dataLen shr 8) and 0xff).toByte()
        header[42] = ((dataLen shr 16) and 0xff).toByte()
        header[43] = ((dataLen shr 24) and 0xff).toByte()

        return header + pcmData
    }

    private fun computeAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            // Fallback estimation based on WAV file size
            val pcmBytes = (file.length() - 44).coerceAtLeast(0)
            val bytesPerSecond = 24000 * 2 // 24kHz 16-bit mono = 48,000 bytes/sec
            if (bytesPerSecond > 0) (pcmBytes * 1000L / bytesPerSecond) else 0L
        }
    }

    fun release() {
        try {
            nativeTts?.stop()
            nativeTts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}

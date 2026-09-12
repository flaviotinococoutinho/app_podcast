package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PodCraftDatabase
import com.example.data.model.PodcastPart
import com.example.data.model.PodcastSession
import com.example.data.repository.PodcastRepository
import com.example.engine.AudioSynthesisEngine
import com.example.engine.CloudExportManager
import com.example.engine.LanguageCatalog
import com.example.engine.PlaybackState
import com.example.engine.PodcastAudioPlayer
import com.example.engine.SupportedLanguage
import com.example.engine.TextChunker
import com.example.engine.TextOptimizer
import com.example.engine.TextSessionChunk
import com.example.engine.VoiceCatalog
import com.example.engine.VoiceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PodCraftUiState(
    val inputText: String = "",
    val selectedLanguage: SupportedLanguage = LanguageCatalog.languages.first(),
    val selectedVoice: VoiceProfile = VoiceCatalog.voices.first(),
    val removeMannerismsEnabled: Boolean = true,
    val previewChunks: List<TextSessionChunk> = emptyList(),
    val isOptimizingText: Boolean = false,
    val isGeneratingAudio: Boolean = false,
    val generatingPartProgress: String = "",
    val activeSession: PodcastSession? = null,
    val activeSessionParts: List<PodcastPart> = emptyList(),
    val showDiffDialog: Boolean = false,
    val showExportSheet: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val exportTargetFilePath: String? = null,
    val exportTargetTitle: String = "",
    val totalEstimatedDurationText: String = "",
    val cloudStatus: String = "Google Drive • Pronto"
)

class PodCraftViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PodCraftDatabase.getDatabase(application)
    private val repository = PodcastRepository(database.podcastDao())
    val audioEngine = AudioSynthesisEngine(application)
    val audioPlayer = PodcastAudioPlayer(application, viewModelScope)

    val savedSessions: StateFlow<List<PodcastSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState

    private val _uiState = MutableStateFlow(PodCraftUiState())
    val uiState: StateFlow<PodCraftUiState> = _uiState.asStateFlow()

    init {
        // Load initial sample podcast script so the user sees a rich ready-to-run example immediately
        loadSampleScript()
    }

    fun updateInputText(newText: String) {
        val chunks = TextChunker.splitIntoSessionParts(newText, _uiState.value.selectedLanguage.code)
        val totalSec = chunks.sumOf { it.estimatedDurationSeconds }
        val durationFormatted = formatSeconds(totalSec)

        _uiState.value = _uiState.value.copy(
            inputText = newText,
            previewChunks = chunks,
            totalEstimatedDurationText = durationFormatted
        )
    }

    fun selectLanguage(language: SupportedLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        updateInputText(_uiState.value.inputText)
    }

    fun selectVoice(voice: VoiceProfile) {
        _uiState.value = _uiState.value.copy(selectedVoice = voice)
    }

    fun toggleMannerismsRemoval() {
        _uiState.value = _uiState.value.copy(
            removeMannerismsEnabled = !_uiState.value.removeMannerismsEnabled
        )
    }

    fun setShowDiffDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDiffDialog = show)
    }

    fun setShowExportSheet(show: Boolean, filePath: String? = null, title: String = "") {
        _uiState.value = _uiState.value.copy(
            showExportSheet = show,
            exportTargetFilePath = filePath ?: _uiState.value.activeSession?.audioFilePath,
            exportTargetTitle = title.ifEmpty { _uiState.value.activeSession?.title ?: "Podcast PodCraft" }
        )
    }

    fun setShowHistoryDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showHistoryDialog = show)
    }

    fun loadSampleScript() {
        val sample = """
            # O Futuro da Inteligência Artificial nos Podcasts

            Olá a todos os ouvintes! Hoje, tipo assim, vamos mergulhar em uma revolução silenciosa: a geração de conteúdo em áudio com síntese neural de voz. 

            Né, antigamente, você precisava de um estúdio profissional, isolamento acústico caro de R$ 5000 e microfones caríssimos para produzir um episódio com voz imponente e presença marcante. Basicamente, os custos eram proibitivos para criadores independentes.

            No entanto, com os avanços recentes da IA [1], modelos profundos conseguem modular respiração, cadência e entonação masculina grave de forma impressionante. Quer dizer, o som agora tem calor e peso.

            - Eliminação automática de vícios de linguagem e pausas desconexas
            - Quebra inteligente de textos longos em sessões temáticas
            - Integração imediata com nuvem e exportação de áudio em segundos

            Portanto, preparem seus fones de ouvido. O que antes levava semanas de gravação e edição, agora acontece em questão de segundos com qualidade broadcast.
        """.trimIndent()
        updateInputText(sample)
    }

    fun clearInput() {
        updateInputText("")
    }

    fun generatePodcast() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingAudio = true,
                isOptimizingText = true,
                generatingPartProgress = "Otimizando texto e removendo maneirismos..."
            )

            val langCode = _uiState.value.selectedLanguage.code
            val optimized = if (_uiState.value.removeMannerismsEnabled) {
                TextOptimizer.optimizeForAudio(text, langCode)
            } else {
                text
            }

            val sessionChunks = TextChunker.splitIntoSessionParts(optimized, langCode)
            val title = TextChunker.deriveEpisodeTitle(optimized)

            val session = PodcastSession(
                title = title,
                language = langCode,
                voiceName = _uiState.value.selectedVoice.id,
                originalText = text,
                optimizedText = optimized,
                totalParts = sessionChunks.size,
                status = "PROCESSING"
            )

            val parts = sessionChunks.map { chunk ->
                PodcastPart(
                    sessionId = 0,
                    partIndex = chunk.partIndex,
                    title = chunk.title,
                    originalChunk = chunk.text,
                    optimizedChunk = chunk.text,
                    status = "PENDING"
                )
            }

            val sessionId = repository.createSession(session, parts)
            val savedSession = repository.getSessionById(sessionId) ?: session.copy(id = sessionId)
            var currentParts = repository.getPartsList(sessionId)

            _uiState.value = _uiState.value.copy(
                activeSession = savedSession,
                activeSessionParts = currentParts,
                isOptimizingText = false
            )

            val generatedFiles = mutableListOf<File>()
            var totalDurationMs = 0L

            for ((index, part) in currentParts.withIndex()) {
                val partNum = index + 1
                _uiState.value = _uiState.value.copy(
                    generatingPartProgress = "Sintetizando Parte $partNum de ${currentParts.size} (${_uiState.value.selectedVoice.name})..."
                )

                val updatedPart = part.copy(status = "GENERATING")
                repository.updatePart(updatedPart)
                currentParts = repository.getPartsList(sessionId)
                _uiState.value = _uiState.value.copy(activeSessionParts = currentParts)

                val result = audioEngine.synthesizeText(
                    text = part.optimizedChunk,
                    voiceProfile = _uiState.value.selectedVoice,
                    languageCode = langCode,
                    sessionId = sessionId,
                    partIndex = part.partIndex
                )

                if (result.isSuccess) {
                    val completedPart = part.copy(
                        status = "COMPLETED",
                        audioFilePath = result.filePath,
                        durationMs = result.durationMs
                    )
                    repository.updatePart(completedPart)
                    generatedFiles.add(File(result.filePath))
                    totalDurationMs += result.durationMs
                } else {
                    val errorPart = part.copy(
                        status = "ERROR",
                        errorMessage = result.errorMessage
                    )
                    repository.updatePart(errorPart)
                }

                currentParts = repository.getPartsList(sessionId)
                _uiState.value = _uiState.value.copy(activeSessionParts = currentParts)
            }

            // Merge full episode audio if multi-part
            val masterAudioFile = if (generatedFiles.isNotEmpty()) {
                audioEngine.mergeSessionParts(generatedFiles, sessionId)
            } else null

            val finalSession = savedSession.copy(
                audioFilePath = masterAudioFile?.absolutePath ?: generatedFiles.firstOrNull()?.absolutePath,
                totalDurationMs = totalDurationMs,
                completedParts = generatedFiles.size,
                status = "COMPLETED"
            )
            repository.updateSession(finalSession)

            _uiState.value = _uiState.value.copy(
                activeSession = finalSession,
                isGeneratingAudio = false,
                generatingPartProgress = "Concluído!"
            )

            // Start playing the first part or master episode
            finalSession.audioFilePath?.let { path ->
                audioPlayer.playFile(path, finalSession.title)
            }
        }
    }

    fun playPart(part: PodcastPart) {
        part.audioFilePath?.let { path ->
            audioPlayer.playFile(path, part.title)
        }
    }

    fun playFullSession(session: PodcastSession) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activeSession = session)
            val parts = repository.getPartsList(session.id)
            _uiState.value = _uiState.value.copy(activeSessionParts = parts)

            session.audioFilePath?.let { path ->
                audioPlayer.playFile(path, session.title)
            }
        }
    }

    fun deleteSession(session: PodcastSession) {
        viewModelScope.launch {
            repository.deleteSession(session.id)
            if (_uiState.value.activeSession?.id == session.id) {
                audioPlayer.stop()
                _uiState.value = _uiState.value.copy(activeSession = null, activeSessionParts = emptyList())
            }
        }
    }

    fun exportToShareSheet(context: Context) {
        val targetPath = _uiState.value.exportTargetFilePath ?: _uiState.value.activeSession?.audioFilePath
        val title = _uiState.value.exportTargetTitle.ifEmpty { _uiState.value.activeSession?.title ?: "Podcast" }

        if (targetPath != null) {
            CloudExportManager.shareAudio(context, targetPath, title)
        } else {
            Toast.makeText(context, "Nenhum arquivo de áudio disponível para exportar.", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToLocalMedia(context: Context) {
        val targetPath = _uiState.value.exportTargetFilePath ?: _uiState.value.activeSession?.audioFilePath
        val title = _uiState.value.exportTargetTitle.ifEmpty { _uiState.value.activeSession?.title ?: "Podcast" }

        if (targetPath != null) {
            viewModelScope.launch {
                val result = CloudExportManager.exportToPublicMusic(context, targetPath, title)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        Toast.makeText(context, "✅ Salvo com sucesso: $result", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Falha ao gravar na pasta de Podcasts.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun saveToCloudUri(context: Context, targetUri: Uri) {
        val targetPath = _uiState.value.exportTargetFilePath ?: _uiState.value.activeSession?.audioFilePath
        if (targetPath != null) {
            viewModelScope.launch {
                val success = CloudExportManager.writeAudioToUri(context, targetPath, targetUri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "☁️ Exportado com sucesso para a Nuvem!", Toast.LENGTH_LONG).show()
                        _uiState.value = _uiState.value.copy(cloudStatus = "Google Drive • Sincronizado")
                    } else {
                        Toast.makeText(context, "Erro ao gravar arquivo na nuvem.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        audioPlayer.release()
    }
}

package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CloudExportManager
import com.example.ui.components.BottomAudioPlayerBar
import com.example.ui.components.ExportCloudSheet
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.components.SavedSessionsDialog
import com.example.ui.components.SessionPartCard
import com.example.ui.components.TextDiffDialog
import com.example.ui.components.VoiceSelectorCard
import com.example.ui.theme.AmberPodcast
import com.example.ui.theme.AmberPodcastVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodCraftMainScreen(
    viewModel: PodCraftViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val savedSessions by viewModel.savedSessions.collectAsState()

    // SAF Document Creator launcher for Cloud Drive export
    val cloudSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri ->
        if (uri != null) {
            viewModel.saveToCloudUri(context, uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AmberPodcast.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Podcasts,
                                contentDescription = null,
                                tint = AmberPodcast,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PodCraft AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Voz Grave • Estúdio Podcast",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Cloud Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nuvem Ativa",
                                fontSize = 11.sp,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    // Saved History Icon
                    IconButton(onClick = { viewModel.setShowHistoryDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Histórico de Podcasts",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioBackground
                )
            )
        },
        bottomBar = {
            BottomAudioPlayerBar(
                playbackState = playbackState,
                onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                onSeek = { viewModel.audioPlayer.seekTo(it) },
                onReplay10 = { viewModel.audioPlayer.replay10() },
                onForward10 = { viewModel.audioPlayer.forward10() },
                onCycleSpeed = { viewModel.audioPlayer.cycleSpeed() },
                onExport = { viewModel.setShowExportSheet(true) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Language Selector Chips
            item {
                Column {
                    Text(
                        text = "Idioma do Áudio",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LanguageSelectorRow(
                        selectedLanguageCode = uiState.selectedLanguage.code,
                        onLanguageSelected = { viewModel.selectLanguage(it) }
                    )
                }
            }

            // 2. Text Input Studio Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Texto para Narração",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Paste from clipboard button
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipData = clipboard?.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                                            if (text.isNotBlank()) {
                                                viewModel.updateInputText(text)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Colar", color = ElectricCyan, fontSize = 12.sp)
                                }

                                if (uiState.inputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearInput() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpar",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            placeholder = {
                                Text(
                                    text = "Cole aqui o texto, artigo ou roteiro... O app otimizará a narrativa e dividirá em partes automaticamente se for longo.",
                                    color = TextTertiary,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPodcast,
                                unfocusedBorderColor = StudioSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = StudioSurfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = StudioSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats bar: word count & estimated duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val wordCount = uiState.inputText.split(Regex("""\s+""")).count { it.isNotBlank() }
                            Text(
                                text = "$wordCount palavras • ${uiState.inputText.length} caracteres",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            if (uiState.totalEstimatedDurationText.isNotEmpty()) {
                                Text(
                                    text = "Duração est.: ~${uiState.totalEstimatedDurationText}",
                                    fontSize = 11.sp,
                                    color = AmberPodcast,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mannerism removal switch
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant.copy(alpha = 0.6f)),
                            border = BorderStroke(0.8.dp, StudioSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberPodcast,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Otimizar Texto para Áudio",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Remove maneirismos (\"né\", \"tipo assim\", vícios) e ajusta pausas",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.removeMannerismsEnabled,
                                    onCheckedChange = { viewModel.toggleMannerismsRemoval() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AmberPodcast,
                                        checkedTrackColor = AmberPodcast.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextTertiary,
                                        uncheckedTrackColor = StudioSurfaceBorder
                                    )
                                )
                            }
                        }

                        // Sample text button if input is empty
                        if (uiState.inputText.isBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.loadSampleScript() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StudioSurfaceBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Carregar Exemplo de Artigo para Podcast",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Voice Selection Card
            item {
                VoiceSelectorCard(
                    selectedVoiceId = uiState.selectedVoice.id,
                    onVoiceSelected = { viewModel.selectVoice(it) }
                )
            }

            // 4. Long Text Multi-Part Session Info
            if (uiState.previewChunks.size > 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberPodcast.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, AmberPodcast.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AmberPodcast.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = AmberPodcast,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Texto Longo: Quebrado em ${uiState.previewChunks.size} Sessões",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AmberPodcast
                                )
                                Text(
                                    text = "O processo foi dividido em partes por sessão para máxima qualidade acústica e entendimento.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Generate Primary Action Button
            item {
                Button(
                    onClick = { viewModel.generatePodcast() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isGeneratingAudio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPodcast,
                        disabledContainerColor = StudioSurfaceVariant
                    )
                ) {
                    if (uiState.isGeneratingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF090D16)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.generatingPartProgress,
                            color = Color(0xFF090D16),
                            style = MaterialTheme.typography.titleSmall
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val partsCount = uiState.previewChunks.size
                        val actionLabel = if (partsCount > 1) "Gerar Podcast ($partsCount Partes)" else "Gerar Áudio Podcast"
                        Text(
                            text = actionLabel,
                            color = Color(0xFF090D16),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // 6. Active Session and Parts Breakdown
            if (uiState.activeSession != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = uiState.activeSession?.title ?: "Episódio Ativo",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sessões processadas: ${uiState.activeSessionParts.size} partes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.setShowDiffDialog(true) }) {
                                Text("Ver Texto Limpo", color = ElectricCyan, fontSize = 12.sp)
                            }

                            IconButton(onClick = { viewModel.setShowExportSheet(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Exportar Tudo",
                                    tint = AmberPodcast
                                )
                            }
                        }
                    }
                }

                items(uiState.activeSessionParts, key = { it.id }) { part ->
                    val isPartActiveInPlayer = playbackState.currentFilePath == part.audioFilePath
                    SessionPartCard(
                        part = part,
                        isCurrentlyPlaying = playbackState.isPlaying,
                        isPlayingThisPart = isPartActiveInPlayer,
                        onPlayToggle = {
                            if (isPartActiveInPlayer) {
                                viewModel.audioPlayer.togglePlayPause()
                            } else {
                                viewModel.playPart(part)
                            }
                        },
                        onSharePart = {
                            part.audioFilePath?.let { path ->
                                viewModel.setShowExportSheet(true, filePath = path, title = part.title)
                            }
                        }
                    )
                }
            }

            // Bottom spacer so content is never obscured by the bottom player
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Dialogs & Sheets
    if (uiState.showDiffDialog) {
        val session = uiState.activeSession
        TextDiffDialog(
            originalText = session?.originalText ?: uiState.inputText,
            optimizedText = session?.optimizedText ?: "",
            onDismiss = { viewModel.setShowDiffDialog(false) }
        )
    }

    if (uiState.showExportSheet) {
        ExportCloudSheet(
            episodeTitle = uiState.exportTargetTitle,
            totalDurationText = uiState.totalEstimatedDurationText,
            onShareAudio = { viewModel.exportToShareSheet(context) },
            onSaveToCloudDrive = {
                val fileName = "PodCraft_${uiState.exportTargetTitle.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")}.wav"
                cloudSaveLauncher.launch(fileName)
            },
            onSaveToLocalMusic = { viewModel.saveToLocalMedia(context) },
            onDismiss = { viewModel.setShowExportSheet(false) }
        )
    }

    if (uiState.showHistoryDialog) {
        SavedSessionsDialog(
            sessions = savedSessions,
            onSelectSession = { viewModel.playFullSession(it) },
            onDeleteSession = { viewModel.deleteSession(it) },
            onDismiss = { viewModel.setShowHistoryDialog(false) }
        )
    }
}

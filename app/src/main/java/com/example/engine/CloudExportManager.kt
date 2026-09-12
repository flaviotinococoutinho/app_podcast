package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object CloudExportManager {
    private const val TAG = "CloudExportManager"

    /**
     * Fast export: Opens Android System Share Sheet to quickly send audio
     * directly to WhatsApp, Google Drive, Telegram, Cloud Storage, or Email.
     */
    fun shareAudio(context: Context, filePath: String, episodeTitle: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Arquivo de áudio não encontrado para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, episodeTitle)
                putExtra(Intent.EXTRA_TEXT, "🎙️ Podcast gerado com PodCraft AI: $episodeTitle")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Exportar Podcast Rapidamente para:").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing audio file", e)
            Toast.makeText(context, "Erro ao compartilhar áudio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Creates an Intent to save audio file to Google Drive or Cloud Provider
     * using the Android Storage Access Framework (SAF).
     */
    fun createSaveToCloudIntent(fileName: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }

    /**
     * Copies synthesized audio file bytes into the SAF Cloud Uri picked by the user.
     */
    suspend fun writeAudioToUri(context: Context, sourceFilePath: String, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourceFilePath)
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write audio to cloud uri: $targetUri", e)
            false
        }
    }

    /**
     * Fast local export: Saves directly to Android device's Public Podcasts / Music directory.
     */
    suspend fun exportToPublicMusic(context: Context, sourceFilePath: String, displayTitle: String): String? = withContext(Dispatchers.IO) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) return@withContext null

        try {
            val fileName = "PodCraft_${displayTitle.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")}.wav"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_PODCASTS + "/PodCraft")
                    put(MediaStore.Audio.Media.TITLE, displayTitle)
                    put(MediaStore.Audio.Media.ARTIST, "PodCraft Deep Voice")
                    put(MediaStore.Audio.Media.ALBUM, "PodCraft Studio Episodes")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    return@withContext "Salvo na pasta Podcasts/PodCraft"
                }
            } else {
                val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS), "PodCraft")
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext destFile.absolutePath
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to public music", e)
            null
        }
    }
}

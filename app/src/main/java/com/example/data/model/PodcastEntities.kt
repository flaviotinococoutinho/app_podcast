package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcast_sessions")
data class PodcastSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val language: String,
    val voiceName: String,
    val originalText: String,
    val optimizedText: String,
    val totalParts: Int,
    val completedParts: Int = 0,
    val audioFilePath: String? = null,
    val totalDurationMs: Long = 0,
    val status: String = "READY", // READY, PROCESSING, COMPLETED, ERROR
    val cloudSyncStatus: String = "LOCAL", // LOCAL, SYNCED, PENDING
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "podcast_parts")
data class PodcastPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val partIndex: Int,
    val title: String,
    val originalChunk: String,
    val optimizedChunk: String,
    val audioFilePath: String? = null,
    val durationMs: Long = 0,
    val status: String = "PENDING", // PENDING, GENERATING, COMPLETED, ERROR
    val errorMessage: String? = null
)

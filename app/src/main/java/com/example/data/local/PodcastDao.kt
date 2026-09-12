package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PodcastPart
import com.example.data.model.PodcastSession
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<PodcastSession>>

    @Query("SELECT * FROM podcast_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): PodcastSession?

    @Query("SELECT * FROM podcast_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionById(sessionId: Long): Flow<PodcastSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PodcastSession): Long

    @Update
    suspend fun updateSession(session: PodcastSession)

    @Query("DELETE FROM podcast_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM podcast_parts WHERE sessionId = :sessionId ORDER BY partIndex ASC")
    fun getPartsForSession(sessionId: Long): Flow<List<PodcastPart>>

    @Query("SELECT * FROM podcast_parts WHERE sessionId = :sessionId ORDER BY partIndex ASC")
    suspend fun getPartsListForSession(sessionId: Long): List<PodcastPart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<PodcastPart>)

    @Update
    suspend fun updatePart(part: PodcastPart)

    @Query("DELETE FROM podcast_parts WHERE sessionId = :sessionId")
    suspend fun deletePartsBySessionId(sessionId: Long)
}

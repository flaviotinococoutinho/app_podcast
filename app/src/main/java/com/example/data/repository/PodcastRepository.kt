package com.example.data.repository

import com.example.data.local.PodcastDao
import com.example.data.model.PodcastPart
import com.example.data.model.PodcastSession
import kotlinx.coroutines.flow.Flow

class PodcastRepository(private val podcastDao: PodcastDao) {
    val allSessions: Flow<List<PodcastSession>> = podcastDao.getAllSessions()

    fun getPartsForSession(sessionId: Long): Flow<List<PodcastPart>> =
        podcastDao.getPartsForSession(sessionId)

    suspend fun getPartsList(sessionId: Long): List<PodcastPart> =
        podcastDao.getPartsListForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): PodcastSession? =
        podcastDao.getSessionById(sessionId)

    suspend fun createSession(session: PodcastSession, parts: List<PodcastPart>): Long {
        val sessionId = podcastDao.insertSession(session)
        val updatedParts = parts.map { it.copy(sessionId = sessionId) }
        podcastDao.insertParts(updatedParts)
        return sessionId
    }

    suspend fun updateSession(session: PodcastSession) {
        podcastDao.updateSession(session)
    }

    suspend fun updatePart(part: PodcastPart) {
        podcastDao.updatePart(part)
    }

    suspend fun deleteSession(sessionId: Long) {
        podcastDao.deletePartsBySessionId(sessionId)
        podcastDao.deleteSessionById(sessionId)
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PodcastPart
import com.example.data.model.PodcastSession

@Database(entities = [PodcastSession::class, PodcastPart::class], version = 1, exportSchema = false)
abstract class PodCraftDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        @Volatile
        private var INSTANCE: PodCraftDatabase? = null

        fun getDatabase(context: Context): PodCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PodCraftDatabase::class.java,
                    "podcraft_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

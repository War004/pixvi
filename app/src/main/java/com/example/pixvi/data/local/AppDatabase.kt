package com.example.pixvi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pixvi.data.local.NovelHistory.NovelHistoryDao
import com.example.pixvi.data.local.NovelHistory.NovelHistoryEntry

//notification database and related elements are in it's viewmodel

@Database(entities = [NovelHistoryEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelHistoryDao(): NovelHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "novel_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
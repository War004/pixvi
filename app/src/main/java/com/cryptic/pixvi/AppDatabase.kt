package com.cryptic.pixvi


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cryptic.pixvi.database.notification.NotificationDao
import com.cryptic.pixvi.database.notification.NotificationEntity

@Database(entities = [NotificationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If instance exists, return it
            return INSTANCE ?: synchronized(this) {
                // If not, create it
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixvi_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
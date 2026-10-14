package com.cryptic.piyek.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserDao
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelBrowsingEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelFinishedEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelHistoryDao

@Database(
    entities = [
        OAuthUserEntity::class,
        NovelBrowsingEntity::class,
        NovelFinishedEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun oAuthUserDao(): OAuthUserDao
    abstract fun novelHistoryDao(): NovelHistoryDao
}
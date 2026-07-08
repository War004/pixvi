package com.cryptic.piyek.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserDao
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity

@Database(entities = [OAuthUserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun oAuthUserDao(): OAuthUserDao
}
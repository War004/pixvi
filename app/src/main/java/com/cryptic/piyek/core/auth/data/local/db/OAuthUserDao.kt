package com.cryptic.piyek.core.auth.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OAuthUserDao {

    // 1. Create/Save the entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: OAuthUserEntity)

    // 2. Get detail by ID
    @Query("SELECT * FROM user_info WHERE id = :id")
    suspend fun getUserById(id: Long): OAuthUserEntity?

    // 3. Change email by ID
    @Query("UPDATE user_info SET mailAddress = :newEmail WHERE id = :id")
    suspend fun updateEmail(id: Long, newEmail: String)

    // 4. Change name by ID
    @Query("UPDATE user_info SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    // 5. Change profile picture by ID
    @Query("UPDATE user_info SET bestProfilePicUrl = :newPicUrl WHERE id = :id")
    suspend fun updateProfilePic(id: Long, newPicUrl: String)

    // 6. Delete the entire entry by ID (Token is naturally deleted with it)
    @Query("DELETE FROM user_info WHERE id = :id")
    suspend fun deleteUserById(id: Long)
}
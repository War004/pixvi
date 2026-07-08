package com.cryptic.piyek.core.auth.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptic.piyek.core.auth.domain.model.OauthUser

/*
* We are only saving the values that are not going to change.
* - Entries like is_premium, x_restrcit, is_mail_authorized can be checked at the startup of the app as they can change frequently
* - Profile picture url is saved for caching even though it can be changed easily
* Only Best profile url is saved to make db entries simple, according to the specs the image is 170*170 which could be loaded fast enough
* */
@Entity("user_info")
data class OAuthUserEntity(
    @PrimaryKey(autoGenerate = false)

    val id: Long,
    val name: String,
    val account: String,
    val mailAddress: String,
    val bestProfilePicUrl: String,
    val encryptedRefreshToken: String,
)

fun OAuthUserEntity.toDomainModel(): OauthUser {
    return OauthUser(
        id = this.id,
        name = this.name,
        account = this.account,
        mailAddress = this.mailAddress,
        bestProfilePicUrl = this.bestProfilePicUrl,
        accessToken = null,
        encryptedRefreshToken = this.encryptedRefreshToken,
    )
}
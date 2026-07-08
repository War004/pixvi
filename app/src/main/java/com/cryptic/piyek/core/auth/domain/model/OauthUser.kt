package com.cryptic.piyek.core.auth.domain.model

import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity

data class OauthUser(
    val id: Long,
    val name: String,
    val account: String,
    val mailAddress: String,
    val bestProfilePicUrl: String,
    val accessToken: String?,
    val encryptedRefreshToken: String
)

fun OauthUser.toEntity(encryptedRefreshToken: String): OAuthUserEntity {
    return OAuthUserEntity(
        id = this.id,
        name = this.name,
        account = this.account,
        mailAddress = this.mailAddress,
        bestProfilePicUrl = this.bestProfilePicUrl,
        encryptedRefreshToken = encryptedRefreshToken,
    )
}
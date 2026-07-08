package com.cryptic.piyek.core.auth.data.remote

import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity
import com.cryptic.piyek.core.auth.domain.model.OauthUser as Independent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. The main wrapper matching the root JSON response
@Serializable
data class OAuthUserResponse(
    @SerialName("response") val response: OAuthUser
)

// 2. Holds token data and the nested user object
@Serializable
data class OAuthUser(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user") val user: User
)

// 3. FIXED: The profile fields belong inside this User class
@Serializable
data class User(
    @SerialName("id") val id: Long,
    @SerialName("name") val nickName: String,
    @SerialName("account") val accountName: String,
    @SerialName("mail_address") val mailAddress: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_mail_authorized") val isMailAuthorized: Boolean, // Pixiv returns this as a Boolean
    @SerialName("require_policy_agreement") val requiresPolicyAgreement: Boolean,
    @SerialName("profile_image_urls") val profileImageUrls: ProfileImageUrlDto
)

@Serializable
data class ProfileImageUrlDto(
    @SerialName("px_16x16") val small: String,
    @SerialName("px_50x50") val medium: String,
    @SerialName("px_170x170") val large: String,
)

// 4. Updated Mappers to dig into response.user
fun OAuthUserResponse.toEntity(encryptedResponseToken: String): OAuthUserEntity {
    val userData = this.response.user
    return OAuthUserEntity(
        id = userData.id,
        name = userData.nickName,
        account = userData.accountName,
        mailAddress = userData.mailAddress,
        bestProfilePicUrl = userData.profileImageUrls.large,
        encryptedRefreshToken = encryptedResponseToken
    )
}

fun OAuthUserResponse.toDomain(encryptedResponseToken: String): Independent {
    val userData = this.response.user
    return Independent(
        id = userData.id,
        name = userData.nickName,
        account = userData.accountName,
        mailAddress = userData.mailAddress,
        bestProfilePicUrl = userData.profileImageUrls.large,
        accessToken = this.response.accessToken,
        encryptedRefreshToken = encryptedResponseToken
    )
}
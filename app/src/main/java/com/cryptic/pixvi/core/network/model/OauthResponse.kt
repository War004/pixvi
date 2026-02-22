package com.cryptic.pixvi.core.network.model

import com.cryptic.pixvi.core.model.OAuthInfo
import com.cryptic.pixvi.core.model.ProfilePicturesUrls
import com.cryptic.pixvi.core.model.UserMetaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OauthResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("scope")
    val scope: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("user")
    val user: PixivUser,

    // The JSON has a nested object called "response" containing duplicate data
    @SerialName("response")
    val response: NestedAuthResponse
)

@Serializable
data class NestedAuthResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("scope")
    val scope: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("user")
    val user: PixivUser
)

@Serializable
data class PixivUser(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,

    @SerialName("account")
    val account: String,

    @SerialName("mail_address")
    val mailAddress: String,

    @SerialName("is_premium")
    val isPremium: Boolean,

    @SerialName("x_restrict")
    val xRestrict: Int,

    @SerialName("is_mail_authorized")
    val isMailAuthorized: Boolean,

    @SerialName("require_policy_agreement")
    val requirePolicyAgreement: Boolean,

    @SerialName("profile_image_urls")
    val userProfileImageUrls: UserProfileImageUrls
)

@Serializable
data class UserProfileImageUrls(
    @SerialName("px_16x16")
    val px16x16: String,

    @SerialName("px_50x50")
    val px50x50: String,

    @SerialName("px_170x170")
    val px170x170: String
)

fun OauthResponse.toOAuthInfo(): OAuthInfo{
    return OAuthInfo(
        tokenType = this.tokenType,
        scope = this.scope,
        user = UserMetaData(
            id = this.user.id,
            name = this.user.name,
            account = this.user.account,
            emailAddress = this.user.mailAddress,
            isPremium = this.user.isPremium,
            xRestrict = this.user.xRestrict,
            isMailAuthorized = this.user.isMailAuthorized,
            requirePolicyAgreement = this.user.requirePolicyAgreement,
            profileImageUrls = ProfilePicturesUrls(
                px16x16 = this.user.userProfileImageUrls.px16x16,
                px50x50 = this.user.userProfileImageUrls.px50x50,
                px170x170 = this.user.userProfileImageUrls.px170x170
            )
        )
    )
}
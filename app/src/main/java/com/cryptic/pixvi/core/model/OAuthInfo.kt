package com.cryptic.pixvi.core.model

import com.cryptic.pixvi.core.network.model.OauthResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


data class OAuthInfo(
    val tokenType: String,
    val scope: String,
    val user: UserMetaData
)

data class UserMetaData(
    val id: Long,
    val name: String,
    val account: String,
    val emailAddress: String,
    val isPremium: Boolean,
    val xRestrict: Int,
    val isMailAuthorized: Boolean,
    val requirePolicyAgreement: Boolean,
    val profileImageUrls: ProfilePicturesUrls
)

data class ProfilePicturesUrls(
    val px16x16: String,
    val px50x50: String,
    val px170x170: String
)

/*
fun OAuthInfo.toOauthResponse(data: OAuthInfo): OauthResponse{

}
*/
package com.example.pixvi.login

import com.google.gson.annotations.SerializedName


/**
 * Data class representing the response body received after a successful token exchange with Pixiv API.
 * This contains access tokens, refresh tokens, token expiry information, and user details.
 *
 * @property access_token The access token used to authenticate API requests. Short-lived.
 * @property expires_in Token expiry time in seconds.
 * @property token_type Type of token, usually "Bearer".
 * @property scope OAuth scopes granted to the access token.
 * @property refresh_token Token used to obtain new access tokens when the current one expires. Long-lived.
 * @property user [User] data class containing user profile information.
 * @property response Optional nested [TokenResponseInner] in some Pixiv API responses. Contains redundant token information.
 */
data class TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
    val scope: String,
    val refresh_token: String,
    val user: User,
    val response: TokenResponseInner? = null
)

/**
 * Data class representing a nested token response structure sometimes found within [TokenResponse].
 * This seems to be a redundant structure in Pixiv's API and mirrors the fields of the outer [TokenResponse].
 *
 * @property access_token (Redundant) Access token, same as in the outer [TokenResponse].
 * @property expires_in (Redundant) Token expiry time, same as in the outer [TokenResponse].
 * @property token_type (Redundant) Token type, same as in the outer [TokenResponse].
 * @property scope (Redundant) OAuth scopes, same as in the outer [TokenResponse].
 * @property refresh_token (Redundant) Refresh token, same as in the outer [TokenResponse].
 * @property user (Redundant) [User] data class, same as in the outer [TokenResponse].
 */
data class TokenResponseInner(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
    val scope: String,
    val refresh_token: String,
    val user: User
)

/**
 * Data class representing user information from the Pixiv API token response.
 * Contains profile details, IDs, account information, and user settings.
 *
 * @property profile_image_urls [ProfileImageUrls] data class containing different sizes of the user's profile image.
 * @property id User's unique identifier (user ID).
 * @property name User's display name.
 * @property account User's account name (login name).
 * @property mail_address User's registered email address.
 * @property is_premium Boolean indicating if the user has a premium Pixiv account.
 * @property x_restrict User's content restriction level (e.g., for R-18 content).
 * @property is_mail_authorized Boolean indicating if the user's email is authorized.
 * @property require_policy_agreement Boolean indicating if the user needs to agree to policy updates.
 */
data class User(
    val profile_image_urls: ProfileImageUrls,
    val id: String,
    val name: String,
    val account: String,
    val mail_address: String,
    val is_premium: Boolean,
    val x_restrict: Int,
    val is_mail_authorized: Boolean,
    val require_policy_agreement: Boolean
)

/**
 * Data class representing different URLs for user profile images in various sizes.
 * Used within the [User] data class.
 *
 * @property px_16x16 URL for a 16x16 pixel profile image.
 * @property px_50x50 URL for a 50x50 pixel profile image.
 * @property px_170x170 URL for a 170x170 pixel profile image.
 */
data class ProfileImageUrls(
    val px_16x16: String,
    val px_50x50: String,
    val px_170x170: String
)

/**
 * Data class representing the response from Pixiv's IDP URLs endpoint.
 * Contains URLs required for different identity provider related actions, including the OAuth token endpoint.
 *
 * @property `account-edit` URL for editing account settings.
 * @property `account-leave-prepare` URL related to account deletion preparation.
 * @property `account-leave-status` URL to check account deletion status.
 * @property `account-setting-prepare` URL for preparing account settings.
 * @property `auth-token` URL for the OAuth 2.0 token endpoint (used for exchanging authorization codes for tokens).
 * @property `auth-token-redirect-uri` Redirect URI for the OAuth 2.0 token endpoint.
 */
data class IdpUrlsResponse(
    val `account-edit`: String,
    val `account-leave-prepare`: String,
    val `account-leave-status`: String,
    val `account-setting-prepare`: String,
    val `auth-token`: String,
    val `auth-token-redirect-uri`: String
)

/**
 * Sealed class representing the different states of the login process in the application.
 * Used with StateFlow to observe and react to changes in login state in the UI.
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Initiated : LoginState()
    data class Success(
        val username: String,
        val accessToken: String,
        val tokenExpiry: Long // Store expiry as timestamp
    ) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Data class for parsing the error JSON response from Pixiv,
 * specifically when an access token is invalid (e.g., "invalid_grant").
 */
data class ErrorResponsePayload(
    @SerializedName("error")
    val error: ErrorDetail?
)

data class ErrorDetail(
    @SerializedName("user_message")
    val userMessage: String?, // "user_message": ""
    @SerializedName("message")
    val message: String?, // "message": "Error occurred at the OAuth process. Please check your Access Token to fix this. Error Message: invalid_grant"
    @SerializedName("reason")
    val reason: String?, // "reason": ""
    @SerializedName("user_message_details")
    val userMessageDetails: Map<String, Any>? // "user_message_details": {}
)

package com.example.pixvi.network.response.AppLoading

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the entire response from the /v1/user/me/state endpoint.
 */
data class UserStateResponse(
    @SerializedName("user_state") val userState: UserStateData?,
    @SerializedName("profile") val profile: ProfileData?
)

/**
 * Contains details about the user's state.
 */
data class UserStateData(
    @SerializedName("is_mail_authorized") val isMailAuthorized: Boolean?,
    @SerializedName("has_mail_address") val hasMailAddress: Boolean?,
    @SerializedName("has_changed_pixiv_id") val hasChangedPixivId: Boolean?,
    @SerializedName("can_change_pixiv_id") val canChangePixivId: Boolean?,
    @SerializedName("has_password") val hasPassword: Boolean?,
    @SerializedName("require_policy_agreement") val requirePolicyAgreement: Boolean?,
    @SerializedName("no_login_method") val noLoginMethod: Boolean?,
    @SerializedName("is_user_restricted") val isUserRestricted: Boolean?
)

/**
 * Contains details about the user's profile.
 */
data class ProfileData(
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("pixiv_id") val pixivId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("profile_image_urls") val profileImageUrls: ProfileImageUrls?,
    @SerializedName("is_premium") val isPremium: Boolean?,
    @SerializedName("x_restrict") val xRestrict: Int?
)

/**
 * Contains URLs for the user's profile image.
 */
data class ProfileImageUrls(
    @SerializedName("medium") val medium: String?
)

/**
 * A simplified data class to represent an account for display purposes in the UI.
 */

object CurrentAccountManager {
    private val _currentAccount = MutableStateFlow<ProfileData?>(null)
    val currentAccount: StateFlow<ProfileData?> = _currentAccount.asStateFlow()

    // Indicates if an attempt to load the initial account is in progress
    private val _isLoadingInitialAccount = MutableStateFlow<Boolean>(false)
    val isLoadingInitialAccount: StateFlow<Boolean> = _isLoadingInitialAccount.asStateFlow()

    // Holds any error message from the initial account loading attempt
    private val _initialAccountError = MutableStateFlow<String?>(null)
    val initialAccountError: StateFlow<String?> = _initialAccountError.asStateFlow()

    fun loginAccount(account: ProfileData) {
        _currentAccount.value = account.copy()
        _isLoadingInitialAccount.value = false // Reset loading/error states on successful login
        _initialAccountError.value = null
    }

    fun logoutAccount() {
        _currentAccount.value = null
        // Optionally clear other states if needed
    }

    fun setLoadingState(isLoading: Boolean) {
        _isLoadingInitialAccount.value = isLoading
        if (isLoading) {
            _initialAccountError.value = null // Clear previous errors when starting to load
        }
    }

    fun setErrorState(errorMessage: String?) {
        _initialAccountError.value = errorMessage
        _isLoadingInitialAccount.value = false // Stop loading on error
    }
}
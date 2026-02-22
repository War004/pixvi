package com.cryptic.pixvi.auth.account

data class AccountDetails(
    val userId: String,
    val name: String,
    val accountName: String,
    val email: String,
    val isPremium: Boolean,
    val profilePicUrlBig: String
)
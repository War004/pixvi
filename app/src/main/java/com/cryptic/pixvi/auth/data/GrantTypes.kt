package com.cryptic.pixvi.auth.data

enum class GrantTypes(val value: String){
    AUTH_CODE("authorization_code"),
    REFRESH_CODE("refresh_token")
}
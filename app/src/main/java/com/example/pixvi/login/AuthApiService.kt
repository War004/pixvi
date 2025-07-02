package com.example.pixvi.login

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthApiService {

    @GET("https://app-api.pixiv.net/idp-urls")
    suspend fun getIdpUrls(
        @Header("App-OS") appOs: String = "android",
        @Header("App-OS-Version") appOsVersion: String,
        @Header("App-Version") appVersion: String = "6.144.0",
        @Header("User-Agent") userAgent: String,
        @Header("X-Client-Time") clientTime: String,
        @Header("X-Client-Hash") clientHash: String,
        @Header("app-accept-language") appAcceptLanguage: String = "en",
        @Header("Accept-Language") acceptLanguage: String = "en_US"
    ): Response<IdpUrlsResponse>

    @FormUrlEncoded
    @POST
    suspend fun exchangeToken(
        @Url authTokenUrl: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") authCode: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("client_id") clientId: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT",
        @Field("client_secret") clientSecret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj",
        @Field("redirect_uri") redirectUri: String = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback",
        @Field("include_policy") includePolicy: Boolean = true,
        @Header("App-OS") appOs: String = "android",
        @Header("App-OS-Version") appOsVersion: String,
        @Header("App-Version") appVersion: String = "7.0.0",
        @Header("User-Agent") userAgent: String,
        @Header("X-Client-Time") clientTime: String,
        @Header("X-Client-Hash") clientHash: String,
        @Header("app-accept-language") appAcceptLanguage: String = "en",
        @Header("Accept-Language") acceptLanguage: String = "en_US",
        @Header("Content-Type") contentType: String = "application/x-www-form-urlencoded"
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST
    suspend fun refreshToken(
        @Url authTokenUrl: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT",
        @Field("client_secret") clientSecret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj",
        @Field("include_policy") includePolicy: Boolean = true,
        @Header("App-OS") appOs: String = "android",
        @Header("App-OS-Version") appOsVersion: String,
        @Header("App-Version") appVersion: String = "7.0.0",
        @Header("User-Agent") userAgent: String,
        @Header("X-Client-Time") clientTime: String,
        @Header("X-Client-Hash") clientHash: String,
        @Header("app-accept-language") appAcceptLanguage: String = "en",
        @Header("Accept-Language") acceptLanguage: String = "en_US",
        @Header("Content-Type") contentType: String = "application/x-www-form-urlencoded"
    ): Response<TokenResponse>
}
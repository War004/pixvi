package com.cryptic.pixvi.core.network.api

import com.cryptic.pixvi.core.network.model.OauthResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApiService {

    //This would be the url that our will app get the end
    /*
    //https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?state=86tg1HoI6wZSjnyzra2VHdig6ByO4kMPH2Q2SLfjd1t0tPufsGI4hKrUI9wFY7ZQ&code=aw7GQzNpwq883mL00cQRd4Us8KRc8_oFiOqYP70-JZk
    @POST("web/v1/users/auth/pixiv/callback?state={state}&code={code}")
    suspend fun
            */
    /*
    //https://oauth.secure.pixiv.net/auth/authorize?client_id=MOBrBDS8blbauoSck0ZfDbtuzpyT&redirect_uri=https%3A%2F%2Fapp-api.pixiv.net%2Fweb%2Fv1%2Fusers%2Fauth%2Fpixiv%2Fcallback&response_type=code&scope=&state=86tg1HoI6wZSjnyzra2VHdig6ByO4kMPH2Q2SLfjd1t0tPufsGI4hKrUI9wFY7ZQ&code_challenge=TkmN43K3UrKdQBkKixSD5-lv_yDaMIHajlh2k-DXSL4&code_challenge_method=S256
    @GET("auth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&response_type={code}&scope={scope}&state={state}&code_challenge={code_challenge}&code_challenge_method={hash_function}")
    fun
            */
//
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun getToken(
        @Field("code_verifier") codeVerifier: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUrl: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("include_policy") includePolicy: Boolean
    ): OauthResponse

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun updateToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("include_policy") includePolicy: Boolean
    ): OauthResponse
}
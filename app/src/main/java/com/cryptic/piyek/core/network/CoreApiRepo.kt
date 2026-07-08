package com.cryptic.piyek.core.network

import com.cryptic.piyek.core.CResponse
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface CoreApiRepo{

    @GET
    suspend fun<T: Any> getMoreRecommendation(
        @Url url: String
    ): CResponse<T>


}
package com.cryptic.pixvi.core.network

import com.cryptic.pixvi.core.network.NetworkResultCall
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type


class NetworkResultCallAdapter(
    private val type: Type
): CallAdapter<Type, Call<NetworkResult<Type>>>{
    override fun responseType(): Type = type


    override fun adapt(call: Call<Type>): Call<NetworkResult<Type>> {
        return NetworkResultCall(call)
    }
}

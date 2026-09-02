package com.cryptic.piyek.core.network

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class NetworkResultCallAdapterFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        check(returnType is ParameterizedType) {
            "Call return type must be parameterized as Call<NetworkResult<Foo>>"
        }

        val callType = getParameterUpperBound(0, returnType)
        if (getRawType(callType) != NetworkResult::class.java) return null
        check(callType is ParameterizedType) {
            "NetworkResult return type must be parameterized as NetworkResult<Foo>. " +
            "If this occurs in release builds, ensure ProGuard retains generic signatures (-keepattributes Signature)."
        }

        val responseType = getParameterUpperBound(0, callType)
        return NetworkResultCallAdapter(responseType)
    }
}
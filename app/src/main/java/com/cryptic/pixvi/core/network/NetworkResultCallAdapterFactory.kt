package com.cryptic.pixvi.core.network

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class NetworkResultCallAdapterFactory: CallAdapter.Factory(){
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation?>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {


        // 1. SUSPEND FUNCTION CHECK
        // Suspend functions are wrapped in a 'Call' automatically.
        // We must check if the outer type is 'Call'.
        if (getRawType(returnType) != Call::class.java) {
            return null
        }


        // 2. UNWRAP CALL
        // Look inside Call<NetworkResult<User>> to get NetworkResult<User>
        check(returnType is ParameterizedType) { "Return type must be parameterized as Call<NetworkResult<Foo>>" }
        val responseType = getParameterUpperBound(0, returnType)


        // 3. CHECK FOR NETWORKRESULT
        // If the inside is NOT NetworkResult, we ignore it (return null)
        if (getRawType(responseType) != NetworkResult::class.java) {
            return null
        }


        // 4. UNWRAP NETWORKRESULT
        // Look inside NetworkResult<User> to get 'User'
        check(responseType is ParameterizedType) { "Response must be parameterized as NetworkResult<Foo>" }
        val successBodyType = getParameterUpperBound(0, responseType)


        // 5. CREATE ADAPTER
        // Pass 'User' to the adapter.
        // Now Retrofit will look for a serializer for 'User', NOT 'NetworkResult'.
        return NetworkResultCallAdapter(successBodyType)
    }


    companion object{
        fun create() = NetworkResultCallAdapterFactory()
    }
}

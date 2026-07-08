package com.cryptic.piyek.core

sealed interface CResponse<out T> {
    data class Success<out T>(val data: T): CResponse<T>
    data class Failed(val exception: Throwable): CResponse<Nothing>
}
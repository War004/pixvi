package com.cryptic.piyek.core.domain.error

typealias RootError = Error

sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>

inline fun <T, E : RootError, R> Result<T, E>.map(
    transform: (T) -> R
): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(transform(this.data))
    }
}

inline fun <T, E : RootError> Result<T, E>.onSuccess(
    action: (T) -> Unit
): Result<T, E> {
    return when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(this.data)
            this
        }
    }
}

inline fun <T, E : RootError> Result<T, E>.onFailure(
    action: (E) -> Unit
): Result<T, E> {
    return when (this) {
        is Result.Error -> {
            action(error)
            this
        }
        is Result.Success -> this
    }
}

fun <T, E : RootError> Result<T, E>.asEmptyResult(): EmptyResult<E> {
    return map { }
}
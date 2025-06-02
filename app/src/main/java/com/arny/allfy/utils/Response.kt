package com.arny.allfy.utils

sealed class Response<out T> {
    object Idle : Response<Nothing>()
    object Loading : Response<Nothing>()
    data class Success<out T>(val data: T, val hasMore: Boolean = false) : Response<T>()
    data class Error(val message: String) : Response<Nothing>()
}

fun <T, R> Response<T>.mapSuccess(transform: (T) -> R): Response<R> {
    return when (this) {
        is Response.Idle -> Response.Idle
        is Response.Loading -> Response.Loading
        is Response.Error -> Response.Error(this.message)
        is Response.Success -> Response.Success(transform(this.data), this.hasMore)
    }
}

val <T> Response<T>.isLoading: Boolean
    get() = this is Response.Loading

val <T> Response<T>.isSuccess: Boolean
    get() = this is Response.Success

val <T> Response<T>.isError: Boolean
    get() = this is Response.Error

val <T> Response<T>.isIdle: Boolean
    get() = this is Response.Idle

fun <T> Response<T>.getErrorMessageOrNull(): String? {
    return when (this) {
        is Response.Error -> message
        else -> null
    }
}

fun <T> Response<T>.getDataOrNull(): T? {
    return when (this) {
        is Response.Success -> data
        else -> null
    }
}
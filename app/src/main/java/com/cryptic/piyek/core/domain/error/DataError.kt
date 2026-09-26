package com.cryptic.piyek.core.domain.error

enum class NetworkError : Error {
    REQUEST_TIMEOUT,       // SocketTimeoutException, HTTP 408, 504
    UNAUTHORIZED,          // HTTP 401
    FORBIDDEN,             // HTTP 403
    NOT_FOUND,             // HTTP 404
    CONFLICT,              // HTTP 409 (e.g., user already exists)
    TOO_MANY_REQUESTS,     // HTTP 429
    NO_INTERNET,           // UnknownHostException, ConnectException
    PAYLOAD_TOO_LARGE,     // HTTP 413 (uploading files too big)
    SERVER_ERROR,          // HTTP 500..599
    SERIALIZATION,         // JSON parsing crashed
    UNKNOWN                // Fallback
}
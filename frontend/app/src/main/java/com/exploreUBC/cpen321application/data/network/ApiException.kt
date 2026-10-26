package com.exploreUBC.cpen321application.data.network

import java.io.IOException

open class ApiException(
    val statusCode: Int,
    val errorCode: String?,
    message: String,
) : IOException(message) {
    companion object {
        private const val UNAUTHORIZED_MESSAGE =
            "Your session is no longer valid. Please sign in again."
        private const val FORBIDDEN_MESSAGE =
            "You do not have permission to perform this action."
        private const val DEFAULT_MESSAGE = "The request could not be completed."

        fun fromHttpStatus(statusCode: Int, errorCode: String? = null): ApiException =
            ApiException(
                statusCode = statusCode,
                errorCode = errorCode,
                message = when (statusCode) {
                    401 -> UNAUTHORIZED_MESSAGE
                    403 -> FORBIDDEN_MESSAGE
                    else -> DEFAULT_MESSAGE
                },
            )
    }
}

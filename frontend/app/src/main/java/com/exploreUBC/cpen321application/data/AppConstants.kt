package com.exploreUBC.cpen321application.data

internal object AppConstants {
    object Auth {
        const val GOOGLE_SIGN_IN_PATH = "/auth/google"
        const val CURRENT_SESSION_PATH = "/auth/session"
    }

    object Network {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 5_000
        const val API_VERSION_PATH = "/api/v1"
        const val ACCEPT_HEADER = "Accept"
        const val CONTENT_TYPE_HEADER = "Content-Type"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val JSON_MEDIA_TYPE = "application/json"
        const val BEARER_PREFIX = "Bearer "
        const val SESSION_REQUIRED_ERROR_CODE = "SESSION_REQUIRED"
        const val SESSION_EXPIRED_ERROR_CODE = "SESSION_EXPIRED"
        const val INVALID_SESSION_ERROR_CODE = "INVALID_SESSION"
    }

    object Session {
        const val RENEWAL_WINDOW_MILLIS = 15 * 60 * 1_000L
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val FILE_VERSION = 1
        const val DEFAULT_KEY_ALIAS = "exploreubc_session_key"
        const val DEFAULT_FILE_NAME = "exploreubc_session.json"
    }
}

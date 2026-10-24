package com.exploreUBC.cpen321application.data.auth

data class AuthenticatedSession(
    val token: String,
    val userId: String,
    val expiresAtEpochMillis: Long,
) {
    init {
        require(token.isNotBlank()) { "Session token must not be blank." }
        require(userId.isNotBlank()) { "Session user ID must not be blank." }
        require(expiresAtEpochMillis > 0) { "Session expiry must be positive." }
    }

    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtEpochMillis <= nowEpochMillis

    fun expiresWithin(
        windowMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        require(windowMillis >= 0) { "Expiry window must not be negative." }
        return expiresAtEpochMillis - nowEpochMillis <= windowMillis
    }
}

package com.exploreUBC.cpen321application.data.auth

import java.util.concurrent.atomic.AtomicReference

class SessionManager(initialSession: AuthenticatedSession? = null) {
    private val session = AtomicReference(initialSession)

    fun currentSession(): AuthenticatedSession? = session.get()

    fun replace(replacement: AuthenticatedSession) {
        session.set(replacement)
    }

    fun clear() {
        session.set(null)
    }
}

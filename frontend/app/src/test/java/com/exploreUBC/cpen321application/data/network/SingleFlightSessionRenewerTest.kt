package com.exploreUBC.cpen321application.data.network

import com.exploreUBC.cpen321application.data.auth.AuthenticatedSession
import com.exploreUBC.cpen321application.data.auth.SessionManager
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SingleFlightSessionRenewerTest {
    @Test
    fun concurrentCallersShareOneRenewal() = runBlocking {
        val failed = AuthenticatedSession("old", "user-id", 1_000L)
        val replacement = AuthenticatedSession("new", "user-id", 2_000L)
        val manager = SessionManager(failed)
        val renewalStarted = CompletableDeferred<Unit>()
        val allowRenewalToFinish = CompletableDeferred<Unit>()
        val requestCount = AtomicInteger()
        val persistedCount = AtomicInteger()
        val renewer = SingleFlightSessionRenewer(
            sessionManager = manager,
            persistSession = {
                persistedCount.incrementAndGet()
            },
            requestReplacement = {
                requestCount.incrementAndGet()
                renewalStarted.complete(Unit)
                allowRenewalToFinish.await()
                replacement
            },
        )

        val first = async { renewer.renew(failed) }
        renewalStarted.await()
        val second = async { renewer.renew(failed) }
        allowRenewalToFinish.complete(Unit)

        assertSame(replacement, first.await())
        assertSame(replacement, second.await())
        assertSame(replacement, manager.currentSession())
        assertEquals(1, requestCount.get())
        assertEquals(1, persistedCount.get())
    }
}

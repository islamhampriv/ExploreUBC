package com.exploreUBC.cpen321application.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStoreTest {
    private lateinit var cipher: AndroidKeystoreSessionCipher
    private lateinit var store: SessionStore

    @Before
    fun setUp() {
        val suffix = UUID.randomUUID().toString()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        cipher = AndroidKeystoreSessionCipher("exploreubc_session_test_$suffix")
        store = SessionStore(
            noBackupFilesDir = context.noBackupFilesDir,
            fileName = "session_test_$suffix.json",
            cipher = cipher,
        )
    }

    @After
    fun tearDown() {
        store.clear()
        cipher.deleteKey()
    }

    @Test
    fun savesLoadsAndClearsWithoutPlaintextToken() {
        val session = AuthenticatedSession(
            token = "secret-session-token",
            userId = "user-id",
            expiresAtEpochMillis = System.currentTimeMillis() + 60_000L,
        )

        store.save(session)

        assertEquals(session, store.load())
        assertFalse(store.sessionFile.readText().contains(session.token))
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun replacingSessionAtomicallyUpdatesStoredValue() {
        val initial = AuthenticatedSession(
            token = "initial-session-token",
            userId = "user-id",
            expiresAtEpochMillis = System.currentTimeMillis() + 60_000L,
        )
        val replacement = initial.copy(
            token = "replacement-session-token",
            expiresAtEpochMillis = initial.expiresAtEpochMillis + 60_000L,
        )

        store.save(initial)
        store.save(replacement)

        assertEquals(replacement, store.load())
        val storedText = store.sessionFile.readText()
        assertFalse(storedText.contains(initial.token))
        assertFalse(storedText.contains(replacement.token))
    }

    @Test
    fun corruptionFailsClosedAndDeletesStoredSession() {
        store.sessionFile.writeText("not-valid-encrypted-json")

        assertNull(store.load())
        assertFalse(store.sessionFile.exists())
    }

    @Test
    fun missingKeystoreKeyFailsClosedAndDeletesStoredSession() {
        store.save(
            AuthenticatedSession(
                token = "secret-session-token",
                userId = "user-id",
                expiresAtEpochMillis = System.currentTimeMillis() + 60_000L,
            ),
        )
        cipher.deleteKey()

        assertNull(store.load())
        assertFalse(store.sessionFile.exists())
    }

    @Test
    fun preservesExpiryForLocalChecks() {
        val now = System.currentTimeMillis()
        val session = AuthenticatedSession(
            token = "secret-session-token",
            userId = "user-id",
            expiresAtEpochMillis = now - 1L,
        )

        store.save(session)

        val restored = store.load()
        assertEquals(session, restored)
        assertTrue(checkNotNull(restored).isExpired(now))
    }
}

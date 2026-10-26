package com.exploreUBC.cpen321application.data.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class GoogleCredentialProvider(
    private val activity: ComponentActivity,
    private val serverClientId: String,
    private val credentialManager: CredentialManager = CredentialManager.create(activity),
) {
    suspend fun requestInteractiveIdToken(): String {
        // Avoid opening Credential Manager's bottom sheet during the first composition frame.
        delay(250)
        val option = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()
        return requestIdToken(option)
    }

    suspend fun requestAuthorizedIdToken(): String {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        return requestIdToken(option)
    }

    private suspend fun requestIdToken(option: androidx.credentials.CredentialOption): String {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = withContext(Dispatchers.Main.immediate) {
            credentialManager.getCredential(
                request = request,
                context = activity,
            )
        }
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleIdTokenParsingException()
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}

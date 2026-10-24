package com.exploreUBC.cpen321application.data.network

import com.exploreUBC.cpen321application.data.auth.AuthenticatedSession
import com.exploreUBC.cpen321application.data.auth.SessionManager
import com.exploreUBC.cpen321application.data.AppConstants
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ApiRequest(
    val method: String,
    val path: String,
    val body: String? = null,
    val requiresAuthentication: Boolean = true,
)

fun interface BackendClient {
    suspend fun execute(request: ApiRequest): String
}

internal data class ApiErrorPayload(
    val code: String?,
)

internal data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

class ApiClient internal constructor(
    apiBaseUrl: String,
    private val sessionManager: SessionManager,
    private val renewalHandler: SessionRenewalHandler? = null,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val openConnection: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
    private val parseError: (String) -> ApiErrorPayload = ::parseApiError,
) : BackendClient {
    private val apiRootUrl = "${apiBaseUrl.trimEnd('/')}" + AppConstants.Network.API_VERSION_PATH

    override suspend fun execute(request: ApiRequest): String {
        val failedSession = if (request.requiresAuthentication) {
            sessionManager.currentSession()
                ?: throw ApiException.fromHttpStatus(
                    statusCode = HttpURLConnection.HTTP_UNAUTHORIZED,
                    errorCode = AppConstants.Network.SESSION_REQUIRED_ERROR_CODE,
                )
        } else {
            null
        }

        val firstResponse = executeOnce(request, failedSession?.token)
        if (firstResponse.statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
            return requireSuccess(firstResponse)
        }

        val firstError = parseError(firstResponse.body)
        if (!shouldRenew(firstError, failedSession)) {
            throw ApiException.fromHttpStatus(firstResponse.statusCode, firstError.code)
        }

        val replacement = renewalHandler?.renew(checkNotNull(failedSession))
            ?: throw ApiException.fromHttpStatus(firstResponse.statusCode, firstError.code)
        val retryResponse = executeOnce(request, replacement.token)
        return requireSuccess(retryResponse)
    }

    private suspend fun executeOnce(
        request: ApiRequest,
        sessionToken: String?,
    ): HttpResponse = withContext(ioDispatcher) {
        require(request.path.startsWith('/')) { "API request path must start with '/'." }
        val connection = openConnection(URL("$apiRootUrl${request.path}"))
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = AppConstants.Network.CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = AppConstants.Network.READ_TIMEOUT_MILLIS
            connection.setRequestProperty(
                AppConstants.Network.ACCEPT_HEADER,
                AppConstants.Network.JSON_MEDIA_TYPE,
            )
            sessionToken?.let {
                connection.setRequestProperty(
                    AppConstants.Network.AUTHORIZATION_HEADER,
                    AppConstants.Network.BEARER_PREFIX + it,
                )
            }
            request.body?.let { body ->
                connection.doOutput = true
                connection.setRequestProperty(
                    AppConstants.Network.CONTENT_TYPE_HEADER,
                    AppConstants.Network.JSON_MEDIA_TYPE,
                )
                connection.outputStream.bufferedWriter().use { writer -> writer.write(body) }
            }

            val statusCode = connection.responseCode
            val responseBody = if (statusCode in 200..299) {
                connection.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            HttpResponse(statusCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun shouldRenew(
        error: ApiErrorPayload,
        failedSession: AuthenticatedSession?,
    ): Boolean {
        if (failedSession == null || renewalHandler == null) return false
        return error.code == AppConstants.Network.SESSION_EXPIRED_ERROR_CODE ||
            (
                error.code == AppConstants.Network.INVALID_SESSION_ERROR_CODE &&
                    failedSession.isExpired(nowEpochMillis())
                )
    }

    private fun requireSuccess(response: HttpResponse): String {
        if (response.statusCode in 200..299) return response.body

        val error = parseError(response.body)
        throw ApiException.fromHttpStatus(response.statusCode, error.code)
    }
}

private fun parseApiError(responseBody: String): ApiErrorPayload = runCatching {
    val error = JSONObject(responseBody).optJSONObject("error")
    ApiErrorPayload(
        code = error?.optString("code")?.takeIf { it.isNotBlank() },
    )
}.getOrElse { ApiErrorPayload(code = null) }

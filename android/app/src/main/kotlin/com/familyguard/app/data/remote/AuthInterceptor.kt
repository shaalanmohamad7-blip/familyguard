package com.familyguard.app.data.remote

import com.familyguard.app.data.local.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <deviceToken>` to every outgoing request (when we have a
 * token), and watches every response for 401/403 — a sign the device's pairing was revoked —
 * to hand off to [SessionManager] exactly once.
 */
class AuthInterceptor(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = secureStorage.deviceToken()

        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
            sessionManager.onUnauthorized()
        }

        return response
    }
}

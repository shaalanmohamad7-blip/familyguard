package com.familyguard.app.data.repository

import android.content.Context
import com.familyguard.app.R
import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.data.remote.ApiService
import com.familyguard.app.data.remote.dto.PairingRedeemRequest
import com.familyguard.app.util.Constants

sealed class PairingResult {
    data object Success : PairingResult()
    data class Failure(val message: String) : PairingResult()
}

class PairingRepository(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val appVersion: String,
    private val context: Context
) {

    suspend fun redeemCode(code: String): PairingResult {
        if (code.isBlank()) {
            return PairingResult.Failure(context.getString(R.string.pairing_error_blank_code))
        }
        return try {
            val response = apiService.redeemPairingCode(
                PairingRedeemRequest(
                    code = code.trim(),
                    platform = Constants.PLATFORM,
                    appVersion = appVersion
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                secureStorage.savePairing(
                    deviceToken = body.deviceToken,
                    deviceId = body.deviceId,
                    familyId = body.familyId
                )
                PairingResult.Success
            } else {
                PairingResult.Failure(mapErrorCode(response.code()))
            }
        } catch (t: Throwable) {
            PairingResult.Failure(context.getString(R.string.common_error_network))
        }
    }

    private fun mapErrorCode(code: Int): String = when (code) {
        400, 404 -> context.getString(R.string.pairing_error_bad_code)
        410 -> context.getString(R.string.pairing_error_expired_code)
        409 -> context.getString(R.string.pairing_error_used_code)
        else -> context.getString(R.string.pairing_error_generic)
    }
}

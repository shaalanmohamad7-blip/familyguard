package com.familyguard.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encrypted, on-device storage for pairing credentials and small pieces of app state.
 *
 * The device token is a bearer credential that lets the backend attribute reports to this
 * device — it is deliberately never written to plain-text SharedPreferences, logs, or backups
 * (see backup_rules.xml / data_extraction_rules.xml).
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    data class PairingState(
        val deviceToken: String?,
        val deviceId: String?,
        val familyId: String?
    ) {
        val isPaired: Boolean get() = !deviceToken.isNullOrBlank() && !deviceId.isNullOrBlank()
    }

    private val _pairingState = MutableStateFlow(readPairingState())
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _disclosureAccepted = MutableStateFlow(prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false))
    val disclosureAccepted: StateFlow<Boolean> = _disclosureAccepted.asStateFlow()

    private val _permissionWizardCompleted =
        MutableStateFlow(prefs.getBoolean(KEY_PERMISSION_WIZARD_DONE, false))
    val permissionWizardCompleted: StateFlow<Boolean> = _permissionWizardCompleted.asStateFlow()

    private fun readPairingState(): PairingState = PairingState(
        deviceToken = prefs.getString(KEY_DEVICE_TOKEN, null),
        deviceId = prefs.getString(KEY_DEVICE_ID, null),
        familyId = prefs.getString(KEY_FAMILY_ID, null)
    )

    fun savePairing(deviceToken: String, deviceId: String, familyId: String) {
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, deviceToken)
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_FAMILY_ID, familyId)
            .apply()
        _pairingState.value = PairingState(deviceToken, deviceId, familyId)
    }

    fun setDisclosureAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, accepted).apply()
        _disclosureAccepted.value = accepted
    }

    fun setPermissionWizardCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSION_WIZARD_DONE, completed).apply()
        _permissionWizardCompleted.value = completed
    }

    fun deviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)
    fun deviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)
    fun familyId(): String? = prefs.getString(KEY_FAMILY_ID, null)
    fun isPaired(): Boolean = pairingState.value.isPaired

    /** Clears everything pairing-related. Called on explicit unpair and on a 401/403 from the backend. */
    fun clearPairing() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_FAMILY_ID)
            .remove(KEY_PERMISSION_WIZARD_DONE)
            .apply()
        _pairingState.value = PairingState(null, null, null)
        _permissionWizardCompleted.value = false
    }

    companion object {
        private const val PREFS_FILE_NAME = "familyguard_secure_prefs"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"
        private const val KEY_PERMISSION_WIZARD_DONE = "permission_wizard_completed"
    }
}

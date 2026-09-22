package com.familyguard.app.ui.status

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.repository.DeviceRepository
import com.familyguard.app.data.repository.LocationRepository
import com.familyguard.app.domain.model.Guardian
import com.familyguard.app.domain.model.PermissionSnapshot
import com.familyguard.app.geofence.GeofenceManager
import com.familyguard.app.location.LocationForegroundService
import com.familyguard.app.location.LocationHelper
import com.familyguard.app.util.Constants
import com.familyguard.app.util.PermissionUtils
import com.familyguard.app.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CheckInStatus {
    data object Idle : CheckInStatus()
    data object InProgress : CheckInStatus()
    data object Success : CheckInStatus()
    data object Failure : CheckInStatus()
}

sealed class SosStatus {
    data object Idle : SosStatus()
    data object Sending : SosStatus()
    data object Sent : SosStatus()
    data object Failed : SosStatus()
    data object NoLocation : SosStatus()
}

data class StatusUiState(
    val snapshot: PermissionSnapshot,
    val guardians: List<Guardian> = emptyList(),
    val isLoadingGuardians: Boolean = true,
    val guardiansError: Boolean = false,
    val checkInStatus: CheckInStatus = CheckInStatus.Idle,
    val sosStatus: SosStatus = SosStatus.Idle,
    val isUnpairing: Boolean = false,
    val unpaired: Boolean = false
) {
    val isSharingActive: Boolean get() = snapshot.canShareLocationContinuously
}

class StatusHomeViewModel(
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val locationHelper: LocationHelper,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatusUiState(
            snapshot = PermissionSnapshot(
                foregroundLocationGranted = false,
                backgroundLocationGranted = false,
                notificationsGranted = false,
                usageAccessGranted = false
            )
        )
    )
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    fun refreshPermissionSnapshot(context: Context) {
        _uiState.value = _uiState.value.copy(snapshot = PermissionUtils.currentSnapshot(context))
    }

    fun loadFamilyInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGuardians = true, guardiansError = false)
            val result = deviceRepository.getFamilyInfo()
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    guardians = result.getOrDefault(emptyList()),
                    isLoadingGuardians = false
                )
            } else {
                _uiState.value.copy(isLoadingGuardians = false, guardiansError = true)
            }
        }
    }

    fun checkInNow() {
        if (_uiState.value.checkInStatus is CheckInStatus.InProgress) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(checkInStatus = CheckInStatus.InProgress)
            val fix = locationHelper.getOneTimeLocation()
            val status = if (fix == null) {
                CheckInStatus.Failure
            } else {
                val result = locationRepository.reportLocation(
                    lat = fix.lat,
                    lng = fix.lng,
                    accuracyM = fix.accuracyM,
                    source = Constants.SOURCE_CHECK_IN
                )
                if (result.isSuccess) CheckInStatus.Success else CheckInStatus.Failure
            }
            _uiState.value = _uiState.value.copy(checkInStatus = status)
        }
    }

    fun clearCheckInStatus() {
        _uiState.value = _uiState.value.copy(checkInStatus = CheckInStatus.Idle)
    }

    fun sendSos(note: String?) {
        if (_uiState.value.sosStatus is SosStatus.Sending) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sosStatus = SosStatus.Sending)
            val fix = locationHelper.getOneTimeLocation()
            val status = if (fix == null) {
                SosStatus.NoLocation
            } else {
                val result = locationRepository.sendSos(
                    lat = fix.lat,
                    lng = fix.lng,
                    accuracyM = fix.accuracyM,
                    note = note?.takeIf { it.isNotBlank() }
                )
                if (result.isSuccess) SosStatus.Sent else SosStatus.Failed
            }
            _uiState.value = _uiState.value.copy(sosStatus = status)
        }
    }

    fun clearSosStatus() {
        _uiState.value = _uiState.value.copy(sosStatus = SosStatus.Idle)
    }

    /**
     * Immediately stops all reporting and removes this device's pairing. Local effects (stop
     * the foreground service, cancel background jobs, clear geofences, clear stored
     * credentials) happen unconditionally, even if the network call to the backend fails —
     * the child's ability to stop sharing must never depend on connectivity.
     */
    fun unpair(context: Context) {
        if (_uiState.value.isUnpairing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUnpairing = true)

            context.stopService(Intent(context, LocationForegroundService::class.java))
            WorkScheduler.cancelAll(context)
            geofenceManager.clearGeofences()

            deviceRepository.unpair()

            _uiState.value = _uiState.value.copy(isUnpairing = false, unpaired = true)
        }
    }
}

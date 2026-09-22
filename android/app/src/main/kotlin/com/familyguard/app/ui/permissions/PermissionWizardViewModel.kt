package com.familyguard.app.ui.permissions

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.familyguard.app.data.local.SecureStorage
import com.familyguard.app.domain.model.AppPermission
import com.familyguard.app.domain.model.PermissionSnapshot
import com.familyguard.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WizardUiState(
    val steps: List<AppPermission>,
    val currentIndex: Int = 0,
    val snapshot: PermissionSnapshot,
    val completed: Boolean = false
) {
    val currentStep: AppPermission? get() = steps.getOrNull(currentIndex)
    val stepNumber: Int get() = currentIndex + 1
    val totalSteps: Int get() = steps.size
}

/**
 * Drives the permission wizard's step order. Every step is skippable; skipping simply means we
 * move on without ever assuming the underlying feature is on. The wizard reads live permission
 * state after every system dialog or Settings visit rather than trusting a click result alone.
 */
class PermissionWizardViewModel(
    context: Context,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WizardUiState(
            steps = buildSteps(),
            snapshot = PermissionUtils.currentSnapshot(context)
        )
    )
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    private fun buildSteps(): List<AppPermission> = buildList {
        add(AppPermission.FOREGROUND_LOCATION)
        add(AppPermission.BACKGROUND_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(AppPermission.NOTIFICATIONS)
        }
        add(AppPermission.USAGE_ACCESS)
    }

    /** Re-reads every permission from the OS — called after any dialog, Settings visit, or resume. */
    fun refreshSnapshot(context: Context) {
        _uiState.value = _uiState.value.copy(snapshot = PermissionUtils.currentSnapshot(context))
    }

    fun advance() {
        val current = _uiState.value
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.steps.size) {
            secureStorage.setPermissionWizardCompleted(true)
            _uiState.value = current.copy(completed = true)
        } else {
            _uiState.value = current.copy(currentIndex = nextIndex)
        }
    }
}

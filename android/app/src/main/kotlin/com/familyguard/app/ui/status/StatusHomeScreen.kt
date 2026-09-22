package com.familyguard.app.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familyguard.app.R
import com.familyguard.app.ServiceLocator
import com.familyguard.app.ui.common.ConfirmDialog
import com.familyguard.app.ui.common.GenericViewModelFactory
import com.familyguard.app.ui.common.PermissionStatusRow
import kotlinx.coroutines.launch

@Composable
fun StatusHomeScreen(
    onManagePermissions: () -> Unit,
    onUnpaired: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: StatusHomeViewModel = viewModel(
        factory = GenericViewModelFactory {
            StatusHomeViewModel(
                ServiceLocator.deviceRepository,
                ServiceLocator.locationRepository,
                ServiceLocator.locationHelper,
                ServiceLocator.geofenceManager
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSosSheet by remember { mutableStateOf(false) }
    var showUnpairConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionSnapshot(context)
        viewModel.loadFamilyInfo()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionSnapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.unpaired) {
        if (uiState.unpaired) onUnpaired()
    }

    val checkInSuccessMessage = stringResource(R.string.status_checkin_sent)
    val checkInFailureMessage = stringResource(R.string.status_checkin_failed)
    LaunchedEffect(uiState.checkInStatus) {
        when (uiState.checkInStatus) {
            CheckInStatus.Success -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(checkInSuccessMessage) }
                viewModel.clearCheckInStatus()
            }
            CheckInStatus.Failure -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(checkInFailureMessage) }
                viewModel.clearCheckInStatus()
            }
            else -> Unit
        }
    }

    val sosSuccessMessage = stringResource(R.string.sos_sent_success)
    val sosFailureMessage = stringResource(R.string.sos_sent_failure)
    val sosNoLocationMessage = stringResource(R.string.sos_no_location)
    LaunchedEffect(uiState.sosStatus) {
        when (uiState.sosStatus) {
            SosStatus.Sent -> {
                showSosSheet = false
                coroutineScope.launch { snackbarHostState.showSnackbar(sosSuccessMessage) }
                viewModel.clearSosStatus()
            }
            SosStatus.Failed -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(sosFailureMessage) }
                viewModel.clearSosStatus()
            }
            SosStatus.NoLocation -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(sosNoLocationMessage) }
                viewModel.clearSosStatus()
            }
            else -> Unit
        }
    }

    if (showSosSheet) {
        SosSheet(
            isSending = uiState.sosStatus is SosStatus.Sending,
            onDismiss = { showSosSheet = false },
            onConfirm = { note -> viewModel.sendSos(note) }
        )
    }

    if (showUnpairConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.status_unpair_confirm_title),
            body = stringResource(R.string.status_unpair_confirm_body),
            confirmLabel = stringResource(R.string.status_unpair_confirm_confirm),
            dismissLabel = stringResource(R.string.status_unpair_confirm_cancel),
            isDestructive = true,
            onConfirm = {
                showUnpairConfirm = false
                viewModel.unpair(context)
            },
            onDismiss = { showUnpairConfirm = false }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { SharingHeader(isSharingActive = uiState.isSharingActive) }

            item {
                Button(
                    onClick = { showSosSheet = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.status_sos_button), style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                OutlinedButton(
                    onClick = viewModel::checkInNow,
                    enabled = uiState.checkInStatus != CheckInStatus.InProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.checkInStatus == CheckInStatus.InProgress) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text(stringResource(R.string.status_checkin_button))
                }
            }

            item {
                GuardiansSection(
                    isLoading = uiState.isLoadingGuardians,
                    error = uiState.guardiansError,
                    guardians = uiState.guardians.map { it.displayName }
                )
            }

            item {
                PermissionsSection(
                    foregroundGranted = uiState.snapshot.foregroundLocationGranted,
                    backgroundGranted = uiState.snapshot.backgroundLocationGranted,
                    notificationsGranted = uiState.snapshot.notificationsGranted,
                    usageAccessGranted = uiState.snapshot.usageAccessGranted,
                    onManagePermissions = onManagePermissions
                )
            }

            item {
                TextButton(
                    onClick = { showUnpairConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.status_unpair_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SharingHeader(isSharingActive: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                imageVector = if (isSharingActive) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (isSharingActive) {
                    stringResource(R.string.status_sharing_on)
                } else {
                    stringResource(R.string.status_sharing_off)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.status_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun GuardiansSection(isLoading: Boolean, error: Boolean, guardians: List<String>) {
    Column {
        Text(stringResource(R.string.status_guardians_heading), style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when {
                    isLoading -> Text(
                        stringResource(R.string.status_loading_family_info),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    error -> Text(
                        stringResource(R.string.status_family_info_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    guardians.isEmpty() -> Text(
                        stringResource(R.string.status_guardians_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    else -> guardians.forEach { name ->
                        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsSection(
    foregroundGranted: Boolean,
    backgroundGranted: Boolean,
    notificationsGranted: Boolean,
    usageAccessGranted: Boolean,
    onManagePermissions: () -> Unit
) {
    Column {
        Text(stringResource(R.string.status_permissions_heading), style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val grantedLabel = stringResource(R.string.status_permission_granted)
                val notGrantedLabel = stringResource(R.string.status_permission_not_granted)
                PermissionStatusRow(
                    label = stringResource(R.string.status_permission_foreground_location),
                    granted = foregroundGranted,
                    grantedLabel = grantedLabel,
                    notGrantedLabel = notGrantedLabel
                )
                PermissionStatusRow(
                    label = stringResource(R.string.status_permission_background_location),
                    granted = backgroundGranted,
                    grantedLabel = grantedLabel,
                    notGrantedLabel = notGrantedLabel
                )
                PermissionStatusRow(
                    label = stringResource(R.string.status_permission_notifications),
                    granted = notificationsGranted,
                    grantedLabel = grantedLabel,
                    notGrantedLabel = notGrantedLabel
                )
                PermissionStatusRow(
                    label = stringResource(R.string.status_permission_usage_access),
                    granted = usageAccessGranted,
                    grantedLabel = grantedLabel,
                    notGrantedLabel = notGrantedLabel
                )
            }
        }
        OutlinedButton(
            onClick = onManagePermissions,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.status_manage_permissions_button))
        }
    }
}

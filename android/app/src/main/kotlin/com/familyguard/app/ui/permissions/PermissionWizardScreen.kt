package com.familyguard.app.ui.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.familyguard.app.domain.model.AppPermission
import com.familyguard.app.ui.common.GenericViewModelFactory

@Composable
fun PermissionWizardScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PermissionWizardViewModel = viewModel(
        factory = GenericViewModelFactory {
            PermissionWizardViewModel(context, ServiceLocator.secureStorage)
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh live permission state whenever the wizard (re)gains the foreground, e.g. after
    // the user comes back from the Usage Access Settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSnapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onFinished()
    }
    if (uiState.completed) return

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSnapshot(context)
        viewModel.advance()
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSnapshot(context)
        viewModel.advance()
    }

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSnapshot(context)
        viewModel.advance()
    }

    val usageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSnapshot(context)
        // Don't auto-advance: let the child see whether it took effect, then tap Continue.
    }

    // Background location can only be meaningfully requested once foreground location is
    // already granted; if it wasn't, this step is a no-op and we move straight past it.
    LaunchedEffect(uiState.currentStep, uiState.snapshot.foregroundLocationGranted) {
        if (uiState.currentStep == AppPermission.BACKGROUND_LOCATION &&
            !uiState.snapshot.foregroundLocationGranted
        ) {
            viewModel.advance()
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { uiState.stepNumber.toFloat() / uiState.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.wizard_step_of, uiState.stepNumber, uiState.totalSteps),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp)
            )

            when (uiState.currentStep) {
                AppPermission.FOREGROUND_LOCATION -> ForegroundLocationStep(
                    onAllow = {
                        foregroundLocationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onSkip = viewModel::advance
                )

                AppPermission.BACKGROUND_LOCATION -> BackgroundLocationStep(
                    onAllow = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            viewModel.advance()
                        }
                    },
                    onSkip = viewModel::advance
                )

                AppPermission.NOTIFICATIONS -> NotificationsStep(
                    onAllow = { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onSkip = viewModel::advance
                )

                AppPermission.USAGE_ACCESS -> UsageAccessStep(
                    granted = uiState.snapshot.usageAccessGranted,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            intent.putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                        }
                        usageAccessLauncher.launch(intent)
                    },
                    onContinue = viewModel::advance
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun StepScaffold(
    icon: ImageVector,
    title: String,
    body: String,
    isLastStep: Boolean,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
            extraContent?.invoke()
        }
        Column {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wizard_allow_button))
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    if (isLastStep) stringResource(R.string.wizard_finish_button)
                    else stringResource(R.string.wizard_skip_button)
                )
            }
        }
    }
}

@Composable
private fun ForegroundLocationStep(onAllow: () -> Unit, onSkip: () -> Unit) {
    StepScaffold(
        icon = Icons.Filled.LocationOn,
        title = stringResource(R.string.wizard_foreground_location_title),
        body = stringResource(R.string.wizard_foreground_location_body),
        isLastStep = false,
        onAllow = onAllow,
        onSkip = onSkip
    )
}

@Composable
private fun BackgroundLocationStep(onAllow: () -> Unit, onSkip: () -> Unit) {
    StepScaffold(
        icon = Icons.Filled.LocationOn,
        title = stringResource(R.string.wizard_background_location_title),
        body = stringResource(R.string.wizard_background_location_body),
        isLastStep = false,
        onAllow = onAllow,
        onSkip = onSkip,
        extraContent = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.wizard_background_location_disclosure),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    )
}

@Composable
private fun NotificationsStep(onAllow: () -> Unit, onSkip: () -> Unit) {
    StepScaffold(
        icon = Icons.Filled.Notifications,
        title = stringResource(R.string.wizard_notifications_title),
        body = stringResource(R.string.wizard_notifications_body),
        isLastStep = false,
        onAllow = onAllow,
        onSkip = onSkip
    )
}

@Composable
private fun UsageAccessStep(granted: Boolean, onOpenSettings: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Filled.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.wizard_usage_access_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.wizard_usage_access_body), style = MaterialTheme.typography.bodyLarge)

            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (granted) {
                            stringResource(R.string.wizard_usage_access_granted)
                        } else {
                            stringResource(R.string.wizard_usage_access_not_granted)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        Column {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wizard_usage_access_open_settings))
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.wizard_finish_button))
            }
        }
    }
}

package com.familyguard.app.ui.disclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familyguard.app.R
import com.familyguard.app.ServiceLocator
import com.familyguard.app.ui.common.GenericViewModelFactory

@Composable
fun DisclosureScreen(onAccepted: () -> Unit) {
    val viewModel: DisclosureViewModel = viewModel(
        factory = GenericViewModelFactory { DisclosureViewModel(ServiceLocator.secureStorage) }
    )

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(stringResource(R.string.disclosure_title), style = MaterialTheme.typography.headlineMedium)
                }
                item {
                    Text(stringResource(R.string.disclosure_intro), style = MaterialTheme.typography.bodyLarge)
                }
                item {
                    SectionCard(
                        title = stringResource(R.string.disclosure_shared_heading),
                        icon = Icons.Filled.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        bullets = listOf(
                            stringResource(R.string.disclosure_shared_location),
                            stringResource(R.string.disclosure_shared_usage),
                            stringResource(R.string.disclosure_shared_battery)
                        )
                    )
                }
                item {
                    Text(
                        stringResource(R.string.disclosure_with_whom_heading),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.disclosure_with_whom_body),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                item {
                    SectionCard(
                        title = stringResource(R.string.disclosure_never_heading),
                        icon = Icons.Filled.Block,
                        iconTint = MaterialTheme.colorScheme.error,
                        bullets = listOf(
                            stringResource(R.string.disclosure_never_messages),
                            stringResource(R.string.disclosure_never_calls),
                            stringResource(R.string.disclosure_never_keylogging),
                            stringResource(R.string.disclosure_never_hidden)
                        )
                    )
                }
                item {
                    Text(
                        stringResource(R.string.disclosure_footer_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(tonalElevation = 4.dp) {
                Button(
                    onClick = {
                        viewModel.acceptDisclosure()
                        onAccepted()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.disclosure_accept_button))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    bullets: List<String>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            bullets.forEach { bullet ->
                Box {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(top = 2.dp, end = 10.dp)
                        )
                        Text(bullet, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

package com.familyguard.app.ui.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familyguard.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosSheet(
    isSending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(stringResource(R.string.sos_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.sos_sheet_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.sos_sheet_note_label)) },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(note) },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
                Text(stringResource(R.string.sos_sheet_confirm))
            }
            TextButton(
                onClick = onDismiss,
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(stringResource(R.string.sos_sheet_cancel))
            }
        }
    }
}

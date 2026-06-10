package com.openlauncher.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Color(0xFFFF5252))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFAAAAAA))
            }
        },
        // The dialog surface is always dark, so pin light content colors —
        // theme defaults would render near-black text here in day mode
        containerColor    = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor  = Color(0xFFCCCCCC)
    )
}

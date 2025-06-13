package com.arny.allfy.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun Dialog(
    title: String,
    message: String,
    confirmText: String = "Yes",
    dismissText: String = "Cancel",
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        },
        confirmButton = {
            if (confirmText.isNotEmpty() && onConfirm != null) {
                TextButton(
                    onClick = { onConfirm() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = confirmText,
                        color = Color(0xFFED4956),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            if (dismissText.isNotEmpty() && onDismiss != null) {
                TextButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dismissText,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(dismissOnClickOutside = true)
    )
}
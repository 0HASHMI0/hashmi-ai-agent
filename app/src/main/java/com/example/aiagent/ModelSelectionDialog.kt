package com.example.aiagent

import androidx.compose.foundation.layout.*
import com.example.aiagent.ModelSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModelSelectionDialog(
    models: List<ModelSource>,
    onDismiss: () -> Unit,
    onModelSelected: (ModelSource) -> Unit,
    onDownloadRequest: (String, String) -> Unit
) {
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select AI Model") },
        text = {
            Column {
                models.forEach { model ->
                    Button(
                        onClick = { onModelSelected(model) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(model.displayName)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Column {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDownloadDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Download New Model")
                }
            }
        }
    )

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download Model") },
            text = {
                Column {
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("Model URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Save As") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (downloadUrl.isNotBlank() && modelName.isNotBlank()) {
                            onDownloadRequest(downloadUrl, modelName)
                            showDownloadDialog = false
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                Button(onClick = { showDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

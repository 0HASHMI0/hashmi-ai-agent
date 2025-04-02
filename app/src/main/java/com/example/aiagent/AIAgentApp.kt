package com.example.aiagent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAgentApp(
    messages: List<Message>,
    modelOptions: List<ModelSource>,
    currentModel: ModelSource?,
    onModelSelected: (ModelSource) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agent") },
                actions = {
                    IconButton(onClick = { showModelSelector = true }) {
                        Icon(Icons.Default.Settings, "Select Model")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Message list
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(items = messages, key = { it.text }) { message ->
                    MessageBubble(message)
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }

        if (showModelSelector) {
            AlertDialog(
                onDismissRequest = { showModelSelector = false },
                title = { Text("Select Model") },
                text = {
                    Column {
                        modelOptions.forEach { model ->
                            ListItem(
                                headlineContent = { Text(model.displayName) },
                                trailingContent = {
                                    if (currentModel == model) {
                                        Icon(Icons.Default.Check, "Selected")
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onModelSelected(model)
                                    showModelSelector = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showModelSelector = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showSettings) {
            SettingsPanel(
                features = listOf("Dark Mode", "Notifications", "Analytics"),
                getState = { false },
                onToggle = { },
                onDismiss = { showSettings = false },
                onAppNameChange = { }
            )
        }
    }
}
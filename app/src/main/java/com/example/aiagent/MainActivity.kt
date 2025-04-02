package com.example.aiagent

import android.content.*
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import okhttp3.Request

/**
 * Main application activity handling UI interactions and model execution.
 * Manages:
 * - User interface state
 * - Model selection
 * - Input/output handling
 * - Preference management
 *
 * Example usage:
 * ```
 * // In AndroidManifest.xml:
 * <activity android:name=".MainActivity" android:exported="true">
 * ```
 */
class MainActivity : AppCompatActivity() {
    private val messages = mutableStateListOf<Message>()
    private val modelOptions = mutableStateListOf(
        ModelSource.HuggingFace("TheBloke/Llama-2-7B-Chat-GGML", "llama-2-7b-chat.ggmlv3.q4_0.bin"),
        ModelSource.LocalFile("/sdcard/Models/custom_model.tflite"),
        ModelSource.Asset("default_model.tflite")
    )
    private var currentModel: ModelSource? = null
    private val localModelManager = LocalModelManager(this)
    private lateinit var aiModelManager: AIModelManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aiModelManager = AIModelManager(this)
        refreshLocalModels()
        setContent {
            AIAgentApp(
                messages = messages,
                modelOptions = modelOptions,
                currentModel = currentModel,
                onModelSelected = { model ->
                    currentModel = model
                    scope.launch { loadModel(model) }
                },
                onSendMessage = { message ->
                    messages.add(Message(message, true))
                    sendMessageToService(message)
                }
            )
        }
        checkAccessibilityPermission()
    }

    private suspend fun loadModel(model: ModelSource) {
        try {
            messages.add(Message("Loading model: ${model.displayName}...", false))
            aiModelManager.loadModel(model)
            currentModel = model
            messages.add(Message("Successfully loaded model", false))
        } catch (e: IOException) {
            messages.add(Message("Failed to load model: ${e.message}", false))
        }
    }

    private fun refreshLocalModels() {
        scope.launch {
            val localModels = localModelManager.getAvailableModels()
                .map { ModelSource.LocalFile(it) }
            modelOptions.removeAll { it is ModelSource.LocalFile }
            modelOptions.addAll(localModels)
        }
    }

    private fun downloadAndAddModel(url: String, name: String) {
        scope.launch {
            try {
                messages.add(Message("Downloading model $name...", false))
                val response = aiModelManager.client.newCall(
                    Request.Builder().url(url).build()
                ).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code}")
                }

                val modelData = response.body?.bytes() ?: throw IOException("Empty response")
                localModelManager.saveModel(name, modelData)
                refreshLocalModels()
                messages.add(Message("Model $name downloaded successfully", false))
            } catch (e: Exception) {
                messages.add(Message("Download failed: ${e.message}", false))
            }
        }
    }

    private val responseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_AI_RESPONSE) {
                val response = intent.getStringExtra(EXTRA_AI_RESPONSE)
                response?.let {
                    messages.add(Message(it, false))
                }
            }
        }
    }

    private fun checkAccessibilityPermission() {
        val service = ComponentName(this, AIService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(service.flattenToString()) == true

        if (!enabled) {
            ContextCompat.startActivity(
                this,
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                null
            )
        }
    }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_WAKE_WORD_DETECTED) {
                messages.add(Message("Listening...", false))
            }
        }
    }

    companion object {
        const val ACTION_WAKE_WORD_DETECTED = "com.example.aiagent.WAKE_WORD_DETECTED"
        const val ACTION_AI_RESPONSE = "com.example.aiagent.AI_RESPONSE"
        const val EXTRA_AI_RESPONSE = "ai_response"
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            responseReceiver,
            IntentFilter(ACTION_AI_RESPONSE)
        )
        registerReceiver(
            wakeWordReceiver,
            IntentFilter(ACTION_WAKE_WORD_DETECTED)
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(responseReceiver)
        unregisterReceiver(wakeWordReceiver)
    }

    private fun sendMessageToService(message: String) {
        val intent = Intent(this, AIService::class.java).apply {
            action = ACTION_USER_MESSAGE
            putExtra(EXTRA_USER_MESSAGE, message)
        }
        startService(intent)
    }
}

/**
 * Composable for application settings dialog.
 *
 * @param features List of available features to toggle
 * @param getState Function to get current feature state
 * @param onToggle Callback when feature is toggled
 * @param onDismiss Callback when dialog is dismissed
 * @param onAppNameChange Callback when app name changes
 */
@Composable
fun SettingsPanel(
    features: List<String>,
    getState: (String) -> Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onAppNameChange: (String) -> Unit
) {
    var showDevSettings by remember { mutableStateOf(false) }
    var appName by remember { mutableStateOf("AI Agent") }
    val scope = rememberCoroutineScope()
    val userPrefs = UserPreferencesManager(LocalContext.current)

    LaunchedEffect(Unit) {
        appName = userPrefs.getAppName()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Phone Controls") },
        text = {
            Column {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(feature)
                        Switch(
                            checked = getState(feature),
                            onCheckedChange = { onToggle(feature) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDevSettings = !showDevSettings },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Developer Options")
                }
            }
        }
    )
}

/**
 * Composable for displaying chat messages.
 *
 * @param message The message to display with text and user flag
 */
@Composable
fun MessageBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Chat message data class.
 *
 * @property text The message content
 * @property isUser Whether the message is from the user (true) or AI (false)
 */
data class Message(val text: String, val isUser: Boolean)


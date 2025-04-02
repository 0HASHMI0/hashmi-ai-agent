package com.example.aiagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import okhttp3.Request
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AIService : AccessibilityService() {
    private lateinit var aiModel: AIModelManager
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Configure accessibility service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info

        // Initialize AI model with default asset model
        aiModel = AIModelManager(this)
        scope.launch {
            try {
                aiModel.loadModel(com.example.aiagent.ModelSource.Asset("default_model.tflite"))
            } catch (e: Exception) {
                // Fallback to local model if available
                val localModel = try {
                    UserPreferencesManager(this@AIService)
                        .getSelectedModel()?.let { com.example.aiagent.ModelSource.LocalFile(it) }
                        ?: com.example.aiagent.ModelSource.HuggingFace("google/gemma-2b-it", "model.tflite")
                } catch (e: Exception) {
                    com.example.aiagent.ModelSource.HuggingFace("google/gemma-2b-it", "model.tflite")
                }
                
                try {
                    aiModel.loadModel(localModel)
                } catch (e: Exception) {
                    // Handle model loading error
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            scope.launch {
                processEvent(it)
            }
        }
    }

    private suspend fun processEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: return
        
        when {
            packageName.contains("whatsapp") -> handleWhatsApp(event, rootNode)
            packageName.contains("wechat") -> handleWeChat(event, rootNode)
            else -> handleGenericApp(event, rootNode)
        }
    }

    private suspend fun handleWhatsApp(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        handleMessagingApp(rootNode, 
            "com.whatsapp:id/entry", 
            "com.whatsapp:id/send")
    }

    private suspend fun handleWeChat(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        handleMessagingApp(rootNode,
            "com.tencent.mm:id/chatting_content_et",
            "com.tencent.mm:id/anv")
    }

    private suspend fun handleMessagingApp(
        rootNode: AccessibilityNodeInfo,
        inputFieldId: String,
        sendButtonId: String
    ) {
        try {
            val chatText = extractChatText(rootNode, inputFieldId)
            val response = aiModel.generateResponse(chatText)
            sendMessage(rootNode, inputFieldId, sendButtonId, response)
        } catch (e: Exception) {
            // Log error
        } finally {
            rootNode.recycle()
        }
    }

    private fun getWhatsAppChatText(node: AccessibilityNodeInfo): String {
        // Implementation for extracting WhatsApp chat text
        return extractChatText(node, "com.whatsapp:id/entry")
    }

    private fun getWeChatChatText(node: AccessibilityNodeInfo): String {
        // Implementation for extracting WeChat chat text
        return extractChatText(node, "com.tencent.mm:id/chatting_content_et")
    }

    private fun extractChatText(node: AccessibilityNodeInfo, inputFieldId: String): String {
        val inputField = node.findAccessibilityNodeInfosByViewId(inputFieldId)?.firstOrNull()
        return inputField?.text?.toString() ?: ""
    }

    private fun sendMessage(
        node: AccessibilityNodeInfo,
        inputFieldId: String,
        sendButtonId: String,
        message: String
    ) {
        try {
            val inputField = node.findAccessibilityNodeInfosByViewId(inputFieldId)?.firstOrNull()
            val sendButton = node.findAccessibilityNodeInfosByViewId(sendButtonId)?.firstOrNull()
            
            inputField?.text = message
            sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } finally {
            node.recycle()
        }
    }

    private suspend fun handleGenericApp(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        val screenText = getTextFromNode(rootNode)
        try {
            val response = aiModel.generateResponse(screenText)
            performAction(response, rootNode)
        } catch (e: Exception) {
            // Handle inference error
        }
    }

    private fun getTextFromNode(node: AccessibilityNodeInfo): String {
        val textBuilder = StringBuilder()
        extractTextFromNode(node, textBuilder)
        return textBuilder.toString()
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append("\n") }
        for (i in 0 until node.childCount) {
            extractTextFromNode(node.getChild(i), builder)
        }
    }

    private val musicController by lazy { MusicController(this) }
    private val multimodalProcessor by lazy { MultimodalProcessor(this) }
    private val userPrefs by lazy { UserPreferencesManager(this) }
    private val webBrowser by lazy { WebBrowser(this) }
    private val webScraper by lazy { WebScraper(this) }
    private val excelGenerator by lazy { ExcelGenerator(this) }
    private val phoneController by lazy { PhoneController(this) }

    private suspend fun performAction(response: String, rootNode: AccessibilityNodeInfo) {
        try {
            userPrefs.learnBehavior("Action performed: $response")
            
            when {
                response.startsWith("play music:") -> musicController.play(
                    Uri.parse(response.removePrefix("play music:")))
                response == "pause music" -> musicController.pause()
                response == "resume music" -> musicController.resume()
                response.startsWith("volume:") -> musicController.setVolume(
                    response.removePrefix("volume:").toFloat())
                response.startsWith("browse:") -> webBrowser.openUrl(
                    response.removePrefix("browse:"))
                response.startsWith("toggle ") -> phoneController.toggleFeature(
                    response.removePrefix("toggle "))
                response.startsWith("process image:") -> processImageCommand(
                    response.removePrefix("process image:"), rootNode)
                response.startsWith("scrape:") -> scrapeCommand(
                    response.removePrefix("scrape:"), rootNode)
                response.startsWith("create excel:") -> excelCommand(
                    response.removePrefix("create excel:"), rootNode)
                response.startsWith("http") -> webBrowser.openUrl(response)
                else -> rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = response
            }
        } finally {
            rootNode.recycle()
        }
    }

    private suspend fun processImageCommand(path: String, rootNode: AccessibilityNodeInfo) {
        val imageBytes = File(path).readBytes()
        val result = multimodalProcessor.processImage(imageBytes)
        rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = result
    }

    private suspend fun scrapeCommand(url: String, rootNode: AccessibilityNodeInfo) {
        val content = webScraper.scrape(url)
        rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = content
    }

    private suspend fun excelCommand(command: String, rootNode: AccessibilityNodeInfo) {
        val parts = command.split("|")
        val fileName = parts[0]
        val data = parts.drop(1).map { it.split(",") }
        val file = excelGenerator.createExcelFile(data, fileName)
        rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = "Excel created: ${file.absolutePath}"
    }

    override fun onInterrupt() {
        aiModel.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_USER_MESSAGE) {
                val message = it.getStringExtra(EXTRA_USER_MESSAGE)
                message?.let { msg ->
                    scope.launch {
                        processUserMessage(msg)
                    }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun processUserMessage(message: String) {
        try {
            val response = aiModel.generateResponse(message)
            sendResponseToActivity(response)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun sendResponseToActivity(response: String) {
        val intent = Intent(ACTION_AI_RESPONSE).apply {
            putExtra(EXTRA_AI_RESPONSE, response)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        aiModel.close()
    }
}

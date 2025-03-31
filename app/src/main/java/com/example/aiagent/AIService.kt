package com.example.aiagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
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

        // Initialize AI model
        aiModel = AIModelManager(this)
        scope.launch {
            try {
                aiModel.loadModel()
            } catch (e: Exception) {
                // Handle model loading error
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
        val chatText = getWhatsAppChatText(rootNode)
        val response = aiModel.generateResponse(chatText)
        sendWhatsAppMessage(response, rootNode)
    }

    private suspend fun handleWeChat(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        val chatText = getWeChatChatText(rootNode)
        val response = aiModel.generateResponse(chatText)
        sendWeChatMessage(response, rootNode)
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

    private fun sendWhatsAppMessage(message: String, node: AccessibilityNodeInfo) {
        val sendButton = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")?.firstOrNull()
        sendButton?.let {
            val inputField = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")?.first()
            inputField?.text = message
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun sendWeChatMessage(message: String, node: AccessibilityNodeInfo) {
        val sendButton = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/anv")?.firstOrNull()
        sendButton?.let {
            val inputField = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/chatting_content_et")?.first()
            inputField?.text = message
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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

    private suspend fun performAction(response: String, rootNode: AccessibilityNodeInfo) {
        // Learn from this interaction
        userPrefs.learnBehavior("Action performed: $response")
        
        when {
            response.startsWith("play music:") -> {
                val uri = Uri.parse(response.removePrefix("play music:"))
                musicController.play(uri)
            }
            response.startsWith("browse:") -> {
                val url = response.removePrefix("browse:")
                webBrowser.openUrl(url)
            }
            response.startsWith("toggle ") -> {
                val feature = response.removePrefix("toggle ")
                phoneController.toggleFeature(feature)
            }
            response.startsWith("process image:") -> {
                val imagePath = response.removePrefix("process image:")
                val imageBytes = File(imagePath).readBytes()
                val result = multimodalProcessor.processImage(imageBytes)
                rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = result
            }
            response.startsWith("scrape:") -> {
                val url = response.removePrefix("scrape:")
                val content = webScraper.scrape(url)
                rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = content
            }
            response.startsWith("create excel:") -> {
                val parts = response.removePrefix("create excel:").split("|")
                val fileName = parts[0]
                val data = parts.drop(1).map { it.split(",") }
                val file = excelGenerator.createExcelFile(data, fileName)
                rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.text = "Excel created: ${file.absolutePath}"
            }
            else -> {
                // Default action for unrecognized commands
                if (response.startsWith("http")) {
                    webBrowser.openUrl(response)
                }
            }
        }
    }
            response == "pause music" -> {
                musicController.pause()
            }
            response == "resume music" -> {
                musicController.resume()
            }
            response.startsWith("volume:") -> {
                val level = response.removePrefix("volume:").toFloat()
                musicController.setVolume(level)
            }
            else -> {
                // Default action - type text if possible
                rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { input ->
                    input.text = response
                }
            }
        }
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

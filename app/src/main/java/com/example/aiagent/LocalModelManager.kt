package com.example.aiagent

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LocalModelManager(private val context: Context) {
    private val modelsDir = File(context.getExternalFilesDir(null), "models")

    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }

    fun getAvailableModels(): List<String> {
        return modelsDir.list()?.toList() ?: emptyList()
    }

    fun saveModel(name: String, modelData: ByteArray): Boolean {
        return try {
            val modelFile = File(modelsDir, name)
            FileOutputStream(modelFile).use { output ->
                output.write(modelData)
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    fun deleteModel(name: String): Boolean {
        return try {
            File(modelsDir, name).delete()
        } catch (e: SecurityException) {
            false
        }
    }

    fun getModelPath(name: String): String? {
        val file = File(modelsDir, name)
        return if (file.exists()) file.absolutePath else null
    }

    fun getModelSize(name: String): Long {
        return File(modelsDir, name).length()
    }

    fun hasModel(name: String): Boolean {
        return File(modelsDir, name).exists()
    }
}

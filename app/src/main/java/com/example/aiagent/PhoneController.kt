package com.example.aiagent

import android.content.Context
import android.provider.Settings
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.media.AudioManager

class PhoneController(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun toggleFeature(feature: String) {
        when (feature.lowercase()) {
            "wifi" -> wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            "bluetooth" -> {
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            }
            "airplane" -> {
                val isEnabled = Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON
                ) != 0
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    if (!isEnabled) 1 else 0
                )
            }
            "silent" -> {
                audioManager.ringerMode = when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
                    else -> AudioManager.RINGER_MODE_SILENT
                }
            }
        }
    }

    fun getFeatureState(feature: String): Boolean {
        return when (feature.lowercase()) {
            "wifi" -> wifiManager.isWifiEnabled
            "bluetooth" -> bluetoothAdapter.isEnabled
            "airplane" -> Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON
            ) != 0
            "silent" -> audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
            else -> false
        }
    }
}

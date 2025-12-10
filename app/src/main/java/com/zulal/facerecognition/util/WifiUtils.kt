package com.zulal.facerecognition.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager

@SuppressLint("MissingPermission")
fun getCurrentSsid(context: Context): String? {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null

    val info = wifiManager.connectionInfo ?: return null
    val ssid = info.ssid ?: return null

    if (ssid == "<unknown ssid>") return null

    return ssid.replace("\"", "")
}

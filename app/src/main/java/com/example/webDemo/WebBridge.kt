package com.example.webDemo

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

/**
 * JavaScript interface exposed to the WebView as `window.Android`.
 * Receives JSON messages from JS via `postMessage` and dispatches them.
 */
class WebBridge(
    private val context: Context,
    private val webViewProvider: () -> WebView?
) {
    companion object {
        private const val TAG = "WebBridge"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // ---- JS → Native entry point ----

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val action = obj.getString("action")
            val payload = obj.optJSONObject("payload") ?: JSONObject()
            Log.d(TAG, "postMessage action=$action payload=$payload")
            when (action) {
                "haptic" -> handleHaptic(payload)
                "ready"  -> handleReady()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JS message: $json", e)
        }
    }

    // ---- Native → JS helper ----

    fun sendToWeb(event: String, data: JSONObject) {
        val script = "window.bridgeReceive(${JSONObject().apply {
            put("event", event)
            put("data", data)
        }})"
        mainHandler.post {
            webViewProvider()?.evaluateJavascript(script, null)
        }
    }

    // ---- Action handlers ----

    private fun handleHaptic(payload: JSONObject) {
        val type = payload.optString("type", "light")
        val (duration, amplitude) = when (type) {
            "heavy"  -> 50L to 255
            "medium" -> 30L to 128
            else     -> 15L to 64   // light
        }

        if (!vibrator.hasVibrator()) {
            Log.w(TAG, "Haptic ignored: device reports no vibrator")
            return
        }

        try {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude)
            )
            Log.d(TAG, "Vibration triggered type=$type duration=${duration}ms amplitude=$amplitude")
        } catch (e: SecurityException) {
            Log.e(TAG, "Vibration blocked by permission/security policy", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected vibration failure", e)
        }
    }

    private fun handleReady() {
        // Web page is ready – no-op for now, could send initial state
    }
}


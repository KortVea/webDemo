package com.example.webDemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.webDemo.ui.theme.WebDemoTheme
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            WebDemoTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = backgroundColor
                ) { innerPadding ->
                    // innerPadding intentionally unused — WebView goes full-screen edge-to-edge
                    innerPadding
                    val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val state = rememberWebViewState("file:///android_asset/index3.html")

                    // Hold a reference to the underlying WebView
                    val webViewRef = remember { arrayOfNulls<WebView>(1) }

                    // Create the bridge
                    val bridge : WebBridge = remember {
                        WebBridge(context) { webViewRef[0] }
                    }

                    // Create the shake detector that pushes events to the web page
                    val shakeDetector = remember {
                        ShakeDetector(context) { axis, intensity ->
                            bridge.sendToWeb(
                                "shake",
                                JSONObject().apply {
                                    put("axis", axis)
                                    put("intensity", intensity)
                                }
                            )
                        }
                    }

                    // Start / stop shake detection with the lifecycle
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> shakeDetector.start()
                                Lifecycle.Event.ON_PAUSE  -> shakeDetector.stop()
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            shakeDetector.stop()
                        }
                    }

                    WebView(
                        state = state,
                        modifier = Modifier
                            .fillMaxSize(),
                        onCreated = { webView ->
                            webView.settings.javaScriptEnabled = true
                            webView.addJavascriptInterface(bridge, "NativeBridge")
                            webViewRef[0] = webView
                        }
                    )
                }
            }
        }
    }
}

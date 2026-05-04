package com.example.webDemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.webDemo.ui.theme.WebDemoTheme
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state = rememberWebViewState("file:///android_asset/index.html")
                    WebView(
                        state = state,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onCreated = { webView ->
                            webView.settings.javaScriptEnabled = true
                        }
                    )
                }
            }
        }
    }
}

package com.volleygames.mwp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// WebViewScreen.ktd
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(modifier: Modifier = Modifier, onWebViewCreated: (WebView) -> Unit = {}) {
  var webView: WebView? by remember { mutableStateOf(null) }

  AndroidView(
      modifier = modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          settings.javaScriptEnabled = true

          // Configure WebViewClient
          webViewClient =
              object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  // Inject JavaScript after page loads
                  evaluateJavascript("javascript:updateFromAndroid('Hello from Compose!')", null)
                }
              }

          // Add JavaScript Interface
          addJavascriptInterface(WebAppInterface(context), "Android")

          // Load content
          loadUrl("https://a75b-98-45-207-226.ngrok-free.app")
          // Or load from web:
          // loadUrl("https://example.com")

          webView = this
          onWebViewCreated(this)
        }
      },
      update = { view -> webView = view })
}

// WebAppInterface.kt
class WebAppInterface(
    private val context: Context,
    private val onMessageReceived: ((String) -> Unit)? = null
) {
  @JavascriptInterface
  fun showToast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JavascriptInterface
  fun sendMessageToAndroid(message: String) {
    // Handle message from JavaScript
    onMessageReceived?.invoke(message)
  }
}

// MainActivity.kt
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      var webViewMessage by remember { mutableStateOf<String?>(null) }
      var webViewInstance by remember { mutableStateOf<WebView?>(null) }
      val context = LocalContext.current
      Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // Optional: Add controls above WebView
          webViewMessage?.let { message ->
            Text(text = "Message from WebView: $message", modifier = Modifier.padding(16.dp))
          }

          // Optional: Add button to send message to WebView
          Button(
              onClick = {
                webViewInstance?.evaluateJavascript(
                    "javascript:updateFromAndroid('Hello from Compose Button!')", null)
              },
              modifier = Modifier.padding(16.dp)) {
                Text("Send Message to WebView")
              }

          // WebView
          WebViewScreen(
              modifier = Modifier.weight(1f),
              onWebViewCreated = { webView ->
                webViewInstance = webView
                webView.addJavascriptInterface(
                    WebAppInterface(
                        context = context,
                        onMessageReceived = { message -> webViewMessage = message }),
                    "Android")
              })
        }
      }
    }
  }
}

// State management (optional)
@Stable
class WebViewState {
  var webView: WebView? by mutableStateOf(null)
    private set

  fun updateWebView(webView: WebView) {
    this.webView = webView
  }

  fun evaluateJavaScript(script: String, callback: ValueCallback<String>? = null) {
    webView?.evaluateJavascript(script, callback)
  }
}

@Composable fun rememberWebViewState() = remember { WebViewState() }

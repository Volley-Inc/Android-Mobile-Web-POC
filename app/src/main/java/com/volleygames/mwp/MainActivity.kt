package com.volleygames.mwp

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// WebViewState.kt
@Stable
class WebViewState {
  private val _webMessages = MutableSharedFlow<String>()
  val webMessages = _webMessages.asSharedFlow()

  private val _loadingState = MutableStateFlow<WebViewLoadingState>(WebViewLoadingState.Idle)
  val loadingState = _loadingState.asStateFlow()

  var webView: WebView? by mutableStateOf(null)
    private set

  fun updateWebView(webView: WebView) {
    this.webView = webView
  }

  suspend fun sendMessageToWeb(message: String) {
    webView?.evaluateJavascript("javascript:updateFromAndroid('$message')", null)
  }

  suspend fun onMessageReceived(message: String) {
    _webMessages.emit(message)
  }

  fun updateLoadingState(state: WebViewLoadingState) {
    _loadingState.value = state
  }
}

sealed class WebViewLoadingState {
  data object Idle : WebViewLoadingState()

  data object Loading : WebViewLoadingState()

  data class Error(val error: String) : WebViewLoadingState()

  data object Finished : WebViewLoadingState()
}

@Composable fun rememberWebViewState() = remember { WebViewState() }

// WebAppInterface.kt
class WebAppInterface(
    private val scope: CoroutineScope,
    private val state: WebViewState,
    private val context: Context
) {
  @JavascriptInterface
  fun showToast(message: String) {
    scope.launch(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
  }

  @JavascriptInterface
  fun sendMessageToAndroid(message: String) {
    scope.launch { state.onMessageReceived(message) }
  }
}

// WebViewScreen.kt
@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    state: WebViewState = rememberWebViewState(),
    onWebViewCreated: (WebView) -> Unit = {}
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val loadingState by state.loadingState.collectAsState()

  LaunchedEffect(Unit) {
    state.webMessages.collect { message ->
      // Handle web messages here
      Log.d("WebView", "Message from web: $message")
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          WebView(ctx).apply {
            settings.apply {
              javaScriptEnabled = true
              useWideViewPort = true
              loadWithOverviewMode = true
              setSupportZoom(false)
              builtInZoomControls = false
              displayZoomControls = false
            }

            webViewClient =
                object : WebViewClient() {
                  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    state.updateLoadingState(WebViewLoadingState.Loading)
                  }

                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    state.updateLoadingState(WebViewLoadingState.Finished)
                    coroutineScope.launch { state.sendMessageToWeb("Connected to Android!") }
                  }

                  override fun onReceivedError(
                      view: WebView?,
                      request: WebResourceRequest?,
                      error: WebResourceError?
                  ) {
                    super.onReceivedError(view, request, error)
                    state.updateLoadingState(
                        WebViewLoadingState.Error(error?.description.toString()))
                  }
                }

            addJavascriptInterface(
                WebAppInterface(scope = coroutineScope, state = state, context = context),
                "Android")

            loadUrl("https://a75b-98-45-207-226.ngrok-free.app")

            state.updateWebView(this)
            onWebViewCreated(this)
          }
        })

    // Loading indicator
    when (loadingState) {
      is WebViewLoadingState.Loading -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
      is WebViewLoadingState.Error -> {
        Text(
            text = "Error: ${(loadingState as WebViewLoadingState.Error).error}",
            color = Color.Red,
            modifier = Modifier.align(Alignment.Center).padding(16.dp))
      }
      else -> {
        /* Do nothing */
      }
    }
  }
}

// Usage in MainActivity
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val webViewState = rememberWebViewState()
      val scope = rememberCoroutineScope()

      Column(modifier = Modifier.fillMaxSize()) {
        // Button to send message to WebView
        Button(
            onClick = {
              scope.launch { webViewState.sendMessageToWeb("Hello from Android Button!") }
            },
            modifier = Modifier.padding(16.dp)) {
              Text("Send Message to Web")
            }

        // WebView with Flow-based communication
        WebViewScreen(modifier = Modifier.weight(1f), state = webViewState)
      }

      // Collect web messages
      LaunchedEffect(Unit) {
        webViewState.webMessages.collect { message ->
          // Handle messages from web
          Log.d("MainActivity", "Received message from web: $message")
        }
      }
    }
  }
}

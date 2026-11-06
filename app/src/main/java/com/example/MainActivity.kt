package com.example

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF07090E)
        ) {
          SlideshowScreen(
            onToggleFullscreen = { isFullscreen ->
              setWindowFullscreen(isFullscreen)
            }
          )
        }
      }
    }
  }

  private fun setWindowFullscreen(fullscreen: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val controller = window.insetsController ?: return
      if (fullscreen) {
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
          WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      } else {
        controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
      }
    } else {
      @Suppress("DEPRECATION")
      if (fullscreen) {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
      } else {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SlideshowScreen(
  onToggleFullscreen: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isFullscreenActive by remember { mutableStateOf(false) }

  BackHandler(enabled = true) {
    webViewRef?.let { wv ->
      // If modal or fullscreen in Web is open, press escape or go back
      wv.evaluateJavascript(
        """
        (function() {
          var modal = document.getElementById('linksModal');
          if (modal && modal.classList.contains('open')) {
            modal.classList.remove('open');
            return true;
          }
          if (document.fullscreenElement) {
            document.exitFullscreen();
            return true;
          }
          return false;
        })()
        """.trimIndent()
      ) { result ->
        if (result == "false" || result == null) {
          // If web didn't consume back event and webview has history, go back
          if (wv.canGoBack()) {
            wv.goBack()
          }
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.destroy()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF07090E))
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("slideshow_webview"),
      factory = { context ->
        WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(android.graphics.Color.parseColor("#07090E"))

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            displayZoomControls = false
            builtInZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
          }

          webViewClient = object : WebViewClient() {}

          webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
              super.onShowCustomView(view, callback)
              isFullscreenActive = true
              onToggleFullscreen(true)
            }

            override fun onHideCustomView() {
              super.onHideCustomView()
              isFullscreenActive = false
              onToggleFullscreen(false)
            }
          }

          loadUrl("file:///android_asset/slideshow.html")
          webViewRef = this
        }
      },
      update = { wv ->
        webViewRef = wv
      }
    )
  }
}

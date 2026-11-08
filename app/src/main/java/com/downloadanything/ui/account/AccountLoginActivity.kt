// 플랫폼 로그인 페이지를 WebView로 띄우고, "완료"를 누르면 쿠키를 계정 연결로 저장한다
package com.downloadanything.ui.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.downloadanything.DownloadAnythingApp
import com.downloadanything.domain.AccountCatalog
import com.downloadanything.domain.AccountCookies
import com.downloadanything.domain.AccountSpec
import com.downloadanything.domain.model.Platform
import com.downloadanything.ui.label
import com.downloadanything.ui.theme.DownloadAnythingTheme

class AccountLoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spec = intent.getStringExtra(EXTRA_PLATFORM)
            ?.let { name -> Platform.entries.firstOrNull { it.name == name } }
            ?.let(AccountCatalog::spec)
        val loginUrl = spec?.loginUrl
        if (spec == null || loginUrl == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            DownloadAnythingTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.safeDrawingPadding()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = ::finish) { Text("취소") }
                            Text(
                                "${spec.platform.label()} 로그인",
                                Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                            )
                            Button(onClick = { complete(spec) }) { Text("완료") }
                        }
                        AndroidView(
                            factory = { createWebView(it, loginUrl) },
                            modifier = Modifier.fillMaxSize(),
                            onRelease = { it.destroy() },
                        )
                    }
                }
            }
        }
    }

    private fun complete(spec: AccountSpec) {
        CookieManager.getInstance().flush()
        val text = AccountCookies.fromWebLogin(spec, WebCookies.read(spec), System.currentTimeMillis() / MILLIS_PER_SECOND)
        if (text == null) {
            Toast.makeText(this, "로그인이 확인되지 않았습니다. 로그인을 마친 뒤 완료를 눌러 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        (application as DownloadAnythingApp).accountStore.save(spec.platform, text)
        Toast.makeText(this, "${spec.platform.label()} 계정을 연결했습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context, url: String) = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // 일부 사이트는 WebView 표시("; wv")가 있으면 지원하지 않는 브라우저로 막는다.
        settings.userAgentString = settings.userAgentString.replace(WEBVIEW_UA_MARK, "")
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = object : WebViewClient() {
            // intent:// 등으로 다른 앱을 여는 이동은 막고 웹 주소만 연다.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.scheme !in WEB_SCHEMES
        }
        loadUrl(url)
    }

    companion object {
        private const val EXTRA_PLATFORM = "platform"
        private const val WEBVIEW_UA_MARK = "; wv"
        private const val MILLIS_PER_SECOND = 1000L
        private val WEB_SCHEMES = setOf("http", "https")

        fun intent(context: Context, platform: Platform): Intent =
            Intent(context, AccountLoginActivity::class.java).putExtra(EXTRA_PLATFORM, platform.name)
    }
}

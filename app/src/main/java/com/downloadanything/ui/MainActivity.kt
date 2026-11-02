// 런처로 여는 메인 화면
package com.downloadanything.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.downloadanything.DownloadAnythingApp
import com.downloadanything.ui.theme.DownloadAnythingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DownloadAnythingApp
        setContent {
            DownloadAnythingTheme {
                RequestNotificationPermission()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(viewModel, onUpdateEngine = app::requestEngineUpdate, modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

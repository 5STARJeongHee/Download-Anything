// 다른 앱의 공유 메뉴에서 링크를 받아 하단 시트로 정보 조회와 다운로드 시작을 처리한다
package com.downloadanything.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadanything.domain.UrlParser
import com.downloadanything.ui.theme.DownloadAnythingTheme
import kotlinx.coroutines.delay

class ShareActivity : ComponentActivity() {

    private val viewModel: LinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val link = UrlParser.parse(sharedText)
        // 화면 회전 등으로 다시 만들어질 때 중복 조회하지 않는다.
        if (link != null && savedInstanceState == null) viewModel.load(link)
        setContent {
            DownloadAnythingTheme {
                RequestNotificationPermission()
                ShareSheet(viewModel, hasLink = link != null, onDismiss = ::finish)
            }
        }
    }
}

private const val CLOSE_AFTER_START_MILLIS = 1_200L

@Composable
private fun ShareSheet(viewModel: LinkViewModel, hasLink: Boolean, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is LinkUiState.Started) {
            delay(CLOSE_AFTER_START_MILLIS)
            onDismiss()
        }
    }
    // 시트 바깥(어두운 영역)을 누르면 닫는다.
    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Download Anything", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (!hasLink) {
                    Text("공유된 내용에서 링크를 찾지 못했습니다.", color = MaterialTheme.colorScheme.error)
                } else {
                    LinkSheetContent(
                        state = state,
                        onDownload = viewModel::download,
                        onRetry = viewModel::retry,
                        onRetryAfterUpdate = viewModel::retryAfterEngineUpdate,
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("닫기") }
            }
        }
    }
}

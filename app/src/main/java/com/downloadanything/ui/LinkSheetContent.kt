// 링크 상태(조회 중·프리셋 선택·실패·시작됨)를 그리는 공용 화면 조각
package com.downloadanything.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.downloadanything.domain.AccountCatalog
import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.Platform

private const val THUMBNAIL_RATIO = 16f / 9f

@Composable
fun LinkSheetContent(
    state: LinkUiState,
    onDownload: (DownloadPreset) -> Unit,
    onRetry: () -> Unit,
    onRetryAfterUpdate: () -> Unit,
    onConnectAccount: (Platform) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            LinkUiState.Idle -> Unit
            is LinkUiState.Loading -> Loading(state.link.platform.label())
            is LinkUiState.Ready -> ReadyContent(state.info, state.presets, onDownload)
            is LinkUiState.Failed -> FailedContent(state, onRetry, onRetryAfterUpdate, onConnectAccount)
            is LinkUiState.Started -> Text(
                "다운로드를 시작했습니다. 진행 상황은 알림에서 볼 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Loading(platformLabel: String) {
    Text("$platformLabel 정보를 가져오는 중…", style = MaterialTheme.typography.bodyMedium)
    LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
}

@Composable
private fun ReadyContent(info: MediaInfo, presets: List<DownloadPreset>, onDownload: (DownloadPreset) -> Unit) {
    info.thumbnailUrl?.let {
        AsyncImage(
            model = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(THUMBNAIL_RATIO).clip(RoundedCornerShape(16.dp)),
        )
    }
    Text(
        info.platform.label().uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
    )
    Text(
        info.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    presets.forEachIndexed { index, preset ->
        val label = preset.label(info.images.size)
        if (index == 0) {
            Button(
                onClick = { onDownload(preset) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(label, fontWeight = FontWeight.Bold) }
        } else {
            OutlinedButton(onClick = { onDownload(preset) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(label)
            }
        }
    }
}

@Composable
private fun FailedContent(
    state: LinkUiState.Failed,
    onRetry: () -> Unit,
    onRetryAfterUpdate: () -> Unit,
    onConnectAccount: (Platform) -> Unit,
) {
    val platform = state.link.platform
    val canConnect = state.reason == FailureReason.LOGIN_REQUIRED && AccountCatalog.spec(platform) != null
    Text(state.reason.userMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    Text(state.link.url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (canConnect) {
            Button(onClick = { onConnectAccount(platform) }) { Text("${platform.label()} 계정 연결") }
        }
        if (state.reason.suggestsEngineUpdate) {
            Button(onClick = onRetryAfterUpdate) { Text("엔진 업데이트 후 다시 시도") }
        }
        TextButton(onClick = onRetry) { Text("다시 시도") }
    }
}

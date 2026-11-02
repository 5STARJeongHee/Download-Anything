// 홈 화면: 링크 입력, 링크 정보·프리셋, 다운로드 목록, 엔진 상태
package com.downloadanything.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadanything.domain.UrlParser
import com.downloadanything.domain.model.EngineState

@Composable
fun HomeScreen(viewModel: LinkViewModel, onUpdateEngine: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val link = remember(input) { UrlParser.parse(input) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Download\nAnything", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("링크 붙여넣기") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { link?.let(viewModel::load) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { link?.let(viewModel::load) },
            enabled = link != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (link == null) "링크를 입력하세요" else "${link.platform.label()} 가져오기", fontWeight = FontWeight.Bold) }
        LinkSheetContent(
            state = state,
            onDownload = viewModel::download,
            onRetry = viewModel::retry,
            onRetryAfterUpdate = viewModel::retryAfterEngineUpdate,
        )
        DownloadList()
        Spacer(Modifier.height(8.dp))
        EngineRow(engineState, onUpdateEngine)
        Text(
            "개인 용도로만 사용하세요. 저작권과 각 서비스 약관을 지킬 책임은 사용자에게 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun EngineRow(state: EngineState, onUpdateEngine: () -> Unit) {
    val (text, color) = when (state) {
        EngineState.Initializing -> "엔진 준비 중…" to MaterialTheme.colorScheme.onBackground
        is EngineState.Updating -> "엔진 업데이트 중…" to MaterialTheme.colorScheme.onBackground
        is EngineState.Ready -> engineSummary(state) to
            if (state.isUpdateFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        is EngineState.Failed -> "엔진 오류: ${state.message}" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        val canUpdate = state is EngineState.Ready || state is EngineState.Failed
        TextButton(onClick = onUpdateEngine, enabled = canUpdate) { Text("엔진 업데이트") }
    }
}

private fun engineSummary(state: EngineState.Ready): String = buildString {
    append("yt-dlp ${state.version ?: "?"}")
    if (!state.isFfmpegAvailable) append(" · ffmpeg 없음(합치기·mp3 불가)")
    if (state.isUpdateFailed) append(" · 업데이트 실패")
}

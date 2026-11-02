// 홈 화면: 엔진 상태와 URL 입력
package com.downloadanything.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downloadanything.domain.UrlParser
import com.downloadanything.domain.model.EngineState

@Composable
fun HomeScreen(engineState: EngineState, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }
    val link = remember(input) { UrlParser.parse(input) }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text(
            text = "Download\nAnything",
            fontSize = 44.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
        )
        EngineStatus(engineState, Modifier.padding(top = 12.dp, bottom = 28.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("링크 붙여넣기") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        link?.let { LinkPreview(it, Modifier.padding(top = 16.dp)) }
    }
}

@Composable
private fun EngineStatus(state: EngineState, modifier: Modifier = Modifier) {
    val (text, color) = when (state) {
        EngineState.Initializing -> "엔진 준비 중…" to MaterialTheme.colorScheme.onBackground
        is EngineState.Ready -> "yt-dlp ${state.version ?: "?"}" to MaterialTheme.colorScheme.primary
        is EngineState.Failed -> "엔진 오류: ${state.message}" to MaterialTheme.colorScheme.error
    }
    Text(text, modifier = modifier, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
}

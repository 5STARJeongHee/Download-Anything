// 다른 앱의 공유 메뉴에서 링크를 받아 하단 시트로 보여준다
package com.downloadanything.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.downloadanything.domain.UrlParser
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.ui.theme.DownloadAnythingTheme

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val link = UrlParser.parse(sharedText)
        setContent {
            DownloadAnythingTheme {
                ShareSheet(link = link, onDismiss = ::finish)
            }
        }
    }
}

@Composable
private fun ShareSheet(link: ParsedLink?, onDismiss: () -> Unit) {
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
                Text("받을 링크", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (link == null) {
                    Text("공유된 내용에서 링크를 찾지 못했습니다.", color = MaterialTheme.colorScheme.error)
                } else {
                    LinkPreview(link)
                    Text("다운로드 연결은 Phase 1에서 추가됩니다.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("닫기") }
            }
        }
    }
}

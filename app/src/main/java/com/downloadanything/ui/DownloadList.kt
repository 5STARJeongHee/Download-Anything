// 진행 중·최근 다운로드 작업 목록(WorkManager 상태)을 그린다
package com.downloadanything.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.work.DownloadScheduler
import com.downloadanything.work.DownloadWorker

private const val MAX_ITEMS = 10

@Composable
fun DownloadList(modifier: Modifier = Modifier) {
    val workManager = WorkManager.getInstance(LocalContext.current)
    val flow = remember { workManager.getWorkInfosByTagFlow(DownloadScheduler.TAG) }
    val works by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    // 진행 중인 작업을 위로 올린다.
    val items = works.sortedBy { it.state.isFinished }.take(MAX_ITEMS)
    if (items.isEmpty()) return

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("다운로드", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        items.forEach { work -> DownloadRow(work, onCancel = { workManager.cancelWorkById(work.id) }) }
    }
}

@Composable
private fun DownloadRow(work: WorkInfo, onCancel: () -> Unit) {
    val data = if (work.state.isFinished) work.outputData else work.progress
    val title = data.getString(DownloadWorker.KEY_TITLE) ?: "대기 중인 작업"
    val percent = work.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
    Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!work.state.isFinished) TextButton(onClick = onCancel) { Text("취소") }
            }
            if (work.state == WorkInfo.State.RUNNING) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
            Text(statusText(work, percent), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun statusText(work: WorkInfo, percent: Int): String = when (work.state) {
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> "대기 중"
    WorkInfo.State.RUNNING -> "$percent%"
    WorkInfo.State.SUCCEEDED -> "저장 완료 · ${work.outputData.getInt(DownloadWorker.KEY_SAVED_COUNT, 0)}개"
    WorkInfo.State.FAILED -> work.outputData.getString(DownloadWorker.KEY_FAILURE)
        ?.let { name -> FailureReason.entries.firstOrNull { it.name == name }?.userMessage }
        ?: "실패"
    WorkInfo.State.CANCELLED -> "취소됨"
}

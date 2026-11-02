// 다운로드 진행·완료·실패 알림을 만든다
package com.downloadanything.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.downloadanything.data.storage.SavedMedia
import com.downloadanything.domain.model.FailureReason
import java.util.UUID

class DownloadNotifications(private val context: Context) {

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(PROGRESS_CHANNEL, "다운로드 진행", NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(RESULT_CHANNEL, "다운로드 결과", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** [percent]가 null이면 준비 중(진행률 모름)으로 표시한다. */
    fun progressInfo(workId: UUID, title: String, percent: Int?): ForegroundInfo {
        val cancel = WorkManager.getInstance(context).createCancelPendingIntent(workId)
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(percent?.let { "$it%" } ?: "준비 중")
            .setProgress(MAX_PROGRESS, percent ?: 0, percent == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "취소", cancel)
            .build()
        // API 34+에서는 타입 없는 foreground 서비스가 크래시한다.
        return ForegroundInfo(progressId(workId), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    fun showCompleted(workId: UUID, title: String, saved: List<SavedMedia>) {
        val first = saved.firstOrNull() ?: return
        val open = Intent(Intent.ACTION_VIEW)
            .setDataAndType(first.uri, first.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context, resultId(workId), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notify(
            resultId(workId),
            NotificationCompat.Builder(context, RESULT_CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(if (saved.size == 1) "저장 완료" else "${saved.size}개 저장 완료")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showFailed(workId: UUID, title: String, reason: FailureReason) {
        notify(
            resultId(workId),
            NotificationCompat.Builder(context, RESULT_CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("실패: $title")
                .setContentText(reason.userMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reason.userMessage))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!isGranted) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun progressId(workId: UUID) = workId.hashCode()
    private fun resultId(workId: UUID) = workId.hashCode() + 1

    private companion object {
        const val PROGRESS_CHANNEL = "download_progress"
        const val RESULT_CHANNEL = "download_result"
        const val MAX_PROGRESS = 100
    }
}

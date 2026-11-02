// 다운로드 한 건을 foreground 작업으로 실행하고 결과를 MediaStore에 저장한다
package com.downloadanything.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.downloadanything.BuildConfig
import com.downloadanything.DownloadAnythingApp
import com.downloadanything.data.ytdlp.EngineException
import com.downloadanything.domain.DownloadOutputs
import com.downloadanything.domain.ErrorClassifier
import com.downloadanything.domain.FormatSelector
import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadRequest
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaUnavailableException
import kotlinx.coroutines.CancellationException
import java.io.File

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val app get() = applicationContext as DownloadAnythingApp
    private val notifications = DownloadNotifications(context)

    @Volatile
    private var lastPercent = -1

    override suspend fun doWork(): Result {
        val request = DownloadRequestCodec.decode(inputData.keyValueMap) ?: return Result.failure()
        setForeground(notifications.progressInfo(id, request.title, percent = null))
        setProgress(workDataOf(KEY_TITLE to request.title, KEY_PROGRESS to 0))
        val workDir = File(applicationContext.cacheDir, "$WORK_DIR_NAME/$id")
        return try {
            workDir.mkdirs()
            val category = DownloadOutputs.categoryFor(request.preset)
            val saved = fetch(request, workDir).map { app.mediaStoreSaver.save(it, category) }
            notifications.showCompleted(id, request.title, saved)
            Result.success(workDataOf(KEY_TITLE to request.title, KEY_SAVED_COUNT to saved.size))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val reason = (e as? MediaUnavailableException)?.reason ?: ErrorClassifier.classify(e.message)
            if (BuildConfig.DEBUG) Log.e(TAG, "다운로드 실패: ${request.url}", e)
            notifications.showFailed(id, request.title, reason)
            Result.failure(workDataOf(KEY_TITLE to request.title, KEY_FAILURE to reason.name))
        } finally {
            workDir.deleteRecursively()
        }
    }

    private suspend fun fetch(request: DownloadRequest, workDir: File): List<File> {
        if (request.preset == DownloadPreset.IMAGE) {
            app.imageDownloader.download(request.images, request.title, workDir) { done, total ->
                report(request.title, done * PERCENT / total.toFloat())
            }
        } else {
            downloadWithEngine(request, workDir)
        }
        val names = workDir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
        val selected = DownloadOutputs.select(names, request.preset)
        if (selected.isEmpty()) throw MediaUnavailableException(FailureReason.NO_MEDIA, "결과 파일 없음: $names")
        return selected.map { File(workDir, it) }
    }

    private suspend fun downloadWithEngine(request: DownloadRequest, workDir: File) {
        val engine = app.awaitReadyEngine()
        val args = FormatSelector.ytDlpArgs(request.preset, engine.isFfmpegAvailable)
            ?: throw MediaUnavailableException(FailureReason.NO_MEDIA, "이 기기에서 만들 수 없는 형식: ${request.preset}")
        try {
            app.engine.download(request.url, args, workDir) { percent, _ -> report(request.title, percent) }
        } catch (e: EngineException) {
            throw MediaUnavailableException(ErrorClassifier.classify(e.message), e.message, e)
        }
    }

    // yt-dlp 진행률 콜백은 자주 오므로 정수 퍼센트가 바뀔 때만 알림을 갱신한다.
    private fun report(title: String, percent: Float) {
        val value = percent.toInt().coerceIn(0, PERCENT)
        if (value == lastPercent) return
        lastPercent = value
        setProgressAsync(workDataOf(KEY_TITLE to title, KEY_PROGRESS to value))
        setForegroundAsync(notifications.progressInfo(id, title, value))
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_PROGRESS = "progress"
        const val KEY_SAVED_COUNT = "saved_count"
        const val KEY_FAILURE = "failure"
        private const val WORK_DIR_NAME = "downloads"
        private const val PERCENT = 100
        private const val TAG = "DownloadWorker"
    }
}

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
import com.downloadanything.domain.UrlParser
import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadRequest
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.RemoteImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val app get() = applicationContext as DownloadAnythingApp
    private val notifications = DownloadNotifications(context)

    @Volatile
    private var lastPercent = -1

    @Volatile
    private var isForeground = false

    override suspend fun doWork(): Result {
        val request = DownloadRequestCodec.decode(inputData.keyValueMap) ?: return Result.failure()
        tryStartForeground(request.title)
        setProgress(workDataOf(KEY_TITLE to request.title, KEY_PROGRESS to 0))
        val workDir = File(applicationContext.cacheDir, "$WORK_DIR_NAME/$id")
        return try {
            workDir.mkdirs()
            val category = DownloadOutputs.categoryFor(request.preset)
            val files = fetch(request, workDir)
            val saved = withContext(Dispatchers.IO) { files.map { app.mediaStoreSaver.save(it, category) } }
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

    // 네트워크를 기다리던 작업이 앱이 백그라운드일 때 시작되면 Android 12+가 foreground 전환을 거부한다.
    // 그때는 알림 없이 계속 진행한다(작업 자체는 WorkManager가 유지한다).
    private suspend fun tryStartForeground(title: String) {
        try {
            setForeground(notifications.progressInfo(id, title, percent = null))
            isForeground = true
        } catch (e: IllegalStateException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "foreground 전환 거부됨", e)
        }
    }

    private suspend fun fetch(request: DownloadRequest, workDir: File): List<File> {
        when {
            request.preset == DownloadPreset.IMAGE -> downloadFiles(request.images, request.title, workDir)
            request.directVideoUrls.isNotEmpty() -> downloadDirectOrEngine(request, workDir)
            else -> downloadWithEngine(request, workDir)
        }
        val names = workDir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
        val selected = DownloadOutputs.select(names, request.preset)
        if (selected.isEmpty()) throw MediaUnavailableException(FailureReason.NO_MEDIA, "결과 파일 없음: $names")
        return selected.map { File(workDir, it) }
    }

    private suspend fun downloadFiles(files: List<RemoteImage>, title: String, workDir: File) {
        app.imageDownloader.download(files, title, workDir) { done, total -> report(title, done * PERCENT / total.toFloat()) }
    }

    // 임베드 API의 mp4 주소가 막히거나 만료되면 yt-dlp로 한 번 더 시도한다.
    private suspend fun downloadDirectOrEngine(request: DownloadRequest, workDir: File) {
        try {
            downloadFiles(request.directVideoUrls.map { RemoteImage(it, DIRECT_VIDEO_EXTENSION) }, request.title, workDir)
        } catch (e: IOException) {
            // 취소로 연결이 끊긴 경우에는 yt-dlp로 넘어가지 않는다.
            currentCoroutineContext().ensureActive()
            if (BuildConfig.DEBUG) Log.w(TAG, "mp4 직접 받기 실패, yt-dlp로 다시 시도: ${request.url}", e)
            workDir.listFiles()?.forEach { it.deleteRecursively() }
            downloadWithEngine(request, workDir)
        }
    }

    private suspend fun downloadWithEngine(request: DownloadRequest, workDir: File) {
        val engine = app.awaitReadyEngine()
        val args = FormatSelector.ytDlpArgs(request.preset, engine.isFfmpegAvailable)
            ?: throw MediaUnavailableException(FailureReason.NO_MEDIA, "이 기기에서 만들 수 없는 형식: ${request.preset}")
        try {
            val cookieArgs = app.accountStore.ytDlpArgs(UrlParser.detectPlatform(request.url))
            app.engine.download(request.url, args + cookieArgs, workDir) { percent, _ -> report(request.title, percent) }
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
        if (isForeground) setForegroundAsync(notifications.progressInfo(id, title, value))
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_PROGRESS = "progress"
        const val KEY_SAVED_COUNT = "saved_count"
        const val KEY_FAILURE = "failure"
        private const val WORK_DIR_NAME = "downloads"
        private const val DIRECT_VIDEO_EXTENSION = "mp4"
        private const val PERCENT = 100
        private const val TAG = "DownloadWorker"
    }
}

// youtubedl-android 라이브러리로 YtDlpEngine을 구현한다
package com.downloadanything.data.ytdlp

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class YoutubeDlAndroidEngine(private val context: Context) : YtDlpEngine {

    private val initMutex = Mutex()

    // 업데이트는 yt-dlp 파일을 바꾸므로 실행 중인 작업이 끝날 때까지 기다리고, 업데이트 중에는 새 실행을 막는다.
    private val updateGate = Mutex()
    private val activeRuns = AtomicInteger(0)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var ffmpegAvailable: Boolean? = null

    // execute()는 --cache-dir이 없으면 --no-cache-dir을 붙인다. YouTube 풀이 스크립트 등을 재사용하려고 지정한다.
    private val cacheDir: File get() = File(context.cacheDir, CACHE_DIR_NAME)

    override suspend fun initialize(): String? {
        ensureInitialized()
        return withContext(Dispatchers.IO) { YoutubeDL.getInstance().version(context) }
    }

    override suspend fun dumpJson(url: String, args: List<String>): String {
        val request = YoutubeDLRequest(url).addCommands(args).addOption("--dump-single-json")
        return run(request) { _, _ -> }.out
    }

    override suspend fun download(
        url: String,
        args: List<String>,
        outputDir: File,
        onProgress: (percent: Float, etaSeconds: Long) -> Unit,
    ) {
        val request = YoutubeDLRequest(url)
            .addCommands(args)
            .addOption("-o", File(outputDir, OUTPUT_TEMPLATE).absolutePath)
        run(request, onProgress)
    }

    override suspend fun update(): UpdateResult = updateGate.withLock {
        ensureInitialized()
        while (activeRuns.get() > 0) delay(RUN_POLL_MILLIS)
        withContext(Dispatchers.IO) {
            val status = try {
                YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY)
            } catch (e: YoutubeDLException) {
                throw EngineException(e.message ?: "yt-dlp 업데이트 실패", e)
            }
            UpdateResult(
                isUpdated = status == YoutubeDL.UpdateStatus.DONE,
                version = YoutubeDL.getInstance().version(context),
            )
        }
    }

    override suspend fun isFfmpegAvailable(): Boolean {
        ffmpegAvailable?.let { return it }
        ensureInitialized()
        val result = withContext(Dispatchers.IO) { checkFfmpeg() }
        ffmpegAvailable = result
        return result
    }

    // youtubedl-android가 ffmpeg를 실행할 때와 같은 라이브러리 경로로 `-version`을 돌려 본다.
    private fun checkFfmpeg(): Boolean = runCatching {
        val packages = File(context.noBackupFilesDir, PACKAGES_DIR)
        val ffmpeg = File(context.applicationInfo.nativeLibraryDir, FFMPEG_BINARY)
        val process = ProcessBuilder(ffmpeg.absolutePath, "-version")
            .redirectErrorStream(true)
            .apply {
                environment()["LD_LIBRARY_PATH"] = listOf("python/usr/lib", "ffmpeg/usr/lib")
                    .joinToString(":") { File(packages, it).absolutePath }
            }
            .start()
        process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(FFMPEG_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0
    }.getOrDefault(false)

    private suspend fun ensureInitialized() {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
            }
            isInitialized = true
        }
    }

    private suspend fun run(
        request: YoutubeDLRequest,
        onProgress: (Float, Long) -> Unit,
    ): YoutubeDLResponse {
        ensureInitialized()
        request.addOption("--cache-dir", cacheDir.absolutePath)
        val processId = UUID.randomUUID().toString()
        updateGate.withLock { activeRuns.incrementAndGet() }
        return try {
            // execute()는 블로킹이라 취소되면 스레드를 인터럽트하고 아래에서 프로세스를 직접 끝낸다.
            runInterruptible(Dispatchers.IO) {
                YoutubeDL.getInstance().execute(request, processId) { progress, eta, _ -> onProgress(progress, eta) }
            }
        } catch (e: CancellationException) {
            YoutubeDL.getInstance().destroyProcessById(processId)
            throw e
        } catch (e: YoutubeDL.CanceledException) {
            throw CancellationException("yt-dlp 작업이 취소됨").apply { initCause(e) }
        } catch (e: YoutubeDLException) {
            throw EngineException(e.message ?: "yt-dlp 실행 실패", e)
        } finally {
            activeRuns.decrementAndGet()
        }
    }

    private companion object {
        const val CACHE_DIR_NAME = "yt-dlp-cache"
        const val OUTPUT_TEMPLATE = "%(title).150B [%(id)s].%(ext)s"
        const val PACKAGES_DIR = "youtubedl-android/packages"
        const val FFMPEG_BINARY = "libffmpeg.so"
        const val FFMPEG_CHECK_TIMEOUT_SECONDS = 10L
        const val RUN_POLL_MILLIS = 500L
    }
}

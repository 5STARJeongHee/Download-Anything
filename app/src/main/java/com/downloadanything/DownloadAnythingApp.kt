// 앱 전역 의존성을 만들고, yt-dlp 엔진 준비·업데이트 상태를 관리한다
package com.downloadanything

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import com.downloadanything.data.ExtractorRouter
import com.downloadanything.data.image.ImageDownloader
import com.downloadanything.data.image.XImageExtractor
import com.downloadanything.data.storage.MediaStoreSaver
import com.downloadanything.data.ytdlp.YoutubeDlAndroidEngine
import com.downloadanything.data.ytdlp.YtDlpEngine
import com.downloadanything.data.ytdlp.YtDlpExtractor
import com.downloadanything.domain.UpdatePolicy
import com.downloadanything.domain.model.EngineState
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class DownloadAnythingApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engineMutex = Mutex()
    private val mutableEngineState = MutableStateFlow<EngineState>(EngineState.Initializing)
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    val engineState: StateFlow<EngineState> = mutableEngineState.asStateFlow()

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    val engine: YtDlpEngine by lazy { YoutubeDlAndroidEngine(this) }
    val extractorRouter: ExtractorRouter by lazy { ExtractorRouter(YtDlpExtractor(engine), XImageExtractor(httpClient)) }
    val imageDownloader: ImageDownloader by lazy { ImageDownloader(httpClient) }
    val mediaStoreSaver: MediaStoreSaver by lazy { MediaStoreSaver(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { engineMutex.withLock { prepare(forceUpdate = false) } }
    }

    /** 엔진이 준비될 때까지 기다린다. 준비에 실패했으면 예외를 던진다. */
    suspend fun awaitReadyEngine(): EngineState.Ready {
        val state = withTimeoutOrNull(ENGINE_WAIT_MILLIS) {
            engineState.first { it is EngineState.Ready || it is EngineState.Failed }
        } ?: throw MediaUnavailableException(FailureReason.UNKNOWN, "엔진 준비 시간 초과")
        return when (state) {
            is EngineState.Ready -> state
            is EngineState.Failed -> throw MediaUnavailableException(FailureReason.UNKNOWN, "엔진 준비 실패: ${state.message}")
            EngineState.Initializing, is EngineState.Updating -> error("도달할 수 없음")
        }
    }

    /** 사용자가 누른 엔진 업데이트. 준비에 실패한 상태였다면 처음부터 다시 준비한다. */
    fun requestEngineUpdate() {
        // 바로 Updating으로 바꿔서, 직후에 awaitReadyEngine()을 부른 쪽이 옛 Ready를 보지 않게 한다.
        when (val state = engineState.value) {
            is EngineState.Ready -> mutableEngineState.value = EngineState.Updating(state.version)
            is EngineState.Failed -> mutableEngineState.value = EngineState.Initializing
            EngineState.Initializing, is EngineState.Updating -> return
        }
        appScope.launch {
            engineMutex.withLock {
                when (val state = engineState.value) {
                    is EngineState.Updating -> {
                        val newVersion = update(state.version)
                        mutableEngineState.value = EngineState.Ready(
                            version = newVersion ?: state.version,
                            isFfmpegAvailable = engine.isFfmpegAvailable(),
                            isUpdateFailed = lastUpdateFailed,
                        )
                    }
                    EngineState.Initializing -> prepare(forceUpdate = true)
                    is EngineState.Failed, is EngineState.Ready -> Unit
                }
            }
        }
    }

    @Volatile
    private var lastUpdateFailed = false

    private suspend fun prepare(forceUpdate: Boolean) {
        mutableEngineState.value = EngineState.Initializing
        val version = try {
            engine.initialize()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // 네이티브 라이브러리 로드 실패(UnsatisfiedLinkError)도 앱을 죽이지 않고 상태로 알린다.
            logError("yt-dlp 엔진 초기화 실패", e)
            mutableEngineState.value = EngineState.Failed(e.message ?: e.javaClass.simpleName)
            return
        }
        val isFfmpegAvailable = engine.isFfmpegAvailable()
        // 내장 yt-dlp는 오래돼 YouTube가 403을 낸다(context-notes). 하루 1회 nightly로 올린다.
        val isDue = forceUpdate || UpdatePolicy.isDue(prefs.lastUpdatedAt(), System.currentTimeMillis())
        val finalVersion = if (isDue) update(version) ?: version else version
        mutableEngineState.value = EngineState.Ready(finalVersion, isFfmpegAvailable, isDue && lastUpdateFailed)
    }

    /** 성공하면 새 버전, 실패하면 null. */
    private suspend fun update(currentVersion: String?): String? {
        mutableEngineState.value = EngineState.Updating(currentVersion)
        return try {
            val result = engine.update()
            prefs.edit { putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis()) }
            lastUpdateFailed = false
            result.version
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError("yt-dlp 업데이트 실패", e)
            lastUpdateFailed = true
            null
        }
    }

    private fun android.content.SharedPreferences.lastUpdatedAt(): Long? =
        if (contains(KEY_LAST_UPDATED_AT)) getLong(KEY_LAST_UPDATED_AT, 0L) else null

    private fun logError(message: String, e: Throwable) {
        if (BuildConfig.DEBUG) Log.e(TAG, message, e)
    }

    private companion object {
        const val TAG = "DownloadAnythingApp"
        const val PREFS_NAME = "engine"
        const val KEY_LAST_UPDATED_AT = "last_updated_at"
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L

        // 첫 실행 업데이트까지 포함해도 넉넉한 시간
        const val ENGINE_WAIT_MILLIS = 3 * 60 * 1000L
    }
}

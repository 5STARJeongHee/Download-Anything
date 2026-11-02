// 앱 시작 시 yt-dlp 엔진을 백그라운드에서 준비하고 상태를 공유한다
package com.downloadanything

import android.app.Application
import android.util.Log
import com.downloadanything.data.ytdlp.YoutubeDlAndroidEngine
import com.downloadanything.data.ytdlp.YtDlpEngine
import com.downloadanything.domain.model.EngineState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadAnythingApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableEngineState = MutableStateFlow<EngineState>(EngineState.Initializing)

    val engineState: StateFlow<EngineState> = mutableEngineState.asStateFlow()
    val engine: YtDlpEngine by lazy { YoutubeDlAndroidEngine(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { mutableEngineState.value = initializeEngine() }
    }

    private suspend fun initializeEngine(): EngineState = try {
        EngineState.Ready(engine.initialize())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // 네이티브 라이브러리 로드 실패(UnsatisfiedLinkError)도 앱을 죽이지 않고 상태로 알린다.
        if (BuildConfig.DEBUG) Log.e(TAG, "yt-dlp 엔진 초기화 실패", e)
        EngineState.Failed(e.message ?: e.javaClass.simpleName)
    }

    private companion object {
        const val TAG = "DownloadAnythingApp"
    }
}

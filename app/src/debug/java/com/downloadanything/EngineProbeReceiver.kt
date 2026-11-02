// 디버그 빌드 전용: adb로 yt-dlp를 실행해 stderr와 결과 크기를 logcat에 남긴다
package com.downloadanything

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 사용 예:
 * adb shell am broadcast -n com.downloadanything/.EngineProbeReceiver --es url "<URL>" --es args "--verbose --dump-single-json"
 * 결과 stdout은 cache/probe-out.txt에 저장된다. 엔진 래퍼를 거치지 않는 것은 stderr 전체를 보기 위해서다.
 */
class EngineProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getStringExtra("action") == "update") {
            runUpdate(context.applicationContext as DownloadAnythingApp)
            return
        }
        val url = intent.getStringExtra("url") ?: return
        val args = intent.getStringExtra("args").orEmpty().split(' ').filter { it.isNotBlank() }
        val app = context.applicationContext as DownloadAnythingApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.engine.initialize()
                val request = YoutubeDLRequest(url).addCommands(args)
                    .addOption("--cache-dir", File(app.cacheDir, "yt-dlp-cache").absolutePath)
                    .addOption("-o", File(app.cacheDir, "probe/%(title).80B [%(id)s].%(ext)s").absolutePath)
                val response = YoutubeDL.getInstance().execute(request)
                File(app.cacheDir, "probe-out.txt").writeText(response.out)
                response.err.lineSequence().forEach { Log.i(TAG, "ERR $it") }
                Log.i(TAG, "DONE exit=${response.exitCode} out=${response.out.length} chars")
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL", e)
            }
        }
    }

    private fun runUpdate(app: DownloadAnythingApp) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "DONE exit=0 update=${app.engine.update()} ffmpeg=${app.engine.isFfmpegAvailable()}")
            } catch (e: Throwable) {
                Log.e(TAG, "FAIL", e)
            }
        }
    }

    private companion object {
        const val TAG = "EngineProbe"
    }
}

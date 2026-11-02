// youtubedl-android 라이브러리로 YtDlpEngine을 구현한다
package com.downloadanything.data.ytdlp

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YoutubeDlAndroidEngine(private val context: Context) : YtDlpEngine {

    override suspend fun initialize(): String? = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        YoutubeDL.getInstance().version(context)
    }
}

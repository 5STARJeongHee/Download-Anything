// yt-dlp 엔진 접근을 감싸는 인터페이스(JVM 테스트에서 가짜로 바꿀 수 있게)
package com.downloadanything.data.ytdlp

import java.io.File

interface YtDlpEngine {
    /** 내장 Python·yt-dlp·ffmpeg를 쓸 준비를 하고 yt-dlp 버전을 돌려준다. 수 초 걸린다. */
    suspend fun initialize(): String?

    /** `--dump-single-json` 결과(JSON 문자열)를 돌려준다. */
    suspend fun dumpJson(url: String, args: List<String>): String

    /** [outputDir]에 받는다. 코루틴이 취소되면 yt-dlp 프로세스도 끝낸다. */
    suspend fun download(
        url: String,
        args: List<String>,
        outputDir: File,
        onProgress: (percent: Float, etaSeconds: Long) -> Unit,
    )

    /** nightly 채널로 yt-dlp를 업데이트하고 새 버전을 돌려준다. */
    suspend fun update(): UpdateResult

    /**
     * 내장 ffmpeg가 실제로 실행되는지. 16KB 페이지 기기에서는 내장 ffmpeg가 링크되지 않아 false다
     * (youtubedl-android #365). false면 합치기·변환 없는 형식을 써야 한다.
     */
    suspend fun isFfmpegAvailable(): Boolean
}

data class UpdateResult(val isUpdated: Boolean, val version: String?)

/** yt-dlp가 0이 아닌 코드로 끝났을 때. [message]에는 yt-dlp 오류 출력이 들어 있다. */
class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)

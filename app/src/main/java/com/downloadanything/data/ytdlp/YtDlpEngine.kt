// yt-dlp 엔진 접근을 감싸는 인터페이스(JVM 테스트에서 가짜로 바꿀 수 있게)
package com.downloadanything.data.ytdlp

interface YtDlpEngine {
    /** 내장 Python·yt-dlp·ffmpeg를 쓸 준비를 하고 yt-dlp 버전을 돌려준다. 수 초 걸린다. */
    suspend fun initialize(): String?
}

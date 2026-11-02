// 사용자가 고르는 다운로드 방식(영상·음악·이미지)
package com.downloadanything.domain.model

enum class DownloadPreset {
    BEST_VIDEO,
    VIDEO_720P,
    AUDIO_M4A,
    AUDIO_MP3,
    IMAGE;

    /** 이미지는 yt-dlp가 받지 못해 별도 추출기가 처리한다(docs/research.md 5절). */
    val usesYtDlp: Boolean get() = this != IMAGE
}

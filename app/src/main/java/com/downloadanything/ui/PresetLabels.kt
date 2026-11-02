// 다운로드 프리셋의 화면 표시 문구
package com.downloadanything.ui

import com.downloadanything.domain.model.DownloadPreset

fun DownloadPreset.label(imageCount: Int): String = when (this) {
    DownloadPreset.BEST_VIDEO -> "영상 · 최고 화질"
    DownloadPreset.VIDEO_720P -> "영상 · 720p"
    DownloadPreset.AUDIO_M4A -> "음악 · m4a"
    DownloadPreset.AUDIO_MP3 -> "음악 · mp3"
    DownloadPreset.IMAGE -> if (imageCount > 1) "이미지 · ${imageCount}장" else "이미지"
}

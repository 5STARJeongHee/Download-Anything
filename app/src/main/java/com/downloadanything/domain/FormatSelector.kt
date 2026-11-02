// 다운로드 프리셋을 yt-dlp 명령 인자로 바꾼다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset

object FormatSelector {

    private val COMMON_ARGS = listOf("--no-playlist")
    private val AUDIO_META_ARGS = listOf("--embed-metadata", "--embed-thumbnail")

    /** yt-dlp가 처리하지 않는 프리셋(IMAGE)은 null. */
    fun ytDlpArgs(preset: DownloadPreset): List<String>? = when (preset) {
        DownloadPreset.BEST_VIDEO -> listOf("-f", "bv*+ba/b", "--merge-output-format", "mp4")
        DownloadPreset.VIDEO_720P ->
            listOf("-f", "bv*[height<=720]+ba/b[height<=720]/b", "--merge-output-format", "mp4")
        DownloadPreset.AUDIO_M4A ->
            listOf("-f", "ba[ext=m4a]/ba", "-x", "--audio-format", "m4a") + AUDIO_META_ARGS
        DownloadPreset.AUDIO_MP3 ->
            listOf("-f", "ba", "-x", "--audio-format", "mp3", "--audio-quality", "0") + AUDIO_META_ARGS
        DownloadPreset.IMAGE -> null
    }?.let { COMMON_ARGS + it }
}

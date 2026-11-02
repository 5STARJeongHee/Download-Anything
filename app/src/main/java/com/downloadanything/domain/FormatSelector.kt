// 다운로드 프리셋을 ffmpeg 유무에 맞는 yt-dlp 명령 인자로 바꾼다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset

object FormatSelector {

    /** 정보 조회용. 형식이 없어도(이미지 핀 등) 결과 JSON을 받는다. */
    val INFO_ARGS = listOf("--no-playlist", "--ignore-no-formats-error")

    private val COMMON_ARGS = listOf("--no-playlist")
    private val AUDIO_META_ARGS = listOf("--embed-metadata", "--embed-thumbnail")
    private val MERGE_TO_MP4 = listOf("--merge-output-format", "mp4")

    /**
     * yt-dlp가 처리하지 않거나(IMAGE) 이 기기에서 만들 수 없는(ffmpeg 없는 mp3) 프리셋은 null.
     * ffmpeg가 없으면 영상·음성을 합치지 못하므로 하나로 된 파일을 고른다.
     */
    fun ytDlpArgs(preset: DownloadPreset, isFfmpegAvailable: Boolean): List<String>? {
        val args = if (isFfmpegAvailable) withFfmpeg(preset) else withoutFfmpeg(preset)
        return args?.let { COMMON_ARGS + it }
    }

    private fun withFfmpeg(preset: DownloadPreset): List<String>? = when (preset) {
        DownloadPreset.BEST_VIDEO -> listOf("-f", "bv*+ba/b") + MERGE_TO_MP4
        DownloadPreset.VIDEO_720P -> listOf("-f", "bv*[height<=720]+ba/b[height<=720]/b") + MERGE_TO_MP4
        DownloadPreset.AUDIO_M4A -> listOf("-f", "ba[ext=m4a]/ba", "-x", "--audio-format", "m4a") + AUDIO_META_ARGS
        DownloadPreset.AUDIO_MP3 ->
            listOf("-f", "ba", "-x", "--audio-format", "mp3", "--audio-quality", "0") + AUDIO_META_ARGS
        DownloadPreset.IMAGE -> null
    }

    private fun withoutFfmpeg(preset: DownloadPreset): List<String>? = when (preset) {
        DownloadPreset.BEST_VIDEO -> listOf("-f", "b/bv*")
        DownloadPreset.VIDEO_720P -> listOf("-f", "b[height<=720]/b/bv*[height<=720]")
        DownloadPreset.AUDIO_M4A -> listOf("-f", "ba[ext=m4a]/ba")
        DownloadPreset.AUDIO_MP3 -> null
        DownloadPreset.IMAGE -> null
    }
}

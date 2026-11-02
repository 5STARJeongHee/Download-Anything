// 미디어 정보와 ffmpeg 유무로 사용자에게 보여줄 다운로드 프리셋을 고른다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.MediaInfo

object PresetPolicy {

    fun available(info: MediaInfo, isFfmpegAvailable: Boolean): List<DownloadPreset> = buildList {
        if (info.images.isNotEmpty()) add(DownloadPreset.IMAGE)
        if (info.hasVideo) {
            add(DownloadPreset.BEST_VIDEO)
            add(DownloadPreset.VIDEO_720P)
        }
        if (info.hasAudio) {
            add(DownloadPreset.AUDIO_M4A)
            // mp3는 변환이 필요해 ffmpeg 없이는 만들 수 없다.
            if (isFfmpegAvailable) add(DownloadPreset.AUDIO_MP3)
        }
    }
}

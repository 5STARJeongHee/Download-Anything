// 직접 받을 수 있는 영상의 화질 후보 중 프리셋에 맞는 주소를 고른다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.RemoteVideo
import com.downloadanything.domain.model.VideoVariant

object DirectVideoSelector {

    private const val MAX_HEIGHT_720P = 720

    /** 영상마다 주소 하나. 영상 프리셋이 아니면 빈 목록(음악은 yt-dlp가 추출한다). */
    fun urlsFor(videos: List<RemoteVideo>, preset: DownloadPreset): List<String> = when (preset) {
        DownloadPreset.BEST_VIDEO -> videos.mapNotNull { best(it.variants)?.url }
        // FormatSelector의 `b[height<=720]/b`와 같게, 720 이하가 없으면 가장 좋은 것을 받는다.
        DownloadPreset.VIDEO_720P -> videos.mapNotNull { video ->
            val capped = video.variants.filter { it.height != null && it.height <= MAX_HEIGHT_720P }
            (best(capped) ?: best(video.variants))?.url
        }
        DownloadPreset.AUDIO_M4A, DownloadPreset.AUDIO_MP3, DownloadPreset.IMAGE -> emptyList()
    }

    private fun best(variants: List<VideoVariant>): VideoVariant? =
        variants.maxWithOrNull(compareBy<VideoVariant> { it.bitrate }.thenBy { it.height ?: 0 })
}

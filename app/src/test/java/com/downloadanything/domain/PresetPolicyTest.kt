// 미디어 정보와 ffmpeg 유무에 따라 고를 수 있는 프리셋이 맞게 나오는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadPreset.AUDIO_M4A
import com.downloadanything.domain.model.DownloadPreset.AUDIO_MP3
import com.downloadanything.domain.model.DownloadPreset.BEST_VIDEO
import com.downloadanything.domain.model.DownloadPreset.IMAGE
import com.downloadanything.domain.model.DownloadPreset.VIDEO_720P
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.Platform
import com.downloadanything.domain.model.RemoteImage
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetPolicyTest {

    private fun info(hasVideo: Boolean = false, hasAudio: Boolean = false, imageCount: Int = 0) = MediaInfo(
        sourceUrl = "https://example.com",
        platform = Platform.OTHER,
        title = "t",
        thumbnailUrl = null,
        durationSeconds = null,
        hasVideo = hasVideo,
        hasAudio = hasAudio,
        images = List(imageCount) { RemoteImage("https://example.com/$it.jpg", "jpg") },
    )

    private fun presets(info: MediaInfo, ffmpeg: Boolean = true): List<DownloadPreset> =
        PresetPolicy.available(info, ffmpeg)

    @Test
    fun `video with audio offers video and audio presets`() {
        assertEquals(listOf(BEST_VIDEO, VIDEO_720P, AUDIO_M4A, AUDIO_MP3), presets(info(hasVideo = true, hasAudio = true)))
    }

    @Test
    fun `audio only offers audio presets`() {
        assertEquals(listOf(AUDIO_M4A, AUDIO_MP3), presets(info(hasAudio = true)))
    }

    @Test
    fun `video without audio stream offers only video`() {
        assertEquals(listOf(BEST_VIDEO, VIDEO_720P), presets(info(hasVideo = true)))
    }

    @Test
    fun `without ffmpeg mp3 is hidden`() {
        assertEquals(listOf(BEST_VIDEO, VIDEO_720P, AUDIO_M4A), presets(info(hasVideo = true, hasAudio = true), ffmpeg = false))
    }

    @Test
    fun `images add image preset first`() {
        assertEquals(listOf(IMAGE), presets(info(imageCount = 2)))
        assertEquals(listOf(IMAGE, BEST_VIDEO, VIDEO_720P, AUDIO_M4A, AUDIO_MP3), presets(info(true, true, 1)))
    }
}

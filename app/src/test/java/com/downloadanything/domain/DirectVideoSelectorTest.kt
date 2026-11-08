// 직접 받을 수 있는 영상 화질 후보 중 프리셋에 맞는 주소를 고르는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.RemoteVideo
import com.downloadanything.domain.model.VideoVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectVideoSelectorTest {

    private val low = VideoVariant("https://v/320x400.mp4", bitrate = 632_000, height = 400)
    private val mid = VideoVariant("https://v/480x600.mp4", bitrate = 950_000, height = 600)
    private val high = VideoVariant("https://v/720x900.mp4", bitrate = 2_176_000, height = 900)
    private val video = RemoteVideo(listOf(mid, high, low))

    @Test
    fun `best video picks highest bitrate`() {
        assertEquals(listOf(high.url), DirectVideoSelector.urlsFor(listOf(video), DownloadPreset.BEST_VIDEO))
    }

    @Test
    fun `720p picks best variant at or under 720 height`() {
        assertEquals(listOf(mid.url), DirectVideoSelector.urlsFor(listOf(video), DownloadPreset.VIDEO_720P))
    }

    @Test
    fun `720p falls back to best when every variant is taller`() {
        val tall = RemoteVideo(listOf(high, high.copy(url = "https://v/1080.mp4", bitrate = 5_000_000, height = 1350)))
        assertEquals(listOf("https://v/1080.mp4"), DirectVideoSelector.urlsFor(listOf(tall), DownloadPreset.VIDEO_720P))
    }

    @Test
    fun `picks one url per video in order`() {
        val other = RemoteVideo(listOf(low))
        assertEquals(listOf(high.url, low.url), DirectVideoSelector.urlsFor(listOf(video, other), DownloadPreset.BEST_VIDEO))
    }

    @Test
    fun `audio and image presets do not use direct videos`() {
        listOf(DownloadPreset.AUDIO_M4A, DownloadPreset.AUDIO_MP3, DownloadPreset.IMAGE).forEach {
            assertTrue(it.name, DirectVideoSelector.urlsFor(listOf(video), it).isEmpty())
        }
    }
}

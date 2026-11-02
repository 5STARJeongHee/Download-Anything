// 다운로드 폴더에서 저장할 파일만 골라내는 규칙을 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.MediaCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadOutputsTest {

    private val names = listOf("clip.mp4", "clip.webp", "clip.mp4.part", "song.m4a", "voice.webm", "pic.jpg", "clip.ytdl")

    @Test
    fun `video presets keep only video files`() {
        assertEquals(listOf("clip.mp4", "voice.webm"), DownloadOutputs.select(names, DownloadPreset.BEST_VIDEO))
    }

    @Test
    fun `audio presets keep audio and audio-in-video containers`() {
        assertEquals(listOf("clip.mp4", "song.m4a", "voice.webm"), DownloadOutputs.select(names, DownloadPreset.AUDIO_M4A))
    }

    @Test
    fun `image preset keeps images`() {
        assertEquals(listOf("clip.webp", "pic.jpg"), DownloadOutputs.select(names, DownloadPreset.IMAGE))
    }

    @Test
    fun `save category follows preset`() {
        assertEquals(MediaCategory.VIDEO, DownloadOutputs.categoryFor(DownloadPreset.VIDEO_720P))
        assertEquals(MediaCategory.AUDIO, DownloadOutputs.categoryFor(DownloadPreset.AUDIO_MP3))
        assertEquals(MediaCategory.IMAGE, DownloadOutputs.categoryFor(DownloadPreset.IMAGE))
    }
}

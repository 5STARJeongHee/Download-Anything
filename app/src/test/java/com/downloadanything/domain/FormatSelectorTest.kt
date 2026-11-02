// 다운로드 프리셋이 ffmpeg 유무에 맞는 yt-dlp 인자로 바뀌는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectorTest {

    private fun args(preset: DownloadPreset, isFfmpegAvailable: Boolean = true) =
        requireNotNull(FormatSelector.ytDlpArgs(preset, isFfmpegAvailable)) { "$preset 인자 없음" }

    private fun List<String>.valueAfter(flag: String): String? =
        indexOf(flag).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

    @Test
    fun `best video merges best streams into mp4 when ffmpeg exists`() {
        val args = args(DownloadPreset.BEST_VIDEO)
        assertEquals("bv*+ba/b", args.valueAfter("-f"))
        assertEquals("mp4", args.valueAfter("--merge-output-format"))
    }

    @Test
    fun `720p video caps height`() {
        assertTrue(args(DownloadPreset.VIDEO_720P).valueAfter("-f")!!.contains("height<=720"))
        assertTrue(args(DownloadPreset.VIDEO_720P, isFfmpegAvailable = false).valueAfter("-f")!!.contains("height<=720"))
    }

    @Test
    fun `without ffmpeg video uses single file formats and never merges`() {
        listOf(DownloadPreset.BEST_VIDEO, DownloadPreset.VIDEO_720P).forEach { preset ->
            val args = args(preset, isFfmpegAvailable = false)
            assertFalse(preset.name, args.valueAfter("-f")!!.contains("+"))
            assertFalse(preset.name, "--merge-output-format" in args)
        }
    }

    @Test
    fun `audio presets extract audio with metadata when ffmpeg exists`() {
        val m4a = args(DownloadPreset.AUDIO_M4A)
        assertTrue("-x" in m4a)
        assertEquals("m4a", m4a.valueAfter("--audio-format"))
        assertTrue("--embed-metadata" in m4a)
        assertTrue("--embed-thumbnail" in m4a)

        val mp3 = args(DownloadPreset.AUDIO_MP3)
        assertEquals("mp3", mp3.valueAfter("--audio-format"))
        assertEquals("0", mp3.valueAfter("--audio-quality"))
    }

    @Test
    fun `without ffmpeg m4a is downloaded as is and mp3 is unavailable`() {
        val m4a = args(DownloadPreset.AUDIO_M4A, isFfmpegAvailable = false)
        assertEquals("ba[ext=m4a]/ba", m4a.valueAfter("-f"))
        assertFalse("-x" in m4a)
        assertFalse("--embed-thumbnail" in m4a)
        assertNull(FormatSelector.ytDlpArgs(DownloadPreset.AUDIO_MP3, isFfmpegAvailable = false))
    }

    @Test
    fun `every yt-dlp preset disables playlists`() {
        DownloadPreset.entries.filter { it.usesYtDlp }.forEach {
            assertTrue(it.name, "--no-playlist" in args(it))
        }
    }

    @Test
    fun `image preset is not handled by yt-dlp`() {
        assertNull(FormatSelector.ytDlpArgs(DownloadPreset.IMAGE, isFfmpegAvailable = true))
    }

    @Test
    fun `info args keep result even without formats`() {
        assertTrue("--ignore-no-formats-error" in FormatSelector.INFO_ARGS)
        assertTrue("--no-playlist" in FormatSelector.INFO_ARGS)
    }
}

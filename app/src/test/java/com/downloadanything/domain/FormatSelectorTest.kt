// 다운로드 프리셋이 올바른 yt-dlp 인자로 바뀌는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectorTest {

    private fun argsOf(preset: DownloadPreset) = FormatSelector.ytDlpArgs(preset)

    private fun yt(preset: DownloadPreset) = requireNotNull(argsOf(preset))

    private fun List<String>.valueAfter(flag: String): String? =
        indexOf(flag).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

    @Test
    fun `best video merges best streams into mp4`() {
        val args = yt(DownloadPreset.BEST_VIDEO)
        assertEquals("bv*+ba/b", args.valueAfter("-f"))
        assertEquals("mp4", args.valueAfter("--merge-output-format"))
        assertFalse("-x" in args)
    }

    @Test
    fun `720p video caps height`() {
        val format = yt(DownloadPreset.VIDEO_720P).valueAfter("-f")
        assertTrue(format!!.contains("height<=720"))
    }

    @Test
    fun `audio presets extract audio with metadata`() {
        val m4a = yt(DownloadPreset.AUDIO_M4A)
        assertTrue("-x" in m4a)
        assertEquals("m4a", m4a.valueAfter("--audio-format"))
        assertTrue("--embed-metadata" in m4a)
        assertTrue("--embed-thumbnail" in m4a)

        val mp3 = yt(DownloadPreset.AUDIO_MP3)
        assertEquals("mp3", mp3.valueAfter("--audio-format"))
        assertEquals("0", mp3.valueAfter("--audio-quality"))
    }

    @Test
    fun `every yt-dlp preset disables playlists by default`() {
        DownloadPreset.entries.filter { it.usesYtDlp }.forEach {
            assertTrue(it.name, "--no-playlist" in yt(it))
        }
    }

    @Test
    fun `image preset is not handled by yt-dlp`() {
        assertNull(argsOf(DownloadPreset.IMAGE))
    }
}

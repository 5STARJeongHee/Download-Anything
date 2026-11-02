// 실제 yt-dlp JSON 샘플을 MediaInfo로 바꾸는 파서를 검증한다
package com.downloadanything.data.ytdlp

import com.downloadanything.TestSamples
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class YtDlpInfoParserTest {

    private fun parse(sample: String, platform: Platform) =
        YtDlpInfoParser.parse(TestSamples.read(sample), ParsedLink("https://example.com/x", platform))

    @Test
    fun `youtube video has video and audio`() {
        val info = parse("youtube.json", Platform.YOUTUBE)
        assertTrue(info.title.startsWith("Big Buck Bunny"))
        assertTrue(info.hasVideo)
        assertTrue(info.hasAudio)
        assertTrue(info.images.isEmpty())
        assertTrue(info.thumbnailUrl!!.startsWith("https://"))
        assertEquals("https://example.com/x", info.sourceUrl)
    }

    @Test
    fun `youtube without formats has no media and is not treated as image`() {
        val info = parse("youtube_no_formats.json", Platform.YOUTUBE)
        assertFalse(info.hasVideo)
        assertFalse(info.hasAudio)
        assertTrue(info.images.isEmpty())
    }

    @Test
    fun `x formats with unknown codecs count as video with audio`() {
        val info = parse("x_photo_video.json", Platform.X)
        assertTrue(info.hasVideo)
        assertTrue(info.hasAudio)
    }

    @Test
    fun `playlist entries are merged`() {
        val info = parse("x_multi.json", Platform.X)
        assertTrue(info.hasVideo)
        assertTrue(info.title.isNotBlank())
        assertTrue(info.thumbnailUrl != null)
    }

    @Test
    fun `pinterest video is video`() {
        val info = parse("pinterest_video.json", Platform.PINTEREST)
        assertTrue(info.hasVideo)
        assertTrue(info.images.isEmpty())
    }

    @Test
    fun `pinterest pin without formats becomes largest image`() {
        val info = parse("pinterest_image_synthetic.json", Platform.PINTEREST)
        assertFalse(info.hasVideo)
        assertEquals(1, info.images.size)
        assertTrue(info.images[0].url.contains("/originals/"))
        assertEquals("jpg", info.images[0].extension)
    }

    @Test
    fun `drm protected media is rejected`() {
        try {
            YtDlpInfoParser.parse("""{"title":"x","_has_drm":true,"formats":[]}""", ParsedLink("https://e.com", Platform.OTHER))
            fail("DRM 미디어는 거부해야 한다")
        } catch (e: MediaUnavailableException) {
            assertEquals(FailureReason.DRM, e.reason)
        }
    }

    @Test
    fun `direct file without formats list uses top level codec`() {
        val info = YtDlpInfoParser.parse(
            """{"title":"clip","url":"https://e.com/a.mp4","ext":"mp4","vcodec":null,"acodec":null}""",
            ParsedLink("https://e.com/a.mp4", Platform.OTHER),
        )
        assertTrue(info.hasVideo)
    }

    @Test
    fun `invalid json is reported as unknown failure`() {
        try {
            YtDlpInfoParser.parse("null", ParsedLink("https://e.com", Platform.OTHER))
            fail("잘못된 JSON은 실패해야 한다")
        } catch (e: MediaUnavailableException) {
            assertEquals(FailureReason.UNKNOWN, e.reason)
        }
    }
}

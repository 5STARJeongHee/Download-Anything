// 플랫폼별 추출기 순서와 폴백·병합 규칙을 검증한다
package com.downloadanything.data

import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaExtractor
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import com.downloadanything.domain.model.RemoteImage
import com.downloadanything.domain.model.RemoteVideo
import com.downloadanything.domain.model.VideoVariant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ExtractorRouterTest {

    private class FakeExtractor(private val result: () -> MediaInfo) : MediaExtractor {
        var calls = 0
        override suspend fun extract(link: ParsedLink): MediaInfo {
            calls++
            return result()
        }
    }

    private val xLink = ParsedLink("https://x.com/u/status/1", Platform.X)
    private val ytLink = ParsedLink("https://youtu.be/abc", Platform.YOUTUBE)

    private fun media(
        link: ParsedLink,
        hasVideo: Boolean = false,
        images: Int = 0,
        title: String = "t",
        directVideos: Int = 0,
    ) = MediaInfo(
        sourceUrl = link.url, platform = link.platform, title = title, thumbnailUrl = null, durationSeconds = null,
        hasVideo = hasVideo, hasAudio = hasVideo,
        images = List(images) { RemoteImage("https://pbs.twimg.com/$it.jpg?name=orig", "jpg") },
        directVideos = List(directVideos) { RemoteVideo(listOf(VideoVariant("https://video.twimg.com/$it.mp4", 1, 720))) },
    )

    @Test
    fun `non x links go straight to yt-dlp`() = runTest {
        val ytDlp = FakeExtractor { media(ytLink, hasVideo = true) }
        val x = FakeExtractor { error("호출되면 안 됨") }
        val info = ExtractorRouter(ytDlp, x).extract(ytLink)
        assertTrue(info.hasVideo)
        assertEquals(0, x.calls)
    }

    @Test
    fun `x photo only tweet skips yt-dlp`() = runTest {
        val ytDlp = FakeExtractor { error("호출되면 안 됨") }
        val info = ExtractorRouter(ytDlp, FakeExtractor { media(xLink, images = 2) }).extract(xLink)
        assertEquals(2, info.images.size)
        assertEquals(0, ytDlp.calls)
    }

    @Test
    fun `x tweet with video merges yt-dlp video and x photos`() = runTest {
        val ytDlp = FakeExtractor { media(xLink, hasVideo = true, title = "from yt-dlp") }
        val info = ExtractorRouter(ytDlp, FakeExtractor { media(xLink, hasVideo = true, images = 1) }).extract(xLink)
        assertTrue(info.hasVideo)
        assertEquals(1, info.images.size)
        assertEquals("from yt-dlp", info.title)
    }

    @Test
    fun `x video tweet with direct mp4 skips yt-dlp`() = runTest {
        val ytDlp = FakeExtractor { error("호출되면 안 됨") }
        val x = FakeExtractor { media(xLink, hasVideo = true, images = 1, directVideos = 2) }
        val info = ExtractorRouter(ytDlp, x).extract(xLink)
        assertEquals(2, info.directVideos.size)
        assertEquals(1, info.images.size)
        assertEquals(0, ytDlp.calls)
    }

    @Test
    fun `x extractor failure falls back to yt-dlp`() = runTest {
        val ytDlp = FakeExtractor { media(xLink, hasVideo = true) }
        val info = ExtractorRouter(ytDlp, FakeExtractor { throw IOException("x down") }).extract(xLink)
        assertTrue(info.hasVideo)
    }

    @Test
    fun `x photos survive yt-dlp failure`() = runTest {
        val ytDlp = FakeExtractor { throw IOException("yt-dlp failed") }
        val info = ExtractorRouter(ytDlp, FakeExtractor { media(xLink, hasVideo = true, images = 1) }).extract(xLink)
        assertEquals(1, info.images.size)
    }

    @Test
    fun `x login wall wins over yt-dlp empty result`() = runTest {
        val loginWall = FakeExtractor { throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED) }
        assertReason(FailureReason.LOGIN_REQUIRED) { ExtractorRouter(FakeExtractor { media(xLink) }, loginWall).extract(xLink) }
    }

    @Test
    fun `x login wall wins over yt-dlp unknown failure`() = runTest {
        val loginWall = FakeExtractor { throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED) }
        val ytDlp = FakeExtractor { throw MediaUnavailableException(FailureReason.UNKNOWN) }
        assertReason(FailureReason.LOGIN_REQUIRED) { ExtractorRouter(ytDlp, loginWall).extract(xLink) }
    }

    @Test
    fun `x login wall keeps network failure`() = runTest {
        val loginWall = FakeExtractor { throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED) }
        val ytDlp = FakeExtractor { throw MediaUnavailableException(FailureReason.NETWORK) }
        assertReason(FailureReason.NETWORK) { ExtractorRouter(ytDlp, loginWall).extract(xLink) }
    }

    @Test
    fun `x login wall still uses yt-dlp result when it has media`() = runTest {
        val loginWall = FakeExtractor { throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED) }
        val info = ExtractorRouter(FakeExtractor { media(xLink, hasVideo = true) }, loginWall).extract(xLink)
        assertTrue(info.hasVideo)
    }

    private suspend fun assertReason(expected: FailureReason, block: suspend () -> Unit) {
        try {
            block()
            fail("$expected 로 실패해야 한다")
        } catch (e: MediaUnavailableException) {
            assertEquals(expected, e.reason)
        }
    }

    @Test
    fun `empty result is reported as no media`() = runTest {
        try {
            ExtractorRouter(FakeExtractor { media(ytLink) }, FakeExtractor { media(xLink) }).extract(ytLink)
            fail("받을 것이 없으면 실패해야 한다")
        } catch (e: MediaUnavailableException) {
            assertEquals(FailureReason.NO_MEDIA, e.reason)
        }
    }
}

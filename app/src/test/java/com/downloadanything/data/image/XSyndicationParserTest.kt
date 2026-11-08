// X 임베드 API 응답에서 사진과 영상 여부를 뽑는 파서를 검증한다
package com.downloadanything.data.image

import com.downloadanything.TestSamples
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class XSyndicationParserTest {

    private val link = ParsedLink("https://x.com/sample_user/status/1577855540407197696", Platform.X)

    @Test
    fun `extracts tweet id from various urls`() {
        assertEquals("1577855540407197696", XSyndicationParser.tweetId("https://x.com/u/status/1577855540407197696"))
        assertEquals("123", XSyndicationParser.tweetId("https://twitter.com/u/status/123/photo/1"))
        assertEquals("456", XSyndicationParser.tweetId("https://mobile.twitter.com/i/web/status/456?s=20"))
        assertNull(XSyndicationParser.tweetId("https://x.com/sample_user"))
    }

    @Test
    fun `photo and video tweet yields original photo and video flag`() {
        val info = XSyndicationParser.parse(TestSamples.read("x_syndication_photo_video.json"), link)
        assertEquals(1, info.images.size)
        assertEquals("https://pbs.twimg.com/media/FeWrKrMaYAELAxK.jpg?name=orig", info.images[0].url)
        assertEquals("jpg", info.images[0].extension)
        assertTrue(info.hasVideo)
        assertTrue(info.title.contains("sample_user"))
        assertFalse(info.title.contains("https://t.co"))
    }

    @Test
    fun `video keeps only https mp4 variants with height from url`() {
        val info = XSyndicationParser.parse(TestSamples.read("x_syndication_photo_video.json"), link)
        assertEquals(1, info.directVideos.size)
        val variants = info.directVideos[0].variants
        assertEquals(3, variants.size)
        assertTrue(variants.all { it.url.contains(".mp4") })
        val best = variants.maxBy { it.bitrate }
        assertEquals(2_176_000L, best.bitrate)
        assertEquals(900, best.height)
        assertEquals(30.03, info.durationSeconds!!, 0.001)
    }

    @Test
    fun `animated gif mp4 without bitrate is a direct video`() {
        val raw = """{"id_str":"2","mediaDetails":[{"type":"animated_gif","video_info":{"variants":[
            {"bitrate":0,"content_type":"video/mp4","url":"https://video.twimg.com/tweet_video/abc.mp4"}]}}]}"""
        val info = XSyndicationParser.parse(raw, link)
        assertTrue(info.hasVideo)
        assertEquals("https://video.twimg.com/tweet_video/abc.mp4", info.directVideos.single().variants.single().url)
        assertNull(info.directVideos.single().variants.single().height)
    }

    @Test
    fun `direct videos are dropped when any video lacks mp4`() {
        val raw = """{"id_str":"3","mediaDetails":[
            {"type":"video","video_info":{"variants":[{"bitrate":1,"content_type":"video/mp4","url":"https://video.twimg.com/a.mp4"}]}},
            {"type":"video","video_info":{"variants":[{"content_type":"application/x-mpegURL","url":"https://video.twimg.com/b.m3u8"}]}}]}"""
        val info = XSyndicationParser.parse(raw, link)
        assertTrue(info.hasVideo)
        assertTrue(info.directVideos.isEmpty())
    }

    @Test
    fun `tombstone tweet is reported as login required`() {
        try {
            XSyndicationParser.parse("""{"__typename":"TweetTombstone","tombstone":{}}""", link)
            fail("숨겨진 트윗은 실패해야 한다")
        } catch (e: MediaUnavailableException) {
            assertEquals(FailureReason.LOGIN_REQUIRED, e.reason)
        }
    }

    @Test
    fun `tweet without media has nothing`() {
        val info = XSyndicationParser.parse("""{"id_str":"1","text":"hi","user":{"screen_name":"a"}}""", link)
        assertTrue(info.images.isEmpty())
        assertFalse(info.hasVideo)
        assertTrue(info.directVideos.isEmpty())
    }
}

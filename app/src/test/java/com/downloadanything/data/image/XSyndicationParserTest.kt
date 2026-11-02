// X 임베드 API 응답에서 사진과 영상 여부를 뽑는 파서를 검증한다
package com.downloadanything.data.image

import com.downloadanything.TestSamples
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `tweet without media has nothing`() {
        val info = XSyndicationParser.parse("""{"id_str":"1","text":"hi","user":{"screen_name":"a"}}""", link)
        assertTrue(info.images.isEmpty())
        assertFalse(info.hasVideo)
    }
}

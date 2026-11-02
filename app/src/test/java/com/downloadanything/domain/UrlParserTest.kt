// UrlParser의 URL 추출·플랫폼 판별·추적 파라미터 제거를 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlParserTest {

    @Test
    fun `extracts url from share text with surrounding words`() {
        val text = "이 영상 봐봐 https://youtu.be/dQw4w9WgXcQ?si=abc123 진짜 웃김"
        val link = UrlParser.parse(text)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", link?.url)
        assertEquals(Platform.YOUTUBE, link?.platform)
    }

    @Test
    fun `returns null when text has no url`() {
        assertNull(UrlParser.parse("링크 없음"))
    }

    @Test
    fun `strips trailing punctuation`() {
        val link = UrlParser.parse("(https://x.com/user/status/123).")
        assertEquals("https://x.com/user/status/123", link?.url)
    }

    @Test
    fun `detects platforms by host`() {
        val cases = mapOf(
            "https://www.youtube.com/watch?v=abc" to Platform.YOUTUBE,
            "https://m.youtube.com/watch?v=abc" to Platform.YOUTUBE,
            "https://music.youtube.com/watch?v=abc" to Platform.YOUTUBE,
            "https://twitter.com/user/status/1" to Platform.X,
            "https://mobile.x.com/user/status/1" to Platform.X,
            "https://www.pinterest.com/pin/123/" to Platform.PINTEREST,
            "https://kr.pinterest.com/pin/123/" to Platform.PINTEREST,
            "https://www.pinterest.co.kr/pin/123/" to Platform.PINTEREST,
            "https://pin.it/abcd" to Platform.PINTEREST,
            "https://www.instagram.com/p/abc/" to Platform.INSTAGRAM,
            "https://vm.tiktok.com/abc/" to Platform.TIKTOK,
            "https://vimeo.com/123" to Platform.OTHER,
        )
        cases.forEach { (url, expected) ->
            assertEquals(url, expected, UrlParser.detectPlatform(url))
        }
    }

    @Test
    fun `does not treat lookalike hosts as known platforms`() {
        assertEquals(Platform.OTHER, UrlParser.detectPlatform("https://notyoutube.com/watch?v=abc"))
        assertEquals(Platform.OTHER, UrlParser.detectPlatform("https://x.com.evil.example/status/1"))
    }

    @Test
    fun `keeps youtube timestamp but removes share tracking`() {
        val link = UrlParser.parse("https://www.youtube.com/watch?v=abc&t=42&si=track&feature=shared")
        assertEquals("https://www.youtube.com/watch?v=abc&t=42", link?.url)
    }

    @Test
    fun `removes x tracking params`() {
        val link = UrlParser.parse("https://x.com/user/status/123?s=20&t=AbCdEf")
        assertEquals("https://x.com/user/status/123", link?.url)
    }

    @Test
    fun `removes utm and instagram share params`() {
        val link = UrlParser.parse("https://www.instagram.com/reel/abc/?igsh=xyz&utm_source=ig_web")
        assertEquals("https://www.instagram.com/reel/abc/", link?.url)
    }

    @Test
    fun `ignores non http schemes`() {
        assertNull(UrlParser.parse("ftp://example.com/file"))
    }

    @Test
    fun `detects platform even when query has characters java uri rejects`() {
        assertEquals(Platform.YOUTUBE, UrlParser.detectPlatform("https://www.youtube.com/watch?v=abc&q=a|b"))
        assertEquals(Platform.X, UrlParser.detectPlatform("https://x.com/user/status/1?text=한글 공백"))
    }

    @Test
    fun `keeps functional params on unknown sites but strips utm`() {
        val link = UrlParser.parse("https://example.com/page?feature=1&si=2&utm_source=x")
        assertEquals("https://example.com/page?feature=1&si=2", link?.url)
    }
}

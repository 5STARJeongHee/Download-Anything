// yt-dlp 오류 메시지가 사용자용 실패 원인으로 올바르게 분류되는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.FailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorClassifierTest {

    @Test
    fun `classifies real yt-dlp messages`() {
        val cases = mapOf(
            "ERROR: Unsupported URL: https://example.com/page" to FailureReason.UNSUPPORTED_URL,
            "ERROR: [youtube] abc: Sign in to confirm your age. This video may be inappropriate" to FailureReason.LOGIN_REQUIRED,
            "ERROR: [instagram] abc: Requested content is not available, rate-limit reached or login required" to FailureReason.LOGIN_REQUIRED,
            "ERROR: [youtube] abc: Private video. Sign in if you've been granted access" to FailureReason.LOGIN_REQUIRED,
            "ERROR: [twitter] 123: NSFW tweet requires authentication. Use --cookies" to FailureReason.LOGIN_REQUIRED,
            "ERROR: [youtube] jNQXAC9IVRU: This video is unavailable" to FailureReason.UNAVAILABLE,
            "ERROR: unable to download video data: HTTP Error 403: Forbidden" to FailureReason.BLOCKED,
            "ERROR: [youtube] abc: HTTP Error 429: Too Many Requests" to FailureReason.BLOCKED,
            "Unable to download API page: [Errno 7] No address associated with hostname" to FailureReason.NETWORK,
            "something completely different" to FailureReason.UNKNOWN,
        )
        cases.forEach { (message, expected) -> assertEquals(message, expected, ErrorClassifier.classify(message)) }
    }

    @Test
    fun `null message is unknown`() {
        assertEquals(FailureReason.UNKNOWN, ErrorClassifier.classify(null))
    }

    @Test
    fun `blocked and no media suggest engine update`() {
        assertTrue(FailureReason.BLOCKED.suggestsEngineUpdate)
        assertTrue(FailureReason.NO_MEDIA.suggestsEngineUpdate)
        assertFalse(FailureReason.NETWORK.suggestsEngineUpdate)
        FailureReason.entries.forEach { assertTrue(it.name, it.userMessage.isNotBlank()) }
    }
}

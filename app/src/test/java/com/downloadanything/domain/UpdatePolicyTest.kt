// yt-dlp 자동 업데이트 주기 판단을 검증한다
package com.downloadanything.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L

    @Test
    fun `never updated means due`() {
        assertTrue(UpdatePolicy.isDue(lastUpdatedAtMillis = null, nowMillis = now))
    }

    @Test
    fun `updated within a day is not due`() {
        assertFalse(UpdatePolicy.isDue(now - 23 * hour, now))
    }

    @Test
    fun `updated a day ago is due`() {
        assertTrue(UpdatePolicy.isDue(now - 24 * hour, now))
    }

    @Test
    fun `clock moved backwards is due`() {
        assertTrue(UpdatePolicy.isDue(now + hour, now))
    }
}

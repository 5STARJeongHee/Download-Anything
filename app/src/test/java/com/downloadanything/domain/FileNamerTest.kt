// 저장 파일명이 안드로이드 파일 시스템에서 안전한지 검증한다
package com.downloadanything.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamerTest {

    @Test
    fun `replaces forbidden characters`() {
        assertEquals("a_b_c_d.mp4", FileNamer.build("a/b:c?d", "mp4"))
    }

    @Test
    fun `keeps korean and collapses whitespace`() {
        assertEquals("노래 제목 (Live).m4a", FileNamer.build("  노래   제목\n(Live) ", "m4a"))
    }

    @Test
    fun `falls back when title is blank`() {
        assertEquals("download.jpg", FileNamer.build("  ...  ", "jpg"))
    }

    @Test
    fun `limits length without breaking extension`() {
        val name = FileNamer.build("가".repeat(500), "mp4")
        assertTrue(name.endsWith(".mp4"))
        assertEquals(FileNamer.MAX_BASE_LENGTH + ".mp4".length, name.length)
    }

    @Test
    fun `normalizes extension`() {
        assertEquals("clip.mp4", FileNamer.build("clip", ".MP4"))
    }

    @Test
    fun `sanitizes extension against path traversal`() {
        assertEquals("clip.mp4", FileNamer.build("clip", "../mp4"))
        assertEquals("clip", FileNamer.build("clip", "/"))
    }

    @Test
    fun `does not leave trailing dot after truncation`() {
        val name = FileNamer.build("a".repeat(FileNamer.MAX_BASE_LENGTH - 1) + ".bbb", "mp4")
        assertEquals("a".repeat(FileNamer.MAX_BASE_LENGTH - 1) + ".mp4", name)
    }
}

// 확장자로 저장 위치(영상·음악·이미지·기타)를 고르는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.MediaCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCategoryTest {

    @Test
    fun `maps extensions case insensitively`() {
        val cases = mapOf(
            "mp4" to MediaCategory.VIDEO, "WEBM" to MediaCategory.VIDEO, "mkv" to MediaCategory.VIDEO,
            "m4a" to MediaCategory.AUDIO, "mp3" to MediaCategory.AUDIO, "opus" to MediaCategory.AUDIO,
            "jpg" to MediaCategory.IMAGE, "png" to MediaCategory.IMAGE, "webp" to MediaCategory.IMAGE, "gif" to MediaCategory.IMAGE,
            "part" to MediaCategory.OTHER, "" to MediaCategory.OTHER,
        )
        cases.forEach { (ext, expected) -> assertEquals(ext, expected, MediaCategory.fromExtension(ext)) }
    }
}

// 다운로드 요청이 WorkManager 입력값으로 손실 없이 오가는지 검증한다
package com.downloadanything.work

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadRequest
import com.downloadanything.domain.model.RemoteImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadRequestCodecTest {

    @Test
    fun `round trips request with images`() {
        val request = DownloadRequest(
            url = "https://x.com/u/status/1",
            title = "사진 트윗",
            preset = DownloadPreset.IMAGE,
            images = listOf(RemoteImage("https://pbs.twimg.com/a.jpg?name=orig", "jpg"), RemoteImage("https://pbs.twimg.com/b.png", "png")),
        )
        assertEquals(request, DownloadRequestCodec.decode(DownloadRequestCodec.encode(request)))
    }

    @Test
    fun `round trips request without images`() {
        val request = DownloadRequest("https://youtu.be/a", "영상", DownloadPreset.AUDIO_M4A, emptyList())
        assertEquals(request, DownloadRequestCodec.decode(DownloadRequestCodec.encode(request)))
    }

    @Test
    fun `round trips request with direct video urls`() {
        val request = DownloadRequest(
            url = "https://x.com/u/status/1",
            title = "영상 트윗",
            preset = DownloadPreset.BEST_VIDEO,
            images = emptyList(),
            directVideoUrls = listOf("https://video.twimg.com/a.mp4", "https://video.twimg.com/b.mp4"),
        )
        assertEquals(request, DownloadRequestCodec.decode(DownloadRequestCodec.encode(request)))
    }

    @Test
    fun `old input without direct video urls still decodes`() {
        val values = DownloadRequestCodec.encode(DownloadRequest("https://youtu.be/a", "영상", DownloadPreset.BEST_VIDEO, emptyList()))
            .minus(DownloadRequestCodec.KEY_DIRECT_VIDEO_URLS)
        assertEquals(emptyList<String>(), DownloadRequestCodec.decode(values)?.directVideoUrls)
    }

    @Test
    fun `missing or broken values decode to null`() {
        assertNull(DownloadRequestCodec.decode(emptyMap()))
        val broken = DownloadRequestCodec.encode(DownloadRequest("https://a", "t", DownloadPreset.BEST_VIDEO, emptyList()))
            .plus(DownloadRequestCodec.KEY_PRESET to "NOT_A_PRESET")
        assertNull(DownloadRequestCodec.decode(broken))
    }
}

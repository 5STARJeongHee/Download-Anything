// 플랫폼에 맞는 추출기를 고르고, 실패하면 다른 추출기로 넘기며 결과를 합친다
package com.downloadanything.data

import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaExtractor
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import kotlinx.coroutines.CancellationException

class ExtractorRouter(
    private val ytDlp: MediaExtractor,
    private val xImages: MediaExtractor,
) {

    suspend fun extract(link: ParsedLink): MediaInfo {
        val info = if (link.platform == Platform.X) extractX(link) else ytDlp.extract(link)
        if (!info.hasMedia) throw MediaUnavailableException(FailureReason.NO_MEDIA)
        return info
    }

    // X 사진은 임베드 API로만 받을 수 있다. 영상도 임베드 API의 mp4가 있으면 그것을 쓴다.
    // yt-dlp의 X 추출기는 X API 변경으로 자주 깨지기 때문이다(context-notes 2026-11-08).
    private suspend fun extractX(link: ParsedLink): MediaInfo {
        val embed = tryExtract(xImages, link)
        if (embed != null && embed.directVideos.isNotEmpty()) return embed
        if (embed != null && embed.images.isNotEmpty() && !embed.hasVideo) return embed

        return try {
            val video = ytDlp.extract(link)
            if (embed == null) video else video.copy(images = video.images + embed.images)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (embed == null || embed.images.isEmpty()) throw e
            embed.copy(hasVideo = false, hasAudio = false)
        }
    }

    private suspend fun tryExtract(extractor: MediaExtractor, link: ParsedLink): MediaInfo? = try {
        extractor.extract(link)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

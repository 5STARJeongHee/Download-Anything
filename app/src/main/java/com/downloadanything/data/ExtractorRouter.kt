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

    // X 사진은 임베드 API로만 받을 수 있고, 영상은 yt-dlp가 더 잘 받는다.
    private suspend fun extractX(link: ParsedLink): MediaInfo {
        val photos = tryExtract(xImages, link)
        if (photos != null && photos.images.isNotEmpty() && !photos.hasVideo) return photos

        return try {
            val video = ytDlp.extract(link)
            if (photos == null) video else video.copy(images = video.images + photos.images)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (photos == null || photos.images.isEmpty()) throw e
            photos.copy(hasVideo = false, hasAudio = false)
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

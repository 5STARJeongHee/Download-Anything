// yt-dlp --dump-single-json 결과를 MediaInfo로 바꾼다
package com.downloadanything.data.ytdlp

import com.downloadanything.data.image.ImageUrls
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.RemoteImage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object YtDlpInfoParser {

    private val json = Json { ignoreUnknownKeys = true }

    // yt-dlp에서 "none"은 해당 스트림이 없다는 뜻이고 null은 모른다는 뜻이다(X의 http 형식 등).
    private const val NONE = "none"
    private const val FALLBACK_TITLE = "download"

    // 이 추출기에서만 "형식 없음 = 이미지 핀"으로 본다. YouTube에서 형식이 0개면 엔진 문제다.
    private const val PINTEREST_EXTRACTOR = "Pinterest"

    fun parse(raw: String, link: ParsedLink): MediaInfo {
        val root = decode(raw)
        val items = listOf(root) + root.entries.filterNotNull()
        if (items.any { it.hasDrm == true }) throw MediaUnavailableException(FailureReason.DRM)

        val formats = items.flatMap { it.effectiveFormats() }
        val isImagePin = formats.isEmpty() && items.any { it.extractorKey == PINTEREST_EXTRACTOR }
        return MediaInfo(
            sourceUrl = link.url,
            platform = link.platform,
            title = items.firstNotNullOfOrNull { it.title?.takeIf(String::isNotBlank) } ?: FALLBACK_TITLE,
            thumbnailUrl = items.firstNotNullOfOrNull { it.thumbnail ?: it.thumbnails.lastOrNull()?.url },
            durationSeconds = items.firstNotNullOfOrNull { it.duration },
            hasVideo = formats.any { it.vcodec != NONE },
            hasAudio = formats.any { it.acodec != NONE },
            images = if (isImagePin) listOfNotNull(largestImage(items)) else emptyList(),
        )
    }

    private fun decode(raw: String): InfoDto = try {
        json.decodeFromString<InfoDto?>(raw)
    } catch (e: SerializationException) {
        throw MediaUnavailableException(FailureReason.UNKNOWN, "yt-dlp 응답을 해석하지 못함", e)
    } catch (e: IllegalArgumentException) {
        throw MediaUnavailableException(FailureReason.UNKNOWN, "yt-dlp 응답을 해석하지 못함", e)
    } ?: throw MediaUnavailableException(FailureReason.UNKNOWN, "yt-dlp 응답이 비어 있음")

    private fun largestImage(items: List<InfoDto>): RemoteImage? {
        val thumbnails = items.flatMap { it.thumbnails }
        val best = thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
            ?: items.firstNotNullOfOrNull { it.thumbnail }
            ?: return null
        return RemoteImage(best, ImageUrls.extensionOf(best))
    }

    private fun InfoDto.effectiveFormats(): List<FormatDto> = when {
        formats.isNotEmpty() -> formats
        // 직접 파일 링크 등은 formats 없이 최상위에 url·코덱만 있다.
        url != null -> listOf(FormatDto(ext = ext, vcodec = vcodec, acodec = acodec))
        else -> emptyList()
    }

    @Serializable
    private data class InfoDto(
        val title: String? = null,
        val url: String? = null,
        val ext: String? = null,
        val vcodec: String? = null,
        val acodec: String? = null,
        val duration: Double? = null,
        val thumbnail: String? = null,
        val thumbnails: List<ThumbnailDto> = emptyList(),
        val formats: List<FormatDto> = emptyList(),
        val entries: List<InfoDto?> = emptyList(),
        @SerialName("extractor_key") val extractorKey: String? = null,
        @SerialName("_has_drm") val hasDrm: Boolean? = null,
    )

    @Serializable
    private data class FormatDto(
        val ext: String? = null,
        val vcodec: String? = null,
        val acodec: String? = null,
    )

    @Serializable
    private data class ThumbnailDto(
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
    )
}

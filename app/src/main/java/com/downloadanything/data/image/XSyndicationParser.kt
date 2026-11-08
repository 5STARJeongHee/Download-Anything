// X 임베드 API(tweet-result) 응답에서 사진과 영상(바로 받을 수 있는 mp4 주소)을 뽑는다
package com.downloadanything.data.image

import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.RemoteImage
import com.downloadanything.domain.model.RemoteVideo
import com.downloadanything.domain.model.VideoVariant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object XSyndicationParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val STATUS_ID = Regex("""/status(?:es)?/(\d+)""")
    private val SHORT_LINK = Regex("""https?://t\.co/\S+""")

    // 영상 주소의 `/vid/720x900/` 또는 `/vid/avc1/720x900/` 부분
    private val RESOLUTION = Regex("""/(\d{2,5})x(\d{2,5})/""")
    private val VIDEO_TYPES = setOf("video", "animated_gif")
    private const val PHOTO_TYPE = "photo"
    private const val MP4_CONTENT_TYPE = "video/mp4"
    private const val HTTPS_PREFIX = "https://"
    private const val ORIGINAL_SIZE_QUERY = "?name=orig"
    private const val MAX_TITLE_TEXT = 80
    private const val MILLIS_PER_SECOND = 1000.0

    fun tweetId(url: String): String? = STATUS_ID.find(url)?.groupValues?.get(1)

    fun parse(raw: String, link: ParsedLink): MediaInfo {
        val tweet = json.decodeFromString<TweetDto>(raw)
        val photos = tweet.mediaDetails
            .filter { it.type == PHOTO_TYPE }
            .mapNotNull { it.mediaUrl }
            .map { RemoteImage(it + ORIGINAL_SIZE_QUERY, ImageUrls.extensionOf(it)) }
        val videoMedia = tweet.mediaDetails.filter { it.type in VIDEO_TYPES }
        val videos = videoMedia.map { RemoteVideo(mp4Variants(it)) }
        val hasVideo = videoMedia.isNotEmpty()
        return MediaInfo(
            sourceUrl = link.url,
            platform = link.platform,
            title = title(tweet),
            thumbnailUrl = tweet.mediaDetails.firstNotNullOfOrNull { it.mediaUrl },
            durationSeconds = videoMedia.firstNotNullOfOrNull { it.videoInfo?.durationMillis }?.div(MILLIS_PER_SECOND),
            hasVideo = hasVideo,
            hasAudio = hasVideo,
            images = photos,
            // 하나라도 mp4가 없으면 일부만 받게 되므로 전부 yt-dlp에 맡긴다.
            directVideos = if (videos.all { it.variants.isNotEmpty() }) videos else emptyList(),
        )
    }

    private fun mp4Variants(media: MediaDto): List<VideoVariant> =
        media.videoInfo?.variants.orEmpty()
            .filter { it.contentType == MP4_CONTENT_TYPE && it.url?.startsWith(HTTPS_PREFIX) == true }
            .map { variant ->
                val url = requireNotNull(variant.url)
                val height = RESOLUTION.find(url)?.groupValues?.get(2)?.toIntOrNull()
                VideoVariant(url, variant.bitrate ?: 0L, height)
            }

    private fun title(tweet: TweetDto): String {
        val text = tweet.text.orEmpty().replace(SHORT_LINK, "").trim().take(MAX_TITLE_TEXT)
        return listOfNotNull(tweet.user?.screenName, text.ifBlank { null })
            .joinToString(" - ")
            .ifBlank { "X ${tweet.id.orEmpty()}".trim() }
    }

    @Serializable
    private data class TweetDto(
        @SerialName("id_str") val id: String? = null,
        val text: String? = null,
        val user: UserDto? = null,
        val mediaDetails: List<MediaDto> = emptyList(),
    )

    @Serializable
    private data class UserDto(@SerialName("screen_name") val screenName: String? = null)

    @Serializable
    private data class MediaDto(
        val type: String? = null,
        @SerialName("media_url_https") val mediaUrl: String? = null,
        @SerialName("video_info") val videoInfo: VideoInfoDto? = null,
    )

    @Serializable
    private data class VideoInfoDto(
        @SerialName("duration_millis") val durationMillis: Long? = null,
        val variants: List<VariantDto> = emptyList(),
    )

    @Serializable
    private data class VariantDto(
        val bitrate: Long? = null,
        @SerialName("content_type") val contentType: String? = null,
        val url: String? = null,
    )
}

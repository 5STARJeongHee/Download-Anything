// X 임베드 API(tweet-result) 응답에서 사진과 영상 여부를 뽑는다
package com.downloadanything.data.image

import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.RemoteImage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object XSyndicationParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val STATUS_ID = Regex("""/status(?:es)?/(\d+)""")
    private val SHORT_LINK = Regex("""https?://t\.co/\S+""")
    private val VIDEO_TYPES = setOf("video", "animated_gif")
    private const val PHOTO_TYPE = "photo"
    private const val ORIGINAL_SIZE_QUERY = "?name=orig"
    private const val MAX_TITLE_TEXT = 80

    fun tweetId(url: String): String? = STATUS_ID.find(url)?.groupValues?.get(1)

    fun parse(raw: String, link: ParsedLink): MediaInfo {
        val tweet = json.decodeFromString<TweetDto>(raw)
        val photos = tweet.mediaDetails
            .filter { it.type == PHOTO_TYPE }
            .mapNotNull { it.mediaUrl }
            .map { RemoteImage(it + ORIGINAL_SIZE_QUERY, ImageUrls.extensionOf(it)) }
        val hasVideo = tweet.mediaDetails.any { it.type in VIDEO_TYPES }
        return MediaInfo(
            sourceUrl = link.url,
            platform = link.platform,
            title = title(tweet),
            thumbnailUrl = tweet.mediaDetails.firstNotNullOfOrNull { it.mediaUrl },
            durationSeconds = null,
            hasVideo = hasVideo,
            hasAudio = hasVideo,
            images = photos,
        )
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
    )
}

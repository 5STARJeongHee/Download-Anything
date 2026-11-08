// 링크에서 찾은 미디어 정보(제목, 썸네일, 영상·음성 유무, 이미지 목록, 직접 받을 수 있는 영상)
package com.downloadanything.domain.model

data class RemoteImage(val url: String, val extension: String)

/** yt-dlp 없이 바로 받을 수 있는 영상 파일 한 화질. 높이를 모르면 null. */
data class VideoVariant(val url: String, val bitrate: Long, val height: Int?)

/** 영상 하나의 화질 후보들. */
data class RemoteVideo(val variants: List<VideoVariant>)

data class MediaInfo(
    val sourceUrl: String,
    val platform: Platform,
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Double?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val images: List<RemoteImage>,
    val directVideos: List<RemoteVideo> = emptyList(),
) {
    val hasMedia: Boolean get() = hasVideo || hasAudio || images.isNotEmpty()
}

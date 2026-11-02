// 링크에서 찾은 미디어 정보(제목, 썸네일, 영상·음성 유무, 이미지 목록)
package com.downloadanything.domain.model

data class RemoteImage(val url: String, val extension: String)

data class MediaInfo(
    val sourceUrl: String,
    val platform: Platform,
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Double?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val images: List<RemoteImage>,
) {
    val hasMedia: Boolean get() = hasVideo || hasAudio || images.isNotEmpty()
}

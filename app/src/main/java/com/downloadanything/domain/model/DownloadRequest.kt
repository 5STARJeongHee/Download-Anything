// 사용자가 고른 다운로드 한 건(링크, 제목, 프리셋, 이미지 목록, 직접 받을 영상 주소)
package com.downloadanything.domain.model

data class DownloadRequest(
    val url: String,
    val title: String,
    val preset: DownloadPreset,
    val images: List<RemoteImage>,
    /** 비어 있지 않으면 yt-dlp 대신 이 주소들을 바로 받는다. 실패하면 yt-dlp로 넘어간다. */
    val directVideoUrls: List<String> = emptyList(),
)

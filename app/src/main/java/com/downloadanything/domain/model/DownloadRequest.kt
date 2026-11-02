// 사용자가 고른 다운로드 한 건(링크, 제목, 프리셋, 이미지 목록)
package com.downloadanything.domain.model

data class DownloadRequest(
    val url: String,
    val title: String,
    val preset: DownloadPreset,
    val images: List<RemoteImage>,
)

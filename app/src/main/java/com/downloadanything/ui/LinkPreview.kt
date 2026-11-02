// 플랫폼 이름 표시 문구
package com.downloadanything.ui

import com.downloadanything.domain.model.Platform

fun Platform.label(): String = when (this) {
    Platform.YOUTUBE -> "YouTube"
    Platform.X -> "X"
    Platform.PINTEREST -> "Pinterest"
    Platform.INSTAGRAM -> "Instagram"
    Platform.TIKTOK -> "TikTok"
    Platform.OTHER -> "기타 사이트"
}

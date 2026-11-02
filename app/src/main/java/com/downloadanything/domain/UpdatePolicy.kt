// yt-dlp 자동 업데이트를 할 때가 됐는지 판단한다
package com.downloadanything.domain

object UpdatePolicy {

    private const val INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

    fun isDue(lastUpdatedAtMillis: Long?, nowMillis: Long): Boolean {
        if (lastUpdatedAtMillis == null) return true
        // 기기 시계가 뒤로 간 경우도 업데이트한다.
        return nowMillis < lastUpdatedAtMillis || nowMillis - lastUpdatedAtMillis >= INTERVAL_MILLIS
    }
}

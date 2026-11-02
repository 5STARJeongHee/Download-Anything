// 공유 텍스트에서 URL을 뽑고 플랫폼을 판별하며 추적 파라미터를 지운다
package com.downloadanything.domain

import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform

object UrlParser {

    private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private const val TRAILING_PUNCTUATION = ".,!?;:)]}>」』"

    // java.net.URI는 쿼리의 |, 공백, 한글에서 예외를 던져 플랫폼을 놓친다. 호스트만 정규식으로 뽑는다.
    private val HOST_REGEX = Regex("""^https?://(?:[^/?#@]*@)?([^/?#:]+)""", RegexOption.IGNORE_CASE)
    private val PINTEREST_HOST = Regex("""(^|\.)pinterest\.(com|[a-z]{2}|co\.[a-z]{2}|com\.[a-z]{2})$""")

    // 어느 사이트든 추적용인 파라미터
    private val UNIVERSAL_TRACKING_PARAMS = setOf("fbclid", "gclid")
    private const val UTM_PREFIX = "utm_"

    // 알려진 플랫폼에서만 추적용인 파라미터. 일반 사이트에서는 기능용일 수 있어 남긴다.
    private val PLATFORM_TRACKING_PARAMS = setOf("si", "feature", "igsh", "igshid")

    // X의 t는 공유 추적값이지만 YouTube의 t는 재생 위치라서 따로 둔다.
    private val X_TRACKING_PARAMS = setOf("s", "t")

    fun parse(text: String): ParsedLink? {
        val raw = URL_REGEX.find(text)?.value?.trimEnd { it in TRAILING_PUNCTUATION } ?: return null
        val platform = detectPlatform(raw)
        return ParsedLink(removeTracking(raw, platform), platform)
    }

    fun detectPlatform(url: String): Platform {
        val host = HOST_REGEX.find(url)?.groupValues?.get(1)?.lowercase() ?: return Platform.OTHER
        return when {
            host.isDomainOf("youtube.com") || host.isDomainOf("youtu.be") -> Platform.YOUTUBE
            host.isDomainOf("x.com") || host.isDomainOf("twitter.com") -> Platform.X
            host.isDomainOf("pin.it") || PINTEREST_HOST.containsMatchIn(host) -> Platform.PINTEREST
            host.isDomainOf("instagram.com") -> Platform.INSTAGRAM
            host.isDomainOf("tiktok.com") -> Platform.TIKTOK
            else -> Platform.OTHER
        }
    }

    private fun String.isDomainOf(domain: String): Boolean = this == domain || endsWith(".$domain")

    private fun removeTracking(url: String, platform: Platform): String {
        val fragment = url.substringAfter('#', "").let { if (it.isEmpty()) "" else "#$it" }
        val withoutFragment = url.substringBefore('#')
        val base = withoutFragment.substringBefore('?')
        val query = withoutFragment.substringAfter('?', "")
        if (query.isEmpty()) return url

        val kept = query.split('&').filter { it.isNotEmpty() && !isTracking(it.substringBefore('='), platform) }
        val newQuery = if (kept.isEmpty()) "" else "?" + kept.joinToString("&")
        return base + newQuery + fragment
    }

    private fun isTracking(name: String, platform: Platform): Boolean = when {
        name.startsWith(UTM_PREFIX) || name in UNIVERSAL_TRACKING_PARAMS -> true
        platform == Platform.OTHER -> false
        platform == Platform.X && name in X_TRACKING_PARAMS -> true
        else -> name in PLATFORM_TRACKING_PARAMS
    }
}

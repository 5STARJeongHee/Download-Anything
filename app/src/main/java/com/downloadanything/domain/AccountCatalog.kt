// 계정을 연결할 수 있는 플랫폼과 각 플랫폼의 로그인 주소·쿠키 도메인·로그인 확인 쿠키
package com.downloadanything.domain

import com.downloadanything.domain.model.Platform

/**
 * @property loginUrl 앱 안 로그인(WebView)에 띄울 주소. null이면 cookies.txt 가져오기만 된다.
 * @property cookieDomains 이 플랫폼 쿠키로 인정하는 도메인(하위 도메인 포함).
 * @property sessionCookieNames 이 중 하나라도 있어야 로그인된 것으로 본다.
 */
data class AccountSpec(
    val platform: Platform,
    val loginUrl: String?,
    val cookieDomains: List<String>,
    val sessionCookieNames: Set<String>,
)

object AccountCatalog {

    val specs: List<AccountSpec> = listOf(
        AccountSpec(Platform.X, "https://x.com/i/flow/login", listOf("x.com", "twitter.com"), setOf("auth_token")),
        AccountSpec(
            Platform.INSTAGRAM,
            "https://www.instagram.com/accounts/login/",
            listOf("instagram.com"),
            setOf("sessionid"),
        ),
        AccountSpec(Platform.TIKTOK, "https://www.tiktok.com/login", listOf("tiktok.com"), setOf("sessionid", "sid_tt")),
        // Google은 WebView 로그인을 막는다(disallowed_useragent). PC 브라우저에서 내보낸 파일만 받는다.
        AccountSpec(
            Platform.YOUTUBE,
            loginUrl = null,
            cookieDomains = listOf("youtube.com", "google.com"),
            sessionCookieNames = setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PSID", "__Secure-3PSID", "LOGIN_INFO"),
        ),
    )

    fun spec(platform: Platform): AccountSpec? = specs.firstOrNull { it.platform == platform }
}

// WebView(CookieManager)에 남은 플랫폼 쿠키를 읽고 지운다
package com.downloadanything.ui.account

import android.webkit.CookieManager
import com.downloadanything.domain.AccountSpec

object WebCookies {

    /** 도메인 → `a=1; b=2`. 하위 도메인(www)에만 있는 쿠키도 함께 모은다. */
    fun read(spec: AccountSpec): Map<String, String> {
        val manager = CookieManager.getInstance()
        return spec.cookieDomains.associateWith { domain ->
            urlsOf(domain).mapNotNull { manager.getCookie(it) }.joinToString("; ")
        }
    }

    /** CookieManager에는 도메인별 삭제가 없어서, 읽힌 이름마다 만료된 쿠키를 덮어쓴다. */
    fun clear(spec: AccountSpec) {
        val manager = CookieManager.getInstance()
        read(spec).forEach { (domain, header) ->
            val names = header.split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.toSet()
            names.forEach { name ->
                urlsOf(domain).forEach { url ->
                    manager.setCookie(url, "$name=; Max-Age=0; Path=/")
                    manager.setCookie(url, "$name=; Max-Age=0; Path=/; Domain=.$domain")
                }
            }
        }
        manager.flush()
    }

    private fun urlsOf(domain: String) = listOf("https://$domain", "https://www.$domain")
}

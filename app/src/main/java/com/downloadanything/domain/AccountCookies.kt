// WebView 쿠키 헤더나 가져온 cookies.txt를 yt-dlp용 Netscape 쿠키 파일 내용으로 바꾼다
package com.downloadanything.domain

object AccountCookies {

    const val HEADER = "# Netscape HTTP Cookie File"

    // WebView는 만료 시각을 알려주지 않는다. yt-dlp는 만료를 무시하고 읽지만, 값이 있어야 형식이 맞는다.
    const val WEB_COOKIE_LIFETIME_SECONDS = 365L * 24 * 60 * 60

    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"
    private const val FIELD_COUNT = 7
    private const val DOMAIN_FIELD = 0
    private const val NAME_FIELD = 5

    /**
     * @param cookieHeaders 도메인 → `CookieManager.getCookie()`가 준 `a=1; b=2` 문자열.
     * @return 로그인 확인 쿠키가 없으면 null.
     */
    fun fromWebLogin(spec: AccountSpec, cookieHeaders: Map<String, String>, nowEpochSeconds: Long): String? {
        val expiresAt = nowEpochSeconds + WEB_COOKIE_LIFETIME_SECONDS
        val lines = spec.cookieDomains.flatMap { domain ->
            parseHeader(cookieHeaders[domain].orEmpty()).map { (name, value) ->
                listOf(".$domain", "TRUE", "/", "TRUE", expiresAt.toString(), name, value).joinToString("\t")
            }
        }
        return build(spec, lines)
    }

    /** 플랫폼 도메인 줄만 남긴다. 로그인 확인 쿠키가 없으면 null. */
    fun fromImportedFile(spec: AccountSpec, text: String): String? {
        val lines = text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { line ->
                val fields = line.split('\t')
                fields.size == FIELD_COUNT && (!line.startsWith("#") || line.startsWith(HTTP_ONLY_PREFIX)) &&
                    isPlatformDomain(fields[DOMAIN_FIELD], spec)
            }
            .toList()
        return build(spec, lines)
    }

    private fun build(spec: AccountSpec, lines: List<String>): String? {
        val names = lines.map { it.split('\t')[NAME_FIELD] }.toSet()
        if (names.none { it in spec.sessionCookieNames }) return null
        return (listOf(HEADER, "") + lines).joinToString("\n", postfix = "\n")
    }

    // 같은 이름이 여러 번 오면 뒤의 값을 쓴다. 탭·줄바꿈이 든 값은 파일 형식을 깨므로 버린다.
    private fun parseHeader(header: String): Map<String, String> = header.split(';')
        .mapNotNull { part ->
            val name = part.substringBefore('=', "").trim()
            val value = part.substringAfter('=', "").trim()
            val isValid = name.isNotEmpty() && value.isNotEmpty() && value.none { it == '\t' || it == '\n' || it == '\r' }
            if (isValid) name to value else null
        }
        .toMap()

    private fun isPlatformDomain(field: String, spec: AccountSpec): Boolean {
        val host = field.removePrefix(HTTP_ONLY_PREFIX).removePrefix(".").lowercase()
        return spec.cookieDomains.any { host == it || host.endsWith(".$it") }
    }
}

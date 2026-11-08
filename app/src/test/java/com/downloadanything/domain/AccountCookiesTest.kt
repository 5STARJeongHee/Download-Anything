// WebView 쿠키와 가져온 cookies.txt가 플랫폼 도메인만 남은 Netscape 형식으로 바뀌는지 검증한다
package com.downloadanything.domain

import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCookiesTest {

    private val x = requireNotNull(AccountCatalog.spec(Platform.X))
    private val youtube = requireNotNull(AccountCatalog.spec(Platform.YOUTUBE))
    private val now = 1_700_000_000L

    private fun lines(text: String) = text.lines().filter { it.isNotBlank() && !it.startsWith("# ") }

    @Test
    fun `web login header becomes netscape lines for the platform domain`() {
        val text = requireNotNull(AccountCookies.fromWebLogin(x, mapOf("x.com" to "guest_id=v1; auth_token=abc; ct0=def"), now))
        assertTrue(text.startsWith(AccountCookies.HEADER))
        val rows = lines(text).map { it.split('\t') }
        assertEquals(3, rows.size)
        val auth = rows.single { it[5] == "auth_token" }
        assertEquals(listOf(".x.com", "TRUE", "/", "TRUE"), auth.take(4))
        assertEquals((now + AccountCookies.WEB_COOKIE_LIFETIME_SECONDS).toString(), auth[4])
        assertEquals("abc", auth[6])
    }

    @Test
    fun `web login without session cookie is not connected`() {
        assertNull(AccountCookies.fromWebLogin(x, mapOf("x.com" to "guest_id=v1; ct0=def"), now))
        assertNull(AccountCookies.fromWebLogin(x, emptyMap(), now))
    }

    @Test
    fun `web login ignores other domains, blank values and duplicates`() {
        val headers = mapOf(
            "x.com" to "auth_token=old; auth_token=new; empty=; bad=a\tb",
            "evil.com" to "auth_token=stolen",
        )
        val rows = lines(requireNotNull(AccountCookies.fromWebLogin(x, headers, now))).map { it.split('\t') }
        assertEquals(listOf("auth_token" to "new"), rows.map { it[5] to it[6] })
    }

    @Test
    fun `imported file keeps only platform domains including httponly lines`() {
        val imported = """
            # Netscape HTTP Cookie File
            # comment
            .youtube.com	TRUE	/	TRUE	1800000000	SAPISID	sap
            #HttpOnly_.youtube.com	TRUE	/	TRUE	1800000000	__Secure-3PSID	sid
            .google.com	TRUE	/	TRUE	1800000000	SID	g
            .example.com	TRUE	/	TRUE	1800000000	other	x
            broken line
        """.trimIndent().replace("\n", "\r\n")
        val text = requireNotNull(AccountCookies.fromImportedFile(youtube, imported))
        val kept = lines(text)
        assertEquals(3, kept.size)
        assertTrue(kept.any { it.startsWith("#HttpOnly_.youtube.com") })
        assertFalse(text.contains("example.com"))
        assertFalse(text.contains("\r"))
    }

    @Test
    fun `imported file without session cookie is rejected`() {
        val imported = ".youtube.com\tTRUE\t/\tFALSE\t0\tPREF\tf=1"
        assertNull(AccountCookies.fromImportedFile(youtube, imported))
        assertNull(AccountCookies.fromImportedFile(youtube, "not a cookie file"))
    }

    @Test
    fun `subdomain of platform matches but lookalike does not`() {
        val imported = """
            api.x.com	FALSE	/	TRUE	0	auth_token	a
            notx.com	TRUE	/	TRUE	0	auth_token	b
        """.trimIndent()
        val kept = lines(requireNotNull(AccountCookies.fromImportedFile(x, imported)))
        assertEquals(listOf("api.x.com"), kept.map { it.substringBefore('\t') })
    }

    @Test
    fun `catalog covers login heavy platforms and youtube is file only`() {
        listOf(Platform.X, Platform.INSTAGRAM, Platform.TIKTOK, Platform.YOUTUBE).forEach {
            assertNotNull(it.name, AccountCatalog.spec(it))
        }
        assertNull(AccountCatalog.spec(Platform.OTHER))
        assertNull(youtube.loginUrl)
        assertTrue(x.loginUrl!!.startsWith("https://"))
    }
}

// 계정 쿠키 파일 저장·삭제와 연결 목록, yt-dlp 인자를 검증한다
package com.downloadanything.data.account

import com.downloadanything.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AccountStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = AccountStore(File(temp.root, "accounts"))

    @Test
    fun `starts with nothing connected`() {
        val store = store()
        assertTrue(store.connected.value.isEmpty())
        assertTrue(store.ytDlpArgs(Platform.X).isEmpty())
    }

    @Test
    fun `save connects platform and adds cookies argument`() {
        val store = store()
        store.save(Platform.X, "cookie text")
        assertEquals(setOf(Platform.X), store.connected.value)
        val args = store.ytDlpArgs(Platform.X)
        assertEquals("--cookies", args[0])
        assertEquals("cookie text", File(args[1]).readText())
        assertTrue(store.ytDlpArgs(Platform.YOUTUBE).isEmpty())
    }

    @Test
    fun `connections survive a new store instance`() {
        store().save(Platform.INSTAGRAM, "c")
        assertEquals(setOf(Platform.INSTAGRAM), store().connected.value)
    }

    @Test
    fun `saving again for the same platform still bumps revision`() {
        val store = store()
        store.save(Platform.X, "a")
        val before = store.revision.value
        store.save(Platform.X, "b")
        assertEquals(before + 1, store.revision.value)
        assertEquals("b", store.cookieFile(Platform.X)!!.readText())
    }

    @Test
    fun `disconnect removes file and connection`() {
        val store = store()
        store.save(Platform.X, "c")
        store.disconnect(Platform.X)
        assertTrue(store.connected.value.isEmpty())
        assertNull(store.cookieFile(Platform.X))
    }
}

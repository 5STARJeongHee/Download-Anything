// 플랫폼별 계정 쿠키 파일을 저장·삭제하고, 연결된 플랫폼 목록과 yt-dlp 쿠키 인자를 제공한다
package com.downloadanything.data.account

import com.downloadanything.domain.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** @param dir 백업에서 빠지는 앱 전용 폴더(noBackupFilesDir 아래)여야 한다. 쿠키는 로그인 비밀번호와 같다. */
class AccountStore(private val dir: File) {

    private val mutableConnected = MutableStateFlow(scan())

    val connected: StateFlow<Set<Platform>> = mutableConnected.asStateFlow()

    private val mutableRevision = MutableStateFlow(0L)

    /** 저장·해제할 때마다 1씩 오른다. 같은 플랫폼에 다시 로그인해도 바뀐 것을 알 수 있다. */
    val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    fun cookieFile(platform: Platform): File? = fileOf(platform).takeIf { it.isFile }

    /** 연결된 플랫폼이면 `--cookies <파일>`, 아니면 빈 목록. */
    fun ytDlpArgs(platform: Platform): List<String> =
        cookieFile(platform)?.let { listOf(COOKIES_OPTION, it.absolutePath) }.orEmpty()

    @Synchronized
    fun save(platform: Platform, cookieText: String) {
        dir.mkdirs()
        // 쓰는 도중 yt-dlp가 반쯤 쓴 파일을 읽지 않게 임시 파일에 쓴 뒤 바꾼다.
        val temp = File(dir, "${platform.name}$TEMP_SUFFIX")
        temp.writeText(cookieText)
        val target = fileOf(platform)
        if (!temp.renameTo(target)) {
            target.delete()
            check(temp.renameTo(target)) { "쿠키 파일 저장 실패: ${platform.name}" }
        }
        mutableConnected.value = scan()
        mutableRevision.value += 1
    }

    @Synchronized
    fun disconnect(platform: Platform) {
        fileOf(platform).delete()
        mutableConnected.value = scan()
        mutableRevision.value += 1
    }

    private fun fileOf(platform: Platform) = File(dir, "${platform.name}$FILE_SUFFIX")

    private fun scan(): Set<Platform> = Platform.entries.filter { fileOf(it).isFile }.toSet()

    private companion object {
        const val COOKIES_OPTION = "--cookies"
        const val FILE_SUFFIX = ".txt"
        const val TEMP_SUFFIX = ".tmp"
    }
}

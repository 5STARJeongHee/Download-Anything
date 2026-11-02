// 미디어 제목으로 안드로이드에서 안전한 파일명을 만든다
package com.downloadanything.domain

object FileNamer {

    const val MAX_BASE_LENGTH = 120
    private const val MAX_EXTENSION_LENGTH = 5
    private const val FALLBACK_NAME = "download"

    private val FORBIDDEN_CHARS = Regex("""[\\/:*?"<>|\x00-\x1F]""")
    private val WHITESPACE = Regex("""\s+""")

    fun build(title: String, extension: String): String {
        // 확장자가 외부 메타데이터에서 올 수 있으므로 영숫자만 남겨 경로 조작을 막는다.
        val ext = extension.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.take(MAX_EXTENSION_LENGTH)
        val base = title
            .replace(WHITESPACE, " ")
            .replace(FORBIDDEN_CHARS, "_")
            .trim('.', ' ')
            .truncate(MAX_BASE_LENGTH)
            .trimEnd('.', ' ')
            .ifEmpty { FALLBACK_NAME }
        return if (ext.isEmpty()) base else "$base.$ext"
    }

    // 이모지 같은 서로게이트 쌍이 반으로 잘리지 않게 한다.
    private fun String.truncate(max: Int): String {
        if (length <= max) return this
        val cut = take(max)
        return if (cut.last().isHighSurrogate()) cut.dropLast(1) else cut
    }
}

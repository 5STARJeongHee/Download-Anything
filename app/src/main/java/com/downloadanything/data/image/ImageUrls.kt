// 이미지 URL에서 확장자를 뽑는 도우미
package com.downloadanything.data.image

object ImageUrls {

    private const val DEFAULT_EXTENSION = "jpg"
    private val EXTENSION = Regex("""^[a-z0-9]{2,5}$""")

    fun extensionOf(url: String): String {
        val path = url.substringBefore('#').substringBefore('?').substringAfterLast('/')
        val ext = path.substringAfterLast('.', "").lowercase()
        return if (EXTENSION.matches(ext)) ext else DEFAULT_EXTENSION
    }
}

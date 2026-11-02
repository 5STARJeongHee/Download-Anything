// DownloadRequest를 WorkManager 입력값(키-값)으로 바꾸고 되돌린다
package com.downloadanything.work

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadRequest
import com.downloadanything.domain.model.RemoteImage

object DownloadRequestCodec {

    const val KEY_URL = "url"
    const val KEY_TITLE = "title"
    const val KEY_PRESET = "preset"
    const val KEY_IMAGE_URLS = "image_urls"
    const val KEY_IMAGE_EXTENSIONS = "image_extensions"

    fun encode(request: DownloadRequest): Map<String, Any> = mapOf(
        KEY_URL to request.url,
        KEY_TITLE to request.title,
        KEY_PRESET to request.preset.name,
        KEY_IMAGE_URLS to request.images.map { it.url }.toTypedArray(),
        KEY_IMAGE_EXTENSIONS to request.images.map { it.extension }.toTypedArray(),
    )

    fun decode(values: Map<String, Any?>): DownloadRequest? {
        val url = values[KEY_URL] as? String ?: return null
        val title = values[KEY_TITLE] as? String ?: return null
        val presetName = values[KEY_PRESET] as? String ?: return null
        val preset = DownloadPreset.entries.firstOrNull { it.name == presetName } ?: return null
        val urls = (values[KEY_IMAGE_URLS] as? Array<*>)?.filterIsInstance<String>().orEmpty()
        val extensions = (values[KEY_IMAGE_EXTENSIONS] as? Array<*>)?.filterIsInstance<String>().orEmpty()
        if (urls.size != extensions.size) return null
        return DownloadRequest(url, title, preset, urls.zip(extensions) { u, e -> RemoteImage(u, e) })
    }
}

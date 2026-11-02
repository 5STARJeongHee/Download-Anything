// 추출한 이미지 URL들을 작업 폴더에 내려받는다
package com.downloadanything.data.image

import com.downloadanything.domain.FileNamer
import com.downloadanything.domain.model.RemoteImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class ImageDownloader(private val client: OkHttpClient) {

    suspend fun download(
        images: List<RemoteImage>,
        title: String,
        outputDir: File,
        onProgress: (done: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        images.forEachIndexed { index, image ->
            ensureActive()
            if (!image.url.startsWith(HTTPS_PREFIX)) throw IOException("https가 아닌 이미지 주소: ${image.url}")
            val baseName = if (images.size == 1) title else "$title ${index + 1}"
            val file = File(outputDir, FileNamer.build(baseName, image.extension))
            client.newCall(Request.Builder().url(image.url).build()).await().use { response ->
                if (!response.isSuccessful) throw IOException("이미지 응답 ${response.code}: ${image.url}")
                response.body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            }
            onProgress(index + 1, images.size)
        }
    }

    private companion object {
        const val HTTPS_PREFIX = "https://"
    }
}

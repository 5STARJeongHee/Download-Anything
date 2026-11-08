// 추출한 이미지·영상 파일 URL들을 작업 폴더에 내려받는다
package com.downloadanything.data.image

import com.downloadanything.domain.FileNamer
import com.downloadanything.domain.model.RemoteImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
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
            if (!image.url.startsWith(HTTPS_PREFIX)) throw IOException("https가 아닌 파일 주소: ${image.url}")
            val baseName = if (images.size == 1) title else "$title ${index + 1}"
            val file = File(outputDir, FileNamer.build(baseName, image.extension))
            val call = client.newCall(Request.Builder().url(image.url).build())
            // await()는 응답 헤더까지만 취소와 연결된다. 영상처럼 본문이 크면 읽는 중에도 끊어야 하므로
            // 취소되면 바로 깨어나는 자식 코루틴이 연결을 끊는다.
            val canceller = launch { try { awaitCancellation() } finally { call.cancel() } }
            try {
                call.await().use { response ->
                    if (!response.isSuccessful) throw IOException("파일 응답 ${response.code}: ${image.url}")
                    response.body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                }
            } finally {
                canceller.cancel()
            }
            onProgress(index + 1, images.size)
        }
    }

    private companion object {
        const val HTTPS_PREFIX = "https://"
    }
}

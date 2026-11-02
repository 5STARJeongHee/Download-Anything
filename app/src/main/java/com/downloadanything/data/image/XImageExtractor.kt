// X 임베드 API로 트윗의 사진을 찾는 추출기(yt-dlp는 X 사진을 받지 못한다)
package com.downloadanything.data.image

import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaExtractor
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class XImageExtractor(private val client: OkHttpClient) : MediaExtractor {

    override suspend fun extract(link: ParsedLink): MediaInfo = withContext(Dispatchers.IO) {
        val id = XSyndicationParser.tweetId(link.url)
            ?: throw MediaUnavailableException(FailureReason.UNSUPPORTED_URL, "트윗 ID 없음")
        val request = Request.Builder().url("$ENDPOINT?id=$id&token=$TOKEN").build()
        client.newCall(request).await().use { response ->
            if (response.code == HTTP_NOT_FOUND) throw MediaUnavailableException(FailureReason.UNAVAILABLE)
            if (!response.isSuccessful) throw IOException("X 임베드 API 응답 ${response.code}")
            XSyndicationParser.parse(response.body.string(), link)
        }
    }

    private companion object {
        const val ENDPOINT = "https://cdn.syndication.twimg.com/tweet-result"

        // 2026-11 기준 서버가 token 값을 검사하지 않는다(임의 값으로 응답 확인). 검사하기 시작하면 계산식으로 바꾼다.
        const val TOKEN = "a"
        const val HTTP_NOT_FOUND = 404
    }
}

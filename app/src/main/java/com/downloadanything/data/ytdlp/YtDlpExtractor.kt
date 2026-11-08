// yt-dlp로 링크 정보를 조회하는 추출기
package com.downloadanything.data.ytdlp

import com.downloadanything.domain.ErrorClassifier
import com.downloadanything.domain.FormatSelector
import com.downloadanything.domain.model.MediaExtractor
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform

/** @param cookieArgs 플랫폼에 연결된 계정이 있으면 yt-dlp 쿠키 인자를 준다. */
class YtDlpExtractor(
    private val engine: YtDlpEngine,
    private val cookieArgs: (Platform) -> List<String> = { emptyList() },
) : MediaExtractor {

    override suspend fun extract(link: ParsedLink): MediaInfo {
        val raw = try {
            engine.dumpJson(link.url, FormatSelector.INFO_ARGS + cookieArgs(link.platform))
        } catch (e: EngineException) {
            throw MediaUnavailableException(ErrorClassifier.classify(e.message), e.message, e)
        }
        return YtDlpInfoParser.parse(raw, link)
    }
}

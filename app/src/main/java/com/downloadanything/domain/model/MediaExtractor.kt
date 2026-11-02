// 링크에서 미디어 정보를 찾는 추출기 인터페이스
package com.downloadanything.domain.model

interface MediaExtractor {
    /** 실패하면 [MediaUnavailableException] 또는 입출력 예외를 던진다. */
    suspend fun extract(link: ParsedLink): MediaInfo
}

// 추출·다운로드 실패 원인과 사용자에게 보여줄 문장
package com.downloadanything.domain.model

enum class FailureReason(val userMessage: String, val suggestsEngineUpdate: Boolean = false) {
    UNSUPPORTED_URL("지원하지 않는 링크입니다."),
    LOGIN_REQUIRED("로그인이 필요하거나 비공개 콘텐츠입니다."),
    UNAVAILABLE("콘텐츠를 볼 수 없습니다. 삭제됐거나 지역 제한일 수 있습니다."),
    BLOCKED("사이트가 요청을 막았습니다. 엔진을 업데이트한 뒤 다시 시도해 주세요.", suggestsEngineUpdate = true),
    NETWORK("네트워크에 연결할 수 없습니다. 연결을 확인해 주세요."),
    NO_MEDIA("받을 수 있는 영상·음악·이미지를 찾지 못했습니다.", suggestsEngineUpdate = true),
    DRM("DRM으로 보호된 콘텐츠는 받을 수 없습니다."),
    UNKNOWN("알 수 없는 오류가 발생했습니다.", suggestsEngineUpdate = true),
}

class MediaUnavailableException(
    val reason: FailureReason,
    detail: String? = null,
    cause: Throwable? = null,
) : Exception(detail ?: reason.name, cause)

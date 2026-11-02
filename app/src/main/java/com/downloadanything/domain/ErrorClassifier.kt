// yt-dlp·네트워크 오류 메시지를 사용자용 실패 원인으로 분류한다
package com.downloadanything.domain

import com.downloadanything.domain.model.FailureReason

object ErrorClassifier {

    // 위에서부터 먼저 맞는 규칙을 쓴다. "login required"가 "not available"보다 먼저 와야 한다.
    private val RULES: List<Pair<FailureReason, List<String>>> = listOf(
        FailureReason.UNSUPPORTED_URL to listOf("unsupported url"),
        FailureReason.DRM to listOf("drm protected", "this video is drm"),
        FailureReason.LOGIN_REQUIRED to listOf("sign in", "login required", "log in", "private video", "cookies"),
        FailureReason.BLOCKED to listOf("http error 403", "http error 429", "too many requests"),
        FailureReason.NETWORK to listOf(
            "errno 7", "errno -2", "errno 101", "no address associated", "name or service not known",
            "network is unreachable", "timed out", "connection refused", "unable to resolve host",
        ),
        FailureReason.NO_MEDIA to listOf("no video formats", "requested format is not available", "no video could be found"),
        FailureReason.UNAVAILABLE to listOf("unavailable", "not available", "has been removed", "does not exist", "http error 404"),
    )

    fun classify(message: String?): FailureReason {
        val text = message?.lowercase() ?: return FailureReason.UNKNOWN
        return RULES.firstOrNull { (_, keywords) -> keywords.any { it in text } }?.first ?: FailureReason.UNKNOWN
    }
}

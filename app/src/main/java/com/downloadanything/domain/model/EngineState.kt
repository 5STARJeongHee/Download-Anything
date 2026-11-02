// 내장 yt-dlp 엔진의 준비 상태
package com.downloadanything.domain.model

sealed interface EngineState {
    data object Initializing : EngineState
    data class Updating(val version: String?) : EngineState
    data class Ready(
        val version: String?,
        val isFfmpegAvailable: Boolean,
        val isUpdateFailed: Boolean = false,
    ) : EngineState
    data class Failed(val message: String) : EngineState
}

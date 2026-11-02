// 저장 위치를 정하는 미디어 종류(영상·음악·이미지·기타)
package com.downloadanything.domain.model

enum class MediaCategory {
    VIDEO, AUDIO, IMAGE, OTHER;

    companion object {
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "flv", "avi", "ts")
        private val AUDIO_EXTENSIONS = setOf("m4a", "mp3", "opus", "ogg", "oga", "aac", "flac", "wav")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "avif")

        fun fromExtension(extension: String): MediaCategory = when (extension.lowercase()) {
            in VIDEO_EXTENSIONS -> VIDEO
            in AUDIO_EXTENSIONS -> AUDIO
            in IMAGE_EXTENSIONS -> IMAGE
            else -> OTHER
        }
    }
}

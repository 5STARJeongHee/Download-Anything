// 다운로드 폴더에서 저장할 파일을 고르고 저장 위치 종류를 정한다
package com.downloadanything.domain

import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.MediaCategory

object DownloadOutputs {

    fun categoryFor(preset: DownloadPreset): MediaCategory = when (preset) {
        DownloadPreset.BEST_VIDEO, DownloadPreset.VIDEO_720P -> MediaCategory.VIDEO
        DownloadPreset.AUDIO_M4A, DownloadPreset.AUDIO_MP3 -> MediaCategory.AUDIO
        DownloadPreset.IMAGE -> MediaCategory.IMAGE
    }

    /** `.part`, 남은 썸네일 등은 빼고 프리셋에 맞는 파일 이름만 정렬해 돌려준다. */
    fun select(fileNames: List<String>, preset: DownloadPreset): List<String> {
        val accepted = when (categoryFor(preset)) {
            MediaCategory.VIDEO -> setOf(MediaCategory.VIDEO)
            // ffmpeg 없이 받은 음악은 webm·mp4 컨테이너일 수 있다.
            MediaCategory.AUDIO -> setOf(MediaCategory.AUDIO, MediaCategory.VIDEO)
            MediaCategory.IMAGE -> setOf(MediaCategory.IMAGE)
            MediaCategory.OTHER -> emptySet()
        }
        return fileNames.filter { MediaCategory.fromExtension(it.substringAfterLast('.', "")) in accepted }.sorted()
    }
}

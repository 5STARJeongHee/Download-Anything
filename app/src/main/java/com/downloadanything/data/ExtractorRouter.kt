// 플랫폼에 맞는 추출기를 고르고, 실패하면 다른 추출기로 넘기며 결과를 합친다
package com.downloadanything.data

import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaExtractor
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.domain.model.Platform
import kotlinx.coroutines.CancellationException

class ExtractorRouter(
    private val ytDlp: MediaExtractor,
    private val xImages: MediaExtractor,
) {

    suspend fun extract(link: ParsedLink): MediaInfo {
        val info = if (link.platform == Platform.X) extractX(link) else ytDlp.extract(link)
        if (!info.hasMedia) throw MediaUnavailableException(FailureReason.NO_MEDIA)
        return info
    }

    // X 사진은 임베드 API로만 받을 수 있다. 영상도 임베드 API의 mp4가 있으면 그것을 쓴다.
    // yt-dlp의 X 추출기는 X API 변경으로 자주 깨지기 때문이다(context-notes 2026-11-08).
    private suspend fun extractX(link: ParsedLink): MediaInfo {
        var embedFailure: FailureReason? = null
        val embed = try {
            xImages.extract(link)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            embedFailure = (e as? MediaUnavailableException)?.reason
            null
        }
        if (embed != null && embed.directVideos.isNotEmpty()) return embed
        if (embed != null && embed.images.isNotEmpty() && !embed.hasVideo) return embed

        // 임베드 API가 로그인 벽을 알렸으면 yt-dlp의 "못 찾음"·알 수 없는 오류보다 그쪽이 정확하다.
        val isLoginWall = embedFailure == FailureReason.LOGIN_REQUIRED
        val video = try {
            ytDlp.extract(link)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (embed != null && embed.images.isNotEmpty()) return embed.copy(hasVideo = false, hasAudio = false)
            val reason = (e as? MediaUnavailableException)?.reason
            if (isLoginWall && reason != FailureReason.NETWORK) {
                throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED, e.message, e)
            }
            throw e
        }
        if (isLoginWall && !video.hasMedia) throw MediaUnavailableException(FailureReason.LOGIN_REQUIRED)
        return if (embed == null) video else video.copy(images = video.images + embed.images)
    }
}

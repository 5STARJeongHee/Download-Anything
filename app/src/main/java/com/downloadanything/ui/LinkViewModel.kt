// 링크 하나의 정보 조회와 다운로드 시작을 관리한다(공유 시트·홈 화면 공용)
package com.downloadanything.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloadanything.DownloadAnythingApp
import com.downloadanything.domain.ErrorClassifier
import com.downloadanything.domain.PresetPolicy
import com.downloadanything.domain.model.DownloadPreset
import com.downloadanything.domain.model.DownloadRequest
import com.downloadanything.domain.model.FailureReason
import com.downloadanything.domain.model.MediaInfo
import com.downloadanything.domain.model.MediaUnavailableException
import com.downloadanything.domain.model.ParsedLink
import com.downloadanything.work.DownloadScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LinkUiState {
    data object Idle : LinkUiState
    data class Loading(val link: ParsedLink) : LinkUiState
    data class Ready(val info: MediaInfo, val presets: List<DownloadPreset>) : LinkUiState
    data class Failed(val link: ParsedLink, val reason: FailureReason) : LinkUiState
    data class Started(val title: String, val preset: DownloadPreset) : LinkUiState
}

class LinkViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DownloadAnythingApp
    private val mutableState = MutableStateFlow<LinkUiState>(LinkUiState.Idle)
    private var loadJob: Job? = null

    val state: StateFlow<LinkUiState> = mutableState.asStateFlow()
    val engineState = app.engineState

    fun load(link: ParsedLink) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = LinkUiState.Loading(link)
            mutableState.value = fetch(link)
        }
    }

    fun retryAfterEngineUpdate() {
        val link = (state.value as? LinkUiState.Failed)?.link ?: return
        app.requestEngineUpdate()
        load(link)
    }

    fun retry() {
        (state.value as? LinkUiState.Failed)?.link?.let(::load)
    }

    fun download(preset: DownloadPreset) {
        val ready = state.value as? LinkUiState.Ready ?: return
        val request = DownloadRequest(
            url = ready.info.sourceUrl,
            title = ready.info.title,
            preset = preset,
            images = if (preset == DownloadPreset.IMAGE) ready.info.images else emptyList(),
        )
        DownloadScheduler.enqueue(app, request)
        mutableState.value = LinkUiState.Started(ready.info.title, preset)
    }

    fun clear() {
        loadJob?.cancel()
        mutableState.value = LinkUiState.Idle
    }

    private suspend fun fetch(link: ParsedLink): LinkUiState = try {
        val engine = app.awaitReadyEngine()
        val info = app.extractorRouter.extract(link)
        LinkUiState.Ready(info, PresetPolicy.available(info, engine.isFfmpegAvailable))
    } catch (e: CancellationException) {
        throw e
    } catch (e: MediaUnavailableException) {
        LinkUiState.Failed(link, e.reason)
    } catch (e: Exception) {
        LinkUiState.Failed(link, ErrorClassifier.classify(e.message))
    }
}

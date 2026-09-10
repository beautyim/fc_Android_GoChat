package com.example.demoproject.product.match

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.MatchStartResult
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.ui.foundation.R as FoundationR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatchViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        MatchUiState(status = str(FoundationR.string.status_ready)),
    )
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    fun onIntent(intent: MatchIntent) {
        when (intent) {
            MatchIntent.LoadInfo -> loadInfo()
            MatchIntent.Start -> start()
        }
    }

    private fun loadInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.match_status_loading_info)) }
            when (val result = runtime.matchRepository.getMatchInfo(source = "feature_match")) {
                is AppResult.Success -> {
                    val info = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(FoundationR.string.status_ok),
                            infoSummary = str(
                                R.string.match_status_info_fmt,
                                info.matchFreeCount,
                                info.payUserMatchPrice.toString(),
                                info.vipMatchPrice.toString(),
                                info.avatarList.size,
                            ),
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, status = result.message) }
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.match_status_starting)) }
            when (val result = runtime.matchRepository.startMatch(source = "feature_match")) {
                is MatchStartResult.Success -> {
                    val info = result.info
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(R.string.match_status_started),
                            startSummary = str(
                                R.string.match_status_start_fmt,
                                info.sessionId?.toString().orEmpty(),
                                info.nextAction.toString(),
                                info.matchFreeCount ?: 0,
                            ),
                        )
                    }
                }
                is MatchStartResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, status = result.message) }
                }
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

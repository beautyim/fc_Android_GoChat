package com.example.demoproject.product.call

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.callkit.CallKitHolder
import com.example.demoproject.platform.callkit.CallMediaType
import com.example.demoproject.platform.callkit.signaling.OutgoingInviteRequest
import com.example.demoproject.platform.data.model.CallRecordType
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.CallCreateResult
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.ui.foundation.R as FoundationR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        CallUiState(
            status = str(FoundationR.string.status_ready),
            mediaHint = str(
                R.string.call_media_hint_fmt,
                CallMediaType.Video.toString(),
                CallMediaType.Voice.toString(),
            ),
        ),
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        onIntent(CallIntent.LoadRecords)
        viewModelScope.launch {
            CallKitHolder.coordinator?.state?.collect { state ->
                _uiState.update { it.copy(coordinatorState = state.toString()) }
            }
        }
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            CallIntent.LoadRecords -> loadRecords()
            CallIntent.StartVideoCall -> startCall(CallRoom.CALL_TYPE_VIDEO)
            CallIntent.StartVoiceCall -> startCall(CallRoom.CALL_TYPE_VOICE)
            CallIntent.Hangup -> hangup()
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.call_status_loading_records)) }
            when (val result = runtime.callRepository.getCallRecords(page = 1)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(
                                R.string.call_status_loaded_fmt,
                                result.data.records.size,
                                result.data.hasMore.toString(),
                            ),
                            records = result.data.records.map { r ->
                                val media = when (r.callType) {
                                    CallRecordType.Voice -> CallMediaType.Voice
                                    CallRecordType.Video -> CallMediaType.Video
                                }
                                str(
                                    R.string.call_row_fmt,
                                    r.peer.nickname,
                                    r.direction.toString(),
                                    r.status.toString(),
                                    media.toString(),
                                    r.callPrice.toString(),
                                )
                            },
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, status = result.message) }
            }
        }
    }

    private fun startCall(callType: Int) {
        viewModelScope.launch {
            val coordinator = CallKitHolder.coordinator
            if (coordinator == null) {
                _uiState.update { it.copy(status = str(R.string.call_status_coordinator_missing)) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, status = str(R.string.call_status_resolving_callee)) }
            val target = when (val discover = runtime.profileRepository.discoverUsers(page = 1)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(R.string.call_status_discover_failed_fmt, discover.message),
                        )
                    }
                    return@launch
                }
                is AppResult.Success -> discover.data.users.firstOrNull()?.id?.toLongOrNull()
            }
            if (target == null || target <= 0L) {
                _uiState.update { it.copy(isLoading = false, status = str(R.string.call_status_no_peer)) }
                return@launch
            }
            _uiState.update { it.copy(status = str(R.string.call_status_creating_fmt, target.toString())) }
            when (val created = runtime.callSessionRepository.createCall(
                targetUid = target,
                callType = callType,
                fromType = CallRoom.FROM_HOME,
            )) {
                is CallCreateResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(
                                R.string.call_status_create_failed_fmt,
                                created.code.toString(),
                                created.message,
                            ),
                        )
                    }
                }
                is CallCreateResult.Success -> {
                    val room = created.room
                    coordinator.resetIfTerminal()
                    coordinator.startOutgoingCall(
                        OutgoingInviteRequest(
                            inviteId = room.effectiveHttpRoomId,
                            calleeUserId = target.toString(),
                            channelId = room.channel.ifBlank { room.roomId.toString() },
                            rtcToken = room.token,
                            rtcUid = room.uid,
                        ),
                    )
                    if (room.appId.isNotBlank() && room.token.isNotBlank()) {
                        runtime.callSessionRepository.reportCallSuccess(
                            roomId = room.effectiveHttpRoomId,
                            fencingToken = room.fencingToken,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(
                                R.string.call_status_outgoing_fmt,
                                room.effectiveHttpRoomId,
                                room.channel,
                                room.appId.length,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun hangup() {
        val coordinator = CallKitHolder.coordinator
        if (coordinator == null) {
            _uiState.update { it.copy(status = str(R.string.call_status_coordinator_missing)) }
            return
        }
        coordinator.hangup()
        _uiState.update {
            it.copy(status = str(R.string.call_status_hangup_fmt, coordinator.state.value.toString()))
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

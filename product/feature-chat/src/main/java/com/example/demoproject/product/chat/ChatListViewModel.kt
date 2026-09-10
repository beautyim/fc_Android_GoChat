package com.example.demoproject.product.chat

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.toMessageTimelineMillis
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Session-scoped dismiss for the notification banner (Figma 154:3269):
 * hidden for the rest of this process; shown again after a cold start.
 */
internal object ChatNotificationBannerSession {
    @Volatile
    var dismissed: Boolean = false
}

class ChatListViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChatListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null
    private var pageJob: Job? = null

    init {
        trackFirstDialogEntered()
        refreshNotificationBannerVisibility()
        viewModelScope.launch {
            runtime.messageRepository.observeConversations().collect { list ->
                _uiState.update { state ->
                    state.copy(conversations = list.map { it.toUi() })
                }
            }
        }
        viewModelScope.launch {
            runtime.messageRepository.observeTotalUnreadCount().collect { count ->
                _uiState.update { it.copy(totalUnread = count) }
            }
        }
        onIntent(ChatListIntent.Refresh)
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            ChatListIntent.Refresh -> refresh(force = true)
            ChatListIntent.LoadMore -> loadMore()
            ChatListIntent.Sync -> sync()
            is ChatListIntent.OpenConversation -> viewModelScope.launch {
                _uiState.update { it.copy(revealedConversationId = null) }
                _effects.send(
                    ChatListEffect.OpenChatDetail(
                        conversationId = intent.conversation.id,
                        nickname = intent.conversation.nickname,
                    ),
                )
            }
            is ChatListIntent.RevealActions -> _uiState.update {
                it.copy(revealedConversationId = intent.conversationId)
            }
            is ChatListIntent.TogglePin -> togglePin(intent.conversationId)
            is ChatListIntent.RequestDelete -> _uiState.update {
                it.copy(
                    pendingDeleteId = intent.conversationId,
                    revealedConversationId = null,
                )
            }
            ChatListIntent.ConfirmDelete -> confirmDelete()
            ChatListIntent.CancelDelete -> _uiState.update {
                it.copy(pendingDeleteId = null, isDeleting = false)
            }
            ChatListIntent.DismissNotificationBanner -> {
                ChatNotificationBannerSession.dismissed = true
                _uiState.update { it.copy(showNotificationBanner = false) }
            }
            ChatListIntent.TurnOnNotifications -> viewModelScope.launch {
                _effects.send(ChatListEffect.RequestNotificationPermission)
            }
            ChatListIntent.NotificationPermissionResult -> refreshNotificationBannerVisibility()
            ChatListIntent.OpenSupport -> viewModelScope.launch {
                _effects.send(ChatListEffect.OpenSupport)
            }
            ChatListIntent.StartChatting -> viewModelScope.launch {
                // Figma 154:3267 — jump to the online list (Home).
                _effects.send(ChatListEffect.OpenHome)
            }
        }
    }

    private fun refresh(force: Boolean) {
        if (loadJob?.isActive == true) return
        val cached = _uiState.value
        if (!force && cached.hasLoaded) return

        loadJob = viewModelScope.launch {
            val keepContent = force && cached.hasLoaded
            _uiState.update {
                it.copy(
                    isLoading = !keepContent,
                    isRefreshing = keepContent,
                    isLoadingMore = false,
                    errorMessage = null,
                    page = if (keepContent) it.page else 1,
                    hasMore = if (keepContent) it.hasMore else false,
                )
            }
            when (val result = runtime.messageRepository.getConversations(1)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            conversations = result.data.conversations.map { c -> c.toUi() },
                            page = 1,
                            hasMore = result.data.hasMore,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            hasLoaded = true,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message,
                            hasLoaded = keepContent,
                        )
                    }
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isLoading || state.isRefreshing) return
        if (pageJob?.isActive == true || loadJob?.isActive == true) return

        pageJob = viewModelScope.launch {
            val nextPage = state.page + 1
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            when (val result = runtime.messageRepository.getConversations(nextPage)) {
                is AppResult.Success -> {
                    val merged = (state.conversations + result.data.conversations.map { it.toUi() })
                        .distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            conversations = merged,
                            page = nextPage,
                            hasMore = result.data.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoadingMore = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            when (val result = runtime.messageRepository.syncConversationList()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _effects.send(ChatListEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun togglePin(conversationId: String) {
        val current = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(revealedConversationId = null) }
            when (
                val result = runtime.messageRepository.setConversationPinned(
                    conversationId,
                    !current.isPinned,
                )
            ) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _effects.send(ChatListEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = runtime.messageRepository.deleteConversation(id)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(pendingDeleteId = null, isDeleting = false)
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _effects.send(ChatListEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun refreshNotificationBannerVisibility() {
        val app = getApplication<Application>()
        val enabled = NotificationManagerCompat.from(app).areNotificationsEnabled() &&
            hasPostNotificationPermission(app)
        _uiState.update {
            it.copy(
                showNotificationBanner = !enabled && !ChatNotificationBannerSession.dismissed,
            )
        }
    }

    private fun hasPostNotificationPermission(app: Application): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun trackFirstDialogEntered() {
        val userId = runtime.sessionManager.currentUserId.orEmpty()
        AnalyticsHolder.tracker?.track(AnalyticsEvent.FirstDialog(userId = userId))
    }

    private fun Conversation.toUi(): ChatConversationUi {
        val previewTime = lastMessage?.createdAt?.toMessageTimelineMillis()
            ?: updatedAt.toMessageTimelineMillis()
        return ChatConversationUi(
            id = id,
            peerId = peer.id,
            peerExternalUserId = peer.externalUserId.ifBlank { peer.id },
            nickname = peer.nickname.ifBlank { peer.id },
            age = peer.age,
            avatarUrl = peer.avatar,
            preview = lastMessage.toPreview(),
            timestampLabel = formatChatTimestamp(previewTime),
            unreadCount = unreadCount,
            isPinned = isPinned,
        )
    }

    private fun Message?.toPreview(): String {
        if (this == null) return ""
        return when (type) {
            MessageType.Text, MessageType.System -> content
            MessageType.Image -> str(R.string.chat_preview_image)
            MessageType.Video -> str(R.string.chat_preview_video)
            MessageType.Voice -> str(R.string.chat_preview_voice)
            MessageType.Emoji -> content.ifBlank { str(R.string.chat_preview_emoji) }
            MessageType.Gift -> str(R.string.chat_preview_gift)
            MessageType.PrivatePhoto -> str(R.string.chat_preview_private_photo)
            MessageType.PrivateVideo -> str(R.string.chat_preview_private_video)
        }
    }

    private fun formatChatTimestamp(millis: Long): String {
        if (millis <= 0L) return ""
        val locale = Locale.getDefault()
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val startOfToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfYesterday = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return when {
            then.timeInMillis >= startOfToday.timeInMillis -> {
                SimpleDateFormat("hh:mma", locale).format(Date(millis))
                    .replace("am", "AM")
                    .replace("pm", "PM")
            }
            then.timeInMillis >= startOfYesterday.timeInMillis -> {
                str(R.string.chat_time_yesterday)
            }
            then.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMMM d", locale).format(Date(millis))
            }
            else -> {
                DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(millis))
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

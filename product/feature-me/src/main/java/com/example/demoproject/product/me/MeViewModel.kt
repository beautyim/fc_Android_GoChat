package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.ProfileHomeDetail
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        viewModelScope.launch {
            runtime.vipStatusStore.status.collect { vip ->
                if (vip == null) return@collect
                _uiState.update {
                    it.copy(
                        isVip = vip.isVip,
                        vipExpiryText = formatVipExpiryForDisplay(vip.expiryText),
                    )
                }
            }
        }
        loadMe(force = false)
    }

    fun onIntent(intent: MeIntent) {
        when (intent) {
            MeIntent.Refresh -> loadMe(force = true)
            MeIntent.OpenPublicProfile -> openPublicProfile()
            MeIntent.OpenCamera -> emitComingSoon(R.string.me_message_camera_soon)
            MeIntent.OpenAddCoins -> viewModelScope.launch { _effects.send(MeEffect.OpenStore) }
            MeIntent.OpenVip -> emitComingSoon(R.string.me_message_vip_soon)
            MeIntent.OpenGift -> emitComingSoon(R.string.me_message_gift_soon)
            MeIntent.OpenVerification -> emitComingSoon(R.string.me_message_verify_soon)
            MeIntent.OpenSettings -> emitComingSoon(R.string.me_message_settings_soon)
        }
    }

    private fun openPublicProfile() {
        val id = _uiState.value.externalUserId.trim()
        if (id.isEmpty()) {
            emitComingSoon(R.string.me_message_profile_soon)
            return
        }
        viewModelScope.launch { _effects.send(MeEffect.OpenPublicProfile(id)) }
    }

    private fun loadMe(force: Boolean) {
        val hasContent = _uiState.value.nickname.isNotBlank()
        if (!force && hasContent && !_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = runtime.profileRepository.getMyHomeDetail()) {
                is AppResult.Success -> applyDetail(result.data)
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun applyDetail(detail: ProfileHomeDetail) {
        val user = detail.user
        detail.coinBalance?.let { runtime.accountBalanceStore.update(it) }
        val expiryDisplay = formatVipExpiryForDisplay(user.vipExpireTime)
        runtime.vipStatusStore.update(
            isVip = user.isVip,
            expiryText = expiryDisplay ?: user.vipExpireTime,
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
                nickname = user.nickname,
                age = user.age,
                gender = user.gender,
                avatarUrl = user.avatar,
                countryFlag = countryCodeToFlagEmoji(user.countryCode),
                countryName = user.countryName.orEmpty(),
                followerCount = detail.followerCount,
                followingCount = detail.followingCount,
                externalUserId = user.externalUserId.ifBlank { user.id },
                isVip = user.isVip,
                vipExpiryText = expiryDisplay,
            )
        }
    }

    private fun emitComingSoon(@StringRes id: Int) {
        viewModelScope.launch {
            _effects.send(MeEffect.ShowMessage(str(id)))
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

/** Figma Me VIP line uses `yy/MM/dd` (e.g. 26/12/31). */
internal fun formatVipExpiryForDisplay(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() && it != "0" } ?: return null
    val epochSeconds = value.toLongOrNull()
    if (epochSeconds != null) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
            .toLocalDate()
            .format(VIP_DISPLAY_FORMATTER)
    }
    val parsed = runCatching {
        LocalDateTime.parse(value, VIP_DATE_TIME_FORMATTER).toLocalDate()
    }.recoverCatching {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    }.recoverCatching {
        LocalDate.parse(value, DateTimeFormatter.ofPattern("yy/MM/dd"))
    }.getOrNull()
    return parsed?.format(VIP_DISPLAY_FORMATTER) ?: value
}

internal fun countryCodeToFlagEmoji(code: String?): String {
    val normalized = code?.trim()?.uppercase().orEmpty()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    val first = 0x1F1E6 + (normalized[0].code - 'A'.code)
    val second = 0x1F1E6 + (normalized[1].code - 'A'.code)
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private val VIP_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val VIP_DISPLAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yy/MM/dd")

package com.example.demoproject.product.me

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoConfirmDialog
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onBack()
                SettingsEffect.OpenBlockedUsers -> onOpenBlockedUsers()
                SettingsEffect.OpenAbout -> onOpenAbout()
                is SettingsEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    SettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DemoColors.sheet)
                    .statusBarsPadding(),
            ) {
                SettingsTopBar(onBack = { onIntent(SettingsIntent.Back) })
            }
            when {
                state.isLoading && state.email == null && state.errorMessage == null -> {
                    SettingsSkeleton(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.md)
                            .readableContentWidth(),
                    )
                }
                state.errorMessage != null && state.email == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            Spacing.sm,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = DemoColors.textSecondary,
                            fontSize = TextSize.sm,
                        )
                        TextButton(onClick = { onIntent(SettingsIntent.Refresh) }) {
                            Text(stringResource(R.string.me_retry))
                        }
                    }
                }
                else -> {
                    SettingsList(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (state.showLogoutConfirm) {
            DemoConfirmDialog(
                title = stringResource(R.string.settings_logout_title),
                body = stringResource(R.string.settings_logout_body),
                negativeText = stringResource(R.string.settings_logout_cancel),
                positiveText = stringResource(R.string.settings_item_logout),
                onNegative = { onIntent(SettingsIntent.DismissLogout) },
                onPositive = { onIntent(SettingsIntent.ConfirmLogout) },
                isPositiveLoading = state.isLoggingOut,
                positiveContainerColor = DemoColors.dialogDestructive,
            )
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backCd = stringResource(R.string.settings_cd_back)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.navHeaderHeight)
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.md),
    ) {
        Image(
            painter = painterResource(R.drawable.settings_ic_back),
            contentDescription = backCd,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(IconSize.md)
                .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                .clickable(onClick = onBack)
                .semantics { contentDescription = backCd },
        )
        Text(
            text = stringResource(R.string.settings_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = IconSize.md + Spacing.sm),
        )
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md)
            .readableContentWidth()
            .padding(top = Spacing.md, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.settingsRowGap),
    ) {
        SettingsRow(
            iconRes = R.drawable.settings_ic_email,
            title = stringResource(R.string.settings_item_email),
            value = state.email.takeIf { state.hasBoundEmail },
            showChevron = state.hasBoundEmail,
            trailing = if (!state.hasBoundEmail) {
                { SettingsBindBadge() }
            } else {
                null
            },
            onClick = { onIntent(SettingsIntent.OpenEmail) },
        )
        SettingsRow(
            iconRes = R.drawable.settings_ic_language,
            title = stringResource(R.string.settings_item_language),
            value = state.languageLabel,
            onClick = { onIntent(SettingsIntent.OpenLanguage) },
        )
        SettingsRow(
            iconRes = R.drawable.settings_ic_blocked,
            title = stringResource(R.string.settings_item_blocked),
            onClick = { onIntent(SettingsIntent.OpenBlockedUsers) },
        )
        if (state.hasBoundEmail) {
            SettingsRow(
                iconRes = R.drawable.settings_ic_password,
                title = stringResource(R.string.settings_item_password),
                onClick = { onIntent(SettingsIntent.OpenChangePassword) },
            )
        }
        SettingsRow(
            iconRes = R.drawable.settings_ic_verify,
            title = stringResource(R.string.settings_item_verify),
            value = if (state.isAuthVerified) {
                stringResource(R.string.settings_verify_done)
            } else {
                null
            },
            showChevron = !state.isAuthVerified,
            onClick = if (state.isAuthVerified) {
                null
            } else {
                { onIntent(SettingsIntent.OpenVerification) }
            },
        )
        SettingsRow(
            iconRes = R.drawable.settings_ic_about,
            title = stringResource(R.string.settings_item_about),
            onClick = { onIntent(SettingsIntent.OpenAbout) },
        )
        if (state.showFakePaymentToggle) {
            val fakePaymentCd = stringResource(R.string.settings_item_fake_payment)
            SettingsRow(
                iconRes = R.drawable.settings_ic_debug,
                title = fakePaymentCd,
                showChevron = false,
                trailing = {
                    Switch(
                        checked = state.fakePaymentEnabled,
                        onCheckedChange = { onIntent(SettingsIntent.ToggleFakePayment(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DemoColors.onPrimaryButton,
                            checkedTrackColor = DemoColors.link,
                            uncheckedThumbColor = DemoColors.sheet,
                            uncheckedTrackColor = DemoColors.divider,
                            uncheckedBorderColor = DemoColors.divider,
                        ),
                        modifier = Modifier.semantics { contentDescription = fakePaymentCd },
                    )
                },
            )
        }
        SettingsRow(
            iconRes = R.drawable.settings_ic_logout,
            title = stringResource(R.string.settings_item_logout),
            iconBackground = DemoColors.settingsLogoutIconBg,
            onClick = { onIntent(SettingsIntent.RequestLogout) },
        )
    }
}

@Composable
private fun SettingsRow(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = true,
    iconBackground: Color = DemoColors.meMenuIconBg,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .then(clickableModifier)
            .padding(horizontal = Spacing.chipGap, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.settingsIconCircle)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(IconSize.settings),
            )
        }
        Text(
            text = title,
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.xs,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (showChevron) {
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    Image(
                        painter = painterResource(R.drawable.settings_ic_chevron),
                        contentDescription = null,
                        modifier = Modifier
                            .size(IconSize.settings)
                            .graphicsLayer { scaleX = if (isRtl) -1f else 1f },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBindBadge() {
    Text(
        text = stringResource(R.string.settings_email_bind),
        color = DemoColors.onPrimaryButton,
        fontSize = TextSize.sm,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .padding(
                horizontal = Spacing.chipGap,
                vertical = Spacing.settingsBindVertical,
            ),
    )
}

@Composable
private fun SettingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.settingsRowGap),
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.settingsIconCircle + Spacing.md * 2)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(DemoColors.sheet),
            )
        }
    }
}

private val PreviewBoundState = SettingsUiState(
    isLoading = false,
    email = "11111@123.com",
    languageLabel = "English",
    isAuthVerified = false,
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Settings bound", locale = "en")
@Composable
private fun SettingsBoundPreview() {
    DemoTheme {
        SettingsScreen(state = PreviewBoundState, onIntent = {})
    }
}

@Preview(name = "Settings unbound", locale = "en")
@Composable
private fun SettingsUnboundPreview() {
    DemoTheme {
        SettingsScreen(
            state = PreviewBoundState.copy(email = null),
            onIntent = {},
        )
    }
}

@Preview(name = "Settings verified", locale = "en")
@Preview(name = "Settings RTL", locale = "ar")
@Composable
private fun SettingsVerifiedPreview() {
    DemoTheme {
        SettingsScreen(
            state = PreviewBoundState.copy(isAuthVerified = true),
            onIntent = {},
        )
    }
}

@Preview(name = "Settings debug fake payment", locale = "en")
@Composable
private fun SettingsFakePaymentPreview() {
    DemoTheme {
        SettingsScreen(
            state = PreviewBoundState.copy(
                showFakePaymentToggle = true,
                fakePaymentEnabled = true,
            ),
            onIntent = {},
        )
    }
}

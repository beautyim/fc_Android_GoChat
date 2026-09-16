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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoConfirmDialog
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AboutUsScreen(
    viewModel: AboutUsViewModel,
    onBack: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                AboutUsEffect.NavigateBack -> onBack()
                AboutUsEffect.OpenContact -> onOpenContact()
                AboutUsEffect.OpenPrivacy -> onOpenPrivacy()
                AboutUsEffect.OpenTerms -> onOpenTerms()
                is AboutUsEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    AboutUsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun AboutUsScreen(
    state: AboutUsUiState,
    onIntent: (AboutUsIntent) -> Unit,
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
                AboutUsTopBar(onBack = { onIntent(AboutUsIntent.Back) })
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md)
                    .readableContentWidth()
                    .padding(top = Spacing.md, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.settingsRowGap),
            ) {
                AboutUsRow(
                    iconRes = R.drawable.about_ic_contact,
                    title = stringResource(R.string.about_contact),
                    onClick = { onIntent(AboutUsIntent.OpenContact) },
                )
                AboutUsRow(
                    iconRes = R.drawable.about_ic_privacy,
                    title = stringResource(R.string.about_privacy),
                    onClick = { onIntent(AboutUsIntent.OpenPrivacy) },
                )
                AboutUsRow(
                    iconRes = R.drawable.about_ic_terms,
                    title = stringResource(R.string.about_terms),
                    onClick = { onIntent(AboutUsIntent.OpenTerms) },
                )
                AboutUsRow(
                    iconRes = R.drawable.about_ic_delete,
                    title = stringResource(R.string.about_delete_account),
                    iconBackground = DemoColors.settingsLogoutIconBg,
                    onClick = { onIntent(AboutUsIntent.RequestDeleteAccount) },
                )
            }
        }
        if (state.showDeleteConfirm) {
            DemoConfirmDialog(
                title = stringResource(R.string.about_delete_title),
                body = stringResource(R.string.about_delete_body),
                negativeText = stringResource(R.string.about_cancel),
                positiveText = stringResource(R.string.about_delete),
                onNegative = { onIntent(AboutUsIntent.DismissDeleteAccount) },
                onPositive = { onIntent(AboutUsIntent.ConfirmDeleteAccount) },
                isPositiveLoading = state.isDeletingAccount,
                positiveContainerColor = DemoColors.dialogDestructive,
                textWidth = ComponentSize.confirmDialogDeleteTextWidth,
            )
        }
    }
}

@Composable
private fun AboutUsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.navHeaderHeight)
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.md),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.about_ic_back),
            contentDescription = stringResource(R.string.about_back),
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            mirrorInRtl = true,
        )
        Text(
            text = stringResource(R.string.about_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            lineHeight = TextSize.mdLine,
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
private fun AboutUsRow(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackground: Color = DemoColors.meMenuIconBg,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .clickable(role = Role.Button, onClick = onClick)
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
            lineHeight = TextSize.smLine,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.about_ic_chevron),
            contentDescription = null,
            modifier = Modifier
                .size(IconSize.settings)
                .graphicsLayer { scaleX = if (isRtl) -1f else 1f },
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "About Us", locale = "en")
@Preview(name = "About Us RTL", locale = "ar")
@Composable
private fun AboutUsPreview() {
    DemoTheme {
        AboutUsScreen(state = AboutUsUiState(), onIntent = {})
    }
}

@Preview(name = "Delete account confirmation", locale = "en")
@Composable
private fun DeleteAccountConfirmationPreview() {
    DemoTheme {
        AboutUsScreen(
            state = AboutUsUiState(showDeleteConfirm = true),
            onIntent = {},
        )
    }
}

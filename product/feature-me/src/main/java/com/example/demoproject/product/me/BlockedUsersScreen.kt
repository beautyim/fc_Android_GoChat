package com.example.demoproject.product.me

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

/** Figma 1:3732 — 14sp / 17sp line box. */
private val BlockedNameStyle = TextStyle(
    fontSize = TextSize.sm,
    lineHeight = TextSize.chatListNameLine,
    fontWeight = FontWeight.Medium,
    color = DemoColors.textPrimary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:3733 — 12sp / 14sp line box. */
private val BlockedCountryStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = TextSize.chatListPreviewLine,
    fontWeight = FontWeight.Normal,
    color = DemoColors.textSecondary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:3735 — 14sp / 17sp line box. */
private val BlockedActionStyle = TextStyle(
    fontSize = TextSize.sm,
    lineHeight = TextSize.chatListNameLine,
    fontWeight = FontWeight.Medium,
    color = DemoColors.onPrimaryButton,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:3717 — 16sp / 19sp line box. */
private val BlockedEmptyStyle = TextStyle(
    fontSize = TextSize.md,
    lineHeight = TextSize.mdLine,
    fontWeight = FontWeight.Medium,
    color = DemoColors.textSecondary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Composable
fun BlockedUsersScreen(
    viewModel: BlockedUsersViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                BlockedUsersEffect.NavigateBack -> onBack()
                is BlockedUsersEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    BlockedUsersScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    state: BlockedUsersUiState,
    onIntent: (BlockedUsersIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.sheet)
                .statusBarsPadding(),
        ) {
            BlockedUsersTopBar(onBack = { onIntent(BlockedUsersIntent.Back) })
        }
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(BlockedUsersIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> BlockedUsersSkeletonList(
                    modifier = Modifier.fillMaxSize(),
                )
                state.errorMessage != null && state.users.isEmpty() -> BlockedUsersError(
                    message = state.errorMessage,
                    onRetry = { onIntent(BlockedUsersIntent.Refresh) },
                    modifier = Modifier.align(Alignment.Center),
                )
                state.isEmpty -> BlockedUsersEmpty(modifier = Modifier.fillMaxSize())
                else -> BlockedUsersList(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun BlockedUsersTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backCd = stringResource(R.string.blocked_back)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.navHeaderHeight)
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.md),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.settings_ic_back),
            contentDescription = backCd,
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            mirrorInRtl = true,
        )
        Text(
            text = stringResource(R.string.blocked_title),
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
private fun BlockedUsersList(
    state: BlockedUsersUiState,
    onIntent: (BlockedUsersIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .readableContentWidth(),
        contentPadding = PaddingValues(
            start = Spacing.md,
            top = Spacing.blockedListTop,
            end = Spacing.md,
            bottom = Spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(
            items = state.users,
            key = { _, user -> user.id },
        ) { index, user ->
            if (index == state.users.lastIndex) {
                LaunchedEffect(state.users.size, state.hasMore) {
                    if (state.hasMore) onIntent(BlockedUsersIntent.LoadMore)
                }
            }
            BlockedUserRow(
                user = user,
                isUnblocking = user.id in state.unblockingIds,
                onUnblock = { onIntent(BlockedUsersIntent.Unblock(user)) },
            )
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = DemoColors.link,
                        modifier = Modifier.size(IconSize.md),
                    )
                }
            }
        }
        item(key = "navigation-inset") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun BlockedUsersSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = ComponentSize.blockedSkeletonCount,
) {
    val pulse = rememberBlockedUsersSkeletonPulse()
    val loadingDescription = stringResource(R.string.me_cd_loading)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .readableContentWidth()
            .semantics { contentDescription = loadingDescription },
        contentPadding = PaddingValues(
            start = Spacing.md,
            top = Spacing.blockedListTop,
            end = Spacing.md,
            bottom = Spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        items(
            count = count,
            key = { "blocked-skeleton-$it" },
        ) {
            BlockedUserRowSkeleton(pulse = pulse)
        }
        item(key = "navigation-inset") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun BlockedUserRowSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ComponentSize.blockedRowMinHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(
                horizontal = Spacing.blockedRowInset,
                vertical = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.blockedAvatar)
                .clip(CircleShape)
                .background(fill),
        )
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.blockedNameGap),
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.onlineSkeletonNameWidth,
                        height = ComponentSize.onlineSkeletonNameHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.onlineSkeletonStatusWidth,
                        height = ComponentSize.onlineSkeletonStatusHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Box(
            modifier = Modifier
                .size(
                    width = ComponentSize.blockedSkeletonUnlockWidth,
                    height = ComponentSize.blockedSkeletonUnlockHeight,
                )
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
    }
}

@Composable
private fun rememberBlockedUsersSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "blockedUsersSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blockedUsersSkeletonPulse",
    )
    return pulse
}

@Composable
private fun BlockedUserRow(
    user: User,
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ComponentSize.blockedRowMinHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(
                horizontal = Spacing.blockedRowInset,
                vertical = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatar,
            contentDescription = stringResource(R.string.blocked_avatar, user.nickname),
            modifier = Modifier
                .size(ComponentSize.blockedAvatar)
                .clip(CircleShape)
                .background(DemoColors.skeleton),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.blockedNameGap),
        ) {
            Text(
                text = user.nickname,
                style = BlockedNameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = blockedUserLocation(user),
                style = BlockedCountryStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        BlockedUnlockButton(isUnblocking = isUnblocking, onClick = onUnblock)
    }
}

@Composable
private fun BlockedUnlockButton(
    isUnblocking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .clickable(enabled = !isUnblocking, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(
                horizontal = Spacing.chipGap,
                vertical = Spacing.blockedActionVertical,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Keep the label laid out while unblocking so the pill width stays put.
        Text(
            text = stringResource(R.string.blocked_unlock),
            style = BlockedActionStyle,
            maxLines = 1,
            modifier = Modifier.alpha(if (isUnblocking) 0f else 1f),
        )
        if (isUnblocking) {
            CircularProgressIndicator(
                color = DemoColors.onPrimaryButton,
                strokeWidth = Spacing.xxs,
                modifier = Modifier.size(IconSize.xs),
            )
        }
    }
}

@Composable
private fun BlockedUsersEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Figma sits the artwork above the optical centre — roughly 1:1.8 of the free space.
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.blocked_ill_empty),
            contentDescription = null,
            modifier = Modifier.size(
                width = ComponentSize.blockedEmptyIllustrationWidth,
                height = ComponentSize.blockedEmptyIllustrationHeight,
            ),
        )
        Spacer(modifier = Modifier.height(Spacing.blockedEmptyGap))
        Text(
            text = stringResource(R.string.blocked_empty),
            style = BlockedEmptyStyle,
        )
        Spacer(modifier = Modifier.weight(1.8f))
    }
}

@Composable
private fun BlockedUsersError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = message.ifBlank { stringResource(R.string.blocked_load_error) },
            color = DemoColors.textSecondary,
            fontSize = TextSize.sm,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.blocked_retry),
                color = DemoColors.link,
            )
        }
    }
}

private fun blockedUserLocation(user: User): String = listOfNotNull(
    countryCodeToFlagEmoji(user.countryCode).takeIf(String::isNotBlank),
    user.countryName?.takeIf(String::isNotBlank),
).joinToString(" ")

private val previewBlockedUsers = List(4) { index ->
    User(
        id = "${index + 1}",
        nickname = if (index == 1) "Sophia with a very long display name" else "Sophia",
        avatar = null,
        gender = Gender.Female,
        age = 23,
        bio = "",
        isOnline = false,
        lastActiveAt = 0,
        countryCode = "US",
        countryName = "USA",
    )
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Blocked users", locale = "en")
@Composable
private fun BlockedUsersPreview() {
    DemoTheme {
        BlockedUsersScreen(
            state = BlockedUsersUiState(
                users = previewBlockedUsers,
                isLoading = false,
                unblockingIds = setOf("2"),
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Blocked users empty", locale = "en")
@Preview(name = "Blocked users empty RTL", locale = "ar")
@Composable
private fun BlockedUsersEmptyPreview() {
    DemoTheme {
        BlockedUsersScreen(
            state = BlockedUsersUiState(isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(name = "Blocked users skeleton", locale = "en")
@Composable
private fun BlockedUsersSkeletonPreview() {
    DemoTheme {
        BlockedUsersScreen(
            state = BlockedUsersUiState(isLoading = true),
            onIntent = {},
        )
    }
}

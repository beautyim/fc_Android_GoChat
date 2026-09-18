package com.example.demoproject.product.me

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTextAutoSize
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MeScreen(
    viewModel: MeViewModel,
    onNavigateTab: (String) -> Unit,
    onOpenStore: () -> Unit,
    onOpenVip: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onOpenRelationshipList: (RelationshipListType, Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is MeEffect.OpenPublicProfile -> onOpenPublicProfile(effect.externalUserId)
                is MeEffect.OpenRelationshipList ->
                    onOpenRelationshipList(effect.type, effect.count)
                MeEffect.OpenStore -> onOpenStore()
                MeEffect.OpenVip -> onOpenVip()
                MeEffect.OpenSettings -> onOpenSettings()
                MeEffect.OpenVerification -> onOpenVerification()
                is MeEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    MeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateTab = onNavigateTab,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    state: MeUiState,
    onIntent: (MeIntent) -> Unit,
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to DemoColors.onlineHeaderStart,
                        0.17f to DemoColors.page,
                        1.0f to DemoColors.page,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MeTopBar()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(MeIntent.Refresh) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ComponentSize.onlineTabBarBodyHeight),
            ) {
                when {
                    state.isLoading && state.nickname.isBlank() -> {
                        MeSkeleton(modifier = Modifier.fillMaxSize())
                    }
                    state.errorMessage != null && state.nickname.isBlank() -> {
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
                            TextButton(onClick = { onIntent(MeIntent.Refresh) }) {
                                Text(stringResource(R.string.me_retry))
                            }
                        }
                    }
                    else -> {
                        MeScrollContent(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        MeBottomBar(
            selected = MeBottomTab.Profile,
            chatUnreadCount = state.chatUnreadCount,
            onSelect = onNavigateTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(ComponentSize.navHeaderHeight)
            // Figma Section: title start 20 (match Online / Messages).
            .padding(start = Spacing.md + Spacing.xs, end = Spacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.me_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.title,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MeScrollContent(
    state: MeUiState,
    onIntent: (MeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md)
            .readableContentWidth()
            .padding(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        MeIdentityHeader(
            state = state,
            onOpenProfile = { onIntent(MeIntent.OpenPublicProfile) },
            onOpenFollowers = { onIntent(MeIntent.OpenFollowers) },
            onOpenFollowing = { onIntent(MeIntent.OpenFollowing) },
            onOpenCamera = { onIntent(MeIntent.OpenCamera) },
        )
        MeBalanceCard(
            balance = state.coinBalance,
            onAddCoins = { onIntent(MeIntent.OpenAddCoins) },
        )
        MeVipCard(
            isVip = state.isVip,
            expiryText = state.vipExpiryText,
            onClick = { onIntent(MeIntent.OpenVip) },
        )
        MeMenuRow(
            iconRes = R.drawable.me_ic_menu_profile,
            titleRes = R.string.me_menu_profile,
            onClick = { onIntent(MeIntent.OpenPublicProfile) },
        )
        MeMenuRow(
            iconRes = R.drawable.me_ic_menu_gift,
            titleRes = R.string.me_menu_gift,
            onClick = { onIntent(MeIntent.OpenGift) },
        )
        MeMenuRow(
            iconRes = R.drawable.me_ic_menu_verify,
            titleRes = R.string.me_menu_verify,
            onClick = { onIntent(MeIntent.OpenVerification) },
        )
        MeMenuRow(
            iconRes = R.drawable.me_ic_menu_settings,
            titleRes = R.string.me_menu_settings,
            onClick = { onIntent(MeIntent.OpenSettings) },
        )
    }
}

@Composable
private fun MeIdentityHeader(
    state: MeUiState,
    onOpenProfile: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenCamera: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(ComponentSize.meAvatar + ComponentSize.meAvatarBorder * 2),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.me_cd_avatar),
                modifier = Modifier
                    .fillMaxSize()
                    .border(ComponentSize.meAvatarBorder, DemoColors.profileAvatarRing, CircleShape)
                    .padding(ComponentSize.meAvatarBorder)
                    .clip(CircleShape)
                    .background(DemoColors.skeleton)
                    .clickable(onClick = onOpenProfile),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(ComponentSize.meCameraBadge)
                    .clip(CircleShape)
                    .background(DemoColors.meCameraBadge)
                    .clickable(onClick = onOpenCamera),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.me_ic_camera),
                    contentDescription = stringResource(R.string.me_cd_camera),
                    modifier = Modifier.size(ComponentSize.meCameraIcon),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenProfile),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(y = ComponentSize.meIdentityNameDown),
            ) {
                Text(
                    text = state.nickname,
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.meName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.age > 0) {
                    Text(
                        text = stringResource(R.string.me_age_suffix_fmt, state.age),
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.meName,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                genderIconRes(state.gender)?.let { icon ->
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.meGenderIcon),
                    )
                }
            }
            val location = listOfNotNull(
                state.countryFlag.takeIf { it.isNotBlank() },
                state.countryName.takeIf { it.isNotBlank() },
            ).joinToString(" ")
            if (location.isNotBlank()) {
                Text(
                    text = location,
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.meMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.offset(y = -ComponentSize.meIdentityStatsUp),
            ) {
                MeStat(
                    count = state.followerCount,
                    label = stringResource(R.string.me_followers),
                    onClick = onOpenFollowers,
                )
                Text(
                    text = "|",
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.meMeta,
                )
                MeStat(
                    count = state.followingCount,
                    label = stringResource(R.string.me_following),
                    onClick = onOpenFollowing,
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.me_ic_chevron),
            contentDescription = null,
            modifier = Modifier
                .size(IconSize.sm)
                .clickable(onClick = onOpenProfile),
            colorFilter = ColorFilter.tint(DemoColors.textAuxiliary),
        )
    }
}

@Composable
private fun MeStat(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = formatCount(count),
            color = DemoColors.textPrimary,
            fontSize = TextSize.meStat,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            color = DemoColors.textSecondary,
            fontSize = TextSize.meMeta,
        )
    }
}

@Composable
private fun MeBalanceCard(
    balance: Int,
    onAddCoins: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(start = Spacing.md, end = Spacing.md + Spacing.xs, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_balance_label),
                color = DemoColors.textAuxiliary,
                fontSize = TextSize.xs,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = formatCount(balance),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.meBalance,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = DemoTextAutoSize.price(TextSize.meBalance),
                )
                Image(
                    painter = painterResource(R.drawable.me_ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.meBalanceCoin),
                )
            }
        }
        val addCoinsCd = stringResource(R.string.me_add_coins)
        Row(
            modifier = Modifier
                .width(ComponentSize.meAddCoinsWidth)
                .height(ComponentSize.meAddCoinsHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(onClick = onAddCoins)
                .padding(horizontal = Spacing.md)
                .semantics { contentDescription = addCoinsCd },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Image(
                painter = painterResource(R.drawable.me_ic_add_plus),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.meAddCoinsPlus),
            )
            Text(
                text = stringResource(R.string.me_add_coins),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.label(TextSize.sm),
            )
        }
    }
}

@Composable
private fun MeVipCard(
    isVip: Boolean,
    expiryText: String?,
    onClick: () -> Unit,
) {
    val background = if (isVip) {
        Brush.linearGradient(
            colors = listOf(DemoColors.meVipCardStart, DemoColors.meVipCardEnd),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(DemoColors.meVipCardInactive, DemoColors.meVipCardInactive),
        )
    }
    val subtitle = if (isVip && !expiryText.isNullOrBlank()) {
        stringResource(R.string.me_vip_expiry, expiryText)
    } else {
        stringResource(R.string.me_vip_not_member)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.meVipCardHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(background)
            .border(ComponentSize.meCardStroke, DemoColors.meVipCardBorder, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(start = Spacing.sm + Spacing.xxs, end = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.me_ic_vip_badge),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.meVipBadge),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_vip_title),
                color = DemoColors.textTitle,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.xs + Spacing.xxs))
            Text(
                text = subtitle,
                color = DemoColors.textAuxiliary,
                fontSize = TextSize.xs,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            painter = painterResource(R.drawable.me_ic_chevron),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            colorFilter = ColorFilter.tint(DemoColors.textAuxiliary),
        )
    }
}

@Composable
private fun MeMenuRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.chipGap, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.meMenuIconCircle)
                .clip(CircleShape)
                .background(DemoColors.meMenuIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
            )
        }
        Text(
            text = stringResource(titleRes),
            color = DemoColors.textPrimary,
            fontSize = TextSize.lg,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.me_ic_chevron),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            colorFilter = ColorFilter.tint(DemoColors.textAuxiliary),
        )
    }
}

private enum class MeBottomTab {
    Home,
    Feed,
    Match,
    Chat,
    Profile,
}

@Composable
private fun MeBottomBar(
    selected: MeBottomTab,
    chatUnreadCount: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.me_bg_tab_bar),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ComponentSize.onlineTabBarAspect)
                    .align(Alignment.BottomCenter),
                contentScale = ContentScale.FillBounds,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ComponentSize.onlineTabBarShadowBottom)
                    .height(ComponentSize.onlineTabRowHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MeTabIcon(
                    iconRes = if (selected == MeBottomTab.Home) {
                        R.drawable.me_ic_tab_home
                    } else {
                        R.drawable.me_ic_tab_home_inactive
                    },
                    onClick = { onSelect("home") },
                )
                MeTabIcon(
                    iconRes = if (selected == MeBottomTab.Feed) {
                        R.drawable.me_ic_tab_feed
                    } else {
                        R.drawable.me_ic_tab_feed_inactive
                    },
                    onClick = { onSelect("feed") },
                )
                MeMatchTab(onClick = { onSelect("match") })
                Box {
                    MeTabIcon(
                        iconRes = if (selected == MeBottomTab.Chat) {
                            R.drawable.me_ic_tab_chat
                        } else {
                            R.drawable.me_ic_tab_chat_inactive
                        },
                        onClick = { onSelect("chat") },
                    )
                    if (chatUnreadCount > 0) {
                        Text(
                            text = if (chatUnreadCount > 99) "99+" else chatUnreadCount.toString(),
                            color = DemoColors.onPrimaryButton,
                            fontSize = TextSize.meMeta,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(DemoColors.meUnreadBadge)
                                .padding(horizontal = Spacing.xs + Spacing.xxs, vertical = Spacing.xxs),
                        )
                    }
                }
                MeTabIcon(
                    iconRes = if (selected == MeBottomTab.Profile) {
                        R.drawable.me_ic_tab_profile
                    } else {
                        R.drawable.me_ic_tab_profile_inactive
                    },
                    onClick = { onSelect("profile") },
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(DemoColors.sheet),
        )
    }
}

@Composable
private fun MeMatchTab(onClick: () -> Unit) {
    val glow = DemoColors.tabMatchGlow
    val glowExtra = ComponentSize.onlineMatchGlowBlur
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ComponentSize.onlineMatchTab)
            .drawBehind {
                val radius = size.minDimension / 2f + glowExtra.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.45f to glow,
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
            .clip(CircleShape)
            .background(DemoColors.tabMatchButton)
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
    ) {
        Image(
            painter = painterResource(R.drawable.me_ic_tab_match),
            contentDescription = stringResource(R.string.me_nav_match),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MeTabIcon(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier
            .size(IconSize.lg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun rememberMeSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "meSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "meSkeletonPulse",
    )
    return pulse
}

@Composable
private fun MeSkeleton(modifier: Modifier = Modifier) {
    val pulse = rememberMeSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.me_cd_loading)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md)
            .readableContentWidth()
            .semantics { contentDescription = loadingCd },
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        MeSkeletonIdentity(fill = fill, highlight = highlight)
        MeSkeletonBalanceCard(fill = fill, highlight = highlight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.meVipCardHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(fill),
        )
        repeat(4) {
            MeSkeletonMenuRow(fill = fill, highlight = highlight)
        }
    }
}

@Composable
private fun MeSkeletonIdentity(
    fill: Color,
    highlight: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.meAvatar)
                .clip(CircleShape)
                .background(highlight),
        )
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs),
        ) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.meSkeletonNameWidth)
                    .height(ComponentSize.meSkeletonNameHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
            Box(
                modifier = Modifier
                    .width(ComponentSize.meSkeletonCountryWidth)
                    .height(ComponentSize.meSkeletonCountryHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Box(
                modifier = Modifier
                    .width(ComponentSize.meSkeletonStatWidth)
                    .height(ComponentSize.meSkeletonStatHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
        }
    }
}

@Composable
private fun MeSkeletonBalanceCard(
    fill: Color,
    highlight: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(
                start = Spacing.md,
                end = Spacing.md + Spacing.xs,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.meSkeletonBalanceLabelWidth)
                    .height(ComponentSize.meSkeletonBalanceLabelHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(
                modifier = Modifier
                    .width(ComponentSize.meSkeletonBalanceValueWidth)
                    .height(ComponentSize.meSkeletonBalanceValueHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
        }
        Box(
            modifier = Modifier
                .width(ComponentSize.meAddCoinsWidth)
                .height(ComponentSize.meAddCoinsHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
    }
}

@Composable
private fun MeSkeletonMenuRow(
    fill: Color,
    highlight: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.chipGap, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.meMenuIconCircle)
                .clip(CircleShape)
                .background(fill),
        )
        Box(
            modifier = Modifier
                .width(ComponentSize.meSkeletonMenuTitleWidth)
                .height(ComponentSize.meSkeletonMenuTitleHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
    }
}

@DrawableRes
private fun genderIconRes(gender: Gender): Int? = when (gender) {
    Gender.Female -> R.drawable.me_ic_gender_female
    Gender.Male -> R.drawable.me_ic_gender_male
    Gender.Other -> null
}

private fun formatCount(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.coerceAtLeast(0))

private val PreviewState = MeUiState(
    isLoading = false,
    nickname = "Isabella",
    age = 23,
    gender = Gender.Female,
    countryFlag = "🇸🇬",
    countryName = "Singapore",
    followerCount = 168,
    followingCount = 124,
    coinBalance = 3650,
    isVip = true,
    vipExpiryText = "26/12/31",
    chatUnreadCount = 6,
    externalUserId = "1001",
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Me VIP", locale = "en")
@Composable
private fun MeScreenVipPreview() {
    DemoTheme {
        MeScreen(state = PreviewState, onIntent = {}, onNavigateTab = {})
    }
}

@Preview(name = "Me non-VIP", locale = "en")
@Preview(name = "Me RTL", locale = "ar")
@Composable
private fun MeScreenNonVipPreview() {
    DemoTheme {
        MeScreen(
            state = PreviewState.copy(isVip = false, vipExpiryText = null),
            onIntent = {},
            onNavigateTab = {},
        )
    }
}

@Preview(name = "Me skeleton", locale = "en")
@Composable
private fun MeSkeletonPreview() {
    DemoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.page),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MeTopBar()
                MeSkeleton(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = ComponentSize.onlineTabBarBodyHeight),
                )
            }
            MeBottomBar(
                selected = MeBottomTab.Profile,
                chatUnreadCount = 0,
                onSelect = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

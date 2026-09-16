package com.example.demoproject.product.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.media.MediaViewer
import com.example.demoproject.product.profile.gift.ProfileGiftSheet
import com.example.demoproject.product.profile.more.ProfileMoreSheet
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoConfirmDialog
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.designsystem.gift.GiftSvgaOverlay
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

private val ProfilePhotoShape = RoundedCornerShape(Radius.sm)
private val ProfileBannerGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, DemoColors.page),
)
private val ProfileVideoThumbGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, DemoColors.profileVideoScrim),
)

/** Figma nickname 22 / leading normal — trim font padding so row gaps match centers. */
private val ProfileNameStyle = TextStyle(
    fontSize = TextSize.title,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private val ProfileCountryStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Medium,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private val ProfileCountryFlagStyle = TextStyle(
    fontSize = TextSize.md,
    lineHeight = 16.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private val ProfileStatStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = 12.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:844 — 12 Regular #666, leading normal (tight wrap). */
private val ProfileBioStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Normal,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private const val ProfileBioExpandMillis = 280
private const val ProfileBioFadeMillis = 200

/**
 * Expands past parent horizontal inset so a full-bleed banner can sit inside a padded
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] without negative [Modifier.padding]
 * (which throws on modern Compose).
 */
private fun Modifier.breakoutHorizontal(inset: Dp): Modifier = layout { measurable, constraints ->
    val insetPx = inset.roundToPx()
    val targetWidth = (constraints.maxWidth + insetPx * 2).coerceAtLeast(0)
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = targetWidth,
            maxWidth = targetWidth,
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-insetPx, 0)
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    onOpenChatDetail: (conversationId: String, nickname: String) -> Unit = { _, _ -> },
    onStartVideoCall: (
        userId: String,
        nickname: String,
        age: Int,
        avatarUrl: String,
    ) -> Unit = { _, _, _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ProfileEffect.NavigateBack -> onBack()
                ProfileEffect.OpenStore -> onOpenStore()
                is ProfileEffect.OpenChatDetail -> {
                    onOpenChatDetail(effect.conversationId, effect.nickname)
                }
                is ProfileEffect.StartVideoCall -> {
                    onStartVideoCall(
                        effect.userId,
                        effect.nickname,
                        effect.age,
                        effect.avatarUrl,
                    )
                }
                is ProfileEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ProfileScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.page),
    ) {
        when {
            state.isLoading && state.nickname.isBlank() -> {
                ProfileSkeleton(
                    onBack = { onIntent(ProfileIntent.Back) },
                    onMore = { onIntent(ProfileIntent.More) },
                    onGift = { onIntent(ProfileIntent.Gift) },
                    onMessage = { onIntent(ProfileIntent.Message) },
                    onVideoChat = { onIntent(ProfileIntent.VideoChat) },
                )
            }
            state.errorMessage != null && state.nickname.isBlank() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = state.errorMessage,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.sm,
                    )
                    TextButton(onClick = { onIntent(ProfileIntent.Load) }) {
                        Text(stringResource(R.string.profile_retry))
                    }
                }
            }
            else -> {
                ProfileBody(state = state, onIntent = onIntent)
            }
        }

        state.viewerIndex?.let { index ->
            MediaViewer(
                items = state.viewerItems,
                initialIndex = index,
                onDismiss = { onIntent(ProfileIntent.CloseMedia) },
                showVideoChat = !state.isSelf,
                onVideoChat = { onIntent(ProfileIntent.VideoChat) },
            )
        }

        if (state.isGiftSheetVisible) {
            ProfileGiftSheet(
                nickname = state.nickname,
                avatarUrl = state.avatarUrl,
                coinBalance = state.coinBalance,
                gifts = state.gifts,
                selectedGiftId = state.selectedGiftId,
                isCatalogLoading = state.isGiftCatalogLoading,
                isSending = state.isGiftSending,
                onDismiss = { onIntent(ProfileIntent.DismissGiftSheet) },
                onSelectGift = { onIntent(ProfileIntent.SelectGift(it)) },
                onSend = { onIntent(ProfileIntent.SendGift) },
                onOpenCoins = { onIntent(ProfileIntent.OpenCoins) },
            )
        }

        if (state.isMoreSheetVisible) {
            ProfileMoreSheet(
                isFollowing = state.isFollowing,
                isBlocked = state.isBlocked,
                onFollow = { onIntent(ProfileIntent.MoreFollow) },
                onBlock = { onIntent(ProfileIntent.MoreBlock) },
                onReport = { onIntent(ProfileIntent.MoreReport) },
                onDismiss = { onIntent(ProfileIntent.DismissMoreSheet) },
            )
        }

        when (state.confirmDialog) {
            ProfileConfirmDialog.Unfollow -> {
                DemoConfirmDialog(
                    title = stringResource(
                        R.string.profile_unfollow_confirm_title,
                        state.nickname,
                    ),
                    negativeText = stringResource(R.string.profile_unfollow_confirm_no),
                    positiveText = stringResource(R.string.profile_unfollow_confirm_sure),
                    onNegative = { onIntent(ProfileIntent.DismissConfirmDialog) },
                    onPositive = { onIntent(ProfileIntent.ConfirmUnfollow) },
                    isPositiveLoading = state.isFollowBusy,
                )
            }
            ProfileConfirmDialog.Block -> {
                DemoConfirmDialog(
                    title = stringResource(
                        R.string.profile_block_confirm_title,
                        state.nickname,
                    ),
                    body = stringResource(R.string.profile_block_confirm_body),
                    negativeText = stringResource(R.string.profile_block_confirm_cancel),
                    positiveText = stringResource(R.string.profile_block_confirm_block),
                    onNegative = { onIntent(ProfileIntent.DismissConfirmDialog) },
                    onPositive = { onIntent(ProfileIntent.ConfirmBlock) },
                    isPositiveLoading = state.isBlockBusy,
                )
            }
            null -> Unit
        }

        state.giftAnimationUrl?.let { url ->
            GiftSvgaOverlay(
                svgaUrl = url,
                onFinished = { onIntent(ProfileIntent.DismissGiftAnimation) },
                closeButton = { onClose ->
                    ProfileOverlayCloseButton(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                },
            )
        }
    }
}

@Composable
private fun ProfileBody(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
) {
    val showBottomBar = !state.isBlockedByPeer
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .readableContentWidth()
                .align(Alignment.TopCenter),
            // Horizontal inset only for grid cells; header breaks out for full-bleed banner.
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                bottom = if (showBottomBar) {
                    ComponentSize.profileActionCircle + Spacing.lg + Spacing.md
                } else {
                    Spacing.lg
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileHeader(
                    state = state,
                    onIntent = onIntent,
                    // Escape grid horizontal padding so banner is edge-to-edge (Figma left-0).
                    modifier = Modifier
                        .fillMaxWidth()
                        .breakoutHorizontal(Spacing.md),
                )
            }
            if (state.isBlockedByPeer) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileBlockedByPeerEmpty(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ComponentSize.profileBlockedEmptyTopInset),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.profile_photos),
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.sm,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                    )
                }
                if (state.media.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.profile_empty_photos),
                            color = DemoColors.textAuxiliary,
                            fontSize = TextSize.xs,
                            modifier = Modifier.padding(vertical = Spacing.lg),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.media,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        ProfilePhotoCell(
                            item = item,
                            onClick = { onIntent(ProfileIntent.OpenMedia(index)) },
                        )
                    }
                }
            }
        }

        if (showBottomBar) {
            ProfileBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .readableContentWidth()
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = ComponentSize.profileBottomBarSide,
                        end = ComponentSize.profileBottomBarSide,
                        bottom = Spacing.md,
                    ),
                onGift = { onIntent(ProfileIntent.Gift) },
                onMessage = { onIntent(ProfileIntent.Message) },
                onVideoChat = { onIntent(ProfileIntent.VideoChat) },
            )
        }
    }
}

/** Figma 136:3389–136:3392 — blocked-by-peer empty state under the profile header. */
@Composable
private fun ProfileBlockedByPeerEmpty(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.profile_ill_blocked),
            contentDescription = null,
            modifier = Modifier.size(
                width = ComponentSize.profileBlockedEmptyWidth,
                height = ComponentSize.profileBlockedEmptyHeight,
            ),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.profile_blocked_by_peer_title),
            color = DemoColors.textPrimary,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.profile_blocked_by_peer_body),
            color = DemoColors.textSecondary,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Banner is full-bleed; box extends below so avatar can hang past the fade (Figma).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.profileBanner + ComponentSize.profileAvatarHang),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.profileBanner)
                    .align(Alignment.TopCenter),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.backgroundUrl ?: state.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.profileBannerFade)
                        .align(Alignment.BottomCenter)
                        .background(ProfileBannerGradient),
                )
                ProfileTopBar(
                    onBack = { onIntent(ProfileIntent.Back) },
                    onMore = { onIntent(ProfileIntent.More) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(ComponentSize.profileAvatar)
                        .border(
                            width = ComponentSize.profileAvatarBorder,
                            color = DemoColors.profileAvatarRing,
                            shape = CircleShape,
                        )
                        .padding(ComponentSize.profileAvatarBorder)
                        .clip(CircleShape)
                        .background(DemoColors.chip),
                )
                // Follow overlays the identity column so its height does not inflate
                // nickname → country → stats gaps (Figma: centers 170 / 199 / 222).
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ComponentSize.profileIdentityTopInset),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                end = if (!state.isSelf) {
                                    ComponentSize.profileFollowReserve + Spacing.sm
                                } else {
                                    0.dp
                                },
                            ),
                        ) {
                            Text(
                                text = state.nickname,
                                color = DemoColors.textPrimary,
                                style = ProfileNameStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (state.age > 0) {
                                Text(
                                    text = stringResource(R.string.profile_age_suffix_fmt, state.age),
                                    color = DemoColors.textPrimary,
                                    style = ProfileNameStyle,
                                    maxLines = 1,
                                )
                            }
                        }
                        if (state.countryName.isNotBlank() || state.countryFlag.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier.padding(
                                    top = ComponentSize.profileIdentityGapNameToCountry,
                                ),
                            ) {
                                if (state.countryFlag.isNotBlank()) {
                                    Text(
                                        text = state.countryFlag,
                                        style = ProfileCountryFlagStyle,
                                    )
                                }
                                if (state.countryName.isNotBlank()) {
                                    Text(
                                        text = state.countryName,
                                        color = DemoColors.textPrimary,
                                        style = ProfileCountryStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                top = ComponentSize.profileIdentityGapCountryToStats,
                            ),
                        ) {
                            ProfileStatText(
                                count = state.followingCount,
                                label = stringResource(R.string.profile_following),
                            )
                            Text(
                                text = " | ",
                                color = DemoColors.profileStatsDivider,
                                style = ProfileStatStyle,
                            )
                            ProfileStatText(
                                count = state.followerCount,
                                label = stringResource(R.string.profile_followers),
                            )
                        }
                    }
                    if (!state.isSelf) {
                        ProfileFollowButton(
                            following = state.isFollowing,
                            busy = state.isFollowBusy,
                            onClick = { onIntent(ProfileIntent.ToggleFollow) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = ComponentSize.profileFollowTopInset),
                        )
                    }
                }
            }
        }

        if (state.bio.isNotBlank()) {
            ProfileBioSection(
                bio = state.bio,
                translatedBio = state.translatedBio,
                showTranslatedBio = state.showTranslatedBio,
                isTranslatingBio = state.isTranslatingBio,
                onToggleTranslate = { onIntent(ProfileIntent.TranslateBio) },
            )
        }
    }
}

/** Figma 1:886 / 1:906–1:915 — bio + optional translation with expand/collapse transition. */
@Composable
private fun ProfileBioSection(
    bio: String,
    translatedBio: String,
    showTranslatedBio: Boolean,
    isTranslatingBio: Boolean,
    onToggleTranslate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val translationVisible = showTranslatedBio && translatedBio.isNotBlank()
    val translateCd = stringResource(R.string.profile_cd_translate)
    val recoveryCd = stringResource(R.string.profile_cd_translate_recovery)
    val chevronScaleY by animateFloatAsState(
        targetValue = if (translationVisible) -1f else 1f,
        animationSpec = tween(
            durationMillis = ProfileBioExpandMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "profileBioChevron",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(top = ComponentSize.profileBioTopInset),
    ) {
        Text(
            text = bio,
            color = DemoColors.textSecondary,
            style = ProfileBioStyle,
        )
        AnimatedVisibility(
            visible = translationVisible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = ProfileBioFadeMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + expandVertically(
                animationSpec = tween(
                    durationMillis = ProfileBioExpandMillis,
                    easing = FastOutSlowInEasing,
                ),
                expandFrom = Alignment.Top,
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = ProfileBioFadeMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + shrinkVertically(
                animationSpec = tween(
                    durationMillis = ProfileBioExpandMillis,
                    easing = FastOutSlowInEasing,
                ),
                shrinkTowards = Alignment.Top,
            ),
        ) {
            Text(
                text = translatedBio,
                color = DemoColors.textSecondary,
                style = ProfileBioStyle,
                modifier = Modifier.padding(top = ComponentSize.profileBioParagraphGap),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            modifier = Modifier
                .padding(top = ComponentSize.profileBioToTranslate)
                .semantics {
                    contentDescription = if (translationVisible) {
                        recoveryCd
                    } else {
                        translateCd
                    }
                }
                .clickable(
                    enabled = !isTranslatingBio,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onToggleTranslate,
                ),
        ) {
            if (isTranslatingBio) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ComponentSize.profileBioToggleIcon),
                    strokeWidth = ComponentSize.profileProgressStroke,
                    color = DemoColors.textAuxiliary,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.profile_ic_translate),
                    contentDescription = null,
                    modifier = Modifier
                        .size(ComponentSize.profileBioToggleIcon)
                        .graphicsLayer { scaleY = chevronScaleY },
                )
            }
            Text(
                text = stringResource(
                    if (translationVisible) {
                        R.string.profile_translate_recovery
                    } else {
                        R.string.profile_translate
                    },
                ),
                color = DemoColors.textAuxiliary,
                fontSize = TextSize.caption,
            )
        }
    }
}

@Composable
private fun ProfileStatText(
    count: Int,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.profile_stat_count_fmt, count),
            color = DemoColors.textPrimary,
            style = ProfileStatStyle.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = label,
            color = DemoColors.textAuxiliary,
            style = ProfileStatStyle.copy(fontWeight = FontWeight.Normal),
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileTopBar(
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(ComponentSize.profileTopBar)
            .padding(horizontal = Spacing.chipGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProfileNavButton(
            iconRes = R.drawable.profile_ic_back,
            contentDescription = stringResource(R.string.profile_cd_back),
            onClick = onBack,
        )
        ProfileNavButton(
            iconRes = R.drawable.profile_ic_more,
            contentDescription = stringResource(R.string.profile_cd_more),
            onClick = onMore,
        )
    }
}

@Composable
private fun ProfileNavButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    DemoNavIconButton(
        icon = painterResource(iconRes),
        contentDescription = contentDescription,
        onClick = onClick,
        iconSize = IconSize.sm,
        size = ComponentSize.profileNavButton,
        containerColor = DemoColors.profileNavScrim,
    )
}

@Composable
private fun ProfileFollowButton(
    following: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (following) R.string.profile_following_action else R.string.profile_follow,
    )
    Box(
        modifier = modifier
            .height(ComponentSize.profileFollowButton)
            .widthIn(min = ComponentSize.onlineChipHeight + Spacing.lg)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                color = DemoColors.onPrimaryButton,
                strokeWidth = ComponentSize.profileProgressStroke,
            )
        } else {
            Text(
                text = label,
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProfilePhotoCell(
    item: ProfileMediaUi,
    onClick: () -> Unit,
) {
    val cd = stringResource(
        if (item.isVideo) R.string.profile_cd_video else R.string.profile_cd_photo,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.profilePhotoAspect)
            .clip(ProfilePhotoShape)
            .background(DemoColors.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.onlineCardAvatar)
                    .align(Alignment.BottomCenter)
                    .background(ProfileVideoThumbGradient),
            )
            Text(
                text = formatDuration(item.durationSeconds),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.caption,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Spacing.sm, bottom = Spacing.xs),
            )
        }
    }
}

@Composable
private fun ProfileBottomBar(
    onGift: () -> Unit,
    onMessage: () -> Unit,
    onVideoChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ProfileCircleAction(
            iconRes = R.drawable.profile_ic_gift,
            contentDescription = stringResource(R.string.profile_cd_gift),
            onClick = onGift,
        )
        ProfileCircleAction(
            iconRes = R.drawable.profile_ic_message,
            contentDescription = stringResource(R.string.profile_cd_message),
            onClick = onMessage,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(ComponentSize.profileVideoChatButton)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(role = Role.Button, onClick = onVideoChat),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.profile_ic_video),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.profile_video_chat),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileCircleAction(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ComponentSize.profileActionCircle)
            .clip(CircleShape)
            .background(DemoColors.sheet)
            .border(ComponentSize.profileActionStroke, DemoColors.link, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize.md),
        )
    }
}

@Composable
private fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return stringResource(R.string.profile_duration_fmt, minutes, seconds)
}

@Composable
private fun rememberProfileSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "profileSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "profileSkeletonPulse",
    )
    return pulse
}

@Composable
private fun ProfileSkeleton(
    onBack: () -> Unit,
    onMore: () -> Unit,
    onGift: () -> Unit,
    onMessage: () -> Unit,
    onVideoChat: () -> Unit,
    modifier: Modifier = Modifier,
    photoPlaceholderCount: Int = 6,
) {
    val pulse = rememberProfileSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.profile_cd_loading)

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingCd },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .readableContentWidth()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                bottom = ComponentSize.profileActionCircle + Spacing.lg + Spacing.md,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            userScrollEnabled = false,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileSkeletonHeader(
                    fill = fill,
                    highlight = highlight,
                    onBack = onBack,
                    onMore = onMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .breakoutHorizontal(Spacing.md),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .padding(top = Spacing.sm, bottom = Spacing.xs)
                        .width(ComponentSize.profileSkeletonPhotosTitleWidth)
                        .height(ComponentSize.profileSkeletonPhotosTitleHeight)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(fill),
                )
            }
            items(photoPlaceholderCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ComponentSize.profilePhotoAspect)
                        .clip(ProfilePhotoShape)
                        .background(fill),
                )
            }
        }

        ProfileBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .readableContentWidth()
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = ComponentSize.profileBottomBarSide,
                    end = ComponentSize.profileBottomBarSide,
                    bottom = Spacing.md,
                ),
            onGift = onGift,
            onMessage = onMessage,
            onVideoChat = onVideoChat,
        )
    }
}

@Composable
private fun ProfileSkeletonHeader(
    fill: Color,
    highlight: Color,
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.profileBanner + ComponentSize.profileAvatarHang),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.profileBanner)
                    .align(Alignment.TopCenter)
                    .background(fill),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.profileBannerFade)
                        .align(Alignment.BottomCenter)
                        .background(ProfileBannerGradient),
                )
                ProfileTopBar(
                    onBack = onBack,
                    onMore = onMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(ComponentSize.profileAvatar)
                        .border(
                            width = ComponentSize.profileAvatarBorder,
                            color = DemoColors.profileAvatarRing,
                            shape = CircleShape,
                        )
                        .padding(ComponentSize.profileAvatarBorder)
                        .clip(CircleShape)
                        .background(highlight),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = ComponentSize.profileIdentityTopInset),
                ) {
                    Box(
                        modifier = Modifier
                            .width(ComponentSize.profileSkeletonNameWidth)
                            .height(ComponentSize.profileSkeletonNameHeight)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(highlight),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = ComponentSize.profileIdentityGapNameToCountry)
                            .width(ComponentSize.profileSkeletonCountryWidth)
                            .height(ComponentSize.profileSkeletonCountryHeight)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(fill),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = ComponentSize.profileIdentityGapCountryToStats)
                            .width(ComponentSize.profileSkeletonStatWidth)
                            .height(ComponentSize.profileSkeletonStatHeight)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(fill),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(top = ComponentSize.profileBioTopInset),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(ComponentSize.profileSkeletonBioLineHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(ComponentSize.profileSkeletonBioLineHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun ProfileScreenPreview() {
    DemoTheme {
        ProfileScreenContent(
            state = ProfileUiState(
                nickname = "Dulce",
                age = 22,
                countryFlag = "🇨🇦",
                countryName = "Canada",
                followingCount = 12,
                followerCount = 12,
                bio = "Just a girl who loves good vibes, deep talks and spontaneous adventures ✨",
                translatedBio = "긍정적인 분위기, 진솔한 대화, 그리고 즉흥적인 모험을 좋아하는 소녀입니다 ✨",
                showTranslatedBio = true,
                isSelf = false,
                media = listOf(
                    ProfileMediaUi(1, false, null, 0),
                    ProfileMediaUi(2, true, null, 324),
                    ProfileMediaUi(3, false, null, 0),
                ),
            ),
            onIntent = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun ProfileBlockedByPeerPreview() {
    DemoTheme {
        ProfileScreenContent(
            state = ProfileUiState(
                nickname = "Dulce",
                age = 22,
                countryFlag = "🇨🇦",
                countryName = "Canada",
                followingCount = 12,
                followerCount = 12,
                bio = "Just a girl who loves good vibes, deep talks and spontaneous adventures ✨",
                isSelf = false,
                isBlockedByPeer = true,
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ProfileSkeletonPreview() {
    DemoTheme {
        ProfileScreenContent(
            state = ProfileUiState(isLoading = true),
            onIntent = {},
        )
    }
}

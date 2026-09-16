package com.example.demoproject.product.profile.gift

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.product.profile.ProfileGiftUi
import com.example.demoproject.product.profile.R
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val GiftSheetShape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)
private val GiftCardShape = RoundedCornerShape(Radius.sm)
private val GiftSheetTopGlow = Brush.verticalGradient(
    colors = listOf(DemoColors.giftSheetGlow, Color.Transparent),
)
/** Keep title / price slots the same height for skeleton and real content. */
private val GiftTitleSlotHeight = 12.dp
private val GiftPriceSlotHeight = 12.dp
/** Matches GiftCard vertical footprint so empty grid cells keep 2×4 page height. */
private val GiftCardSlotHeight =
    (Spacing.sm - Spacing.xxs) * 2 +
        ComponentSize.giftCardImage +
        Spacing.xs +
        GiftTitleSlotHeight +
        Spacing.xs +
        GiftPriceSlotHeight

private val GiftPlaceholderPage: List<ProfileGiftUi> = List(ComponentSize.giftPageSize) { index ->
    ProfileGiftUi(
        id = -1L - index,
        title = "",
        price = 0,
        iconUrl = "",
        isPlaceholder = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileGiftSheet(
    nickname: String,
    avatarUrl: String?,
    coinBalance: Int,
    gifts: List<ProfileGiftUi>,
    selectedGiftId: Long?,
    isCatalogLoading: Boolean,
    isSending: Boolean,
    onDismiss: () -> Unit,
    onSelectGift: (Long) -> Unit,
    onSend: () -> Unit,
    onOpenCoins: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = GiftSheetShape,
        containerColor = DemoColors.sheet,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
    ) {
        ProfileGiftSheetContent(
            nickname = nickname,
            avatarUrl = avatarUrl,
            coinBalance = coinBalance,
            gifts = gifts,
            selectedGiftId = selectedGiftId,
            isCatalogLoading = isCatalogLoading,
            isSending = isSending,
            onSelectGift = onSelectGift,
            onSend = onSend,
            onOpenCoins = onOpenCoins,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
internal fun ProfileGiftSheetContent(
    nickname: String,
    avatarUrl: String?,
    coinBalance: Int,
    gifts: List<ProfileGiftUi>,
    selectedGiftId: Long?,
    isCatalogLoading: Boolean,
    isSending: Boolean,
    onSelectGift: (Long) -> Unit,
    onSend: () -> Unit,
    onOpenCoins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showPlaceholders = isCatalogLoading && gifts.isEmpty()
    val catalogGifts = if (showPlaceholders) GiftPlaceholderPage else gifts
    val pages = remember(catalogGifts) {
        catalogGifts.chunked(ComponentSize.giftPageSize).ifEmpty { listOf(emptyList()) }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(GiftSheetTopGlow),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GiftSheetHeader(
                nickname = nickname,
                avatarUrl = avatarUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )
            Spacer(modifier = Modifier.height(Spacing.md + Spacing.xs))

            when {
                catalogGifts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.profile_gift_empty),
                        color = DemoColors.textAuxiliary,
                        fontSize = TextSize.sm,
                        modifier = Modifier.padding(vertical = Spacing.xl),
                    )
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !showPlaceholders,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = Spacing.chipGap),
                        pageSpacing = Spacing.giftCardGap,
                        // Pager height follows a full 2×4 page; keep short last pages top-aligned.
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        GiftPageGrid(
                            gifts = pages[page],
                            selectedGiftId = selectedGiftId,
                            onSelectGift = onSelectGift,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    GiftPagerIndicator(
                        pageCount = if (showPlaceholders) 1 else pages.size,
                        pageIndex = if (showPlaceholders) {
                            0f
                        } else {
                            pagerState.currentPage + pagerState.currentPageOffsetFraction
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            GiftSheetActions(
                coinBalance = coinBalance,
                canSend = selectedGiftId != null && !isSending && gifts.isNotEmpty(),
                isSending = isSending,
                onOpenCoins = onOpenCoins,
                onSend = onSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.chipGap)
                    .padding(bottom = Spacing.md),
            )
        }
    }
}

@Composable
private fun GiftSheetHeader(
    nickname: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val mid = stringResource(R.string.profile_gift_intro_mid)
    val nameLine = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DemoColors.textPrimary)) {
            append(nickname)
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = DemoColors.textPrimary)) {
            append(mid)
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_gift_intro_lead),
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ComponentSize.giftHeaderAvatar)
                    .clip(CircleShape)
                    .background(DemoColors.chip),
            )
            Text(
                text = nameLine,
                fontSize = TextSize.md,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val tail = stringResource(R.string.profile_gift_intro_tail)
        if (tail.isNotBlank()) {
            Text(
                text = tail,
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GiftPageGrid(
    gifts: List<ProfileGiftUi>,
    selectedGiftId: Long?,
    onSelectGift: (Long) -> Unit,
) {
    // Always lay out a fixed rows×cols grid so a short last page keeps cell sizes.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.giftCardGap),
    ) {
        repeat(ComponentSize.giftPageRows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.giftCardGap),
            ) {
                repeat(ComponentSize.giftPageColumns) { colIndex ->
                    val gift = gifts.getOrNull(rowIndex * ComponentSize.giftPageColumns + colIndex)
                    Box(modifier = Modifier.weight(1f)) {
                        if (gift != null) {
                            GiftCard(
                                gift = gift,
                                selected = !gift.isPlaceholder && gift.id == selectedGiftId,
                                onClick = {
                                    if (!gift.isPlaceholder) onSelectGift(gift.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(GiftCardSlotHeight),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberGiftSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "giftSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "giftSkeletonPulse",
    )
    return pulse
}

@Composable
private fun GiftCard(
    gift: ProfileGiftUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectCd = stringResource(R.string.profile_gift_cd_select)
    val pulse = rememberGiftSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    Column(
        modifier = modifier
            .clip(GiftCardShape)
            .background(if (selected) DemoColors.giftCardSelected else DemoColors.giftCardSurface)
            .border(
                width = 1.dp,
                color = if (selected) DemoColors.link else DemoColors.giftCardBorder,
                shape = GiftCardShape,
            )
            .then(
                if (gift.isPlaceholder) {
                    Modifier
                } else {
                    Modifier
                        .clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                        .semantics { contentDescription = selectCd }
                },
            )
            .padding(vertical = Spacing.sm - Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (gift.isPlaceholder) {
            Box(
                modifier = Modifier
                    .size(ComponentSize.giftCardImage)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(gift.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = gift.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(ComponentSize.giftCardImage),
            )
        }
        Box(
            modifier = Modifier.height(GiftTitleSlotHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (gift.isPlaceholder) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(GiftTitleSlotHeight)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(fill),
                )
            } else {
                Text(
                    text = gift.title,
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.caption,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = GiftTitleSlotHeight.value.sp,
                )
            }
        }
        Box(
            modifier = Modifier.height(GiftPriceSlotHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (gift.isPlaceholder) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(GiftPriceSlotHeight)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(fill),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Text(
                        text = gift.price.toString(),
                        color = DemoColors.giftPrice,
                        fontSize = TextSize.giftPrice,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        lineHeight = GiftPriceSlotHeight.value.sp,
                    )
                    Image(
                        painter = painterResource(R.drawable.profile_ic_coin),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.giftCardCoin),
                    )
                }
            }
        }
    }
}

@Composable
private fun GiftPagerIndicator(
    pageCount: Int,
    pageIndex: Float,
) {
    if (pageCount <= 1) {
        Spacer(modifier = Modifier.height(ComponentSize.giftPagerTrackHeight))
        return
    }
    val travel = (ComponentSize.giftPagerTrackWidth - ComponentSize.giftPagerThumbWidth)
    val maxIndex = (pageCount - 1).coerceAtLeast(1)
    val fraction = (pageIndex / maxIndex).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(ComponentSize.giftPagerTrackWidth)
            .height(ComponentSize.giftPagerTrackHeight)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.giftPagerTrack),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = travel * fraction)
                .width(ComponentSize.giftPagerThumbWidth)
                .height(ComponentSize.giftPagerTrackHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.link),
        )
    }
}

@Composable
private fun GiftSheetActions(
    coinBalance: Int,
    canSend: Boolean,
    isSending: Boolean,
    onOpenCoins: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val balanceCd = stringResource(R.string.profile_gift_cd_balance)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .height(ComponentSize.giftActionHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.giftBalancePill)
                .clickable(role = Role.Button, onClick = onOpenCoins)
                .semantics { contentDescription = balanceCd }
                .padding(start = Spacing.md, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = coinBalance.toString(),
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Image(
                painter = painterResource(R.drawable.profile_ic_coin),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.giftBalanceIcon),
            )
            Image(
                painter = painterResource(R.drawable.profile_ic_chevron),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.giftBalanceIcon),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(ComponentSize.giftActionHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(
                    enabled = canSend,
                    role = Role.Button,
                    onClick = onSend,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ComponentSize.giftBalanceIcon),
                    color = DemoColors.onPrimaryButton,
                    strokeWidth = ComponentSize.profileProgressStroke,
                )
            } else {
                Text(
                    text = stringResource(R.string.profile_gift_send),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfileGiftSheetPreview() {
    DemoTheme {
        ProfileGiftSheetContent(
            nickname = "Valeria",
            avatarUrl = null,
            coinBalance = 100,
            gifts = List(8) { index ->
                ProfileGiftUi(
                    id = index.toLong(),
                    title = "Love Heart",
                    price = 199,
                    iconUrl = "",
                )
            },
            selectedGiftId = 1L,
            isCatalogLoading = false,
            isSending = false,
            onSelectGift = {},
            onSend = {},
            onOpenCoins = {},
        )
    }
}

@Preview(name = "ProfileGiftSheet partial last page")
@Composable
private fun ProfileGiftSheetPartialPagePreview() {
    DemoTheme {
        ProfileGiftSheetContent(
            nickname = "Valeria",
            avatarUrl = null,
            coinBalance = 100,
            gifts = List(5) { index ->
                ProfileGiftUi(
                    id = index.toLong(),
                    title = "Love Heart",
                    price = 199,
                    iconUrl = "",
                )
            },
            selectedGiftId = 1L,
            isCatalogLoading = false,
            isSending = false,
            onSelectGift = {},
            onSend = {},
            onOpenCoins = {},
        )
    }
}

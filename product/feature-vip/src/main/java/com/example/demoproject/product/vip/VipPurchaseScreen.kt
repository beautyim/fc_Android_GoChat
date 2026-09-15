package com.example.demoproject.product.vip

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.currentWindowSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VipPurchaseScreen(
    viewModel: VipPurchaseViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current

    DisposableEffect(activity) {
        viewModel.bindActivity(activity)
        onDispose { viewModel.bindActivity(null) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is VipPurchaseEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    VipPurchaseScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
fun VipPurchaseScreen(
    state: VipPurchaseUiState,
    onIntent: (VipPurchaseIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSize = currentWindowSize()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet),
    ) {
        Image(
            painter = painterResource(R.drawable.vip_bg_page),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            VipTopBar(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (windowSize.isCompactWidth) {
                            Modifier
                        } else {
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .readableContentWidth()
                        },
                    ),
            )
            when {
                state.isLoading && state.plans.isEmpty() -> {
                    VipPurchaseSkeleton(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (windowSize.isCompactWidth) {
                                    Modifier
                                } else {
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .readableContentWidth()
                                },
                            ),
                    )
                }
                state.errorMessage != null && state.plans.isEmpty() -> {
                    VipErrorState(
                        message = state.errorMessage,
                        onRetry = { onIntent(VipPurchaseIntent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    VipPurchaseContent(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (windowSize.isCompactWidth) {
                                    Modifier
                                } else {
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .readableContentWidth()
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun VipTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backCd = stringResource(R.string.vip_cd_back)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .height(ComponentSize.navHeaderHeight)
            .padding(horizontal = Spacing.md),
    ) {
        Image(
            painter = painterResource(R.drawable.vip_ic_back),
            contentDescription = backCd,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = ComponentSize.vipNavIconTop)
                .size(ComponentSize.vipNavIcon)
                .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                .semantics { contentDescription = backCd }
                .clickable(onClick = onBack),
        )
    }
}

@Composable
private fun VipPurchaseContent(
    state: VipPurchaseUiState,
    onIntent: (VipPurchaseIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showExpiry = state.isVip && !state.expiryText.isNullOrBlank()
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.vipContentInset),
        ) {
            Spacer(
                modifier = Modifier.height(
                    if (state.isVip) Spacing.vipHeroTopMember else Spacing.vipHeroTop,
                ),
            )
            VipHeroHeader(
                isVip = state.isVip,
                expiryText = state.expiryText,
            )
            Spacer(
                modifier = Modifier.height(
                    if (showExpiry) Spacing.vipExpiryToBenefits else Spacing.vipHeroToBenefits,
                ),
            )
            VipBenefitsCard(
                benefits = state.benefits,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.vipBenefitsToSection))
            Text(
                text = stringResource(R.string.vip_choose_plan),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                lineHeight = TextSize.lg,
            )
            Spacer(modifier = Modifier.height(Spacing.vipSectionToPlans))
            if (state.plans.isEmpty()) {
                Text(
                    text = stringResource(R.string.vip_empty),
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.sm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg),
                    textAlign = TextAlign.Center,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.vipPlanGap),
                ) {
                    state.plans.forEach { plan ->
                        VipPlanCard(
                            plan = plan,
                            selected = plan.id == state.selectedPlanId,
                            enabled = !state.isPurchasing,
                            onClick = { onIntent(VipPurchaseIntent.SelectPlan(plan.id)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
        VipBottomBar(
            isVip = state.isVip,
            enabled = state.selectedPlanId != null,
            isLoading = state.isPurchasing,
            onPurchase = { onIntent(VipPurchaseIntent.PurchaseSelected) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.sm),
        )
    }
}

@Composable
private fun VipHeroHeader(
    isVip: Boolean,
    expiryText: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = Spacing.vipHeroStartExtra)) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = DemoColors.vipHeroTitle,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = TextSize.vipHero,
                    ),
                ) {
                    append(
                        if (isVip) {
                            stringResource(R.string.vip_hero_member_prefix)
                        } else {
                            stringResource(R.string.vip_hero_upgrade_prefix)
                        },
                    )
                }
                withStyle(
                    SpanStyle(
                        color = DemoColors.link,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = TextSize.vipHero,
                    ),
                ) {
                    append(
                        if (isVip) {
                            stringResource(R.string.vip_hero_member_accent)
                        } else {
                            stringResource(R.string.vip_hero_upgrade_accent)
                        },
                    )
                }
            },
            // Figma 1:5326 / 1:5399 — two-line hero (upgrade & member), 32 / 46.
            softWrap = true,
            lineHeight = TextSize.vipHeroLine,
        )
        if (isVip && !expiryText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xs + Spacing.xxs))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .border(ComponentSize.meCardStroke, DemoColors.link, RoundedCornerShape(Radius.pill))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.vip_expiry_fmt, expiryText),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.meMeta,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun VipBenefitsCard(
    benefits: List<VipBenefitUi>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    val columns = benefits.take(VipBenefitColumnCount)
    Column(
        modifier = modifier
            .heightIn(min = ComponentSize.vipBenefitCardHeight)
            .shadow(
                elevation = ComponentSize.vipBenefitCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipBenefitCardShadow,
                spotColor = DemoColors.vipBenefitCardShadow,
            )
            .clip(shape)
            .background(DemoColors.sheet)
            .padding(
                top = Spacing.vipBenefitCardTop,
                bottom = Spacing.vipBenefitCardBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.vipBenefitStarGap),
        ) {
            Image(
                painter = painterResource(R.drawable.vip_ic_star),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipBenefitStar),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.vip_benefit_section),
                color = DemoColors.link,
                fontSize = TextSize.md,
                fontWeight = FontWeight.Bold,
                lineHeight = TextSize.md,
            )
            Image(
                painter = painterResource(R.drawable.vip_ic_star),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipBenefitStar),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.vipBenefitTitleToItems))
        if (columns.isNotEmpty()) {
            // Full-bleed row: each privilege occupies exactly 1/N of the card width.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                columns.forEachIndexed { index, benefit ->
                    Box(modifier = Modifier.weight(1f)) {
                        VipBenefitItem(
                            benefit = benefit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (index < columns.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = Spacing.vipBenefitDividerTop)
                                    .width(ComponentSize.meCardStroke)
                                    .height(ComponentSize.vipBenefitDividerHeight)
                                    .background(DemoColors.divider),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VipBenefitItem(
    benefit: VipBenefitUi,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = rememberVipBenefitLabel(benefit.title)
    Column(
        modifier = modifier.padding(horizontal = Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (benefit.iconUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(ComponentSize.vipBenefitIcon))
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(benefit.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = benefit.title,
                modifier = Modifier.size(ComponentSize.vipBenefitIcon),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.vipBenefitIconToLabel))
        Text(
            text = label,
            color = DemoColors.textPrimary,
            fontSize = TextSize.xs,
            textAlign = TextAlign.Center,
            softWrap = true,
            overflow = TextOverflow.Clip,
            lineHeight = TextSize.vipBenefitLabelLine,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberVipBenefitLabel(title: String): AnnotatedString {
    return remember(title) {
        val parts = title.trim().split(Regex("\\s+"), limit = 2)
        buildAnnotatedString {
            if (parts.size == 2) {
                val first = parts[0]
                val second = parts[1]
                val firstWeight =
                    if (first.any { it.isDigit() }) FontWeight.SemiBold else FontWeight.Normal
                withStyle(SpanStyle(fontWeight = firstWeight)) {
                    append(first)
                }
                append(' ')
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(second)
                }
            } else {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(title.trim())
                }
            }
        }
    }
}

@Composable
private fun VipPlanCard(
    plan: VipPlanUi,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipPlanCard)
    val saleBottom = if (selected) {
        Spacing.vipPlanCardBottomSelected
    } else {
        Spacing.vipPlanCardBottom
    }
    Box(
        modifier = modifier
            .height(ComponentSize.vipPlanCardHeight)
            .shadow(
                elevation = ComponentSize.vipPlanCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipPlanCardShadow,
                spotColor = DemoColors.vipPlanCardShadow,
            )
            .clip(shape)
            .background(
                if (selected) DemoColors.vipPlanSelectedFill else DemoColors.storeCoinCardSurface,
            )
            .then(
                if (selected) {
                    Modifier.border(ComponentSize.meCardStroke, DemoColors.link, shape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.vipPlanTitleTop)
                .padding(horizontal = Spacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = plan.title,
                color = DemoColors.textPrimary,
                fontSize = TextSize.storePrice,
                fontWeight = FontWeight.SemiBold,
                lineHeight = TextSize.vipPlanTitleLine,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.vipPlanTitleToPrice))
            Text(
                text = plan.price,
                color = if (selected) DemoColors.link else DemoColors.vipPlanMuted,
                fontSize = TextSize.vipPlanPrice,
                fontWeight = FontWeight.Black,
                lineHeight = TextSize.vipPlanPriceLine,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            plan.originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
                Spacer(modifier = Modifier.height(Spacing.vipPlanPriceToOriginal))
                Text(
                    text = original,
                    color = if (selected) DemoColors.textSecondary else DemoColors.vipPlanMuted,
                    fontSize = TextSize.xs,
                    lineHeight = TextSize.xs,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (plan.dayDesc.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.vipPlanTextGap))
                Text(
                    text = plan.dayDesc,
                    color = DemoColors.vipPlanMuted,
                    fontSize = TextSize.xs,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = TextSize.xs,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (plan.label.isNotBlank()) {
            VipPlanTopBadge(
                text = plan.label,
                selected = selected,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        // Pin to card bottom (Figma 1:5382) so fixed card height never clips the sale chip.
        if (plan.saleText.isNotBlank()) {
            VipSaleBadge(
                text = plan.saleText,
                selected = selected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = saleBottom),
            )
        }
    }
}

@Composable
private fun VipPlanTopBadge(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val brush: Brush =
        if (selected) DemoGradients.vipPlanBadgePopular else DemoGradients.vipPlanBadgeMuted
    Box(
        modifier = modifier
            .widthIn(min = ComponentSize.vipPlanBadgeMinWidth)
            .height(ComponentSize.vipPlanBadgeHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = Radius.sm,
                    bottomEnd = Radius.sm,
                ),
            )
            .background(brush)
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) DemoColors.onPrimaryButton else DemoColors.vipPlanBadgeText,
            fontSize = TextSize.caption,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = TextSize.meMeta,
        )
    }
}

@Composable
private fun VipSaleBadge(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.vipPlanDiscountHot)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.caption,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = TextSize.meMeta,
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.vipPlanMuted)
                .padding(horizontal = Spacing.xs, vertical = ComponentSize.vipPlanSaleMutedVertical),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = DemoColors.vipPlanDiscountMutedText,
                fontSize = TextSize.caption,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = TextSize.meMeta,
            )
        }
    }
}

@Composable
private fun VipBottomBar(
    isVip: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickEnabled = enabled && !isLoading
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.vipCtaToNote),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.vipCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(enabled = clickEnabled, onClick = onPurchase),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                val loadingCd = stringResource(R.string.vip_cd_loading)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(IconSize.sm)
                        .semantics { contentDescription = loadingCd },
                    color = DemoColors.onPrimaryButton,
                    strokeWidth = ComponentSize.profileProgressStroke,
                )
            } else {
                Text(
                    text = stringResource(
                        if (isVip) R.string.vip_cta_resubscribe else R.string.vip_cta_get,
                    ),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Image(
                painter = painterResource(R.drawable.vip_ic_shield),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipDisclaimerIcon),
            )
            Text(
                text = stringResource(R.string.vip_billing_note),
                color = DemoColors.vipPlanMuted,
                fontSize = TextSize.storeRibbon,
                lineHeight = TextSize.storeRibbon,
            )
        }
    }
}

@Composable
private fun rememberVipSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "vipSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "vipSkeletonPulse",
    )
    return pulse
}

@Composable
private fun VipPurchaseSkeleton(modifier: Modifier = Modifier) {
    val pulse = rememberVipSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.vip_cd_loading)
    Column(
        modifier = modifier.semantics { contentDescription = loadingCd },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.vipContentInset),
        ) {
            Spacer(modifier = Modifier.height(Spacing.vipHeroTop))
            VipHeroSkeleton(fill = fill, highlight = highlight)
            Spacer(modifier = Modifier.height(Spacing.vipHeroToBenefits))
            VipBenefitsCardSkeleton(fill = fill, highlight = highlight)
            Spacer(modifier = Modifier.height(Spacing.vipBenefitsToSection))
            Box(
                modifier = Modifier
                    .width(ComponentSize.vipSkeletonSectionWidth)
                    .height(ComponentSize.vipSkeletonSectionHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
            Spacer(modifier = Modifier.height(Spacing.vipSectionToPlans))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.vipPlanGap),
            ) {
                repeat(ComponentSize.vipSkeletonPlanCount) {
                    VipPlanCardSkeleton(
                        fill = fill,
                        highlight = highlight,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        VipBottomBarSkeleton(
            fill = fill,
            highlight = highlight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.sm),
        )
    }
}

@Composable
private fun VipHeroSkeleton(
    fill: Color,
    highlight: Color,
) {
    Column(
        modifier = Modifier.padding(start = Spacing.vipHeroStartExtra),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonHeroWidth)
                .height(ComponentSize.vipSkeletonHeroHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonSectionWidth)
                .height(ComponentSize.vipSkeletonHeroHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(fill),
        )
    }
}

@Composable
private fun VipBenefitsCardSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ComponentSize.vipBenefitCardHeight)
            .shadow(
                elevation = ComponentSize.vipBenefitCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipBenefitCardShadow,
                spotColor = DemoColors.vipBenefitCardShadow,
            )
            .clip(shape)
            .background(DemoColors.sheet)
            .padding(
                top = Spacing.vipBenefitCardTop,
                bottom = Spacing.vipBenefitCardBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonBenefitTitleWidth)
                .height(ComponentSize.vipSkeletonBenefitTitleHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
        Spacer(modifier = Modifier.height(Spacing.vipBenefitTitleToItems))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            repeat(ComponentSize.vipSkeletonBenefitCount) { index ->
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ComponentSize.vipBenefitIcon)
                                .clip(CircleShape)
                                .background(fill),
                        )
                        Spacer(modifier = Modifier.height(Spacing.vipBenefitIconToLabel))
                        Box(
                            modifier = Modifier
                                .width(ComponentSize.vipSkeletonBenefitLabelWidth)
                                .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                                .clip(RoundedCornerShape(Radius.sm))
                                .background(fill),
                        )
                    }
                    if (index < ComponentSize.vipSkeletonBenefitCount - 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = Spacing.vipBenefitDividerTop)
                                .width(ComponentSize.meCardStroke)
                                .height(ComponentSize.vipBenefitDividerHeight)
                                .background(DemoColors.divider),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VipPlanCardSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipPlanCard)
    Column(
        modifier = modifier
            .height(ComponentSize.vipPlanCardHeight)
            .shadow(
                elevation = ComponentSize.vipPlanCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipPlanCardShadow,
                spotColor = DemoColors.vipPlanCardShadow,
            )
            .clip(shape)
            .background(DemoColors.storeCoinCardSurface)
            .padding(top = Spacing.vipPlanTitleTop)
            .padding(horizontal = Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonPlanTitleWidth)
                .height(ComponentSize.vipSkeletonPlanTitleHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(fill),
        )
        Spacer(modifier = Modifier.height(Spacing.vipPlanTitleToPrice))
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonPlanPriceWidth)
                .height(ComponentSize.vipSkeletonPlanPriceHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
        Spacer(modifier = Modifier.height(Spacing.vipPlanPriceToOriginal))
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonPlanTitleWidth)
                .height(ComponentSize.vipSkeletonPlanTitleHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(fill),
        )
        Spacer(modifier = Modifier.height(Spacing.vipPlanTextGap))
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonPlanTitleWidth)
                .height(ComponentSize.storeDiscountChipMinHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
    }
}

@Composable
private fun VipBottomBarSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.vipCtaToNote),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.vipCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
        Box(
            modifier = Modifier
                .width(ComponentSize.vipSkeletonDisclaimerWidth)
                .height(ComponentSize.vipSkeletonDisclaimerHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
    }
}

@Composable
private fun VipErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = DemoColors.textSecondary,
            fontSize = TextSize.sm,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.vip_retry))
        }
    }
}

private const val VipBenefitColumnCount = 5

private val previewState = VipPurchaseUiState(
    isVip = false,
    benefits = listOf(
        VipBenefitUi(3, "333 Coins", null),
        VipBenefitUi(4, "2 Matches", null),
        VipBenefitUi(1, "VIP Benefits", null),
        VipBenefitUi(5, "Exclusive Discounts", null),
        VipBenefitUi(2, "Priority Match", null),
    ),
    plans = listOf(
        VipPlanUi(1, "vip_1", "1 Month", "$6.99", "$14.99", "Week", "53% OFF", "Best Deal"),
        VipPlanUi(2, "vip_2", "1 Month", "$6.99", "$14.99", "Week", "76% OFF", "Most Popular"),
        VipPlanUi(3, "vip_3", "1 Month", "$6.99", "$14.99", "Week", "53% OFF", "Try Now"),
    ),
    selectedPlanId = 2,
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "VIP RTL", locale = "ar")
@Composable
private fun VipPurchaseScreenPreview() {
    DemoTheme {
        VipPurchaseScreen(
            state = previewState,
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "VIP skeleton")
@Composable
private fun VipPurchaseSkeletonPreview() {
    DemoTheme {
        VipPurchaseScreen(
            state = VipPurchaseUiState(isLoading = true),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "VIP member")
@Composable
private fun VipPurchaseMemberPreview() {
    DemoTheme {
        VipPurchaseScreen(
            state = previewState.copy(isVip = true, expiryText = "26/12/31"),
            onIntent = {},
            onBack = {},
        )
    }
}

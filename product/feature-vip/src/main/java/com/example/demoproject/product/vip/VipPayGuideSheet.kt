package com.example.demoproject.product.vip

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val VipPayGuideSheetShape = RoundedCornerShape(
    topStart = Radius.vipPayGuideSheet,
    topEnd = Radius.vipPayGuideSheet,
)

private val VipGuideTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private val VipGuideTitleStyle = TextStyle(
    fontSize = TextSize.vipGuideTitle,
    fontWeight = FontWeight.SemiBold,
    lineHeight = TextSize.vipGuideTitleLine,
    letterSpacing = TextSize.vipGuideTitleTracking,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VipGuideTightLineHeight,
)

private val VipGuideCaptionStyle = TextStyle(
    fontSize = TextSize.caption,
    fontWeight = FontWeight.Normal,
    lineHeight = TextSize.vipGuideCaptionLine,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VipGuideTightLineHeight,
)

private val VipGuidePlanTitleStyle = TextStyle(
    fontSize = TextSize.md,
    fontWeight = FontWeight.Bold,
    lineHeight = TextSize.vipGuidePlanLine,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VipGuideTightLineHeight,
)

private val VipGuidePlanPriceStyle = TextStyle(
    fontSize = TextSize.md,
    fontWeight = FontWeight.Bold,
    lineHeight = TextSize.vipGuidePlanLine,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VipGuideTightLineHeight,
)

private val VipGuidePlanOriginalStyle = TextStyle(
    fontSize = TextSize.xs,
    fontWeight = FontWeight.Medium,
    lineHeight = TextSize.xs,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VipGuideTightLineHeight,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipPayGuideSheet(
    state: VipPayGuideUiState,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VipPayGuideSheetShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
    ) {
        VipPayGuideSheetContent(
            state = state,
            onDismiss = onDismiss,
            onUpgrade = onUpgrade,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
internal fun VipPayGuideSheetContent(
    state: VipPayGuideUiState,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.vip_guide_cd_close)
    val sheetShape = VipPayGuideSheetShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(DemoColors.sheet),
    ) {
        Image(
            painter = painterResource(R.drawable.vip_guide_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .matchParentSize()
                .clip(sheetShape),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.vipGuideTitleTop)
                .padding(bottom = Spacing.vipGuideCtaBottom),
        ) {
            VipPayGuideTitle(
                modifier = Modifier.padding(horizontal = Spacing.vipGuideContentInset),
            )
            Spacer(modifier = Modifier.height(Spacing.vipGuideTitleToSubtitle))
            if (state.isLoading) {
                VipPayGuideSkeletonBody(
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VipPayGuideSubtitle(
                    peerNickname = state.peerNickname,
                    modifier = Modifier.padding(horizontal = Spacing.vipGuideContentInset),
                )
                Spacer(modifier = Modifier.height(Spacing.vipGuideSubtitleToBenefits))
                VipPayGuideBenefitsCard(
                    benefits = state.benefits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.vipGuidePlanInset),
                )
                Spacer(modifier = Modifier.height(Spacing.vipGuideBenefitsToPlan))
                state.plan?.let { plan ->
                    VipPayGuidePlanCard(
                        plan = plan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.vipGuidePlanInset),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.vipGuidePlanToCta))
                VipPayGuideCta(
                    isPurchasing = state.isPurchasing,
                    enabled = state.plan != null && !state.isPurchasing,
                    onUpgrade = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.vipGuideCloseInset)
                .clip(CircleShape)
                .background(DemoColors.vipGuideCloseChip)
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(Spacing.vipGuideClosePadding)
                .semantics { contentDescription = closeCd },
        ) {
            Image(
                painter = painterResource(R.drawable.vip_guide_ic_close),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipGuideCloseIcon),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun VipPayGuideTitle(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = DemoColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSize.vipGuideTitle,
                    letterSpacing = TextSize.vipGuideTitleTracking,
                ),
            ) {
                append(stringResource(R.string.vip_guide_title_line1))
                append('\n')
            }
            withStyle(
                SpanStyle(
                    color = DemoColors.vipGuideAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSize.vipGuideTitle,
                    letterSpacing = TextSize.vipGuideTitleTracking,
                ),
            ) {
                append(stringResource(R.string.vip_guide_title_accent))
            }
            withStyle(
                SpanStyle(
                    color = DemoColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSize.vipGuideTitle,
                    letterSpacing = TextSize.vipGuideTitleTracking,
                ),
            ) {
                append(stringResource(R.string.vip_guide_title_emoji))
            }
        },
        style = VipGuideTitleStyle,
        modifier = modifier,
    )
}

@Composable
private fun VipPayGuideSubtitle(
    peerNickname: String,
    modifier: Modifier = Modifier,
) {
    val name = peerNickname.ifBlank { stringResource(R.string.vip_guide_peer_fallback) }
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = DemoColors.textPrimary,
                    fontWeight = FontWeight.Normal,
                    fontSize = TextSize.caption,
                ),
            ) {
                append(stringResource(R.string.vip_guide_subtitle_prefix))
            }
            withStyle(
                SpanStyle(
                    color = DemoColors.vipGuideSubtitleMuted,
                    fontWeight = FontWeight.Normal,
                    fontSize = TextSize.caption,
                ),
            ) {
                append(stringResource(R.string.vip_guide_subtitle_mid))
                append('\n')
                append(stringResource(R.string.vip_guide_subtitle_with, name))
            }
        },
        style = VipGuideCaptionStyle,
        modifier = modifier,
    )
}

@Composable
private fun VipPayGuideBenefitsCard(
    benefits: List<VipPayGuideBenefitUi>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    Column(
        modifier = modifier
            .heightIn(min = ComponentSize.vipBenefitCardHeight)
            .shadow(
                elevation = ComponentSize.vipGuideBenefitCardElevation,
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
                painter = painterResource(R.drawable.vip_guide_ic_star),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipBenefitStar),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.vip_guide_why_section),
                color = DemoColors.link,
                fontSize = TextSize.xs,
                fontWeight = FontWeight.Medium,
                lineHeight = TextSize.xs,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = VipGuideTightLineHeight,
                ),
            )
            Image(
                painter = painterResource(R.drawable.vip_guide_ic_star),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.vipBenefitStar),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.vipBenefitTitleToItems))
        if (benefits.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                benefits.forEachIndexed { index, benefit ->
                    Box(modifier = Modifier.weight(1f)) {
                        VipPayGuideBenefitItem(
                            benefit = benefit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (index < benefits.lastIndex) {
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
private fun VipPayGuideBenefitItem(
    benefit: VipPayGuideBenefitUi,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = rememberVipGuideBenefitLabel(benefit.title)
    val fallbackRes = remember(benefit.id) { privilegeIconFallback(benefit.id) }
    Column(
        modifier = modifier.padding(horizontal = Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!benefit.iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(benefit.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = benefit.title,
                placeholder = painterResource(fallbackRes),
                error = painterResource(fallbackRes),
                modifier = Modifier.size(ComponentSize.vipBenefitIcon),
                contentScale = ContentScale.Fit,
            )
        } else {
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = benefit.title,
                modifier = Modifier.size(ComponentSize.vipBenefitIcon),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.vipBenefitIconToLabel))
        Text(
            text = label,
            color = DemoColors.textPrimary,
            fontSize = TextSize.caption,
            textAlign = TextAlign.Center,
            softWrap = true,
            overflow = TextOverflow.Clip,
            style = VipGuideCaptionStyle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberVipGuideBenefitLabel(title: String) = remember(title) {
    val parts = title.trim().split(Regex("\\s+"), limit = 2)
    buildAnnotatedString {
        if (parts.size == 2) {
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(parts[0]) }
            append('\n')
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(parts[1]) }
        } else {
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(title.trim()) }
        }
    }
}

@DrawableRes
private fun privilegeIconFallback(privilegeId: Int): Int = when (privilegeId) {
    1 -> R.drawable.vip_guide_ic_coins
    2 -> R.drawable.vip_guide_ic_match
    3 -> R.drawable.vip_guide_ic_badge
    4 -> R.drawable.vip_ic_shield
    5 -> R.drawable.vip_guide_ic_match
    else -> R.drawable.vip_guide_ic_badge
}

@Composable
private fun VipPayGuidePlanCard(
    plan: VipPayGuidePlanUi,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    Row(
        modifier = modifier
            .height(ComponentSize.vipGuidePlanCardHeight)
            .shadow(
                elevation = ComponentSize.vipGuidePlanCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipGuidePlanShadow,
                spotColor = DemoColors.vipGuidePlanShadow,
            )
            .clip(shape)
            .background(DemoGradients.vipGuidePlanCard)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.vip_guide_ic_plan),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.vipGuidePlanIcon),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(Spacing.vipGuidePlanIconGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.title,
                color = DemoColors.textPrimary,
                style = VipGuidePlanTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.vipGuidePlanTextGap))
            Text(
                text = stringResource(R.string.vip_guide_plan_subtitle),
                color = DemoColors.textSecondary,
                fontWeight = FontWeight.Medium,
                style = VipGuideCaptionStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = plan.price,
                color = DemoColors.textPrimary,
                style = VipGuidePlanPriceStyle,
                maxLines = 1,
            )
            plan.originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
                Text(
                    text = original,
                    color = DemoColors.textAuxiliary,
                    style = VipGuidePlanOriginalStyle,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VipPayGuideCta(
    isPurchasing: Boolean,
    enabled: Boolean,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier
            .height(ComponentSize.vipGuideCtaHeight)
            .clip(shape)
            .background(DemoGradients.primaryButton)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onUpgrade,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isPurchasing) {
            CircularProgressIndicator(
                modifier = Modifier.size(ComponentSize.vipDisclaimerIcon),
                color = DemoColors.onPrimaryButton,
                strokeWidth = Spacing.xxs,
            )
        } else {
            Text(
                text = stringResource(R.string.vip_guide_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                lineHeight = TextSize.vipGuidePlanLine,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = VipGuideTightLineHeight,
                ),
            )
        }
    }
}

@Composable
private fun rememberVipGuideSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "vipGuideSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "vipGuideSkeletonPulse",
    )
    return pulse
}

@Composable
private fun VipPayGuideSkeletonBody(modifier: Modifier = Modifier) {
    val pulse = rememberVipGuideSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.vip_cd_loading)
    Column(
        modifier = modifier.semantics { contentDescription = loadingCd },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.vipGuideContentInset),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(highlight),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(fill),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.vipGuideSubtitleToBenefits))
        VipPayGuideBenefitsSkeleton(
            fill = fill,
            highlight = highlight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.vipGuidePlanInset),
        )
        Spacer(modifier = Modifier.height(Spacing.vipGuideBenefitsToPlan))
        VipPayGuidePlanSkeleton(
            fill = fill,
            highlight = highlight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.vipGuidePlanInset),
        )
        Spacer(modifier = Modifier.height(Spacing.vipGuidePlanToCta))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .height(ComponentSize.vipGuideCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
    }
}

@Composable
private fun VipPayGuideBenefitsSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    Column(
        modifier = modifier
            .heightIn(min = ComponentSize.vipBenefitCardHeight)
            .shadow(
                elevation = ComponentSize.vipGuideBenefitCardElevation,
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
                                .clip(RoundedCornerShape(Radius.xxs))
                                .background(fill),
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Box(
                            modifier = Modifier
                                .width(ComponentSize.vipSkeletonBenefitLabelWidth * 0.75f)
                                .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                                .clip(RoundedCornerShape(Radius.xxs))
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
private fun VipPayGuidePlanSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.vipBenefitCard)
    Row(
        modifier = modifier
            .height(ComponentSize.vipGuidePlanCardHeight)
            .shadow(
                elevation = ComponentSize.vipGuidePlanCardElevation,
                shape = shape,
                ambientColor = DemoColors.vipGuidePlanShadow,
                spotColor = DemoColors.vipGuidePlanShadow,
            )
            .clip(shape)
            .background(DemoGradients.vipGuidePlanCard)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.vipGuidePlanIcon)
                .clip(CircleShape)
                .background(highlight),
        )
        Spacer(modifier = Modifier.width(Spacing.vipGuidePlanIconGap))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.vipSkeletonPlanTitleWidth)
                    .height(ComponentSize.vipSkeletonPlanTitleHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(fill),
            )
            Spacer(modifier = Modifier.height(Spacing.vipGuidePlanTextGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(highlight),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.vipSkeletonPlanPriceWidth)
                    .height(ComponentSize.vipSkeletonPlanTitleHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(fill),
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Box(
                modifier = Modifier
                    .width(ComponentSize.vipSkeletonPlanTitleWidth)
                    .height(ComponentSize.vipSkeletonBenefitLabelHeight)
                    .clip(RoundedCornerShape(Radius.xxs))
                    .background(highlight),
            )
        }
    }
}

private val PreviewGuideState = VipPayGuideUiState(
    peerNickname = "Terry",
    benefits = listOf(
        VipPayGuideBenefitUi(1, "250 Coins", null),
        VipPayGuideBenefitUi(2, "2 Matches", null),
        VipPayGuideBenefitUi(3, "VIP Benefits", null),
        VipPayGuideBenefitUi(4, "Exclusive Discounts", null),
        VipPayGuideBenefitUi(5, "Priority Match", null),
    ),
    plan = VipPayGuidePlanUi(
        id = 200205,
        sku = "com.gochat.vip.month.tr",
        productType = 2,
        title = "1 Month VIP",
        price = "$4.99",
        originalPrice = "$19.96",
    ),
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, locale = "ar")
@Composable
private fun VipPayGuideSheetPreview() {
    DemoTheme {
        VipPayGuideSheetContent(
            state = PreviewGuideState,
            onDismiss = {},
            onUpgrade = {},
        )
    }
}

@Preview(showBackground = true, name = "VIP guide skeleton")
@Composable
private fun VipPayGuideSheetSkeletonPreview() {
    DemoTheme {
        VipPayGuideSheetContent(
            state = VipPayGuideUiState(isLoading = true, peerNickname = "Terry"),
            onDismiss = {},
            onUpgrade = {},
        )
    }
}

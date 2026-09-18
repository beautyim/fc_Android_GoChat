package com.example.demoproject.product.me

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradientPillButton
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest

private val VerifyLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private val VerifyTightTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = VerifyLineHeightStyle,
)

@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel,
    onBack: () -> Unit,
    onOpenCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                VerificationEffect.NavigateBack -> onBack()
                VerificationEffect.OpenCapture -> onOpenCapture()
                VerificationEffect.RequestCameraPermission -> Unit
                is VerificationEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    VerificationScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun VerificationScreen(
    state: VerificationUiState,
    onIntent: (VerificationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.page)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.sheet)
                .statusBarsPadding(),
        ) {
            VerificationTopBar(onBack = { onIntent(VerificationIntent.Back) })
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.verifyHeroWash)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DemoColors.verifyHeroWash, DemoColors.page),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .readableContentWidth()
                    .padding(horizontal = Spacing.verifyContentInset),
            ) {
                VerificationHeroCopy(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = Spacing.verifyHeroTitleTop,
                            end = Spacing.verifyHeroTextEndInset,
                        ),
                )
                Spacer(modifier = Modifier.height(Spacing.verifySubtitleToBenefits))
                VerificationBenefitsCard(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(Spacing.verifyBenefitsToHow))
                Text(
                    text = stringResource(R.string.verify_how_title),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.verifySectionTitle,
                    lineHeight = TextSize.verifySectionTitleLine,
                    fontWeight = FontWeight.SemiBold,
                    style = VerifyTightTextStyle,
                )
                Spacer(modifier = Modifier.height(Spacing.verifyHowToSteps))
                VerificationSteps(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.weight(1f))
            }
            Image(
                painter = painterResource(R.drawable.verify_ill_hero),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Spacing.verifyHeroArtTop)
                    .width(ComponentSize.verifyHeroWidth)
                    .height(ComponentSize.verifyHeroHeight),
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopEnd,
            )
        }
        DemoGradientPillButton(
            text = stringResource(R.string.verify_action_start),
            onClick = { onIntent(VerificationIntent.StartVerification) },
            modifier = Modifier
                .padding(horizontal = Spacing.verifyContentInset)
                .padding(bottom = Spacing.verifyCtaBottom)
                .readableContentWidth()
                .fillMaxWidth(),
            height = ComponentSize.verifyCtaHeight,
        )
    }
}

@Composable
private fun VerificationTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.verifyTopBar)
            .padding(horizontal = Spacing.md),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.verify_ic_back),
            contentDescription = stringResource(R.string.verify_cd_back),
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            iconSize = IconSize.md,
            size = IconSize.md,
            mirrorInRtl = true,
        )
        Text(
            text = stringResource(R.string.verify_title),
            modifier = Modifier.align(Alignment.Center),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            lineHeight = TextSize.mdLine,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = VerifyTightTextStyle,
        )
    }
}

@Composable
private fun VerificationHeroCopy(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.verify_hero_title_line1))
                append('\n')
                withStyle(SpanStyle(color = DemoColors.link)) {
                    append(stringResource(R.string.verify_hero_title_real))
                }
                append(stringResource(R.string.verify_hero_title_rest))
            },
            color = DemoColors.textPrimary,
            fontSize = TextSize.verifyHeroTitle,
            lineHeight = TextSize.verifyHeroTitleLine,
            fontWeight = FontWeight.SemiBold,
            style = VerifyTightTextStyle,
        )
        Spacer(modifier = Modifier.height(Spacing.verifyHeroTitleToSubtitle))
        Text(
            text = stringResource(R.string.verify_hero_subtitle),
            color = DemoColors.textSecondary,
            fontSize = TextSize.verifyHeroSubtitle,
            lineHeight = TextSize.verifyHeroSubtitleLine,
            style = VerifyTightTextStyle,
        )
    }
}

@Composable
private fun VerificationBenefitsCard(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(
                horizontal = Spacing.verifyBenefitCardPadH,
                vertical = Spacing.verifyBenefitCardPadV,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        VerificationBenefitItem(
            iconRes = R.drawable.verify_ic_shield,
            title = stringResource(R.string.verify_benefit_safer_title),
            body = stringResource(R.string.verify_benefit_safer_body),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(
            modifier = Modifier.height(ComponentSize.verifyIconCircle * 2),
            color = DemoColors.divider,
        )
        VerificationBenefitItem(
            iconRes = R.drawable.verify_ic_person,
            title = stringResource(R.string.verify_benefit_trust_title),
            body = stringResource(R.string.verify_benefit_trust_body),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(
            modifier = Modifier.height(ComponentSize.verifyIconCircle * 2),
            color = DemoColors.divider,
        )
        VerificationBenefitItem(
            iconRes = R.drawable.verify_ic_diamond,
            title = stringResource(R.string.verify_benefit_badge_title),
            body = stringResource(R.string.verify_benefit_badge_body),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VerificationBenefitItem(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.verifyBenefitTextGap),
    ) {
        VerificationIconDisc(iconRes = iconRes)
        Text(
            text = title,
            color = DemoColors.textPrimary,
            fontSize = TextSize.verifyBenefitTitle,
            lineHeight = TextSize.verifyBenefitTitleLine,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = VerifyTightTextStyle,
        )
        Text(
            text = body,
            color = DemoColors.textSecondary,
            fontSize = TextSize.verifyBenefitBody,
            lineHeight = TextSize.verifyBenefitBodyLine,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = VerifyTightTextStyle,
        )
    }
}

@Composable
private fun VerificationSteps(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.verifyStepGap),
    ) {
        VerificationStepRow(
            iconRes = R.drawable.verify_ic_camera,
            title = stringResource(R.string.verify_step_selfie_title),
            body = stringResource(R.string.verify_step_selfie_body),
        )
        VerificationStepRow(
            iconRes = R.drawable.verify_ic_face,
            title = stringResource(R.string.verify_step_liveness_title),
            body = stringResource(R.string.verify_step_liveness_body),
        )
        VerificationStepRow(
            iconRes = R.drawable.verify_ic_shield,
            title = stringResource(R.string.verify_step_done_title),
            body = stringResource(R.string.verify_step_done_body),
        )
    }
}

@Composable
private fun VerificationStepRow(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val mirrorChevron = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(Spacing.verifyStepCardPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.verifyStepIconGap),
        ) {
            VerificationIconDisc(iconRes = iconRes)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.verifyStepTextGap),
            ) {
                Text(
                    text = title,
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.verifyStepTitle,
                    lineHeight = TextSize.verifyStepTitleLine,
                    fontWeight = FontWeight.SemiBold,
                    style = VerifyTightTextStyle,
                )
                Text(
                    text = body,
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.verifyStepBody,
                    lineHeight = TextSize.verifyStepBodyLine,
                    style = VerifyTightTextStyle,
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.verify_ic_chevron),
            contentDescription = null,
            modifier = Modifier
                .size(IconSize.settings)
                .graphicsLayer { scaleX = if (mirrorChevron) -1f else 1f },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun VerificationIconDisc(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ComponentSize.verifyIconCircle)
            .clip(CircleShape)
            .background(DemoColors.verifyIconBg),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(IconSize.md),
            contentScale = ContentScale.Fit,
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun VerificationScreenPreview() {
    DemoTheme {
        VerificationScreen(
            state = VerificationUiState(),
            onIntent = {},
        )
    }
}

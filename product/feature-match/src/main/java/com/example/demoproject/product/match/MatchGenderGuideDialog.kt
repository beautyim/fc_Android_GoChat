package com.example.demoproject.product.match

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_FEMALE
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_MALE
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTextAutoSize
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Match gender-guide popup — Figma 268:4336 / 318:5314.
 *
 * Layout tokens are the Figma 343×382 board positions; [BoxWithConstraints]
 * scales them so avatars/checkboxes stay aligned when the card width changes.
 */
@Composable
fun MatchGenderGuideDialog(
    selectedSex: Int,
    onSelectSex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnScrim: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(30f)
            .background(DemoColors.scrim)
            .pointerInput(dismissOnScrim) {
                detectTapGestures(
                    onTap = {
                        if (dismissOnScrim) onDismiss()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = ComponentSize.matchGenderGuideWidth)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            MatchGenderGuideCard(
                selectedSex = selectedSex,
                onSelectSex = onSelectSex,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun MatchGenderGuideCard(
    selectedSex: Int,
    onSelectSex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.match_gender_guide_cd_close)
    val maleCd = stringResource(R.string.match_filter_male)
    val femaleCd = stringResource(R.string.match_filter_female)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.matchGenderGuideAspect)
            .clip(RoundedCornerShape(Radius.lg))
            .background(DemoColors.sheet),
    ) {
        val scale = maxWidth / ComponentSize.matchGenderGuideWidth
        fun Dp.scaled(): Dp = this * scale

        Image(
            painter = painterResource(R.drawable.match_gender_guide_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Arrow — Figma left 133 / top 18.48 / 80×67.841
        Image(
            painter = painterResource(R.drawable.match_gender_guide_arrow),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.matchGenderGuideArrowStart.scaled(),
                    top = ComponentSize.matchGenderGuideArrowTop.scaled(),
                )
                .size(
                    width = ComponentSize.matchGenderGuideArrowWidth.scaled(),
                    height = ComponentSize.matchGenderGuideArrowHeight.scaled(),
                ),
            contentScale = ContentScale.Fit,
        )

        // Male avatar — Figma left 37.5 / top 66.36 / 107×111.265
        GenderGuideAvatar(
            avatarRes = R.drawable.match_gender_guide_male,
            contentDescription = maleCd,
            onClick = { onSelectSex(MATCH_SEX_MALE) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.matchGenderGuideMaleStart.scaled(),
                    top = ComponentSize.matchGenderGuideMaleTop.scaled(),
                )
                .size(
                    width = ComponentSize.matchGenderGuideMaleWidth.scaled(),
                    height = ComponentSize.matchGenderGuideMaleHeight.scaled(),
                ),
        )

        // Female avatar — Figma left 190 / top 59 / 120
        GenderGuideAvatar(
            avatarRes = R.drawable.match_gender_guide_female,
            contentDescription = femaleCd,
            onClick = { onSelectSex(MATCH_SEX_FEMALE) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.matchGenderGuideFemaleStart.scaled(),
                    top = ComponentSize.matchGenderGuideFemaleTop.scaled(),
                )
                .size(ComponentSize.matchGenderGuideFemale.scaled()),
        )

        // Male checkbox — Figma left 78 / top 190 / 25
        GenderGuideCheckbox(
            selected = selectedSex == MATCH_SEX_MALE,
            onClick = { onSelectSex(MATCH_SEX_MALE) },
            contentDescription = maleCd,
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.matchGenderGuideMaleCheckboxStart.scaled(),
                    top = ComponentSize.matchGenderGuideCheckboxTop.scaled(),
                ),
        )

        // Female checkbox — Figma left 239 / top 190 / 25
        GenderGuideCheckbox(
            selected = selectedSex == MATCH_SEX_FEMALE,
            onClick = { onSelectSex(MATCH_SEX_FEMALE) },
            contentDescription = femaleCd,
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.matchGenderGuideFemaleCheckboxStart.scaled(),
                    top = ComponentSize.matchGenderGuideCheckboxTop.scaled(),
                ),
        )

        Text(
            text = stringResource(R.string.match_gender_guide_message),
            color = DemoColors.textPrimary,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = ComponentSize.matchGenderGuideCopyHorizontal.scaled(),
                    end = ComponentSize.matchGenderGuideCopyHorizontal.scaled(),
                    top = ComponentSize.matchGenderGuideMessageTop.scaled(),
                )
                .fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = ComponentSize.matchGenderGuideCtaHorizontal.scaled(),
                    end = ComponentSize.matchGenderGuideCtaHorizontal.scaled(),
                    top = ComponentSize.matchGenderGuideCtaTop.scaled(),
                )
                .fillMaxWidth()
                .height(ComponentSize.matchGenderGuideCtaHeight.scaled())
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onConfirm,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.match_gender_guide_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                autoSize = DemoTextAutoSize.label(TextSize.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
            )
        }

        Image(
            painter = painterResource(R.drawable.match_gender_guide_close),
            contentDescription = closeCd,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(ComponentSize.matchGenderGuideCloseInset.scaled())
                .size(ComponentSize.matchGenderGuideClose.scaled())
                .clip(RoundedCornerShape(Radius.md))
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onDismiss,
                )
                .semantics { contentDescription = closeCd },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun GenderGuideAvatar(
    avatarRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(avatarRes),
        contentDescription = contentDescription,
        modifier = modifier
            .clickable(
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun GenderGuideCheckbox(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val size = ComponentSize.matchGenderGuideCheckbox * scale
    val radius = ComponentSize.matchGenderGuideCheckboxRadius * scale
    val stroke = ComponentSize.matchGenderGuideCheckboxStroke * scale
    val checkIcon = ComponentSize.matchGenderGuideCheckIcon * scale
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .then(
                if (selected) {
                    Modifier.background(DemoColors.link)
                } else {
                    Modifier
                        .background(DemoColors.sheet)
                        .border(
                            width = stroke,
                            color = DemoColors.link,
                            shape = RoundedCornerShape(radius),
                        )
                },
            )
            .clickable(
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Image(
                painter = painterResource(R.drawable.match_gender_guide_check),
                contentDescription = null,
                modifier = Modifier.size(checkIcon),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Gender guide", showBackground = true, widthDp = 375, heightDp = 812)
@Preview(name = "Gender guide RTL", locale = "ar", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MatchGenderGuideDialogPreview() {
    DemoTheme {
        MatchGenderGuideDialog(
            selectedSex = MATCH_SEX_FEMALE,
            onSelectSex = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

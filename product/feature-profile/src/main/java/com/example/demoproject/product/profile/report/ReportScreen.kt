package com.example.demoproject.product.profile.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.platform.data.repository.ReportReason
import com.example.demoproject.product.profile.R
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

/**
 * Local reason glyphs used only when backend [ReportReason.iconUrl] is blank.
 * Each entry is unique; assignment never reuses the same drawable for two rows.
 */
private val ReportReasonIconPool = listOf(
    R.drawable.report_ic_reason_content,
    R.drawable.report_ic_reason_harassment,
    R.drawable.report_ic_reason_spam,
    R.drawable.report_ic_reason_fake,
    R.drawable.report_ic_reason_underage,
    R.drawable.report_ic_reason_other,
    R.drawable.report_ic_reason_fraud,
    R.drawable.report_ic_reason_political,
    R.drawable.report_ic_reason_avatar,
    R.drawable.report_ic_reason_violence,
    R.drawable.report_ic_reason_privacy,
    R.drawable.report_ic_reason_copyright,
    R.drawable.report_ic_reason_flag,
    R.drawable.report_ic_reason_link,
)

/** Preferred icon for a title, or null when no confident match. */
@DrawableRes
private fun preferredReportReasonIcon(title: String): Int? {
    val key = title.trim().lowercase()
    return when {
        key.containsAny(
            "underage", "minor", "child", "18+", "under age",
            "未成年", "未满",
        ) -> R.drawable.report_ic_reason_underage
        key.containsAny(
            "impersonat", "pretend", "冒充",
        ) || key.contains("fake profile") || key.contains("虚假") ||
            key.contains("假资料") || key.contains("假账号") ->
            R.drawable.report_ic_reason_fake
        key.containsAny(
            "avatar", "signature", "bio", "background",
            "头像", "背景", "签名",
        ) -> R.drawable.report_ic_reason_avatar
        key.containsAny(
            "politic", "敏感", "政治",
        ) -> R.drawable.report_ic_reason_political
        key.containsAny(
            "violen", "attack", "weapon", "暴力", "攻击",
        ) -> R.drawable.report_ic_reason_violence
        key.containsAny(
            "privacy", "personal info", "隐私", "个人信息",
        ) -> R.drawable.report_ic_reason_privacy
        key.containsAny(
            "copyright", "知识产权", "版权",
        ) -> R.drawable.report_ic_reason_copyright
        key.containsAny(
            "phish", "link", "钓鱼", "外链",
        ) -> R.drawable.report_ic_reason_link
        key.containsAny(
            "fraud", "scam", "cheat", "欺诈", "诈骗",
        ) -> R.drawable.report_ic_reason_fraud
        key.containsAny(
            "spam", "advert", "垃圾", "广告",
        ) -> R.drawable.report_ic_reason_spam
        key.containsAny(
            "harass", "abus", "bully", "insult", "threat",
            "骚扰", "辱骂", "欺凌", "威胁",
        ) -> R.drawable.report_ic_reason_harassment
        key.containsAny(
            "inappropriate", "photo", "nud", "porn",
            "不当", "色情", "照片", "图片",
        ) -> R.drawable.report_ic_reason_content
        key.containsAny(
            "other", "else", "其他", "其它",
        ) -> R.drawable.report_ic_reason_other
        else -> null
    }
}

/**
 * Maps each reason id → a distinct local fallback icon.
 * Preferred title matches win first; leftovers take the next unused pool glyph.
 */
private fun resolveReportReasonFallbackIcons(
    reasons: List<ReportReason>,
): Map<Int, Int> {
    val assigned = LinkedHashMap<Int, Int>(reasons.size)
    val usedIcons = mutableSetOf<Int>()

    for (reason in reasons) {
        val preferred = preferredReportReasonIcon(reason.title) ?: continue
        if (preferred in usedIcons) continue
        assigned[reason.id] = preferred
        usedIcons += preferred
    }

    val unused = ReportReasonIconPool.filterNot { it in usedIcons }
    var next = 0
    for (reason in reasons) {
        if (reason.id in assigned) continue
        val icon = unused.getOrElse(next) {
            // Exhausted pool: still avoid colliding with already-used glyphs when possible.
            ReportReasonIconPool.firstOrNull { it !in usedIcons }
                ?: ReportReasonIconPool[next % ReportReasonIconPool.size]
        }
        next++
        assigned[reason.id] = icon
        usedIcons += icon
    }
    return assigned
}

private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { contains(it) }

@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit = onBack,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ReportEffect.NavigateBack -> onBack()
                ReportEffect.SubmitSucceeded -> onSubmitted()
                is ReportEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    ReportScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onIntent: (ReportIntent) -> Unit,
) {
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(ReportViewModel.MAX_PHOTOS),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onIntent(ReportIntent.AddPhotos(uris.map { it.toString() }))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ReportTopBar(onBack = { onIntent(ReportIntent.Back) })
        when {
            state.isLoading && state.reasons.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = DemoColors.link)
                }
            }
            state.errorMessage != null && state.reasons.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.errorMessage,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.sm,
                    )
                    TextButton(onClick = { onIntent(ReportIntent.Retry) }) {
                        Text(text = stringResource(R.string.report_retry))
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.md)
                            .padding(top = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ReportContentCard(
                            state = state,
                            onSelectReason = { onIntent(ReportIntent.SelectReason(it)) },
                            onAddPhotos = {
                                val remaining = ReportViewModel.MAX_PHOTOS - state.photos.size
                                if (remaining <= 0) return@ReportContentCard
                                pickImages.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            onRemovePhoto = { onIntent(ReportIntent.RemovePhoto(it)) },
                            modifier = Modifier
                                .readableContentWidth()
                                .fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }
                    DemoGradientPillButton(
                        text = stringResource(R.string.report_submit),
                        onClick = { onIntent(ReportIntent.Submit) },
                        enabled = state.canSubmit,
                        isLoading = state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md)
                            .padding(bottom = Spacing.reportSubmitBottom)
                            .readableContentWidth()
                            .align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportTopBar(
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.reportTopBar)
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.md),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.report_ic_back),
            contentDescription = stringResource(R.string.report_cd_back),
            onClick = onBack,
            iconSize = IconSize.md,
            size = IconSize.md,
            mirrorInRtl = true,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = stringResource(R.string.report_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ReportContentCard(
    state: ReportUiState,
    onSelectReason: (Int) -> Unit,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.sheet)
            .padding(
                horizontal = Spacing.reportCardInset,
                vertical = Spacing.reportCardVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.reportSectionGap),
    ) {
        ReportUserHeader(
            nickname = state.displayName,
            avatarUrl = state.avatarUrl,
            isOnline = state.isOnline,
        )
        ReportReasonsSection(
            reasons = state.reasons,
            selectedReasonId = state.selectedReasonId,
            onSelectReason = onSelectReason,
        )
        ReportScreenshotsSection(
            photos = state.photos,
            onAddPhotos = onAddPhotos,
            onRemovePhoto = onRemovePhoto,
        )
    }
}

@Composable
private fun ReportUserHeader(
    nickname: String,
    avatarUrl: String?,
    isOnline: Boolean?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.reportAvatarGap),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ComponentSize.reportAvatar)
                .clip(CircleShape)
                .background(DemoColors.chip),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.reportLabelGap),
        ) {
            Text(
                text = nickname.ifBlank { stringResource(R.string.report_user_placeholder) },
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isOnline == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Image(
                        painter = painterResource(R.drawable.report_ic_online_dot),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.reportOnlineDot),
                    )
                    Text(
                        text = stringResource(R.string.report_status_online),
                        color = DemoColors.reportOnlineLabel,
                        fontSize = TextSize.xs,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportReasonsSection(
    reasons: List<ReportReason>,
    selectedReasonId: Int?,
    onSelectReason: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.reportLabelGap),
        ) {
            Text(
                text = stringResource(R.string.report_reason_title),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
            )
            Text(
                text = stringResource(R.string.report_reason_hint),
                color = DemoColors.textSecondary,
                fontSize = TextSize.xs,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.reportReasonGap),
        ) {
            val fallbackIcons = remember(reasons) { resolveReportReasonFallbackIcons(reasons) }
            reasons.forEach { reason ->
                ReportReasonRow(
                    reason = reason,
                    selected = reason.id == selectedReasonId,
                    fallbackIconRes = fallbackIcons[reason.id]
                        ?: R.drawable.report_ic_reason_flag,
                    onClick = { onSelectReason(reason.id) },
                )
            }
        }
    }
}

@Composable
private fun ReportReasonRow(
    reason: ReportReason,
    selected: Boolean,
    @DrawableRes fallbackIconRes: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val borderColor = if (selected) DemoColors.link else DemoColors.reportReasonBorder
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = ComponentSize.reportReasonStroke,
                color = borderColor,
                shape = shape,
            )
            .clickable(
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = Spacing.reportReasonPadH,
                vertical = Spacing.reportReasonPadV,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.reportReasonIconGap),
        ) {
            Box(
                modifier = Modifier
                    .size(ComponentSize.reportReasonIconWell)
                    .clip(CircleShape)
                    .background(DemoColors.reportReasonIconBg),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(reason.iconUrl?.takeIf { it.isNotBlank() } ?: fallbackIconRes)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    error = painterResource(fallbackIconRes),
                    placeholder = painterResource(fallbackIconRes),
                    modifier = Modifier.size(ComponentSize.reportReasonIcon),
                )
            }
            Text(
                text = reason.title,
                color = DemoColors.textPrimary,
                fontSize = TextSize.xs,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 240.dp),
            )
        }
        Image(
            painter = painterResource(
                if (selected) R.drawable.report_ic_check else R.drawable.report_ic_radio,
            ),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.reportRadio),
        )
    }
}

@Composable
private fun ReportScreenshotsSection(
    photos: List<ReportPhotoUi>,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.reportLabelGap),
        ) {
            Text(
                text = stringResource(R.string.report_screenshots_title),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
            )
            Text(
                text = stringResource(R.string.report_screenshots_hint),
                color = DemoColors.textSecondary,
                fontSize = TextSize.xs,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (photos.size < ReportViewModel.MAX_PHOTOS) {
                ReportAddShotSlot(onClick = onAddPhotos)
            }
            photos.forEach { photo ->
                ReportShotThumb(
                    uri = photo.localUri,
                    onRemove = { onRemovePhoto(photo.localUri) },
                )
            }
        }
    }
}

@Composable
private fun ReportAddShotSlot(
    onClick: () -> Unit,
) {
    val strokeColor = DemoColors.link
    val strokeWidth = ComponentSize.reportShotDashStroke
    val corner = Radius.sm
    Box(
        modifier = Modifier
            .width(ComponentSize.reportShotWidth)
            .height(ComponentSize.reportShotHeight)
            .clip(RoundedCornerShape(corner))
            .drawBehind {
                val stroke = Stroke(
                    width = strokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        0f,
                    ),
                )
                drawRoundRect(
                    color = strokeColor,
                    style = stroke,
                    cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
                )
            }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.report_ic_add),
            contentDescription = stringResource(R.string.report_cd_add_photo),
            modifier = Modifier.size(IconSize.md),
        )
    }
}

@Composable
private fun ReportShotThumb(
    uri: String,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(ComponentSize.reportShotWidth)
            .height(ComponentSize.reportShotHeight)
            .clip(RoundedCornerShape(Radius.sm)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.reportShotRemoveInset)
                .size(ComponentSize.reportShotRemove)
                .clip(CircleShape)
                .background(DemoColors.reportShotRemoveBg)
                .clickable(
                    role = Role.Button,
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.report_ic_remove),
                contentDescription = stringResource(R.string.report_cd_remove_photo),
                modifier = Modifier.size(ComponentSize.reportShotRemoveIcon),
            )
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun ReportScreenPreviewEmpty() {
    DemoTheme {
        ReportScreen(
            state = ReportUiState(
                isLoading = false,
                nickname = "Monica",
                age = 22,
                isOnline = true,
                reasons = previewReasons(),
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ReportScreenPreviewSelected() {
    DemoTheme {
        ReportScreen(
            state = ReportUiState(
                isLoading = false,
                nickname = "Monica",
                age = 22,
                isOnline = true,
                reasons = previewReasons(),
                selectedReasonId = 1,
                photos = listOf(
                    ReportPhotoUi("https://example.com/a.jpg"),
                    ReportPhotoUi("https://example.com/b.jpg"),
                ),
            ),
            onIntent = {},
        )
    }
}

private fun previewReasons(): List<ReportReason> = listOf(
    ReportReason(1, "Inappropriate photos or content"),
    ReportReason(2, "Harassment or abusive behavior"),
    ReportReason(3, "Spam or scams"),
    ReportReason(4, "Fake profile"),
    ReportReason(5, "Underage"),
    ReportReason(6, "Other"),
)

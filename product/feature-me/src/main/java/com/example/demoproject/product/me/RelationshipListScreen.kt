package com.example.demoproject.product.me

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipListScreen(
    state: RelationshipListUiState,
    onIntent: (RelationshipListIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet)
            .statusBarsPadding(),
    ) {
        RelationshipTopBar(
            state = state,
            onBack = onBack,
        )
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(RelationshipListIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.page),
        ) {
            when {
                state.isLoading -> RelationshipSkeletonList(
                    modifier = Modifier.fillMaxSize(),
                )
                state.users.isEmpty() -> RelationshipEmptyState(
                    hasError = state.errorMessage != null,
                    onRetry = { onIntent(RelationshipListIntent.Refresh) },
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .readableContentWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Spacing.md,
                        top = Spacing.md,
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
                                if (state.hasMore) {
                                    onIntent(RelationshipListIntent.LoadMore)
                                }
                            }
                        }
                        RelationshipUserRow(
                            user = user,
                            onOpenProfile = {
                                onIntent(RelationshipListIntent.OpenProfile(user))
                            },
                            onOpenAction = {
                                onIntent(RelationshipListIntent.OpenAction(user))
                            },
                        )
                    }
                    if (state.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ComponentSize.relationshipAction),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = DemoColors.link,
                                    modifier = Modifier.size(ComponentSize.relationshipBackIcon),
                                )
                            }
                        }
                    }
                    item(key = "navigation-inset") {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 6,
) {
    val pulse = rememberRelationshipSkeletonPulse()
    val loadingDescription = stringResource(R.string.me_cd_loading)
    LazyColumn(
        modifier = modifier
            .readableContentWidth()
            .semantics { contentDescription = loadingDescription },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Spacing.md,
            top = Spacing.md,
            end = Spacing.md,
            bottom = Spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(
            count = count,
            key = { "relationship-skeleton-$it" },
        ) {
            RelationshipRowSkeleton(pulse = pulse)
        }
    }
}

@Composable
private fun RelationshipRowSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ComponentSize.relationshipRowMinHeight)
            .clip(RoundedCornerShape(Radius.md))
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.sm + Spacing.xxs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.relationshipAvatar)
                .clip(CircleShape)
                .background(fill),
        )
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
                        width = ComponentSize.onlineSkeletonStatusWidth +
                            ComponentSize.relationshipGenderIcon,
                        height = ComponentSize.onlineSkeletonStatusHeight,
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
                .size(ComponentSize.relationshipAction)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

@Composable
private fun rememberRelationshipSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "relationshipSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "relationshipSkeletonPulse",
    )
    return pulse
}

@Composable
private fun RelationshipTopBar(
    state: RelationshipListUiState,
    onBack: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.relationshipTopBar)
            .background(DemoColors.sheet),
    ) {
        val count = state.expectedCount.takeIf { it > 0 } ?: state.users.size
        Text(
            text = when (state.type) {
                RelationshipListType.Following ->
                    stringResource(R.string.relationship_following_title, count)
                RelationshipListType.Followers ->
                    stringResource(R.string.relationship_followers_title, count)
            },
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .width(ComponentSize.relationshipTopBar)
                .fillMaxHeight()
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.relationship_ic_back),
                contentDescription = stringResource(R.string.relationship_back),
                modifier = Modifier
                    .size(ComponentSize.relationshipBackIcon)
                    .graphicsLayer {
                        scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                    },
            )
        }
    }
}

@Composable
private fun RelationshipUserRow(
    user: User,
    onOpenProfile: () -> Unit,
    onOpenAction: () -> Unit,
) {
    val presence = user.onlinePresence()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ComponentSize.relationshipRowMinHeight)
            .clip(RoundedCornerShape(Radius.md))
            .background(DemoColors.sheet)
            .clickable(onClick = onOpenProfile)
            .padding(horizontal = Spacing.sm + Spacing.xxs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatar,
            contentDescription = stringResource(R.string.relationship_avatar, user.nickname),
            modifier = Modifier
                .size(ComponentSize.relationshipAvatar)
                .clip(CircleShape)
                .background(DemoColors.skeleton),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(Spacing.chipGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append(user.nickname)
                        if (user.age > 0) append(",${user.age}")
                    },
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.sm,
                    lineHeight = TextSize.chatListNameLine,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                genderIcon(user.gender)?.let { icon ->
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.relationshipGenderIcon),
                    )
                }
            }
            Text(
                text = relationshipLocation(user),
                color = DemoColors.textAuxiliary,
                fontSize = TextSize.xs,
                lineHeight = TextSize.chatListPreviewLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Image(
                    painter = painterResource(statusDot(presence)),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.relationshipStatusDot),
                )
                Text(
                    text = stringResource(statusLabel(presence)),
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.xs,
                    lineHeight = TextSize.chatListPreviewLine,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Box(
            modifier = Modifier
                .size(ComponentSize.relationshipAction)
                .clickable(onClick = onOpenAction),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.relationship_action_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            val isMessage = presence.prefersMessageAction
            Image(
                painter = painterResource(
                    if (isMessage) R.drawable.relationship_ic_chat
                    else R.drawable.relationship_ic_video,
                ),
                contentDescription = stringResource(
                    if (isMessage) R.string.relationship_chat
                    else R.string.relationship_video_call,
                ),
                modifier = Modifier.size(ComponentSize.relationshipActionIcon),
            )
        }
    }
}

@Composable
private fun RelationshipEmptyState(
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(
                if (hasError) R.string.relationship_load_error
                else R.string.relationship_empty,
            ),
            color = DemoColors.textAuxiliary,
            fontSize = TextSize.sm,
        )
        if (hasError) {
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.relationship_retry),
                    color = DemoColors.link,
                )
            }
        }
    }
}

private fun relationshipLocation(user: User): String = listOfNotNull(
    countryCodeToFlagEmoji(user.countryCode).takeIf(String::isNotBlank),
    user.countryName?.takeIf(String::isNotBlank),
).joinToString(" ")

@DrawableRes
private fun genderIcon(gender: Gender): Int? = when (gender) {
    Gender.Male -> R.drawable.relationship_ic_male
    Gender.Female -> R.drawable.relationship_ic_female
    Gender.Other -> null
}

@DrawableRes
private fun statusDot(presence: OnlinePresence): Int = when (presence) {
    OnlinePresence.Online -> R.drawable.relationship_status_online
    OnlinePresence.Offline -> R.drawable.relationship_status_offline
    OnlinePresence.Chatting -> R.drawable.relationship_status_chatting
}

private fun statusLabel(presence: OnlinePresence): Int = when (presence) {
    OnlinePresence.Online -> R.string.relationship_online
    OnlinePresence.Offline -> R.string.relationship_offline
    OnlinePresence.Chatting -> R.string.relationship_chatting
}

private val previewUsers = listOf(
    User(
        id = "1",
        nickname = "Brayan",
        avatar = null,
        gender = Gender.Male,
        age = 19,
        bio = "",
        isOnline = true,
        lastActiveAt = 0,
        countryCode = "SG",
        countryName = "Singapore",
        onlineStatusCode = 1,
        isVideoCallAvailable = true,
    ),
    User(
        id = "2",
        nickname = "Alexandra with a long display name",
        avatar = null,
        gender = Gender.Female,
        age = 24,
        bio = "",
        isOnline = false,
        lastActiveAt = 0,
        countryCode = "SG",
        countryName = "Singapore",
    ),
    User(
        id = "3",
        nickname = "Mia",
        avatar = null,
        gender = Gender.Female,
        age = 21,
        bio = "",
        isOnline = true,
        lastActiveAt = 0,
        countryCode = "SG",
        countryName = "Singapore",
        onlineStatusCode = 2,
    ),
)

@PreviewScreenSizes
@Composable
private fun RelationshipListPreview() {
    DemoTheme {
        RelationshipListScreen(
            state = RelationshipListUiState(
                type = RelationshipListType.Followers,
                expectedCount = previewUsers.size,
                users = previewUsers,
                isLoading = false,
                hasMore = false,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun RelationshipListAccessibilityPreview() {
    RelationshipListPreview()
}

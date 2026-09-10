package com.example.demoproject.product.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradientPillButton
import com.example.demoproject.ui.designsystem.DemoOutlinedPillButton
import com.example.demoproject.ui.designsystem.DemoPrimaryPillButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val AuthStepTransitionMs = 280
private val AuthStepSlideSpec = tween<IntOffset>(durationMillis = AuthStepTransitionMs)
private val AuthStepFadeSpec = tween<Float>(durationMillis = AuthStepTransitionMs)

private val AuthSheetTopPadding = 51.dp
private val AuthOrTopGap = 27.dp
private val AuthQuickTopGap = 26.dp
private val AuthEmailFormHorizontal = 20.dp
private val AuthEmailButtonHorizontal = 16.dp
private val AuthLegalHorizontal = 25.dp
private val AuthWelcomeTop = 75.dp
private val AuthSubtitleTop = 12.dp
private val AuthFieldsTop = 50.dp
private val AuthFieldGap = 32.dp
private val AuthLabelFieldGap = 6.dp
private val AuthFieldErrorTop = 6.dp
private val AuthSubmitTop = 34.dp
/** Gap from legal block to home-indicator / nav bar (Figma 778 - 766). */
private val AuthLegalBottom = 12.dp
/** Gap from focused field / CTA to IME when keyboard is visible. */
internal val AuthImeGap = 10.dp
private val AuthCreateAccountDialogWidth = 315.dp
private val AuthCreateAccountDialogMinHeight = 178.dp
private val AuthCreateAccountContentWidth = 275.dp
private val AuthCreateAccountTextWidth = 237.dp
private val AuthCreateAccountActionWidth = 128.dp
private val AuthCreateAccountHorizontal = 20.dp
private val AuthCreateAccountActionsGap = 19.dp
private val AuthCreateAccountSectionGap = 16.dp
private val AuthCreateAccountTitleGap = 8.dp
private val AuthDividerThickness = 1.dp
private val AuthLegalMaxWidth = 325.dp
private val AuthInputStartPadding = 15.dp
private val AuthInputEndPadding = 12.dp
private val AuthWelcomeLineHeight = 33.sp
private val AuthInputLineHeight = 17.sp
private val AuthCreateAccountBodyLineHeight = 18.sp
internal const val AuthImeBringIntoViewDelayMs = 64L

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit = {},
    termsUrl: String = "",
    privacyUrl: String = "",
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, termsUrl, privacyUrl) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AuthEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                AuthEffect.OpenTerms -> openUrl(context, termsUrl)
                AuthEffect.OpenPrivacy -> openUrl(context, privacyUrl)
            }
        }
    }

    AuthScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
fun AuthScreenContent(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        when {
            state.showCreateAccountDialog -> onIntent(AuthIntent.DismissCreateAccountDialog)
            state.isLoading -> Unit
            state.step == AuthStep.ForgetPassword -> onIntent(AuthIntent.ShowEmailForm)
            state.step == AuthStep.EmailForm -> onIntent(AuthIntent.ShowLanding)
            state.step == AuthStep.Landing -> onBack()
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    AnimatedContent(
        targetState = state.step,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val rtl = layoutDirection == LayoutDirection.Rtl
            val enterFromEnd = if (rtl) -1 else 1
            if (forward) {
                (
                    slideInHorizontally(AuthStepSlideSpec) { enterFromEnd * it } +
                        fadeIn(AuthStepFadeSpec)
                    ) togetherWith (
                    slideOutHorizontally(AuthStepSlideSpec) { -enterFromEnd * it / 4 } +
                        fadeOut(AuthStepFadeSpec)
                    )
            } else {
                (
                    slideInHorizontally(AuthStepSlideSpec) { -enterFromEnd * it / 4 } +
                        fadeIn(AuthStepFadeSpec)
                    ) togetherWith (
                    slideOutHorizontally(AuthStepSlideSpec) { enterFromEnd * it } +
                        fadeOut(AuthStepFadeSpec)
                    )
            }
        },
        label = "auth_step",
    ) { step ->
        when (step) {
            AuthStep.Landing -> AuthLandingPane(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize(),
            )
            AuthStep.EmailForm -> AuthEmailFormPane(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize(),
            )
            AuthStep.ForgetPassword -> AuthForgetPasswordPane(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AuthLandingPane(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.auth_hero_landing),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            AuthLandingSheet(
                state = state,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun AuthLandingSheet(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
) {
    val sheetShape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // White sheet extends under the system nav bar (Figma: home indicator on #FFFFFF).
            // Shape fill without clipping children so pill button shadows stay visible.
            .background(color = DemoColors.sheet, shape = sheetShape)
            .navigationBarsPadding()
            .padding(top = AuthSheetTopPadding)
            .padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.readableContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DemoGradientPillButton(
                text = stringResource(R.string.auth_action_continue_email),
                onClick = { onIntent(AuthIntent.ShowEmailForm) },
                enabled = !state.isLoading,
                icon = painterResource(R.drawable.auth_ic_email),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            DemoOutlinedPillButton(
                text = stringResource(R.string.auth_action_continue_google),
                onClick = { onIntent(AuthIntent.GoogleLogin) },
                enabled = !state.isLoading,
                icon = painterResource(R.drawable.auth_ic_google),
            )
            Spacer(modifier = Modifier.height(AuthOrTopGap))
            AuthOrDivider()
            Spacer(modifier = Modifier.height(AuthQuickTopGap))
            DemoOutlinedPillButton(
                text = stringResource(R.string.auth_action_quick_login),
                onClick = { onIntent(AuthIntent.GuestLogin) },
                enabled = !state.isLoading,
                isLoading = state.loadingAction == AuthLoadingAction.GuestLogin,
                icon = painterResource(R.drawable.auth_ic_quick_login),
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            AuthLegalText(
                onOpenTerms = { onIntent(AuthIntent.OpenTerms) },
                onOpenPrivacy = { onIntent(AuthIntent.OpenPrivacy) },
                modifier = Modifier.padding(bottom = AuthLegalBottom),
            )
        }
    }
}

@Composable
private fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(AuthDividerThickness)
                .background(DemoColors.divider),
        )
        Text(
            text = stringResource(R.string.auth_label_or),
            color = DemoColors.orLabel,
            fontSize = TextSize.md,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(AuthDividerThickness)
                .background(DemoColors.divider),
        )
    }
}

@Composable
private fun AuthEmailFormPane(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val ctaBringIntoView = remember { BringIntoViewRequester() }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    var legalReserve by remember { mutableStateOf(0.dp) }

    fun bringCtaAboveIme() {
        scope.launch {
            delay(AuthImeBringIntoViewDelayMs)
            ctaBringIntoView.bringIntoView()
        }
    }

    LaunchedEffect(isImeVisible, imeBottomPx) {
        if (isImeVisible) {
            delay(AuthImeBringIntoViewDelayMs)
            ctaBringIntoView.bringIntoView()
        }
    }

    LaunchedEffect(state.showCreateAccountDialog) {
        if (state.showCreateAccountDialog) {
            keyboard?.hide()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { keyboard?.hide() })
            },
    ) {
        Image(
            painter = painterResource(R.drawable.auth_bg_email),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(
                    if (isImeVisible) {
                        Modifier.imePadding()
                    } else {
                        Modifier.navigationBarsPadding()
                    },
                )
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (isImeVisible) 0.dp else legalReserve),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .readableContentWidth()
                    .fillMaxWidth()
                    .padding(horizontal = AuthEmailFormHorizontal)
                    .padding(top = AuthWelcomeTop),
            ) {
                Text(
                    text = stringResource(R.string.auth_welcome),
                    color = DemoColors.textTitle,
                    fontSize = TextSize.xl,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = AuthWelcomeLineHeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AuthSubtitleTop))
                Text(
                    text = stringResource(R.string.auth_welcome_subtitle),
                    color = DemoColors.textTertiary,
                    fontSize = TextSize.md,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AuthFieldsTop))
                Text(
                    text = stringResource(R.string.auth_label_email),
                    color = DemoColors.textTertiary,
                    fontSize = TextSize.md,
                )
                Spacer(modifier = Modifier.height(AuthLabelFieldGap))
                AuthInputField(
                    value = state.email,
                    onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                    placeholder = stringResource(R.string.auth_hint_email),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    enabled = !state.isLoading,
                    isError = state.emailErrorRes != null,
                    onFocused = ::bringCtaAboveIme,
                )
                AuthFieldErrorSlot(
                    errorRes = state.emailErrorRes,
                    slotHeight = AuthFieldGap,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_label_password),
                        color = DemoColors.textTertiary,
                        fontSize = TextSize.md,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.auth_action_forgot_password),
                        color = DemoColors.link.copy(
                            alpha = if (state.isLoading) 0.5f else 1f,
                        ),
                        fontSize = TextSize.xs,
                        modifier = Modifier.clickable(
                            enabled = !state.isLoading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onIntent(AuthIntent.ForgotPassword) },
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(AuthLabelFieldGap))
                AuthInputField(
                    value = state.password,
                    onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                    placeholder = stringResource(R.string.auth_hint_password),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        AuthPasswordVisibilityToggle(
                            visible = state.isPasswordVisible,
                            onClick = { onIntent(AuthIntent.TogglePasswordVisibility) },
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            onIntent(AuthIntent.EmailLogin)
                        },
                    ),
                    enabled = !state.isLoading,
                    isError = state.passwordErrorRes != null,
                    onFocused = ::bringCtaAboveIme,
                )
                AuthFieldErrorSlot(
                    errorRes = state.passwordErrorRes,
                    slotHeight = AuthSubmitTop,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(ctaBringIntoView),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DemoPrimaryPillButton(
                    text = stringResource(R.string.auth_action_login_signup),
                    onClick = {
                        keyboard?.hide()
                        onIntent(AuthIntent.EmailLogin)
                    },
                    enabled = !state.isLoading,
                    isLoading = state.loadingAction == AuthLoadingAction.EmailSubmit,
                    modifier = Modifier
                        .padding(horizontal = AuthEmailButtonHorizontal)
                        .readableContentWidth(),
                )
                // Keeps the CTA 10dp above the IME once brought into view.
                Spacer(modifier = Modifier.height(AuthImeGap))
            }
            if (state.status.isNotBlank()) {
                Text(
                    text = state.status,
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.xs,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = AuthEmailFormHorizontal)
                        .fillMaxWidth(),
                )
            }
        }

        AuthLegalText(
            onOpenTerms = { onIntent(AuthIntent.OpenTerms) },
            onOpenPrivacy = { onIntent(AuthIntent.OpenPrivacy) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = AuthLegalHorizontal)
                .padding(bottom = AuthLegalBottom)
                .onGloballyPositioned { coordinates ->
                    legalReserve = with(density) {
                        coordinates.size.height.toDp()
                    } + AuthLegalBottom
                },
        )

        if (state.showCreateAccountDialog) {
            CreateAccountDialog(
                onCancel = { onIntent(AuthIntent.DismissCreateAccountDialog) },
                onSignUp = { onIntent(AuthIntent.ConfirmSignUp) },
                isSigningUp = state.loadingAction == AuthLoadingAction.ConfirmSignUp,
            )
        }
    }
}

@Composable
private fun AuthFieldErrorSlot(
    @androidx.annotation.StringRes errorRes: Int?,
    slotHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(slotHeight),
    ) {
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                color = DemoColors.error,
                fontSize = TextSize.xs,
                lineHeight = TextSize.sm,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AuthFieldErrorTop),
            )
        }
    }
}

@Composable
internal fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    cornerRadius: Dp = Radius.md,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocused: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isError) DemoColors.error else DemoColors.inputBorder
    val fieldTextStyle = TextStyle(
        color = DemoColors.textPrimary,
        fontSize = TextSize.sm,
        lineHeight = AuthInputLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.authInput)
            .clip(shape)
            .background(DemoColors.sheet)
            .border(AuthDividerThickness, borderColor, shape)
            .padding(
                start = AuthInputStartPadding,
                end = if (trailingIcon != null) AuthInputEndPadding else AuthInputStartPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = DemoColors.inputPlaceholder,
                    fontSize = TextSize.sm,
                    lineHeight = AuthInputLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = fieldTextStyle,
                cursorBrush = SolidColor(DemoColors.link),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocused?.invoke()
                        }
                    },
            )
        }
        if (trailingIcon != null) {
            trailingIcon()
        }
    }
}

@Composable
private fun AuthLegalText(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefix = stringResource(R.string.auth_legal_prefix)
    val terms = stringResource(R.string.auth_legal_terms)
    val middle = stringResource(R.string.auth_legal_and)
    val privacy = stringResource(R.string.auth_legal_privacy)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = DemoColors.link,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = DemoColors.textSecondary)) {
            append(prefix)
        }
        withLink(
            LinkAnnotation.Clickable(
                tag = "terms",
                styles = linkStyle,
                linkInteractionListener = { onOpenTerms() },
            ),
        ) {
            append(terms)
        }
        withStyle(SpanStyle(color = DemoColors.textSecondary)) {
            append(middle)
        }
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy",
                styles = linkStyle,
                linkInteractionListener = { onOpenPrivacy() },
            ),
        ) {
            append(privacy)
        }
    }

    Text(
        text = annotated,
        fontSize = TextSize.xs,
        lineHeight = TextSize.md,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(max = AuthLegalMaxWidth),
    )
}

@Composable
private fun CreateAccountDialog(
    onCancel: () -> Unit,
    onSignUp: () -> Unit,
    isSigningUp: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.scrim)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isSigningUp) {
                            onCancel()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(AuthCreateAccountDialogWidth)
                .heightIn(min = AuthCreateAccountDialogMinHeight)
                .clip(RoundedCornerShape(Radius.dialog))
                .background(DemoColors.sheet)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = AuthCreateAccountHorizontal, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.width(AuthCreateAccountContentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AuthCreateAccountSectionGap),
            ) {
                Column(
                    modifier = Modifier.width(AuthCreateAccountTextWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AuthCreateAccountTitleGap),
                ) {
                    Text(
                        text = stringResource(R.string.auth_dialog_create_account_title),
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.md,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.auth_dialog_create_account_body),
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.sm,
                        lineHeight = AuthCreateAccountBodyLineHeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AuthCreateAccountActionsGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CreateAccountActionButton(
                        text = stringResource(R.string.auth_dialog_cancel),
                        onClick = onCancel,
                        containerColor = DemoColors.dialogSecondary,
                        contentColor = DemoColors.textPrimary,
                        enabled = !isSigningUp,
                    )
                    CreateAccountActionButton(
                        text = stringResource(R.string.auth_dialog_sign_up),
                        onClick = onSignUp,
                        containerColor = DemoColors.link,
                        contentColor = DemoColors.onPrimaryButton,
                        enabled = !isSigningUp,
                        isLoading = isSigningUp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAccountActionButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Box(
        modifier = modifier
            .width(AuthCreateAccountActionWidth)
            .height(ComponentSize.dialogAction)
            .clip(RoundedCornerShape(Radius.pill))
            .background(containerColor)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(
            context,
            context.getString(R.string.auth_status_link_unavailable),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.auth_status_link_unavailable),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Landing", locale = "en", showBackground = true)
@Preview(name = "Landing RTL", locale = "ar", showBackground = true)
@Composable
private fun AuthLandingPreview() {
    DemoTheme {
        AuthScreenContent(
            state = AuthUiState(step = AuthStep.Landing),
            onIntent = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Email empty", locale = "en", showBackground = true)
@Preview(name = "Email filled", locale = "en", showBackground = true)
@Preview(name = "Email RTL", locale = "ar", showBackground = true)
@Composable
private fun AuthEmailPreview() {
    DemoTheme {
        AuthScreenContent(
            state = AuthUiState(step = AuthStep.EmailForm),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Email validation errors", locale = "en", showBackground = true)
@Composable
private fun AuthEmailValidationPreview() {
    DemoTheme {
        AuthScreenContent(
            state = AuthUiState(
                step = AuthStep.EmailForm,
                email = "a".repeat(65) + "@x.com",
                password = "123",
                emailErrorRes = R.string.auth_error_email_local_too_long,
                passwordErrorRes = R.string.auth_error_password_length,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Create account dialog", locale = "en", showBackground = true)
@Composable
private fun AuthCreateAccountDialogPreview() {
    DemoTheme {
        AuthScreenContent(
            state = AuthUiState(
                step = AuthStep.EmailForm,
                email = "sdchu@123.com",
                password = "123456",
                showCreateAccountDialog = true,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

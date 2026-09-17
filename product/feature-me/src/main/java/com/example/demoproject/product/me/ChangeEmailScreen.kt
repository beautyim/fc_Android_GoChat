package com.example.demoproject.product.me

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoPrimaryPillButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val EmailImeBringIntoViewDelayMs = 64L
private val EmailImeGap = Spacing.sm
private const val EmailHeroAlpha = 0.7f

@Composable
fun ChangeEmailScreen(
    viewModel: ChangeEmailViewModel,
    onBack: () -> Unit,
    onSucceeded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ChangeEmailEffect.NavigateBack -> onBack()
                ChangeEmailEffect.ChangeSucceeded -> onSucceeded()
                is ChangeEmailEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    ChangeEmailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun ChangeEmailScreen(
    state: ChangeEmailUiState,
    onIntent: (ChangeEmailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val codeBringIntoView = remember { BringIntoViewRequester() }
    val newEmailBringIntoView = remember { BringIntoViewRequester() }
    var focusedBringIntoView by remember { mutableStateOf<BringIntoViewRequester?>(null) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    val isSendingCode = state.loadingAction == EmailLoadingAction.SendCode
    val isSubmitting = state.loadingAction == EmailLoadingAction.Submit
    val canSubmit = EmailCredentialsRules.canSubmitChange(
        newEmail = state.newEmail,
        code = state.verificationCode,
    ) && !state.isLoading
    val sendEnabled = state.sendCodeCooldownSec == 0 && !state.isLoading

    fun bringFieldAboveIme(requester: BringIntoViewRequester) {
        focusedBringIntoView = requester
        scope.launch {
            delay(EmailImeBringIntoViewDelayMs)
            requester.bringIntoView()
        }
    }

    LaunchedEffect(isImeVisible, imeBottomPx, focusedBringIntoView) {
        val requester = focusedBringIntoView
        if (isImeVisible && requester != null) {
            delay(EmailImeBringIntoViewDelayMs)
            requester.bringIntoView()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.page)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { keyboard?.hide() })
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.sheet)
                .statusBarsPadding(),
        ) {
            EmailTopBar(
                onBack = { onIntent(ChangeEmailIntent.Back) },
                enabled = !state.isLoading,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isImeVisible) {
                        Modifier
                            .imePadding()
                            .padding(bottom = EmailImeGap)
                    } else {
                        Modifier.navigationBarsPadding()
                    },
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.emailHeroTop)
                .padding(bottom = Spacing.emailCtaBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .readableContentWidth()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.email_ill_change),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(
                            width = ComponentSize.emailChangeHeroWidth,
                            height = ComponentSize.emailChangeHeroHeight,
                        )
                        .graphicsLayer { alpha = EmailHeroAlpha },
                )
                Spacer(modifier = Modifier.height(Spacing.emailChangeHeroToTitle))
                EmailSectionTitle(text = stringResource(R.string.email_change_title))
                Spacer(modifier = Modifier.height(Spacing.emailTitleToSubtitle))
                EmailSectionSubtitle(text = stringResource(R.string.email_change_subtitle))
                Spacer(modifier = Modifier.height(Spacing.emailSubtitleToForm))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.emailFieldGroupGap),
                ) {
                    EmailLabeledField(
                        label = stringResource(R.string.email_label_current),
                        errorRes = null,
                    ) {
                        EmailReadonlyField(
                            value = state.currentEmail.ifBlank {
                                stringResource(R.string.email_hint_current_loading)
                            },
                        )
                    }
                    EmailLabeledField(
                        label = stringResource(R.string.email_label_code),
                        errorRes = state.codeErrorRes,
                        modifier = Modifier.bringIntoViewRequester(codeBringIntoView),
                    ) {
                        EmailInputField(
                            value = state.verificationCode,
                            onValueChange = { onIntent(ChangeEmailIntent.CodeChanged(it)) },
                            placeholder = stringResource(R.string.email_hint_code),
                            leadingIconRes = R.drawable.email_ic_shield,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            enabled = !state.isLoading,
                            isError = state.codeErrorRes != null,
                            onFocused = { bringFieldAboveIme(codeBringIntoView) },
                            trailingContent = {
                                EmailSendCodeAction(
                                    cooldownSec = state.sendCodeCooldownSec,
                                    enabled = sendEnabled,
                                    isSending = isSendingCode,
                                    onClick = { onIntent(ChangeEmailIntent.SendCode) },
                                )
                            },
                        )
                    }
                    EmailLabeledField(
                        label = stringResource(R.string.email_label_new),
                        errorRes = state.newEmailErrorRes,
                        modifier = Modifier.bringIntoViewRequester(newEmailBringIntoView),
                    ) {
                        EmailInputField(
                            value = state.newEmail,
                            onValueChange = { onIntent(ChangeEmailIntent.NewEmailChanged(it)) },
                            placeholder = stringResource(R.string.email_hint_new),
                            leadingIconRes = R.drawable.email_ic_envelope,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    onIntent(ChangeEmailIntent.Submit)
                                },
                            ),
                            enabled = !state.isLoading,
                            isError = state.newEmailErrorRes != null,
                            onFocused = { bringFieldAboveIme(newEmailBringIntoView) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.emailChangeFormToCta))
                DemoPrimaryPillButton(
                    text = stringResource(R.string.email_action_verify_save),
                    onClick = {
                        keyboard?.hide()
                        onIntent(ChangeEmailIntent.Submit)
                    },
                    enabled = canSubmit || isSubmitting,
                    isLoading = isSubmitting,
                )
            }
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Change email", locale = "en", showBackground = true)
@Composable
private fun ChangeEmailPreview() {
    DemoTheme {
        ChangeEmailScreen(
            state = ChangeEmailUiState(
                currentEmail = "dhciowsju@dod.com",
                isLoadingCurrent = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Change email RTL", locale = "ar", showBackground = true)
@Composable
private fun ChangeEmailRtlPreview() {
    DemoTheme {
        ChangeEmailScreen(
            state = ChangeEmailUiState(
                currentEmail = "user@example.com",
                newEmail = "new@example.com",
                verificationCode = "1234",
                isLoadingCurrent = false,
                sendCodeCooldownSec = 40,
            ),
            onIntent = {},
        )
    }
}

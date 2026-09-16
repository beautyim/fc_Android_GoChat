package com.example.demoproject.product.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoPrimaryPillButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ForgetPasswordHorizontal = 20.dp
private val ForgetPasswordTopBarHeight = 53.dp
private val ForgetPasswordContentTop = 32.dp
private val ForgetPasswordFieldGap = 20.dp
private val ForgetPasswordLabelGap = 6.dp
private val ForgetPasswordConfirmTop = 48.dp
private val ForgetPasswordConfirmHorizontal = 16.dp
private val SendCodeProgressStroke = 2.dp

@Composable
internal fun AuthForgetPasswordPane(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val emailBringIntoView = remember { BringIntoViewRequester() }
    val codeBringIntoView = remember { BringIntoViewRequester() }
    val newPasswordBringIntoView = remember { BringIntoViewRequester() }
    val confirmPasswordBringIntoView = remember { BringIntoViewRequester() }
    var focusedBringIntoView by remember { mutableStateOf<BringIntoViewRequester?>(null) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    val isSendingCode = state.loadingAction == AuthLoadingAction.SendResetCode
    val isConfirming = state.loadingAction == AuthLoadingAction.ConfirmResetPassword
    val canConfirm = AuthCredentialsRules.canSubmitReset(
        email = state.email,
        code = state.verificationCode,
        password = state.newPassword,
        confirmPassword = state.confirmPassword,
    ) && !state.isLoading
    val sendEnabled = state.sendCodeCooldownSec == 0 && !state.isLoading
    val passwordVisible = state.isResetPasswordVisible
    val passwordTransformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    fun bringFieldAboveIme(requester: BringIntoViewRequester) {
        focusedBringIntoView = requester
        scope.launch {
            delay(AuthImeBringIntoViewDelayMs)
            requester.bringIntoView()
        }
    }

    LaunchedEffect(isImeVisible, imeBottomPx, focusedBringIntoView) {
        val requester = focusedBringIntoView
        if (isImeVisible && requester != null) {
            delay(AuthImeBringIntoViewDelayMs)
            requester.bringIntoView()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.page)
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { keyboard?.hide() })
                },
        ) {
            ForgetPasswordTopBar(
                onBack = { onIntent(AuthIntent.ShowEmailForm) },
                enabled = !state.isLoading,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isImeVisible) {
                            Modifier
                                .imePadding()
                                .padding(bottom = AuthImeGap)
                        } else {
                            Modifier.navigationBarsPadding()
                        },
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ForgetPasswordHorizontal)
                    .padding(top = ForgetPasswordContentTop),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .readableContentWidth()
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ForgetPasswordFieldGap),
                ) {
                    ForgetPasswordLabeledField(
                        label = stringResource(R.string.auth_label_reset_email),
                        errorRes = state.emailErrorRes,
                        modifier = Modifier.bringIntoViewRequester(emailBringIntoView),
                    ) {
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
                            cornerRadius = Radius.sm,
                            onFocused = { bringFieldAboveIme(emailBringIntoView) },
                        )
                    }
                    ForgetPasswordLabeledField(
                        label = stringResource(R.string.auth_label_verification_code),
                        errorRes = state.codeErrorRes,
                        modifier = Modifier.bringIntoViewRequester(codeBringIntoView),
                    ) {
                        AuthInputField(
                            value = state.verificationCode,
                            onValueChange = { onIntent(AuthIntent.VerificationCodeChanged(it)) },
                            placeholder = stringResource(R.string.auth_hint_verification_code),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            enabled = !state.isLoading,
                            isError = state.codeErrorRes != null,
                            cornerRadius = Radius.sm,
                            onFocused = { bringFieldAboveIme(codeBringIntoView) },
                            trailingIcon = {
                                if (isSendingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(IconSize.sm),
                                        color = DemoColors.link,
                                        strokeWidth = SendCodeProgressStroke,
                                    )
                                } else {
                                    Text(
                                        text = if (state.sendCodeCooldownSec > 0) {
                                            stringResource(
                                                R.string.auth_action_send_code_countdown,
                                                state.sendCodeCooldownSec,
                                            )
                                        } else {
                                            stringResource(R.string.auth_action_send_code)
                                        },
                                        color = DemoColors.link.copy(
                                            alpha = if (sendEnabled) 1f else 0.5f,
                                        ),
                                        fontSize = TextSize.sm,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable(
                                            enabled = sendEnabled,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            role = Role.Button,
                                            onClick = { onIntent(AuthIntent.SendResetCode) },
                                        ),
                                    )
                                }
                            },
                        )
                    }
                    ForgetPasswordLabeledField(
                        label = stringResource(R.string.auth_label_new_password),
                        errorRes = state.newPasswordErrorRes,
                        modifier = Modifier.bringIntoViewRequester(newPasswordBringIntoView),
                    ) {
                        AuthInputField(
                            value = state.newPassword,
                            onValueChange = { onIntent(AuthIntent.NewPasswordChanged(it)) },
                            placeholder = stringResource(R.string.auth_hint_new_password),
                            visualTransformation = passwordTransformation,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            enabled = !state.isLoading,
                            isError = state.newPasswordErrorRes != null,
                            cornerRadius = Radius.sm,
                            onFocused = { bringFieldAboveIme(newPasswordBringIntoView) },
                            trailingIcon = {
                                AuthPasswordVisibilityToggle(
                                    visible = passwordVisible,
                                    onClick = { onIntent(AuthIntent.ToggleResetPasswordVisibility) },
                                )
                            },
                        )
                    }
                    ForgetPasswordLabeledField(
                        label = stringResource(R.string.auth_label_confirm_password),
                        errorRes = state.confirmPasswordErrorRes,
                        modifier = Modifier.bringIntoViewRequester(confirmPasswordBringIntoView),
                    ) {
                        AuthInputField(
                            value = state.confirmPassword,
                            onValueChange = { onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                            placeholder = stringResource(R.string.auth_hint_new_password),
                            visualTransformation = passwordTransformation,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    onIntent(AuthIntent.ConfirmResetPassword)
                                },
                            ),
                            enabled = !state.isLoading,
                            isError = state.confirmPasswordErrorRes != null,
                            cornerRadius = Radius.sm,
                            onFocused = { bringFieldAboveIme(confirmPasswordBringIntoView) },
                            trailingIcon = {
                                AuthPasswordVisibilityToggle(
                                    visible = passwordVisible,
                                    onClick = { onIntent(AuthIntent.ToggleResetPasswordVisibility) },
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ForgetPasswordConfirmTop))
                DemoPrimaryPillButton(
                    text = stringResource(R.string.auth_action_confirm),
                    onClick = {
                        keyboard?.hide()
                        onIntent(AuthIntent.ConfirmResetPassword)
                    },
                    enabled = canConfirm || isConfirming,
                    isLoading = isConfirming,
                    modifier = Modifier
                        .padding(horizontal = ForgetPasswordConfirmHorizontal)
                        .readableContentWidth(),
                )
                if (state.status.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = state.status,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.xs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ForgetPasswordConfirmHorizontal),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun ForgetPasswordTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ForgetPasswordTopBarHeight)
            .background(DemoColors.sheet),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.auth_ic_back),
            contentDescription = stringResource(R.string.auth_cd_back),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.md),
            enabled = enabled,
        )
        Text(
            text = stringResource(R.string.auth_title_forget_password),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        )
    }
}

@Composable
private fun ForgetPasswordLabeledField(
    label: String,
    @androidx.annotation.StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ForgetPasswordLabelGap),
    ) {
        Text(
            text = label,
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        content()
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                color = DemoColors.error,
                fontSize = TextSize.xs,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun AuthPasswordVisibilityToggle(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(
            if (visible) {
                R.drawable.auth_ic_password_hidden
            } else {
                R.drawable.auth_ic_password_visible
            },
        ),
        contentDescription = stringResource(
            if (visible) {
                R.string.auth_cd_hide_password
            } else {
                R.string.auth_cd_show_password
            },
        ),
        modifier = modifier
            .size(IconSize.sm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    )
}

@Preview(name = "Forget password empty", locale = "en", showBackground = true)
@Composable
private fun AuthForgetPasswordEmptyPreview() {
    DemoTheme {
        AuthForgetPasswordPane(
            state = AuthUiState(
                step = AuthStep.ForgetPassword,
                email = "sdchu@123.com",
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Forget password errors", locale = "en", showBackground = true)
@Composable
private fun AuthForgetPasswordErrorsPreview() {
    DemoTheme {
        AuthForgetPasswordPane(
            state = AuthUiState(
                step = AuthStep.ForgetPassword,
                email = "sdchu@123.com",
                verificationCode = "12345",
                newPassword = "12345",
                confirmPassword = "123456",
                sendCodeCooldownSec = 55,
                codeErrorRes = R.string.auth_error_verification_code,
                newPasswordErrorRes = R.string.auth_error_password_min_length,
                confirmPasswordErrorRes = R.string.auth_error_password_mismatch,
            ),
            onIntent = {},
        )
    }
}

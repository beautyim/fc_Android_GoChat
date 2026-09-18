package com.example.demoproject.product.me

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.min

@Composable
fun VerificationCaptureScreen(
    viewModel: VerificationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(VerificationIntent.CameraPermissionResult(granted))
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                VerificationEffect.NavigateBack -> onBack()
                VerificationEffect.OpenCapture -> Unit
                VerificationEffect.RequestCameraPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
                is VerificationEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.markCameraPermission(granted)
        if (!granted) {
            viewModel.onIntent(VerificationIntent.RequestCameraPermission)
        }
    }

    VerificationCaptureScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun VerificationCaptureScreen(
    state: VerificationUiState,
    onIntent: (VerificationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.profileVideoScrim),
    ) {
        when {
            state.cameraPermissionGranted -> {
                VerificationCameraPreview(modifier = Modifier.fillMaxSize())
                VerificationFaceOvalOverlay(modifier = Modifier.fillMaxSize())
            }
            state.cameraPermissionDenied -> {
                ColumnCenteredPermissionDenied(
                    onRetry = { onIntent(VerificationIntent.RequestCameraPermission) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.md),
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.verify_camera_requesting),
                        color = DemoColors.onPrimaryButton,
                        fontSize = TextSize.sm,
                    )
                }
            }
        }

        VerificationCaptureBackButton(
            onClick = { onIntent(VerificationIntent.Back) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = Spacing.md, top = Spacing.sm)
                .align(Alignment.TopStart),
        )
    }
}

@Composable
private fun VerificationCaptureBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .size(IconSize.md)
            .clip(CircleShape)
            .background(DemoColors.profileNavScrim)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.verify_ic_back),
            contentDescription = stringResource(R.string.verify_cd_back),
            modifier = Modifier
                .size(IconSize.md)
                .graphicsLayer { scaleX = if (mirror) -1f else 1f },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(DemoColors.onPrimaryButton),
        )
    }
}

@Composable
private fun ColumnCenteredPermissionDenied(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.verify_camera_permission_denied),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Medium,
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.verify_camera_permission_retry),
                    color = DemoColors.link,
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun VerificationCameraPreview(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var bindError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                )
            }.onFailure { error ->
                bindError = error.message
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        bindError?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.md),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.sm,
            )
        }
    }
}

@Composable
private fun VerificationFaceOvalOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val ovalWidth = size.width * ComponentSize.verifyOvalWidthFraction
        val ovalHeight = ovalWidth / ComponentSize.verifyOvalAspect
        val fittedHeight = min(ovalHeight, size.height * 0.78f)
        val fittedWidth = fittedHeight * ComponentSize.verifyOvalAspect
        val outerLeft = (size.width - fittedWidth) / 2f
        val outerTop = (size.height - fittedHeight) / 2f - Spacing.verifyOvalCenterLift.toPx()
        val outerRect = Rect(
            left = outerLeft,
            top = outerTop,
            right = outerLeft + fittedWidth,
            bottom = outerTop + fittedHeight,
        )
        val insetX = fittedWidth * ((330f - 307f) / 330f) / 2f
        val insetY = fittedHeight * ((610f - 567.455f) / 610f) / 2f
        val innerRect = Rect(
            left = outerRect.left + insetX,
            top = outerRect.top + insetY,
            right = outerRect.right - insetX,
            bottom = outerRect.bottom - insetY,
        )

        val hole = Path().apply { addOval(outerRect) }
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(DemoColors.verifyCaptureScrim)
        }
        drawOval(
            color = DemoColors.verifyOvalStroke,
            topLeft = outerRect.topLeft,
            size = outerRect.size,
            style = Stroke(width = ComponentSize.verifyOvalStroke.toPx()),
        )
        drawOval(
            color = DemoColors.onPrimaryButton,
            topLeft = innerRect.topLeft,
            size = innerRect.size,
            style = Stroke(
                width = ComponentSize.verifyOvalDashStroke.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 8.dp.toPx()),
                    0f,
                ),
            ),
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun VerificationCaptureScreenPreview() {
    DemoTheme {
        VerificationCaptureScreen(
            state = VerificationUiState(cameraPermissionDenied = true),
            onIntent = {},
        )
    }
}

package com.example.demoproject.payment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.R
import com.example.demoproject.platform.data.repository.BillingPaymentCheck
import com.example.demoproject.platform.data.repository.BillingPaymentMethod
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.ui.designsystem.DemoActionSheetContent
import com.example.demoproject.ui.designsystem.DemoActionSheetItem
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.foundation.Spacing
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PaymentMethodSheetState(
    val requestId: Long,
    val check: BillingPaymentCheck,
)

class PaymentMethodSheetController {
    private val nextRequestId = AtomicLong()
    private val _state = MutableStateFlow<PaymentMethodSheetState?>(null)
    val state = _state.asStateFlow()
    private var pending: CompletableDeferred<BillingPaymentType?>? = null

    suspend fun select(check: BillingPaymentCheck): BillingPaymentType? {
        pending?.complete(null)
        val deferred = CompletableDeferred<BillingPaymentType?>()
        pending = deferred
        _state.value = PaymentMethodSheetState(
            requestId = nextRequestId.incrementAndGet(),
            check = check,
        )
        return try {
            deferred.await()
        } finally {
            if (pending === deferred) {
                pending = null
                _state.value = null
            }
        }
    }

    fun choose(requestId: Long, paymentType: BillingPaymentType) {
        if (_state.value?.requestId != requestId) return
        pending?.complete(paymentType)
    }

    fun dismiss(requestId: Long) {
        if (_state.value?.requestId != requestId) return
        pending?.complete(null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSheetHost(
    controller: PaymentMethodSheetController,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val current = state ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { controller.dismiss(current.requestId) },
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        DemoActionSheetContent(
            actions = current.check.methods.map { method ->
                DemoActionSheetItem(
                    label = method.displayLabel(),
                    onClick = { controller.choose(current.requestId, method.type) },
                )
            },
            cancelLabel = stringResource(R.string.payment_method_cancel),
            onCancel = { controller.dismiss(current.requestId) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.chipGap)
                .padding(bottom = Spacing.lg),
        )
    }
}

@Composable
private fun BillingPaymentMethod.displayLabel(): String =
    title.takeIf { it.isNotBlank() } ?: stringResource(
        when (type) {
            BillingPaymentType.GooglePlay -> R.string.payment_method_google_play
            BillingPaymentType.ThirdParty -> R.string.payment_method_other
        },
    )

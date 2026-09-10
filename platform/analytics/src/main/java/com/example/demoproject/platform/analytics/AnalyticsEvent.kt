package com.example.demoproject.platform.analytics

sealed interface AnalyticsEvent {
    val name: String
    val isSandboxData: Boolean get() = false

    data class Active(
        override val isSandboxData: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "active"
    }

    data class FirstDialog(
        val userId: String,
        override val isSandboxData: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "first_dialog"
    }

    data class OrderSubmit(
        val goodsId: Long,
        val productId: String,
        val orderNo: String,
        override val isSandboxData: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "order_submit"
    }

    data class Pay(
        val goodsId: Long,
        val productId: String,
        val orderNo: String,
        val revenue: Double?,
        val currency: String?,
        override val isSandboxData: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "pay"
    }

    data class Register(
        val userId: String,
        override val isSandboxData: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "register"
    }
}

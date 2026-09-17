package com.example.demoproject.platform.data.promotion

/** Constants for welfare / treasure / winning promotion scheduling. */
object PromotionScheduleConstants {
    const val GLOBAL_COOLDOWN_MILLIS = 2 * 60_000L
    const val NETWORK_RETRY_MILLIS = 30_000L
    const val APP_OPEN_WAIT_MILLIS = 3_000L
    const val MQTT_REPLAY_MAX_AGE_MILLIS = 10_000L

    const val WELFARE_REPOOL_MILLIS = 30 * 60_000L
    const val UNPAID_TREASURE_REPOOL_MILLIS = 5 * 60_000L
    const val PAID_TREASURE_REPOOL_MILLIS = 3 * 60 * 60_000L
    const val VIP_EXPIRED_TREASURE_INTERVAL_MILLIS = 3 * 60 * 60_000L

    const val WINNING_COLD_START_DELAY_MILLIS = 30_000L
    const val WINNING_PURCHASE_PAGE_DELAY_MILLIS = 2_000L
    const val WINNING_RECHARGE_GUIDE_DELAY_MILLIS = 5_000L
    const val WINNING_BALANCE_LIMIT = 40

    const val UNPAID_ENTRY_COUNTDOWN_SECONDS = 3_600L

    const val DEFAULT_TREASURE_FROM_TYPE = 47
    const val WINNING_CREATE_FROM_TYPE = 0
    const val DEFAULT_WINNING_SID = 10

    const val SID_TREASURE_UNPAID_COLD = 1
    const val SID_TREASURE_PAID_AFTER_PURCHASE = 6
    const val SID_TREASURE_PAID_DAILY = 7
    const val SID_TREASURE_VIP_EXPIRED = 8

    const val SID_WINNING_COLD = 10
    const val SID_WINNING_PURCHASE_PAGE = 11
    const val SID_WINNING_RECHARGE_GUIDE = 12

    const val TYPE_TREASURE_UNPAID = 2
    const val TYPE_TREASURE_PAID = 3
}

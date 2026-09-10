package com.example.demoproject.platform.data.model

data class CallRecordPage(
    val records: List<CallRecord>,
    val hasMore: Boolean,
)

data class CallPriceCatalog(
    val pricesByUserId: Map<String, Int> = emptyMap(),
    val fallbackPrice: Int? = null,
) {
    fun priceForUser(userId: String): Int? =
        pricesByUserId[userId] ?: fallbackPrice

    companion object {
        val Empty = CallPriceCatalog()

        fun fromRecords(records: List<CallRecord>): CallPriceCatalog {
            val positivePrices = records.filter { it.callPrice > 0 }
            val pricesByUserId = buildMap {
                positivePrices.forEach { record ->
                    putIfAbsent(record.peer.id, record.callPrice)
                }
            }
            return CallPriceCatalog(
                pricesByUserId = pricesByUserId,
                fallbackPrice = positivePrices.firstOrNull()?.callPrice,
            )
        }
    }
}

data class CallRecord(
    val id: Long,
    val peer: User,
    val roomId: Long,
    val callType: CallRecordType,
    val status: CallRecordStatus,
    val direction: CallRecordDirection,
    val description: String,
    val startedAtSeconds: Long,
    val callPrice: Int,
    val isMatch: Boolean,
)

enum class CallRecordType { Video, Voice }

enum class CallRecordDirection { Incoming, Outgoing }

enum class CallRecordStatus { Connected, Missed, Declined, Cancelled, Match, Unknown }

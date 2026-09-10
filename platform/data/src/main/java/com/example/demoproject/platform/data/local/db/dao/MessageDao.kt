package com.example.demoproject.platform.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.demoproject.platform.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM messages
        WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId
        ORDER BY createdAt ASC
        """,
    )
    fun observeByConversation(ownerUserId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId
        ORDER BY createdAt DESC
        """,
    )
    fun observeByConversationDesc(ownerUserId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getByConversation(ownerUserId: String, conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun getById(ownerUserId: String, id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE ownerUserId = :ownerUserId AND id IN (:ids)")
    suspend fun getByIds(ownerUserId: String, ids: List<String>): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun updateStatus(ownerUserId: String, id: String, status: String)

    /**
     * Bulk status rewrite used to recover outbound rows left as `Sending` after the
     * process died mid-upload / mid-send. Those coroutines cannot resume, so the UI
     * must offer retry (Failed) instead of a permanent spinner.
     */
    @Query(
        """
        UPDATE messages
        SET status = :toStatus
        WHERE ownerUserId = :ownerUserId AND status = :fromStatus
        """,
    )
    suspend fun updateStatusWhere(
        ownerUserId: String,
        fromStatus: String,
        toStatus: String,
    ): Int

    @Query(
        """
        UPDATE messages
        SET status = :toStatus
        WHERE ownerUserId = :ownerUserId
          AND status = :fromStatus
          AND id NOT IN (:excludeIds)
        """,
    )
    suspend fun updateStatusWhereExcept(
        ownerUserId: String,
        fromStatus: String,
        toStatus: String,
        excludeIds: List<String>,
    ): Int

    @Query("DELETE FROM messages WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId")
    suspend fun deleteByConversation(ownerUserId: String, conversationId: String)

    @Query("DELETE FROM messages WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun deleteById(ownerUserId: String, id: String)

    @Query(
        """
        SELECT MIN(createdAt)
        FROM messages
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND isGapMarker = 1
        """,
    )
    suspend fun getGapMarkerTs(ownerUserId: String, conversationId: String): Long?

    @Query(
        """
        UPDATE messages
        SET isGapMarker = 0
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND isGapMarker = 1
        """,
    )
    suspend fun clearGapMarkers(ownerUserId: String, conversationId: String)

    @Query(
        """
        UPDATE messages
        SET isGapMarker = 1
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND createdAt = (
            SELECT MIN(createdAt)
            FROM messages
            WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId
          )
        """,
    )
    suspend fun setGapMarkerOnOldest(ownerUserId: String, conversationId: String)

    @Query(
        """
        SELECT MAX(createdAt)
        FROM messages
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND status IN ('Sent', 'Delivered', 'Read')
        """,
    )
    suspend fun getLatestSuccessTs(ownerUserId: String, conversationId: String): Long?

    /**
     * Latest `createdAt` across every message in the conversation (any status),
     * used to anchor a newly-created outbound Sending/Failed message after the
     * rest of the known timeline instead of trusting the raw device clock.
     */
    @Query(
        """
        SELECT MAX(createdAt)
        FROM messages
        WHERE ownerUserId = :ownerUserId AND conversationId = :conversationId
        """,
    )
    suspend fun getLatestCreatedAt(ownerUserId: String, conversationId: String): Long?

    @Query(
        """
        SELECT MAX(createdAt)
        FROM messages
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND type != 'System'
          AND createdAt >= :microsecondEpochThreshold
        """,
    )
    suspend fun getLatestNonSystemMicrosCreatedAt(
        ownerUserId: String,
        conversationId: String,
        microsecondEpochThreshold: Long,
    ): Long?

    @Query(
        """
        SELECT *
        FROM messages
        WHERE ownerUserId = :ownerUserId
          AND conversationId = :conversationId
          AND status IN ('Sending', 'Failed')
          AND (id LIKE 'local\_%' ESCAPE '\' OR id LIKE 'local-%')
        """,
    )
    suspend fun getLocalOutboundPending(
        ownerUserId: String,
        conversationId: String,
    ): List<MessageEntity>

    @Query(
        """
        UPDATE messages
        SET createdAt = :createdAt
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun updateCreatedAt(ownerUserId: String, id: String, createdAt: Long)

    @Query(
        """
        UPDATE messages
        SET translatedText = :text
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun updateTranslatedText(ownerUserId: String, id: String, text: String)

    @Query(
        """
        SELECT COUNT(*)
        FROM (
            SELECT conversationId
            FROM messages
            WHERE ownerUserId = :ownerUserId
              AND isGapMarker = 0
            GROUP BY conversationId
            HAVING
                SUM(
                    CASE
                        WHEN senderId = :ownerUserId
                         AND status IN ('Sent', 'Delivered', 'Read')
                        THEN 1
                        ELSE 0
                    END
                ) > 0
            AND SUM(
                    CASE
                        WHEN senderId != :ownerUserId
                        THEN 1
                        ELSE 0
                    END
                ) > 0
        )
        """,
    )
    suspend fun countMutualConnections(ownerUserId: String): Int
}

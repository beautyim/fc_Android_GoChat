package com.example.demoproject.platform.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.demoproject.platform.data.local.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query(
        """
        SELECT * FROM conversations
        WHERE ownerUserId = :ownerUserId
          AND peerId != 0
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeAll(ownerUserId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun getById(ownerUserId: String, id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ConversationEntity>)

    @Query("SELECT MAX(updatedAt) FROM conversations WHERE ownerUserId = :ownerUserId")
    suspend fun getMaxUpdatedAt(ownerUserId: String): Long?

    @Query("SELECT MIN(updatedAt) FROM conversations WHERE ownerUserId = :ownerUserId")
    suspend fun getMinUpdatedAt(ownerUserId: String): Long?

    @Query("DELETE FROM conversations WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun deleteById(ownerUserId: String, id: String)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = MAX(0, unreadCount + :delta)
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun adjustUnread(ownerUserId: String, id: String, delta: Int)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = MAX(0, :count)
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun setUnreadCount(ownerUserId: String, id: String, count: Int)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = 0
        WHERE ownerUserId = :ownerUserId
        """,
    )
    suspend fun clearAllUnread(ownerUserId: String)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = 0
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun clearUnread(ownerUserId: String, id: String)

    @Query(
        """
        SELECT COALESCE(SUM(unreadCount), 0)
        FROM conversations
        WHERE ownerUserId = :ownerUserId
          AND isMuted = 0
        """,
    )
    fun observeTotalUnreadCount(ownerUserId: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(unreadCount), 0)
        FROM conversations
        WHERE ownerUserId = :ownerUserId
        """,
    )
    fun observeTotalUnreadCountIncludingMuted(ownerUserId: String): Flow<Int>

    @Query(
        """
        UPDATE conversations
        SET isPinned = :isPinned
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun setPinned(ownerUserId: String, id: String, isPinned: Boolean)

    @Query(
        """
        UPDATE conversations
        SET isMuted = :isMuted
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun setMuted(ownerUserId: String, id: String, isMuted: Boolean)

    @Query(
        """
        UPDATE conversations
        SET lastMsgText = :text,
            lastMsgType = :msgType,
            updatedAt = :updatedAt
        WHERE ownerUserId = :ownerUserId AND id = :id
        """,
    )
    suspend fun updatePreview(ownerUserId: String, id: String, text: String, msgType: Int, updatedAt: Long)
}

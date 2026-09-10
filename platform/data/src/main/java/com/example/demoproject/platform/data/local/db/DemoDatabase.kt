package com.example.demoproject.platform.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.demoproject.platform.data.local.db.converter.Converters
import com.example.demoproject.platform.data.local.db.dao.ConversationDao
import com.example.demoproject.platform.data.local.db.dao.MessageDao
import com.example.demoproject.platform.data.local.db.dao.PostDao
import com.example.demoproject.platform.data.local.db.dao.UserDao
import com.example.demoproject.platform.data.local.db.entity.ConversationEntity
import com.example.demoproject.platform.data.local.db.entity.MessageEntity
import com.example.demoproject.platform.data.local.db.entity.PostEntity
import com.example.demoproject.platform.data.local.db.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        MessageEntity::class,
        ConversationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DemoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}

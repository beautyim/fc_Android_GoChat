package com.example.demoproject.platform.data.local.db

import android.content.Context
import androidx.room.Room
import com.example.demoproject.platform.data.local.crypto.DatabasePassphraseProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object LocalDatabaseFactory {
    const val DATABASE_NAME = "demoproject.db"

    fun create(context: Context): DemoDatabase {
        val appContext = context.applicationContext
        System.loadLibrary("sqlcipher")
        val passphrase = DatabasePassphraseProvider(appContext).getOrCreatePassphrase()
        val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
        return Room.databaseBuilder(
            appContext,
            DemoDatabase::class.java,
            databaseFile.absolutePath,
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8)))
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}

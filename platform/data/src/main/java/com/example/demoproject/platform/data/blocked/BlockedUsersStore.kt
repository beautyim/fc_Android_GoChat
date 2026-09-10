package com.example.demoproject.platform.data.blocked

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.blockedUsersDataStore by preferencesDataStore(name = "blocked_users")

class BlockedUsersStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _blockedUids = MutableStateFlow<Set<Long>>(emptySet())
    val blockedUids: StateFlow<Set<Long>> = _blockedUids.asStateFlow()

    private object Keys {
        val UIDS = stringSetPreferencesKey("blocked_uids")
    }

    init {
        scope.launch {
            val stored = appContext.blockedUsersDataStore.data.first()[Keys.UIDS].orEmpty()
            _blockedUids.value = stored.mapNotNull { it.toLongOrNull() }.filter { it > 0L }.toSet()
        }
    }

    fun block(uid: Long) {
        if (uid <= 0L) return
        persist(_blockedUids.value + uid)
    }

    fun addAll(uids: Collection<Long>) {
        val next = uids.filter { it > 0L }
        if (next.isEmpty()) return
        persist(_blockedUids.value + next)
    }

    fun unblock(uid: Long) {
        if (uid <= 0L) return
        persist(_blockedUids.value - uid)
    }

    fun replaceAll(uids: Collection<Long>) {
        persist(uids.filter { it > 0L }.toSet())
    }

    fun isBlocked(uid: Long): Boolean = uid > 0L && uid in _blockedUids.value

    fun isBlocked(userId: String): Boolean = userId.toLongOrNull()?.let(::isBlocked) == true

    fun clear() {
        persist(emptySet())
    }

    private fun persist(next: Set<Long>) {
        _blockedUids.value = next
        scope.launch {
            appContext.blockedUsersDataStore.edit { prefs ->
                prefs[Keys.UIDS] = next.map { it.toString() }.toSet()
            }
        }
    }
}

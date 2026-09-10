package com.example.demoproject.platform.data.session

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.local.pref.SessionPrefs
import com.example.demoproject.platform.data.model.Session
import com.example.demoproject.platform.network.provider.AuthTokenProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SessionManager(
    private val sessionPrefs: SessionPrefs,
) : AuthTokenProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialHydration = CompletableDeferred<Session?>()
    private val _session: MutableStateFlow<Session?> = MutableStateFlow(null)

    init {
        scope.launch {
            sessionPrefs.sessionFlow.collect { session ->
                _session.value = session
                if (!initialHydration.isCompleted) {
                    initialHydration.complete(session)
                }
            }
        }
    }

    val sessionFlow: Flow<Session?> get() = _session
    val currentSessionSnapshot: Session? get() = _session.value
    val isLoggedIn: Boolean get() = _session.value != null
    val currentUserId: String? get() = _session.value?.userId
    val accessTokenSnapshot: String? get() = _session.value?.token

    override fun currentAccessToken(): String? = accessTokenSnapshot

    suspend fun awaitInitialHydration(): Session? = initialHydration.await()

    suspend fun saveSession(session: Session) {
        sessionPrefs.saveSession(session)
        _session.value = session
        AppLogger.i(TAG, "Session saved user=${session.userId}")
    }

    suspend fun markProfileComplete() {
        sessionPrefs.markProfileComplete()
        _session.value = _session.value?.copy(profileComplete = true)
    }

    suspend fun clearSession() {
        sessionPrefs.clearSession()
        _session.value = null
        AppLogger.i(TAG, "Session cleared")
    }

    companion object {
        private const val TAG = "SessionMgr"
    }
}

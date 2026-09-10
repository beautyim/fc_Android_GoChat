package com.example.demoproject.platform.data.blocked

import com.example.demoproject.platform.data.model.CallRecordPage
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.User

fun List<User>.excludingBlockedUsers(store: BlockedUsersStore): List<User> =
    filterNot { store.isBlocked(it.id) }

fun List<Conversation>.excludingBlockedConversations(store: BlockedUsersStore): List<Conversation> =
    filterNot { store.isBlocked(it.id) || store.isBlocked(it.peer.id) }

fun CallRecordPage.excludingBlockedCallRecords(store: BlockedUsersStore): CallRecordPage =
    copy(records = records.filterNot { store.isBlocked(it.peer.id) })

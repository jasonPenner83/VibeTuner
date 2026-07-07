package com.jpenner.vibetuner.data.sync

import java.time.OffsetDateTime

/** What pull should do with one document, given remote row + local bookkeeping. */
sealed interface SyncAction {
    data object ApplyRemote : SyncAction
    data object ApplyTombstone : SyncAction
    data object PushLocal : SyncAction
    data object Skip : SyncAction
}

/** True when [remote] is strictly newer than [lastSeen] (null lastSeen = never seen). */
fun isNewer(remote: String, lastSeen: String?): Boolean {
    if (lastSeen == null) return true
    return OffsetDateTime.parse(remote).toInstant() > OffsetDateTime.parse(lastSeen).toInstant()
}

/**
 * Last-push-wins with client-side dirty protection:
 *  - dirty local edits always push (a push gets a fresh server timestamp and wins);
 *  - remote tombstones remove the local profile;
 *  - otherwise a remote newer than lastSeen is applied;
 *  - a local doc with no remote row pushes (first-sign-in seeding).
 * [remoteUpdatedAt] null means "no remote row exists".
 */
fun decideDocAction(
    remoteUpdatedAt: String?,
    remoteDeleted: Boolean,
    state: DocState,
    localExists: Boolean,
): SyncAction = when {
    state.dirty -> SyncAction.PushLocal
    remoteUpdatedAt == null -> if (localExists) SyncAction.PushLocal else SyncAction.Skip
    remoteDeleted -> if (localExists) SyncAction.ApplyTombstone else SyncAction.Skip
    isNewer(remoteUpdatedAt, state.lastSeen) -> SyncAction.ApplyRemote
    else -> SyncAction.Skip
}

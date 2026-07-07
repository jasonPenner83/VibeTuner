package com.jpenner.vibetuner.data.sync

/** Receives store-write notifications; implemented by SyncManager. */
interface SyncListener {
    fun onDocChanged(profileId: String, kind: String)
    fun onProfileDeleted(profileId: String)
}

/**
 * Global seam between the data stores and sync. Stores announce user-driven
 * writes here; sync-applied imports deliberately bypass it (they call the
 * stores' import/removeFromSync APIs, which never notify) so a pull can't
 * mark docs dirty and echo them back up. Null listener (signed out or sync
 * not yet constructed) = every notification is a no-op.
 */
object SyncHooks {
    @Volatile var listener: SyncListener? = null

    fun notifyChanged(profileId: String, kind: String) {
        listener?.onDocChanged(profileId, kind)
    }

    fun notifyProfileDeleted(profileId: String) {
        listener?.onProfileDeleted(profileId)
    }
}

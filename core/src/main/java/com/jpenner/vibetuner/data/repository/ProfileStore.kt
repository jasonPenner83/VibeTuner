package com.jpenner.vibetuner.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.data.model.ProfileType
import com.jpenner.vibetuner.data.model.Rating
import com.jpenner.vibetuner.data.model.profileFromJson
import com.jpenner.vibetuner.data.model.toJson
import com.jpenner.vibetuner.data.sync.SyncHooks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * The single source of truth for viewer profiles + their restrictions, persisted
 * to vibetuner_profiles.json (the picker still reads the active id from
 * vibetuner_active_profile.txt), mirroring the per-file JSON pattern used by
 * ChannelOverrideStore. Seeded with the previous fixed sample set on first run.
 * The Profile Manager is the only writer; the picker and the content gate are
 * readers. Shared via [get] so every screen observes the same state.
 *
 * PINs are never stored in plaintext — [setPin] salts and SHA-256-hashes the
 * 4 digits, and [verifyPin] re-hashes the attempt with the stored salt. The
 * hash is "salt:digest" (both hex).
 */
class ProfileStore internal constructor(private val file: File) {

    private val _profiles = MutableStateFlow(load())

    fun profiles(): StateFlow<List<Profile>> = _profiles.asStateFlow()

    fun profilesNow(): List<Profile> = _profiles.value

    fun byId(id: String): Profile? = _profiles.value.firstOrNull { it.id == id }

    // ---- CRUD ----

    fun create(name: String = "New Profile"): Profile {
        val gradient = Profile.SampleGradients[_profiles.value.size % Profile.SampleGradients.size]
        val profile = Profile(id = "p_${UUID.randomUUID().toString().take(8)}", name = name, gradient = gradient)
        persist(_profiles.value + profile)
        SyncHooks.notifyChanged(profile.id, "profile")
        return profile
    }

    fun edit(id: String, transform: (Profile) -> Profile) {
        persist(_profiles.value.map { if (it.id == id) transform(it) else it })
        SyncHooks.notifyChanged(id, "profile")
    }

    fun delete(id: String) {
        if (_profiles.value.size <= 1) return   // never delete the last profile
        persist(_profiles.value.filterNot { it.id == id })
        SyncHooks.notifyProfileDeleted(id)
    }

    fun rename(id: String, name: String) = edit(id) { it.copy(name = name) }

    /** Preset seeds rating + allowed types; user tweaks still stick after. */
    fun applyType(id: String, type: ProfileType) = edit(id) {
        it.copy(type = type, maxRating = type.defaultMaxRating, allowedTypes = type.defaultTypes)
    }

    fun setMaxRating(id: String, rating: Rating) = edit(id) { it.copy(maxRating = rating) }

    fun toggleType(id: String, type: String) = edit(id) {
        val next = if (type in it.allowedTypes) it.allowedTypes - type else it.allowedTypes + type
        it.copy(allowedTypes = next)
    }

    fun toggleFavourite(id: String, channelId: String) = edit(id) {
        val next = if (channelId in it.favouriteChannelIds)
            it.favouriteChannelIds - channelId
        else
            it.favouriteChannelIds + channelId
        it.copy(favouriteChannelIds = next)
    }



    /** Enabling adult content always locks the profile behind its PIN; the
     *  view model guarantees a PIN exists before calling with on = true. */
    fun setAdultContent(id: String, on: Boolean) = edit(id) {
        it.copy(adultContent = on, requirePin = it.requirePin || on)
    }

    /** Dropping PIN protection also drops the adult unlock — an adult-enabled
     *  profile can never remain unlocked without a PIN. */
    fun setRequirePin(id: String, require: Boolean) = edit(id) {
        it.copy(requirePin = require, adultContent = if (require) it.adultContent else false)
    }

    // ---- PIN ----

    fun setPin(id: String, pin: String) = edit(id) {
        it.copy(pinHash = hash(pin, newSalt()), requirePin = true)
    }

    fun clearPin(id: String) = edit(id) {
        it.copy(pinHash = null, requirePin = false, adultContent = false)
    }

    /** Constant-time compare of the attempt against the stored salted hash. */
    fun verifyPin(profile: Profile, attempt: String): Boolean {
        val stored = profile.pinHash ?: return true
        val salt = stored.substringBefore(':')
        return MessageDigest.isEqual(hash(attempt, salt).toByteArray(), stored.toByteArray())
    }

    private fun newSalt(): String =
        ByteArray(8).also { SecureRandom().nextBytes(it) }.toHex()

    private fun hash(pin: String, salt: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray())
        return "$salt:${d.toHex()}"
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    // ---- sync (no hook notifications: these apply REMOTE state) ----

    fun exportProfile(id: String): JSONObject? = byId(id)?.toJson()

    /** Upsert one profile from a sync payload. Never notifies SyncHooks. */
    fun importProfile(o: JSONObject) {
        val incoming = profileFromJson(o) ?: return
        val current = _profiles.value
        persist(
            if (current.any { it.id == incoming.id })
                current.map { if (it.id == incoming.id) incoming else it }
            else current + incoming
        )
    }

    /** Apply a remote tombstone. Keeps the never-delete-the-last-profile guard. Never notifies. */
    fun removeFromSync(id: String) {
        if (_profiles.value.size <= 1) return
        persist(_profiles.value.filterNot { it.id == id })
    }

    // ---- persistence ----

    private fun persist(profiles: List<Profile>) {
        _profiles.value = profiles
        writeFile(profiles)
    }

    private fun writeFile(profiles: List<Profile>) {
        runCatching {
            val arr = JSONArray()
            profiles.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        }.onFailure { Log.e("VibeTuner Profiles", "🔥 write error: ${it.message}") }
    }

    // runs before _profiles exists, so it must only touch the file
    private fun load(): List<Profile> {
        if (!file.exists()) return seed().also(::writeFile)
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::profileFromJson) }
        }.getOrElse {
            Log.e("VibeTuner Profiles", "🔥 read error: ${it.message}")
            seed()
        }.ifEmpty { seed() }
    }

    // first-run default: a single unrestricted profile, renameable in the Profile Manager
    private fun seed(): List<Profile> = listOf(
        Profile("person1", "Person 1", Profile.SampleGradients[0]),
    )


    companion object {
        @Volatile private var instance: ProfileStore? = null

        /** One shared store per process so every screen observes the same flow. */
        fun get(context: Context): ProfileStore =
            instance ?: synchronized(this) {
                instance ?: ProfileStore(File(context.filesDir, "vibetuner_profiles.json"))
                    .also { instance = it }
            }
    }
}

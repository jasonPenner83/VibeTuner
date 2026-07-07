package com.jpenner.vibetuner.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject

/**
 * A viewer profile shown on the "Who's watching?" picker.
 *  - [type]         a preset that seeds sensible defaults
 *  - [maxRating]    nothing rated above this is playable
 *  - [allowedTypes] the manifest content types ("movie"/"series"/custom) this
 *                   profile may browse; empty = every type allowed
 *  - [pinHash]      salted SHA-256 of the 4-digit PIN ("salt:digest"); null = no PIN
 *  - [requirePin]   ask for the PIN when this profile is selected
 *  - [adultContent] Adult-profile opt-in that lifts the adult addon/catalog block
 *
 * [gradient] is the two stops of the avatar's linear gradient,
 * so ProfileTile can render the brush without knowing the palette.
 */
data class Profile(
    val id: String,
    val name: String,
    val gradient: List<Color>,     // 2 stops: top-left -> bottom-right
    val type: ProfileType = ProfileType.ADULT,
    val maxRating: Rating = Rating.TVMA,
    val allowedTypes: Set<String> = emptySet(),   // manifest types; empty = all
    val pinHash: String? = null,
    val requirePin: Boolean = false,
    val adultContent: Boolean = false,
    val favouriteChannelIds: Set<String> = emptySet(),
) {
    /** First grapheme, used as the avatar monogram. */
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"

    val hasPin: Boolean get() = pinHash != null

    /** Bridges the legacy kids-content lock: existing call sites that read
     *  profile.isKid keep working — KID is simply the strictest [type]. */
    val isKid: Boolean get() = type == ProfileType.KID

    /** Effective adult unlock: the stored flag only takes effect on Adult
     *  profiles — remembered but suppressed while [type] is Teen/Kid. */
    val allowsAdult: Boolean get() = adultContent && type == ProfileType.ADULT

    /** True once any lock is active — drives the list summary + lock glyph. */
    val isRestricted: Boolean
        get() = requirePin ||
            maxRating != Rating.TVMA ||
            allowedTypes.isNotEmpty()

    /** Gate checks the guide / player run before playback. */
    fun allows(rating: Rating): Boolean = rating.ceiling <= maxRating.ceiling
    fun allows(type: String): Boolean = allowedTypes.isEmpty() || type in allowedTypes

    /** One-line summary shown under the name in the manager list. */
    fun summary(): String = when {
        !isRestricted && !allowsAdult -> "All content"
        else -> buildList {
            add(type.label)
            if (maxRating != Rating.TVMA) add("up to ${maxRating.label}")
            if (allowedTypes.isNotEmpty()) add("${allowedTypes.size} types")
            if (allowsAdult) add("18+")
        }.joinToString(" · ")
    }

    companion object {
        // sample palette stops matching the design mocks
        val SampleGradients = listOf(
            listOf(Color(0xFF46507A), Color(0xFF2A3052)),
            listOf(Color(0xFF7C5A9E), Color(0xFF4A3568)),
            listOf(Color(0xFF3D8A7A), Color(0xFF225048)),
            listOf(Color(0xFFB07A3A), Color(0xFF6E4A22)),  // Kids
        )
    }
}

/**
 * Preset applied when the type segmented control changes. Seeds [Profile.maxRating]
 * and [Profile.allowedTypes]; both remain independently editable afterwards.
 */
enum class ProfileType(
    val label: String,
    val defaultMaxRating: Rating,
    /** Empty = no type restriction (all manifest types allowed). */
    val defaultTypes: Set<String>,
) {
    ADULT("Adult", Rating.TVMA, emptySet()),
    TEEN("Teen", Rating.TV14, emptySet()),
    KID("Kid", Rating.TVY, setOf("movie", "series"));   // no live tv / channel
}

/** JSON codec for a single profile — used by ProfileStore's file and by sync payloads. */
fun Profile.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("gradient", JSONArray().apply { gradient.forEach { put(it.toArgb()) } })
    put("type", type.name)
    put("maxRating", maxRating.name)
    put("allowedTypes", JSONArray().apply { allowedTypes.forEach { put(it) } })
    pinHash?.let { put("pinHash", it) }
    put("requirePin", requirePin)
    put("adultContent", adultContent)
    put("favouriteChannelIds", JSONArray().apply {favouriteChannelIds.forEach { put(it) }})
}

fun profileFromJson(o: JSONObject): Profile? {
    val id = o.optString("id").ifBlank { return null }
    val stops = o.optJSONArray("gradient")
        ?.let { arr -> (0 until arr.length()).map { Color(arr.getInt(it)) } }
        .takeUnless { it.isNullOrEmpty() } ?: Profile.SampleGradients[0]
    return Profile(
        id = id,
        name = o.optString("name", "?"),
        gradient = stops,
        type = runCatching { ProfileType.valueOf(o.optString("type")) }.getOrDefault(ProfileType.ADULT),
        maxRating = runCatching { Rating.valueOf(o.optString("maxRating")) }.getOrDefault(Rating.TVMA),
        allowedTypes = o.optJSONArray("allowedTypes")
            ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }.orEmpty(),
        pinHash = o.optString("pinHash").ifBlank { null },
        requirePin = o.optBoolean("requirePin", false),
        adultContent = o.optBoolean("adultContent", false),
        favouriteChannelIds = o.optJSONArray("favouriteChannelIds")
            ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }.orEmpty(),
    )
}

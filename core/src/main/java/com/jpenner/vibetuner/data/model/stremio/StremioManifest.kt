package com.jpenner.vibetuner.data.model.stremio

import org.json.JSONObject

/**
 * A Stremio addon manifest (the subset VibeTuner needs). Fetched once from the
 * addon's `/manifest.json` and cached on disk per profile. Each entry in
 * [catalogs] becomes one channel (EPG row) in the guide.
 */
data class StremioManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val logo: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val catalogs: List<StremioCatalog> = emptyList(),
    /** Manifest-level `behaviorHints.adult`. */
    val adult: Boolean = false,
) {
    /** True when this addon advertises catalog content (i.e. can back channels). */
    val servesCatalogs: Boolean
        get() = resources.contains("catalog") && catalogs.isNotEmpty()

    /** Hard adult block for the whole addon (spec §4): behaviorHints.adult or any adult types[] entry. */
    val adultBlocked: Boolean
        get() = adult || types.any(::isAdultType)
}

/** One `extra` prop a catalog supports. [options] non-empty means each option can back a sub-channel. */
data class StremioExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
)

/**
 * A single catalog declared by an addon. The (type, id) pair is the catalog's
 * identity within its addon and is fetched at `/catalog/{type}/{id}.json`.
 */
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String,
    /** Supported extra props (e.g. "search", "genre", "skip") with their option lists. */
    val extra: List<StremioExtra> = emptyList()
) {
    /** Extras that can fan out into sub-channels (genre lists etc.). */
    val optionExtras: List<StremioExtra> get() = extra.filter { it.options.isNotEmpty() }

    /** A required extra means the catalog is NOT fetchable bare — no base channel. */
    val requiresExtra: Boolean get() = extra.any { it.isRequired }

    val adultBlocked: Boolean get() = isAdultType(type)
}

private val ADULT_TYPES = setOf("porn", "xxx", "adult", "erotic")

/** True when a manifest/catalog `type` names adult content ("Porn", "xxx", ...). */
fun isAdultType(raw: String): Boolean = raw.trim().lowercase() in ADULT_TYPES

/**
 * Parse a raw `/manifest.json` body into a [StremioManifest], or null when the
 * required identity fields are missing (id / name / version).
 */
fun parseManifest(json: String): StremioManifest? = runCatching {
    val root = JSONObject(json)
    val id = root.optString("id", "").trim()
    val name = root.optString("name", "").trim()
    val version = root.optString("version", "").trim()
    if (id.isBlank() || name.isBlank() || version.isBlank()) return null

    StremioManifest(
        id = id,
        name = name,
        version = version,
        description = root.optString("description", ""),
        logo = root.optString("logo", "").ifBlank { null },
        types = root.optJSONArray("types").toStringList(),
        resources = parseResources(root),
        catalogs = parseCatalogs(root),
        adult = root.optJSONObject("behaviorHints")?.optBoolean("adult", false) ?: false
    )
}.getOrNull()

/** `resources` entries are either bare strings or `{ name, types, idPrefixes }` objects. */
private fun parseResources(root: JSONObject): List<String> {
    val arr = root.optJSONArray("resources") ?: return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        when (val el = arr.opt(i)) {
            is String -> out.add(el)
            is JSONObject -> el.optString("name", "").takeIf { it.isNotBlank() }?.let(out::add)
        }
    }
    return out
}

private fun parseCatalogs(root: JSONObject): List<StremioCatalog> {
    val arr = root.optJSONArray("catalogs") ?: return emptyList()
    val out = mutableListOf<StremioCatalog>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val type = obj.optString("type", "").trim()
        val id = obj.optString("id", "").trim()
        if (type.isBlank() || id.isBlank()) continue
        out.add(
            StremioCatalog(
                type = type,
                id = id,
                name = obj.optString("name", "").ifBlank { id },
                extra = parseExtra(obj)
            )
        )
    }
    return out
}

/**
 * `extra` is `[{ name, isRequired, options }]`. Legacy addons instead use bare
 * `extraSupported: [name]` and/or a catalog-level `genres: [...]` options list.
 */
private fun parseExtra(catalog: JSONObject): List<StremioExtra> {
    val out = mutableListOf<StremioExtra>()
    catalog.optJSONArray("extra")?.let { arr ->
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isBlank()) continue
            out.add(StremioExtra(
                name = name,
                isRequired = obj.optBoolean("isRequired", false),
                options = obj.optJSONArray("options").toStringList(),
            ))
        }
    }
    if (out.isEmpty()) {
        catalog.optJSONArray("extraSupported").toStringList().forEach { out.add(StremioExtra(it)) }
    }
    // Legacy genres list = options for the genre extra (create it if missing).
    val genres = catalog.optJSONArray("genres").toStringList()
    if (genres.isNotEmpty()) {
        val i = out.indexOfFirst { it.name == "genre" }
        if (i >= 0) { if (out[i].options.isEmpty()) out[i] = out[i].copy(options = genres) }
        else out.add(StremioExtra("genre", options = genres))
    }
    return out
}

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optString(i, "").takeIf { it.isNotBlank() } }
}

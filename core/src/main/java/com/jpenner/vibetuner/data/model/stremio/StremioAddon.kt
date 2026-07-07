package com.jpenner.vibetuner.data.model.stremio

import org.json.JSONArray
import org.json.JSONObject

/**
 * An installed Stremio addon for a profile: the manifest URL it was added from,
 * the cached [manifest] (so we can list catalogs without re-fetching), and whether
 * it is currently [enabled]. Disabled addons contribute no channels and are skipped
 * during stream resolution.
 */
data class StremioAddon(
    val manifestUrl: String,
    val manifest: StremioManifest,
    val enabled: Boolean = true,
    /** Chosen sub-channel options, keyed by [selectionKey] ("type/catalogId/extraName"). */
    val selections: Map<String, List<String>> = emptyMap(),
) {
    /** Base URL for the addon's routes (manifest URL without the trailing /manifest.json). */
    val baseUrl: String
        get() = manifestUrl.trim().removeSuffix("/").removeSuffix("/manifest.json")

    val id: String get() = manifest.id

    fun selectedOptions(catalog: StremioCatalog, extraName: String): List<String> =
        selections[selectionKey(catalog.type, catalog.id, extraName)].orEmpty()
}

/** Key for one catalog-extra's chosen options inside [StremioAddon.selections]. */
fun selectionKey(catalogType: String, catalogId: String, extraName: String): String =
    "$catalogType/$catalogId/$extraName"

/** Drop selections whose catalog/extra/option no longer exists in [manifest] (spec §3). */
fun pruneSelections(
    selections: Map<String, List<String>>,
    manifest: StremioManifest,
): Map<String, List<String>> {
    val valid: Map<String, Set<String>> = manifest.catalogs.flatMap { c ->
        c.optionExtras.map { e -> selectionKey(c.type, c.id, e.name) to e.options.toSet() }
    }.toMap()
    return buildMap {
        selections.forEach { (key, opts) ->
            val allowed = valid[key] ?: return@forEach
            val kept = opts.filter { it in allowed }
            if (kept.isNotEmpty()) put(key, kept)
        }
    }
}

/**
 * Seed the first option of every required extra that has options but no current
 * selection. Required-extra catalogs aren't fetchable bare (spec §3), so without
 * a selection they can produce no channel at all — and with no channel row there
 * is no drill-down to pick options from. Existing selections are never modified;
 * returns this unchanged when there is nothing to seed.
 */
fun StremioAddon.seedRequiredSelections(): StremioAddon {
    val seeded = manifest.catalogs.flatMap { c ->
        c.extra
            .filter { it.isRequired && it.options.isNotEmpty() }
            .map { e -> selectionKey(c.type, c.id, e.name) to e.options.first() }
    }
        .filter { (key, _) -> selections[key].orEmpty().isEmpty() }
        .associate { (key, first) -> key to listOf(first) }
    return if (seeded.isEmpty()) this else copy(selections = selections + seeded)
}

/** Select or clear every option of [catalog]'s option extras at once (sub-channel Select all). */
fun StremioAddon.withAllOptions(catalog: StremioCatalog, selected: Boolean): StremioAddon {
    val updated = catalog.optionExtras.associate { e ->
        selectionKey(catalog.type, catalog.id, e.name) to (if (selected) e.options else emptyList())
    }
    return copy(selections = (selections + updated).filterValues { it.isNotEmpty() })
}

// ── Derived UI metadata (not persisted) ────────────────────────────────────────

/** Official add-ons get an "OFFICIAL" tag. Only Cinemeta today. */
val StremioAddon.official: Boolean
    get() = manifest.id.startsWith("com.linvo.cinemeta")

/** Display host from the manifest URL (e.g. "v3-cinemeta.strem.io"); empty if unparseable. */
val StremioAddon.host: String
    get() = runCatching { java.net.URI(manifestUrl).host.orEmpty() }.getOrDefault("")

/** 1–2 char logo fallback derived from the add-on name. */
val StremioAddon.abbreviation: String
    get() = manifest.name.take(2).uppercase()

// ── JSON persistence (per-profile addon file) ──────────────────────────────────

fun StremioAddon.toJson(): JSONObject = JSONObject().apply {
    put("manifestUrl", manifestUrl)
    put("enabled", enabled)
    put("manifest", manifest.toJson())
    put("selections", JSONObject().apply {
        selections.forEach { (k, v) -> put(k, JSONArray(v)) }
    })
}

fun addonFromJson(obj: JSONObject): StremioAddon? {
    val url = obj.optString("manifestUrl", "").ifBlank { return null }
    val manifest = obj.optJSONObject("manifest")?.let(::manifestFromJson) ?: return null
    return StremioAddon(
        manifestUrl = url,
        manifest = manifest,
        enabled = obj.optBoolean("enabled", true),
        selections = obj.optJSONObject("selections")?.let { sel ->
            sel.keys().asSequence().associateWith { k -> sel.optJSONArray(k).toStringList() }
                .filterValues { it.isNotEmpty() }
        } ?: emptyMap()
    )
}

private fun StremioManifest.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("version", version)
    put("description", description)
    put("logo", logo ?: JSONObject.NULL)
    put("types", JSONArray(types))
    put("resources", JSONArray(resources))
    put("catalogs", JSONArray().apply { catalogs.forEach { put(it.toJson()) } })
    put("adult", adult)
}

private fun StremioCatalog.toJson(): JSONObject = JSONObject().apply {
    put("type", type)
    put("id", id)
    put("name", name)
    put("extra", JSONArray().apply {
        extra.forEach { e ->
            put(JSONObject().apply {
                put("name", e.name)
                put("isRequired", e.isRequired)
                put("options", JSONArray(e.options))
            })
        }
    })
}

private fun manifestFromJson(obj: JSONObject): StremioManifest? {
    val id = obj.optString("id", "").ifBlank { return null }
    return StremioManifest(
        id = id,
        name = obj.optString("name", id),
        version = obj.optString("version", "0.0.0"),
        description = obj.optString("description", ""),
        logo = obj.optString("logo", "").ifBlank { null },
        types = obj.optJSONArray("types").toStringList(),
        resources = obj.optJSONArray("resources").toStringList(),
        catalogs = obj.optJSONArray("catalogs").toCatalogList(),
        adult = obj.optBoolean("adult", false)
    )
}

private fun catalogFromJson(obj: JSONObject): StremioCatalog? {
    val type = obj.optString("type", "").ifBlank { return null }
    val id = obj.optString("id", "").ifBlank { return null }
    return StremioCatalog(
        type = type,
        id = id,
        name = obj.optString("name", id),
        extra = parseExtraJson(obj.optJSONArray("extra"))
    )
}

/** New files store extra as objects; pre-options files stored a bare name array. */
private fun parseExtraJson(arr: JSONArray?): List<StremioExtra> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        when (val el = arr.opt(i)) {
            is String -> el.takeIf { it.isNotBlank() }?.let { StremioExtra(it) }
            is JSONObject -> el.optString("name", "").takeIf { it.isNotBlank() }?.let {
                StremioExtra(it, el.optBoolean("isRequired", false), el.optJSONArray("options").toStringList())
            }
            else -> null
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optString(i, "").takeIf { it.isNotBlank() } }
}

private fun JSONArray?.toCatalogList(): List<StremioCatalog> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(::catalogFromJson) }
}

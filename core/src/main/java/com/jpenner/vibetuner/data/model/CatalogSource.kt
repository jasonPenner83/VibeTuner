package com.jpenner.vibetuner.data.model

/**
 * Where a channel came from, lifted verbatim from the add-on's manifest.json entry.
 * Surfaced in the Channel Manager so colliding default names (Cinemeta ships two
 * catalogs both called "Popular") stay distinguishable. [itemCount] is null until the
 * guide has harvested the catalog (see ChannelRepository.cachedItemCount).
 */
data class CatalogSource(
    val addonId: String,
    val addonName: String,
    val addonAbbrev: String,
    val type: String,        // "movie" | "series" | custom
    val catalogId: String,
    val catalogName: String,
    val itemCount: Int? = null,
    val extraName: String? = null,   // e.g. "genre" for option sub-channels
    val option: String? = null,      // e.g. "Action"
) {
    val typeLabel: String get() = if (type == "series") "Series" else "Movie"

    /** "Popular · Action" for option sub-channels, else just the catalog name. */
    val catalogLabel: String get() = option?.let { "$catalogName · $it" } ?: catalogName

    /** "1,850 titles" / "1,240 shows"; em dash when the count isn't known yet. */
    val libraryLabel: String
        get() = itemCount?.let { String.format(java.util.Locale.US, "%,d %s", it, if (type == "series") "shows" else "titles") } ?: "—"

    val sourceKey: String get() = "stremio:$addonId:$type:$catalogId" +
        (if (extraName != null && option != null) ":$extraName=$option" else "")
}

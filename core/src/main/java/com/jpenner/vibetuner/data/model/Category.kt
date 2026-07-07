package com.jpenner.vibetuner.data.model

import androidx.compose.ui.graphics.Color

/**
 * The single category taxonomy for the app. Each value carries its own accent [color]
 * (used for ProgramCard left edges, channel-row labels, the focused-program chip) and a
 * display [label], so UI components never branch on the enum themselves.
 *
 * [isGenre] distinguishes real genres (seeded as channels, shown in the harvest picker)
 * from the two discovery buckets ([Featured], [ForYou]) which appear only as sidebar tabs.
 *
 * This enum is the single source of truth for categories (it replaced the old string-based
 * `CategoryConfig`). Persistence is by [label] via [fromLabel]; never persist [name].
 *
 * NOTE: media type ("Movie"/"Episode"/"TV Show") is a separate axis — see [Program.mediaType].
 */
enum class Category(val label: String, val color: Color, val isGenre: Boolean = true) {
    Movies("Movies",             Color(0xFFB07CFF)),
    Series("Series",             Color(0xFFFFB495)),
    Kids("Kids",                 Color(0xFFFFB454)),
    Sports("Sports",             Color(0xFF46D18B)),
    Music("Music",               Color(0xFFFF7AC0)),
    News("News",                 Color(0xFF5B8DEF)),
    SciFi("Sci-Fi",              Color(0xFF7C9CFF)),
    Action("Action",             Color(0xFFFF6B5C)),
    Comedy("Comedy",             Color(0xFFFFD24A)),
    Drama("Drama",               Color(0xFFFF8A5C)),
    Horror("Horror",             Color(0xFFB23A48)),
    Thriller("Thriller",         Color(0xFF8E7CFF)),
    Documentary("Documentary",   Color(0xFF34D0C0)),
    Romance("Romance",           Color(0xFFFF6FA5)),
    Animation("Animation",       Color(0xFF4AC0E0)),
    Crime("Crime",               Color(0xFF6E7687)),
    Fantasy("Fantasy",           Color(0xFF9B6DFF)),
    Adventure("Adventure",       Color(0xFF4CC38A)),
    Mystery("Mystery",           Color(0xFF5C6BC0)),
    Featured("Featured",         Color(0xFFFFC24A), isGenre = false),
    ForYou("For You",            Color(0xFF3D9BFF), isGenre = false);

    companion object {
        /** Genres seeded as channels (one each) and offered in the harvest-source picker. */
        val GENRES: List<Category> = entries.filter { it.isGenre }

        /** Fallback for new/unknown records. */
        val DEFAULT: Category = Movies

        /** Legacy persisted labels that no longer match a [label] verbatim. */
        private val LEGACY = mapOf("My Lists" to ForYou)

        /** Strict label match (no legacy/default fallback), for probing raw strings. */
        fun fromLabelOrNull(raw: String): Category? =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) }

        /** Resolve a persisted/raw label to a [Category]; subsumes the old migrateCategory(). */
        fun fromLabel(raw: String): Category =
            fromLabelOrNull(raw) ?: LEGACY[raw] ?: DEFAULT

        /** Default category for a Stremio catalog `type` (user override still wins). */
        fun forStremioType(raw: String): Category {
            val t = raw.trim()
            fromLabelOrNull(t)?.takeIf { it.isGenre }?.let { return it }
            return when (t.lowercase()) {
                "movie" -> Movies
                "series", "tv", "channel" -> Series
                "anime" -> Animation
                "documentary", "docs" -> Documentary
                else -> DEFAULT
            }
        }
    }
}

package com.jpenner.vibetuner.data.model

/**
 * Content rating shown as a chip in the preview / detail screens, and used by
 * Profile.maxRating for the parental lock. [ceiling] is a severity index
 * (low -> high) so "allow up to X" is a single comparison and the
 * Maximum-rating stepper has a stable order.
 */
enum class Rating(val label: String, val ceiling: Int) {
    TVY("TV-Y", 0), TVG("TV-G", 1), TVPG("TV-PG", 2),
    TV14("TV-14", 4), TVMA("TV-MA", 5), PG13("PG-13", 3);

    override fun toString() = label

    companion object {
        /** Order walked by the Maximum-rating stepper (‹ / ›). */
        val ladder: List<Rating> = entries.sortedBy { it.ceiling }
    }
}

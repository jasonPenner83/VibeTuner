package com.jpenner.vibetuner.data.model

import org.json.JSONObject

/**
 * A user's per-channel customizations, keyed by the channel's stable sourceKey in
 * ChannelOverrideStore. Any null field means "not overridden — use the catalog default".
 * marathonLimit null = None (binge each show fully); enabled null = default (true).
 */
data class ChannelOverride(
    val name: String? = null,
    val category: String? = null,     // Category.label
    val mode: String? = null,         // "RANDOM" | "CHRONOLOGICAL"
    val marathonLimit: Int? = null,   // null = None
    val enabled: Boolean? = null,
    val orderIndex: Int? = null,
)

fun ChannelOverride.toJson(): JSONObject = JSONObject().apply {
    put("name", name ?: JSONObject.NULL)
    put("category", category ?: JSONObject.NULL)
    put("mode", mode ?: JSONObject.NULL)
    put("marathonLimit", marathonLimit ?: JSONObject.NULL)
    put("enabled", if (enabled == null) JSONObject.NULL else enabled)
    put("orderIndex", orderIndex ?: JSONObject.NULL)
}

fun channelOverrideFromJson(o: JSONObject): ChannelOverride = ChannelOverride(
    name = o.optNullableString("name"),
    category = o.optNullableString("category"),
    mode = o.optNullableString("mode"),
    marathonLimit = if (o.isNull("marathonLimit")) null else o.optInt("marathonLimit"),
    enabled = if (o.isNull("enabled")) null else o.optBoolean("enabled"),
    orderIndex = if (o.isNull("orderIndex")) null else o.optInt("orderIndex"),
)

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key, "").ifBlank { null }

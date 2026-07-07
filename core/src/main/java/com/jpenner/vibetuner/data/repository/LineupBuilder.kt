package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.CatalogSource
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.ChannelOverride
import com.jpenner.vibetuner.data.model.SourceType

/**
 * Derive the channel lineup from the profile's enabled catalog specs plus user overrides.
 * Channel id is deterministic (== sourceKey) so harvest-cache and program ids stay stable
 * across launches without persisting the lineup. Ordering is by override.orderIndex, falling
 * back to catalog discovery order; channel numbers are assigned 100,110,120,... after sorting.
 */
fun buildLineup(
    specs: List<CatalogChannelSpec>,
    overrides: Map<String, ChannelOverride>,
): List<Channel> {
    val channels = specs.mapIndexed { index, spec ->
        val (addon, catalog, extraName, option) = spec
        val sourceKey = stremioSourceKey(addon.id, catalog, extraName, option)
        val ov = overrides[sourceKey]
        val name = ov?.name ?: option ?: catalog.name
        // Option channels default to the genre they name; everything else follows the catalog type.
        val defaultCategory = option?.let { Category.fromLabelOrNull(it)?.takeIf { c -> c.isGenre } }
            ?: Category.forStremioType(catalog.type)
        Channel(
            id = sourceKey,
            number = "0",
            name = name,
            abbreviation = channelAbbreviation(name),
            description = addon.manifest.name,
            logoUrl = addon.manifest.logo ?: "",
            category = ov?.category?.let { Category.fromLabel(it) } ?: defaultCategory,
            sortingRule = ov?.mode ?: "RANDOM",
            marathonLimit = ov?.marathonLimit,
            orderIndex = ov?.orderIndex ?: index,
            sourceType = SourceType.STREMIO_CATALOG,
            sourceValue = stremioSourceValue(addon.id, catalog, extraName, option),
            sourceKey = sourceKey,
            enabled = ov?.enabled ?: true,
            autoImported = true,
            source = CatalogSource(
                addonId = addon.id,
                addonName = addon.manifest.name,
                addonAbbrev = addon.manifest.name.take(2).uppercase(),
                type = catalog.type,
                catalogId = catalog.id,
                catalogName = catalog.name,
                itemCount = null,
                extraName = extraName,
                option = option,
            ),
        )
    }.sortedBy { it.orderIndex }
    return channels.mapIndexed { i, ch -> ch.copy(number = (100 + i * 10).toString()) }
}

/** Swap [sourceKey] with its neighbor (up/down) in an ordered list of source keys. Edge = no-op. */
fun reorderedSourceKeys(order: List<String>, sourceKey: String, up: Boolean): List<String> {
    val i = order.indexOf(sourceKey)
    if (i < 0) return order
    val j = if (up) i - 1 else i + 1
    if (j !in order.indices) return order
    return order.toMutableList().also { it[i] = it[j]; it[j] = sourceKey }
}

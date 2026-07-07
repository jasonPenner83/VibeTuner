package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioCatalog

/**
 * One channel-to-be: a catalog plus, for option sub-channels, the extra prop and
 * chosen option it is filtered by (e.g. genre=Action). option == null is the base
 * catalog channel.
 */
data class CatalogChannelSpec(
    val addon: StremioAddon,
    val catalog: StremioCatalog,
    val extraName: String? = null,
    val option: String? = null,
)

/** One row in the Channel Manager's sub-channel drill-down. */
data class SubChannelToggle(val extraName: String, val option: String, val selected: Boolean)

/**
 * Expand enabled addons into channel specs: per non-blocked catalog, the base spec
 * (unless an extra is required — such catalogs aren't fetchable bare, spec §3) plus
 * one spec per selected option. Adult addons/catalogs are hard-blocked (spec §4)
 * unless the active profile lifts the block via [allowAdult].
 */
fun expandCatalogSpecs(addons: List<StremioAddon>, allowAdult: Boolean = false): List<CatalogChannelSpec> =
    addons
        .filter { it.enabled && it.manifest.servesCatalogs && (allowAdult || !it.manifest.adultBlocked) }
        .flatMap { addon ->
            addon.manifest.catalogs.filter { allowAdult || !it.adultBlocked }.flatMap { catalog ->
                val base = if (catalog.requiresExtra) emptyList()
                           else listOf(CatalogChannelSpec(addon, catalog))
                base + catalog.optionExtras.flatMap { extra ->
                    addon.selectedOptions(catalog, extra.name)
                        .filter { it in extra.options }
                        .map { CatalogChannelSpec(addon, catalog, extra.name, it) }
                }
            }
        }

/** All toggleable options of a catalog with their current selection state. */
fun optionToggles(addon: StremioAddon, catalog: StremioCatalog): List<SubChannelToggle> =
    catalog.optionExtras.flatMap { extra ->
        val selected = addon.selectedOptions(catalog, extra.name)
        extra.options.map { SubChannelToggle(extra.name, it, it in selected) }
    }

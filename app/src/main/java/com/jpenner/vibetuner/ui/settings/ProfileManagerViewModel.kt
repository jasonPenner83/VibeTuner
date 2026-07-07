package com.jpenner.vibetuner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.data.model.ProfileType
import com.jpenner.vibetuner.data.model.Rating
import com.jpenner.vibetuner.data.model.stremio.isAdultType
import com.jpenner.vibetuner.data.repository.AddonRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine

class ProfileManagerViewModel(
    private val store: ProfileStore,
    private val addons: AddonRepository,   // supplies the manifest type universe
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val prompt = MutableStateFlow<PinPrompt?>(null)
    private val pinError = MutableStateFlow(false)
    private val confirmingDelete = MutableStateFlow(false)

    val state: StateFlow<ProfileManagerUiState> =
        combine(store.profiles(), selectedId, prompt, pinError, confirmingDelete) { profiles, sel, pr, err, del ->
            val id = sel?.takeIf { s -> profiles.any { it.id == s } } ?: profiles.firstOrNull()?.id
            ProfileManagerUiState(
                rows = profiles.map { it.toRow() },
                selectedId = id,
                editing = profiles.firstOrNull { it.id == id }?.toEdit(availableTypes(id)),
                prompt = pr,
                pinError = err,
                confirmingDelete = del,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileManagerUiState())

    /** The type allow-list offers exactly the types the installed add-ons advertise —
     *  minus adult types unless this profile's adult unlock is in effect. */
    private fun availableTypes(profileId: String?): List<String> {
        val installed = profileId?.let { addons.getAddons(it) }.orEmpty()
            .ifEmpty { addons.getAddons("default") }
        val allowAdult = profileId?.let { store.byId(it) }?.allowsAdult == true
        return installed.flatMap { it.manifest.types }
            .filter { allowAdult || !isAdultType(it) }
            .distinct().sorted()
            .ifEmpty { listOf("movie", "series") }
    }

    /** Selecting a PIN-locked profile opens the unlock dialog first. */
    fun select(id: String) {
        val p = store.byId(id) ?: return
        if (p.hasPin && p.requirePin) prompt.value = PinPrompt.Unlock(id, p.name)
        else selectedId.value = id
    }

    fun createProfile() { selectedId.value = store.create().id }

    fun rename(id: String, name: String) = store.rename(id, name)
    fun applyType(id: String, type: ProfileType) = store.applyType(id, type)

    /** Empty allow-list means "all on", so toggling off from there materializes
     *  the full set minus [type]; re-completing the set collapses back to empty. */
    fun toggleType(id: String, type: String) {
        val avail = availableTypes(id).toSet()
        store.edit(id) {
            val current = it.allowedTypes.ifEmpty { avail }
            val next = if (type in current) current - type else current + type
            when {
                next.isEmpty() -> it                                   // at least one type stays on
                next == avail -> it.copy(allowedTypes = emptySet())    // back to "all"
                else -> it.copy(allowedTypes = next)
            }
        }
    }

    fun setRequirePin(id: String, on: Boolean) {
        if (on && store.byId(id)?.pinHash == null) {
            prompt.value = PinPrompt.SetPin(id, nameOf(id), confirming = false)   // must set one first
        } else {
            store.setRequirePin(id, on)
        }
    }

    /** Enabling adult content always proves PIN knowledge first: no PIN yet ->
     *  run SetPin and enable on confirm; PIN exists -> verify it via Unlock. */
    fun setAdultContent(id: String, on: Boolean) {
        if (!on) { store.setAdultContent(id, false); return }
        prompt.value = if (store.byId(id)?.pinHash == null) {
            PinPrompt.SetPin(id, nameOf(id), confirming = false, thenEnableAdult = true)
        } else {
            PinPrompt.Unlock(id, nameOf(id), thenEnableAdult = true)
        }
    }

    /** ‹ / › on the Maximum-rating stepper. */
    fun stepRating(id: String, current: Rating, step: Int) {
        val l = Rating.ladder
        store.setMaxRating(id, l[(l.indexOf(current) + step).coerceIn(0, l.lastIndex)])
    }

    fun requestSetPin(id: String) { prompt.value = PinPrompt.SetPin(id, nameOf(id), confirming = false) }
    fun dismissPrompt() { prompt.value = null; pinError.value = false; firstEntry = null }

    fun requestDelete() { confirmingDelete.value = true }
    fun dismissDelete() { confirmingDelete.value = false }
    fun confirmDelete(id: String) {
        store.delete(id)
        confirmingDelete.value = false
        selectedId.value = null   // fall back to the first remaining profile
    }

    // ---- dialog submit ----
    private var firstEntry: String? = null

    /** Called when 4 digits are entered. Routes by prompt mode. */
    fun submitPin(entered: String) {
        when (val pr = prompt.value) {
            is PinPrompt.Unlock -> {
                val p = store.byId(pr.profileId)
                if (p != null && store.verifyPin(p, entered)) {
                    if (pr.thenEnableAdult) store.setAdultContent(pr.profileId, true)
                    selectedId.value = pr.profileId
                    dismissPrompt()
                } else {
                    pinError.value = true
                }
            }
            is PinPrompt.SetPin -> {
                if (!pr.confirming) {                     // stage 1: remember, ask again
                    firstEntry = entered
                    pinError.value = false
                    prompt.value = pr.copy(confirming = true)
                } else if (entered == firstEntry) {       // stage 2: matches -> persist
                    store.setPin(pr.profileId, entered)
                    if (pr.thenEnableAdult) store.setAdultContent(pr.profileId, true)
                    dismissPrompt()
                } else {
                    pinError.value = true
                    prompt.value = pr.copy(confirming = false)
                    firstEntry = null
                }
            }
            null -> Unit
        }
    }

    private fun nameOf(id: String) = store.byId(id)?.name.orEmpty()

    private fun Profile.toRow() = ProfileRow(
        id = id, name = name, initial = initial, gradient = gradient,
        summary = summary(), type = type, restricted = isRestricted, hasPin = hasPin,
    )

    private fun Profile.toEdit(availableTypes: List<String>) = ProfileEdit(
        id = id, name = name, initial = initial, gradient = gradient, type = type,
        maxRating = maxRating, allowedTypes = allowedTypes, availableTypes = availableTypes,
        requirePin = requirePin, hasPin = hasPin, adultContent = adultContent,
    )
}

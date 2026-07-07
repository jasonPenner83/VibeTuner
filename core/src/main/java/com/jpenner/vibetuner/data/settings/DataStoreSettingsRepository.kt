package com.jpenner.vibetuner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jpenner.vibetuner.data.repository.SettingsRepository
import com.jpenner.vibetuner.data.model.SettingControl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by
preferencesDataStore(name = "aerial_settings")

/**
 * [SettingsRepository] backed by a Preferences [DataStore]. Each edited row is stored
 * as one JSON string keyed by [SettingItem.key]; defaults are never written, so the
 * file stays small. SettingControl is encoded by hand via org.json (matching the rest
 * of the data layer) rather than kotlinx-serialization.
 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    override fun overrides(): Flow<Map<String, SettingControl>> =
        dataStore.data
            // A read failure shouldn't crash settings — fall back to no overrides.
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                prefs.asMap().mapNotNull { (key, value) ->
                    (value as? String)?.let { decode(it) }?.let { key.name to it }
                }.toMap()
            }

    override suspend fun save(key: String, control: SettingControl) {
        dataStore.edit { it[stringPreferencesKey(key)] = encode(control) }
    }

    override suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        fun encode(control: SettingControl): String = JSONObject().apply {
            when (control) {
                is SettingControl.Toggle -> { put("t", "toggle"); put("on", control.on) }
                is SettingControl.Slider -> { put("t", "slider"); put("f", control.fraction.toDouble()) }
                is SettingControl.Value  -> { put("t", "value"); put("text", control.text); put("danger", control.danger) }
                is SettingControl.Info   -> { put("t", "info"); put("text", control.text) }
            }
        }.toString()

        fun decode(raw: String): SettingControl? = runCatching {
            val o = JSONObject(raw)
            when (o.getString("t")) {
                "toggle" -> SettingControl.Toggle(o.getBoolean("on"))
                "slider" -> SettingControl.Slider(o.getDouble("f").toFloat())
                "value"  -> SettingControl.Value(o.getString("text"), o.optBoolean("danger", false))
                "info"   -> SettingControl.Info(o.getString("text"))
                else     -> null
            }
        }.getOrNull()
    }
}

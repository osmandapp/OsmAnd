package net.osmand.plus.shared

import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.preferences.StringPreference
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.api.KStateChangedListener

class SettingsAPIImpl(private val app: OsmandApplication) : SettingsAPI {

	override fun registerPreference(name: String, defValue: String, global: Boolean, shared: Boolean) {
		val preference = app.settings.registerStringPreference(name, defValue)
		if (global) {
			preference.makeGlobal()
		}
		if (shared) {
			preference.makeShared()
		}
	}

	override fun getStringPreference(name: String): String? {
		val pref = app.settings.getPreference(name)
		return when (pref) {
			is StringPreference -> pref.get()
			else -> null
		}
	}

	override fun setStringPreference(name: String, value: String) {
		val pref = app.settings.getPreference(name)
		if (pref is StringPreference) pref.set(value)
	}

	override fun addStringPreferenceListener(name: String, listener: KStateChangedListener<String>) {
		val pref = app.settings.getPreference(name)
		if (pref is StringPreference) {
			pref.addListener(object : StateChangedListener<String> {
				override fun stateChanged(change: String) {
					listener.stateChanged(change)
				}
			})
		}
	}
}
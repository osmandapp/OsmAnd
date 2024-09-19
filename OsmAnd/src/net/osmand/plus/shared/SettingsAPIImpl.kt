package net.osmand.plus.shared

import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.api.KStateChangedListener

class SettingsAPIImpl(private val app: OsmandApplication) : SettingsAPI {

	override fun registerPreference(
		name: String,
		defValue: String,
		global: Boolean,
		shared: Boolean) {
		getOrRegisterPreference(name, defValue, global, shared)
	}

	private fun getOrRegisterPreference(
		name: String,
		defValue: String,
		global: Boolean = true,
		shared: Boolean = true): CommonPreference<String> {
		val preference = app.settings.registerStringPreference(name, defValue)
		if (global) {
			preference.makeGlobal()
		}
		if (shared) {
			preference.makeShared()
		}
		return preference
	}

	override fun getStringPreference(
		name: String,
		defValue: String,
		global: Boolean,
		shared: Boolean): String {
		return getOrRegisterPreference(name, defValue, global, shared).get()
	}

	override fun setStringPreference(name: String, value: String) {
		getOrRegisterPreference(name, value).set(value)
	}

	override fun <T> addPreferenceListener(name: String, listener: KStateChangedListener<T>) {
		getOrRegisterPreference(name, "").addListener { }
	}
}
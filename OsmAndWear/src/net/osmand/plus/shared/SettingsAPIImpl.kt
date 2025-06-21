package net.osmand.plus.shared

import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.preferences.FloatPreference
import net.osmand.plus.settings.backend.preferences.StringPreference
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.api.KStateChangedListener

class SettingsAPIImpl(private val app: OsmandApplication) : SettingsAPI {

	override fun registerPreference(
		name: String,
		defValue: String,
		global: Boolean,
		shared: Boolean) {
		val preference = app.settings.registerStringPreference(name, defValue)
		if (global) {
			preference.makeGlobal()
		}
		if (shared) {
			preference.makeShared()
		}
	}

	override fun getStringPreference(name: String): String? {
		return when (val pref = app.settings.getPreference(name)) {
			is StringPreference -> pref.get()
			else -> null
		}
	}

	override fun setStringPreference(name: String, value: String) {
		val pref = app.settings.getPreference(name)
		if (pref is StringPreference) pref.set(value)
	}

	override fun addStringPreferenceListener(
		name: String,
		listener: KStateChangedListener<String>) {
		val pref = app.settings.getPreference(name)
		if (pref is StringPreference) {
			pref.addListener(StateChangedListener { change -> listener.stateChanged(change) })
		}
	}

	override fun registerPreference(
		name: String,
		defValue: Float,
		global: Boolean,
		shared: Boolean) {
		val preference = app.settings.registerFloatPreference(name, defValue)
		if (global) {
			preference.makeGlobal()
		}
		if (shared) {
			preference.makeShared()
		}
	}

	override fun getFloatPreference(name: String): Float? {
		return when (val pref = app.settings.getPreference(name)) {
			is FloatPreference -> pref.get()
			else -> null
		}
	}

	override fun setFloatPreference(name: String, value: Float) {
		val pref = app.settings.getPreference(name)
		if (pref is FloatPreference) pref.set(value)
	}

	override fun addFloatPreferenceListener(name: String, listener: KStateChangedListener<Float>) {
		val pref = app.settings.getPreference(name)
		if (pref is FloatPreference) {
			pref.addListener(StateChangedListener { change -> listener.stateChanged(change) })
		}
	}
}
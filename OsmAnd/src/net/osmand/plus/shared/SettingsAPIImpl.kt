package net.osmand.plus.shared

import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.preferences.EnumStringPreference
import net.osmand.plus.settings.backend.preferences.FloatPreference
import net.osmand.plus.settings.backend.preferences.StringPreference
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.api.KStateChangedListener
import java.util.WeakHashMap
import kotlin.reflect.KClass

class SettingsAPIImpl(private val app: OsmandApplication) : SettingsAPI {

	// Store strong references to the platform listeners, bound to the lifecycle
	// of the shared listeners, to prevent them from being garbage collected
	// by the SharedPreferences WeakHashMap.
	private val listenersCache = WeakHashMap<KStateChangedListener<*>, StateChangedListener<*>>()

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
			val wrappedListener = StateChangedListener<String> { change -> listener.stateChanged(change) }
			listenersCache[listener] = wrappedListener
			pref.addListener(wrappedListener)
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
			val wrappedListener = StateChangedListener<Float> { change -> listener.stateChanged(change) }
			listenersCache[listener] = wrappedListener
			pref.addListener(wrappedListener)
		}
	}

	override fun <T : Enum<T>> registerEnumPreference(name: String, defValue: T, values: Array<T>,
	                                                  clazz: KClass<T>, global: Boolean, shared: Boolean) {
		val preference = app.settings.registerEnumStringPreference(name, defValue, values, clazz.java)
		if (global) {
			preference.makeGlobal()
		}
		if (shared) {
			preference.makeShared()
		}
	}

	override fun <T : Enum<T>> getEnumPreference(name: String): T? {
		val pref = app.settings.getPreference(name)
		if (pref is EnumStringPreference<*>) {
			@Suppress("UNCHECKED_CAST")
			return pref.get() as T
		}
		return null
	}

	override fun <T : Enum<T>> setEnumPreference(name: String, value: T) {
		val pref = app.settings.getPreference(name)
		if (pref is EnumStringPreference<*>) {
			@Suppress("UNCHECKED_CAST")
			(pref as EnumStringPreference<T>).set(value)
		}
	}

	override fun <T : Enum<T>> addEnumPreferenceListener(
		name: String,
		listener: KStateChangedListener<T>
	) {
		val pref = app.settings.getPreference(name)
		if (pref is EnumStringPreference<*>) {
			val wrappedListener = StateChangedListener<T> { change -> listener.stateChanged(change) }
			listenersCache[listener] = wrappedListener
			@Suppress("UNCHECKED_CAST")
			(pref as EnumStringPreference<T>).addListener(wrappedListener)
		}
	}
}
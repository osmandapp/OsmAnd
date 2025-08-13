package net.osmand.shared.api

interface SettingsAPI {
	fun registerPreference(name: String, defValue: String, global: Boolean, shared: Boolean)
	fun addStringPreferenceListener(name: String, listener: KStateChangedListener<String>)

	fun getStringPreference(name: String): String?
	fun setStringPreference(name: String, value: String)

	fun registerPreference(name: String, defValue: Float, global: Boolean, shared: Boolean)
	fun addFloatPreferenceListener(name: String, listener: KStateChangedListener<Float>)

	fun getFloatPreference(name: String): Float?
	fun setFloatPreference(name: String, value: Float)
}
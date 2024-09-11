package net.osmand.shared.api

interface SettingsAPI {
	fun getStringPreference(
		name: String,
		defValue: String,
		global: Boolean = true,
		shared: Boolean = true): String

	fun setStringPreference(name: String, value: String)
	fun <T> addPreferenceListener(name: String, listener: KStateChangedListener<T>)
	fun registerPreference(
		name: String,
		defValue: String,
		global: Boolean = true,
		shared: Boolean = true)
}
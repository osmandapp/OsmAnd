package net.osmand.shared.api

interface SettingsAPI {

	// world readable
	fun getPreferenceObject(key: String): Any?

	interface SettingsEditor {
		fun putString(key: String, value: String): SettingsEditor
		fun putBoolean(key: String, value: Boolean): SettingsEditor
		fun putFloat(key: String, value: Float): SettingsEditor
		fun putInt(key: String, value: Int): SettingsEditor
		fun putLong(key: String, value: Long): SettingsEditor
		fun remove(key: String): SettingsEditor
		fun clear(): SettingsEditor
		fun commit(): Boolean
	}

	fun edit(pref: Any?): SettingsEditor

	fun getString(pref: Any?, key: String, defValue: String): String
	fun getFloat(pref: Any?, key: String, defValue: Float): Float
	fun getBoolean(pref: Any?, key: String, defValue: Boolean): Boolean
	fun getInt(pref: Any?, key: String, defValue: Int): Int
	fun getLong(pref: Any?, key: String, defValue: Long): Long
	fun contains(pref: Any?, key: String): Boolean

	//TODO: Remove after settings moved to shared lib
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
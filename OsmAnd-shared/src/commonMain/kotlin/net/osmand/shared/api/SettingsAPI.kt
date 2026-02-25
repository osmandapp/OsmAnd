package net.osmand.shared.api

import kotlin.reflect.KClass

interface SettingsAPI {
	fun registerPreference(name: String, defValue: String, global: Boolean, shared: Boolean)
	fun addStringPreferenceListener(name: String, listener: KStateChangedListener<String>)

	fun getStringPreference(name: String): String?
	fun setStringPreference(name: String, value: String)

	fun registerPreference(name: String, defValue: Float, global: Boolean, shared: Boolean)
	fun addFloatPreferenceListener(name: String, listener: KStateChangedListener<Float>)

	fun getFloatPreference(name: String): Float?
	fun setFloatPreference(name: String, value: Float)

	fun <T : Enum<T>> registerEnumPreference(name: String, defValue: T, values: Array<T>,
	                                         clazz: KClass<T>, global: Boolean, shared: Boolean)
	fun <T : Enum<T>> addEnumPreferenceListener(name: String, listener: KStateChangedListener<T>)
	fun <T : Enum<T>> getEnumPreference(name: String): T?
	fun <T : Enum<T>> setEnumPreference(name: String, value: T)
}
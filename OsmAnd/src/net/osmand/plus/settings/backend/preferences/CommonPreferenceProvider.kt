package net.osmand.plus.settings.backend.preferences

interface CommonPreferenceProvider<T> {
	fun getPreference(): CommonPreference<T>
}
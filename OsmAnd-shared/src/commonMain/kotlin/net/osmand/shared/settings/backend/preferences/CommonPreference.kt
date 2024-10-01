package net.osmand.shared.settings.backend.preferences

import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.settings.backend.ApplicationMode
import net.osmand.shared.settings.backend.OsmandSettings

abstract class CommonPreference<T>(
	private val settings: OsmandSettings,
	private val id: String,
	private var defaultValue: T
) : PreferenceWithListener<T>() {

	private var cachedPreference: Any? = null
	private var cachedValue: T? = null
	private var defaultValues: MutableMap<ApplicationMode, T>? = null

	private var cache: Boolean = false
	private var global: Boolean = false
	private var shared: Boolean = false
	private var lastModifiedTimeStored: Boolean = false
	private var pluginId: String? = null

	init {
		settings.registerInternalPreference(id, this)
	}

	override fun getId(): String = id

	abstract fun getValue(prefs: Any?, defaultValue: T): T

	protected open fun getLastModifiedTime(prefs: Any?): Long {
		if (!lastModifiedTimeStored) {
			throw IllegalStateException("Setting $id is not allowed to store last modified time")
		}
		return settings.getSettingsAPI().getLong(prefs, getLastModifiedTimeId(), 0)
	}

	protected open fun setLastModifiedTime(prefs: Any?, lastModifiedTime: Long) {
		if (!lastModifiedTimeStored) {
			throw IllegalStateException("Setting $id is not allowed to store last modified time")
		}
		settings.getSettingsAPI().edit(prefs).putLong(getLastModifiedTimeId(), lastModifiedTime).commit()
	}

	protected open fun setValue(prefs: Any?, value: T): Boolean {
		if (lastModifiedTimeStored) {
			setLastModifiedTime(prefs, currentTimeMillis())
		}
		return true
	}

	abstract override fun parseString(s: String): T

	protected open fun toString(value: T?): String? {
		return value?.toString()
	}

	protected fun getSettingsAPI(): SettingsAPI = settings.getSettingsAPI()

	protected fun getApplicationMode(): ApplicationMode = settings.applicationMode

	protected fun getContext(): OsmandApplication = settings.context

	fun makeGlobal(): CommonPreference<T> {
		global = true
		return this
	}

	fun cache(): CommonPreference<T> {
		cache = true
		return this
	}

	fun makeProfile(): CommonPreference<T> {
		global = false
		return this
	}

	fun makeShared(): CommonPreference<T> {
		shared = true
		return this
	}

	fun storeLastModifiedTime(): CommonPreference<T> {
		lastModifiedTimeStored = true
		return this
	}

	fun setRelatedPlugin(plugin: OsmandPlugin?) {
		pluginId = plugin?.id
	}

	protected open fun getPreferences(): Any? {
		return settings.getPreferences(global)
	}

	fun setModeDefaultValue(mode: ApplicationMode, defValue: T) {
		if (defaultValues == null) {
			defaultValues = LinkedHashMap()
		}
		defaultValues!![mode] = defValue
	}

	override fun setModeValue(m: ApplicationMode, obj: T): Boolean {
		return if (global) {
			set(obj)
		} else {
			val profilePrefs = settings.getProfilePreferences(m)
			val changed = obj != getModeValue(m)
			val valueSaved = setValue(profilePrefs, obj)
			if (valueSaved) {
				if (changed) {
					settings.updateLastPreferencesEditTime(profilePrefs)
				}
				if (cache && cachedPreference == profilePrefs) {
					cachedValue = obj
				}
			}
			fireEvent(obj)
			valueSaved
		}
	}

	fun getProfileDefaultValue(mode: ApplicationMode?): T {
		return if (global) {
			defaultValue
		} else {
			defaultValues?.get(mode) ?: mode?.parent?.let { getProfileDefaultValue(it) } ?: defaultValue
		}
	}

	fun setDefaultValue(defaultValue: T) {
		this.defaultValue = defaultValue
	}

	fun hasDefaultValues(): Boolean = defaultValues?.isNotEmpty() ?: false

	fun hasDefaultValueForMode(mode: ApplicationMode): Boolean {
		return defaultValues?.containsKey(mode) ?: false
	}

	fun getDefaultValue(): T {
		return getProfileDefaultValue(settings.applicationMode)
	}

	override fun overrideDefaultValue(newDefaultValue: T) {
		this.defaultValue = newDefaultValue
	}

	override fun getModeValue(m: ApplicationMode): T {
		return if (global) {
			get()
		} else {
			val plugin = getRelatedPlugin()
			if (plugin != null && plugin.disablePreferences()) {
				getProfileDefaultValue(m)
			} else {
				getValue(settings.getProfilePreferences(m), getProfileDefaultValue(m))
			}
		}
	}

	override fun get(): T {
		val plugin = getRelatedPlugin()
		if (plugin != null && plugin.disablePreferences()) {
			return getDefaultValue()
		}
		if (cache && cachedValue != null && cachedPreference == getPreferences()) {
			return cachedValue as T
		}
		cachedPreference = getPreferences()
		cachedValue = getValue(cachedPreference, getDefaultValue())
		return cachedValue as T
	}

	override fun resetToDefault() {
		set(getProfileDefaultValue(settings.applicationMode))
	}

	override fun resetModeToDefault(m: ApplicationMode) {
		if (global) {
			resetToDefault()
		} else {
			setModeValue(m, getProfileDefaultValue(m))
		}
	}

	override fun set(obj: T): Boolean {
		val prefs = getPreferences()
		val changed = obj != get()
		if (setValue(prefs, obj)) {
			cachedValue = obj
			cachedPreference = prefs
			if (changed && shared) {
				settings.updateLastPreferencesEditTime(prefs)
			}
			fireEvent(obj)
			return true
		}
		return false
	}

	fun getLastModifiedTimeModeValue(mode: ApplicationMode): Long {
		return if (global) {
			getLastModifiedTime()
		} else {
			getLastModifiedTime(settings.getProfilePreferences(mode))
		}
	}

	fun getLastModifiedTime(): Long {
		return getLastModifiedTime(getPreferences())
	}

	fun setLastModifiedTime(lastModifiedTime: Long) {
		setLastModifiedTime(getPreferences(), lastModifiedTime)
	}

	override fun isSet(): Boolean {
		return settings.isSet(global, id)
	}

	override fun isSetForMode(m: ApplicationMode): Boolean {
		return settings.isSet(m, id)
	}

	fun isGlobal(): Boolean = global

	fun isShared(): Boolean = shared

	fun isLastModifiedTimeStored(): Boolean = lastModifiedTimeStored

	fun getRelatedPlugin(): OsmandPlugin? {
		return pluginId?.let { PluginsHelper.getPlugin(it) }
	}

	protected open fun getLastModifiedTimeId(): String {
		return "$id_last_modified"
	}

	override fun asString(): String {
		return toString(get()) ?: ""
	}

	override fun asStringModeValue(m: ApplicationMode): String {
		return toString(getModeValue(m)) ?: ""
	}

	override fun toString(): String {
		return id
	}
}
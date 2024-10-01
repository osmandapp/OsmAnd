package net.osmand.shared.settings.backend.preferences

import kotlinx.serialization.json.JsonObject
import net.osmand.shared.api.KStateChangedListener
import net.osmand.shared.settings.backend.ApplicationMode

interface OsmandPreference<T> {
	fun get(): T

	fun set(obj: T): Boolean

	fun setModeValue(m: ApplicationMode, obj: T): Boolean

	fun getModeValue(m: ApplicationMode): T

	fun getId(): String

	fun resetToDefault()

	fun resetModeToDefault(m: ApplicationMode)

	fun overrideDefaultValue(newDefaultValue: T)

	fun addListener(listener: KStateChangedListener<T>)

	fun removeListener(listener: KStateChangedListener<T>)

	fun isSet(): Boolean

	fun isSetForMode(m: ApplicationMode): Boolean

	fun writeToJson(json: JsonObject, appMode: ApplicationMode): Boolean

	fun readFromJson(json: JsonObject, appMode: ApplicationMode): Boolean

	fun asString(): String

	fun asStringModeValue(m: ApplicationMode): String

	fun parseString(s: String): T
}
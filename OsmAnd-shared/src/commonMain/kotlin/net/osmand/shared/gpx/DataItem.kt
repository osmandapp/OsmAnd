package net.osmand.shared.gpx

import kotlin.collections.MutableMap
import kotlin.collections.HashMap
import net.osmand.shared.gpx.GpxParameter.*
import net.osmand.shared.io.KFile

abstract class DataItem(val file: KFile) {
	protected val map: MutableMap<GpxParameter, Any?> = HashMap()

	init {
		initFileParameters()
	}

	private fun initFileParameters() {
		map[FILE_NAME] = file.name()
		map[FILE_DIR] = GpxDbUtils.getGpxFileDir(file)
		map[FILE_LAST_MODIFIED_TIME] = file.lastModified()
	}

	fun hasData(): Boolean {
		return map.isNotEmpty()
	}

	fun hasAppearanceData(): Boolean {
		return hasData() && GpxParameter.getAppearanceParameters().any { key -> map[key] != null }
	}

	abstract fun getParameters(): Map<GpxParameter, Any?>

	fun <T> requireParameter(parameter: GpxParameter): T {
		val res = getParameter(parameter) as? T
		return res ?: throw IllegalStateException("Requested parameter '$parameter' is null.")
	}

	@Suppress("UNCHECKED_CAST")
	fun <T> getParameter(parameter: GpxParameter): T? {
		var value: Any? = null
		if (map.containsKey(parameter)) {
			value = map[parameter]
		}
		if (value == null && !parameter.isAppearanceParameter()) {
			value = parameter.defaultValue
		}
		return value as? T
	}

	fun setParameter(parameter: GpxParameter, value: Any?): Boolean {
		return if (isValidValue(parameter, value)) {
			map[parameter] = value
			true
		} else {
			false
		}
	}

	open fun isValidValue(parameter: GpxParameter, value: Any?): Boolean {
		return true
	}

	override fun hashCode(): Int {
		return file.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || other !is DataItem) return false

		return file == other.file
	}
}

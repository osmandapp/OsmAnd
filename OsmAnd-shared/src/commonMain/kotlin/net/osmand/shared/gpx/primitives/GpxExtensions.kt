package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.COLOR_NAME_EXTENSION
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsWriter
import net.osmand.shared.gpx.GpxUtilities.LINE_WIDTH_EXTENSION
import net.osmand.shared.util.KAlgorithms

open class GpxExtensions {

	// [key, value, key, value ...] sorted by key - see GpxExtensionsMap
	internal var extensionsArray: Array<String?>? = null
	internal var deferredArray: Array<String?>? = null
	var extensionsWriters: MutableMap<String, GpxExtensionsWriter>? = null

	var extensions: MutableMap<String, String>?
		get() = if (extensionsArray == null) null else GpxExtensionsMap(this, false)
		set(value) {
			extensionsArray = GpxExtensionsMap.toArray(value)
		}

	var deferredExtensions: MutableMap<String, String>?
		get() = if (deferredArray == null) null else GpxExtensionsMap(this, true)
		set(value) {
			deferredArray = GpxExtensionsMap.toArray(value)
		}

	/** One extension without building the map view - for the getters called per point per frame. */
	fun getExtension(key: String): String? {
		val array = extensionsArray ?: return null
		val index = GpxExtensionsMap.indexOf(array, key)
		return if (index >= 0) array[index + 1] else null
	}

	fun getExtensionsToRead(): Map<String, String> {
		return if (extensionsArray == null) emptyMap() else GpxExtensionsMap(this, false)
	}

	fun getExtensionsToWrite(): MutableMap<String, String> {
		return GpxExtensionsMap(this, false)
	}

	fun getDeferredExtensionsToRead(): Map<String, String> {
		return if (deferredArray == null) emptyMap() else GpxExtensionsMap(this, true)
	}

	fun getDeferredExtensionsToWrite(): MutableMap<String, String> {
		return GpxExtensionsMap(this, true)
	}

	fun getExtensionsWritersToWrite(): MutableMap<String, GpxExtensionsWriter> {
		if (extensionsWriters == null) {
			extensionsWriters = LinkedHashMap()
		}
		return extensionsWriters!!
	}

	fun getExtensionsWriter(key: String?): GpxExtensionsWriter? {
		return extensionsWriters?.get(key)
	}

	fun setExtensionsWriter(key: String, extensionsWriter: GpxExtensionsWriter) {
		getExtensionsWritersToWrite()[key] = extensionsWriter
	}

	fun removeExtensionsWriter(key: String) {
		extensionsWriters?.remove(key)
	}

	fun copyExtensions(e: GpxExtensions) {
		val source = e.extensionsArray ?: return
		if (extensionsArray == null) {
			// a fresh copy, the usual case: one array instead of a map and an insert per key
			extensionsArray = source.copyOf()
		} else {
			getExtensionsToWrite().putAll(e.getExtensionsToRead().toMap())
		}
	}

	fun getColor(defColor: Int?): Int? {
		val clrValue = getExtension(COLOR_NAME_EXTENSION)
			?: getExtension("colour")
			?: getExtension("displaycolor")
			?: getExtension("displaycolour")
		return GpxUtilities.parseColor(clrValue, defColor)
	}

	fun setColor(color: Int?) {
		setColor(if (color != null) KAlgorithms.colorToString(color) else null)
	}

	fun setColor(color: String?) {
		color?.let {
			getExtensionsToWrite()[COLOR_NAME_EXTENSION] = it
		}
	}

	fun removeColor() {
		getExtensionsToWrite().remove(COLOR_NAME_EXTENSION)
	}

	fun getWidth(defaultWidth: String?) = getExtension(LINE_WIDTH_EXTENSION) ?: defaultWidth

	fun setWidth(width: String?) {
		width?.let {
			getExtensionsToWrite()[LINE_WIDTH_EXTENSION] = it
		}
	}
}

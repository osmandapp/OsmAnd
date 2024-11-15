package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.util.KAlgorithms

open class GpxExtensions {
	var extensions: MutableMap<String, String>? = null
	var deferredExtensions: MutableMap<String, String>? = null
	var extensionsWriters: MutableMap<String, GpxUtilities.GpxExtensionsWriter>? = null

	fun getExtensionsToRead(): Map<String, String> {
		return extensions ?: emptyMap()
	}

	fun getExtensionsToWrite(): MutableMap<String, String> {
		if (extensions == null) {
			extensions = LinkedHashMap()
		}
		return extensions!!
	}

	fun getDeferredExtensionsToRead(): Map<String, String> {
		return deferredExtensions ?: emptyMap()
	}

	fun getDeferredExtensionsToWrite(): MutableMap<String, String> {
		if (deferredExtensions == null) {
			deferredExtensions = LinkedHashMap()
		}
		return deferredExtensions!!
	}

	fun getExtensionsWritersToWrite(): MutableMap<String, GpxUtilities.GpxExtensionsWriter> {
		if (extensionsWriters == null) {
			extensionsWriters = LinkedHashMap()
		}
		return extensionsWriters!!
	}

	fun getExtensionsWriter(key: String?): GpxUtilities.GpxExtensionsWriter? {
		return extensionsWriters?.get(key)
	}

	fun setExtensionsWriter(key: String, extensionsWriter: GpxUtilities.GpxExtensionsWriter) {
		getExtensionsWritersToWrite()[key] = extensionsWriter
	}

	fun removeExtensionsWriter(key: String) {
		extensionsWriters?.remove(key)
	}

	fun copyExtensions(e: GpxExtensions) {
		val extensionsToRead = e.getExtensionsToRead()
		if (extensionsToRead.isNotEmpty()) {
			getExtensionsToWrite().putAll(extensionsToRead)
		}
	}

	fun getColor(defColor: Int?): Int? {
		var clrValue: String? = null
		val extensions = this.extensions
		if (extensions != null) {
			clrValue = extensions[GpxUtilities.COLOR_NAME_EXTENSION]
			if (clrValue == null) {
				clrValue = extensions["colour"]
			}
			if (clrValue == null) {
				clrValue = extensions["displaycolor"]
			}
			if (clrValue == null) {
				clrValue = extensions["displaycolour"]
			}
		}
		return GpxUtilities.parseColor(clrValue, defColor)
	}

	fun setColor(color: Int?) {
		setColor(if (color != null) KAlgorithms.colorToString(color) else null)
	}

	fun setColor(color: String?) {
		color?.let {
			getExtensionsToWrite()[GpxUtilities.COLOR_NAME_EXTENSION] = it
		}
	}

	fun removeColor() {
		getExtensionsToWrite().remove(GpxUtilities.COLOR_NAME_EXTENSION)
	}

	companion object {
		const val OBF_GPX_EXTENSION_TAG_PREFIX = "gpx_" // enlisted in poi_types.xml under name="route_track"
	}
}

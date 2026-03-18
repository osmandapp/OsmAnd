package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.COLOR_NAME_EXTENSION
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsWriter
import net.osmand.shared.gpx.GpxUtilities.LINE_WIDTH_EXTENSION
import net.osmand.shared.util.KAlgorithms

open class GpxExtensions {

	var extensions: MutableMap<String, String>? = null
	var deferredExtensions: MutableMap<String, String>? = null
	var extensionsWriters: MutableMap<String, GpxExtensionsWriter>? = null

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
		val extensionsToRead = e.getExtensionsToRead()
		if (extensionsToRead.isNotEmpty()) {
			getExtensionsToWrite().putAll(extensionsToRead.toMap())
		}
	}

	fun getColor(defColor: Int?): Int? {
		var clrValue: String? = null
		val extensions = this.extensions
		if (extensions != null) {
			clrValue = extensions[COLOR_NAME_EXTENSION]
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
			getExtensionsToWrite()[COLOR_NAME_EXTENSION] = it
		}
	}

	fun removeColor() {
		getExtensionsToWrite().remove(COLOR_NAME_EXTENSION)
	}

	fun getWidth(defaultWidth: String?) = extensions?.get(LINE_WIDTH_EXTENSION) ?: defaultWidth

	fun setWidth(width: String?) {
		width?.let {
			getExtensionsToWrite()[LINE_WIDTH_EXTENSION] = it
		}
	}
}

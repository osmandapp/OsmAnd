package net.osmand.plus.backup

import net.osmand.shared.util.KUnicode
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class LocalFileForms {

	private val nfcDirNames = ConcurrentHashMap<File, MutableMap<String, String>>()

	fun fileToWrite(file: File): File {
		val parent = file.parentFile
		if (parent == null || KUnicode.isAscii(file.path)) {
			return file
		}
		val dir = if (parent.exists()) parent else fileToWrite(parent)
		val name = file.name
		if (KUnicode.isAscii(name)) {
			return File(dir, name)
		}
		val names = nfcDirNames.computeIfAbsent(dir) { readNfcNames(it) }
		val existingName = names.putIfAbsent(KUnicode.normalizeNFC(name), name)
		return File(dir, existingName ?: name)
	}

	private fun readNfcNames(dir: File): MutableMap<String, String> {
		val result = ConcurrentHashMap<String, String>()
		dir.list()?.forEach { dirName ->
			val nfcName = KUnicode.normalizeNFC(dirName)
			val existing = result[nfcName]
			if (existing == null || File(dir, dirName).lastModified() > File(dir, existing).lastModified()) {
				result[nfcName] = dirName
			}
		}
		return result
	}
}

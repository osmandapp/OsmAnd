package net.osmand.shared.binary

import kotlin.jvm.JvmField

/**
 * One top level section of an obf file: its name and where its bytes are.
 *
 * The fields stay public so the readers can fill them in while parsing, and so the C++ core can
 * read `length` and `filePointer` off a route region through JNI.
 */
abstract class BinaryIndexPart {

	@JvmField
	var name: String? = null

	@JvmField
	var length: Long = 0

	@JvmField
	var filePointer: Long = 0

	/** Human readable section name, used when inspecting and merging obf files. */
	abstract fun getPartName(): String

	/** Number of the field this section occupies in the obf root message. */
	abstract fun getFieldNumber(): Int

	fun getLength(): Long = length

	fun setLength(length: Long) {
		this.length = length
	}

	fun getFilePointer(): Long = filePointer

	fun setFilePointer(filePointer: Long) {
		this.filePointer = filePointer
	}

	open fun getName(): String? = name

	fun setName(name: String?) {
		this.name = name
	}

	fun isBasemap(): Boolean = name?.startsWith("basemap") == true
}

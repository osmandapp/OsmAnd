package net.osmand.shared.util

/** Something that can be written to and read back from a [StringBundle] of type [T]. */
interface StringExternalizable<T : StringBundle> {

	fun writeToBundle(bundle: T)

	fun readFromBundle(bundle: T)
}

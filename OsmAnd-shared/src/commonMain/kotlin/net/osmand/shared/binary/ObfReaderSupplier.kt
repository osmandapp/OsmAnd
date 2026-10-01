package net.osmand.shared.binary

/**
 * Where an [AmenityIndexRepository] gets its reader from. It is asked on every call and answers
 * null while the file is not open, because the platform closes and reopens obf files as maps are
 * enabled, downloaded or updated.
 */
fun interface ObfReaderSupplier {

	fun getReader(): BinaryMapIndexReader?
}

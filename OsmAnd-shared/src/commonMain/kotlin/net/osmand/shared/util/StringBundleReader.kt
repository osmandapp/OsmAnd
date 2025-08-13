package net.osmand.shared.util

abstract class StringBundleReader {

	private val bundle = StringBundle()

	fun getBundle(): StringBundle {
		return bundle
	}

	abstract fun readBundle()
}

package net.osmand.shared.util

abstract class StringBundleWriter(private val bundle: StringBundle) {

	fun getBundle(): StringBundle {
		return bundle
	}

	protected abstract fun writeItem(name: String, item: StringBundle.Item<*>)

	open fun writeBundle() {
		for ((key, value) in bundle.getMap()) {
			writeItem("osmand:$key", value)
		}
	}
}

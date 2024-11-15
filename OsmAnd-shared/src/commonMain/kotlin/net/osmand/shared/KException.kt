package net.osmand.shared

class KException(message: String?, cause: Throwable?) : Exception(message, cause) {
	constructor(message: String?) : this(message, null)
}

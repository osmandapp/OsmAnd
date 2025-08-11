package net.osmand.shared.obd

data class OBDResponse(val result: IntArray) {
	companion object {
		val OK = OBDResponse(intArrayOf(1))
		val QUESTION_MARK = OBDResponse(intArrayOf(0))
		val NO_DATA = OBDResponse(intArrayOf(-2))
		val ERROR = OBDResponse(intArrayOf(-1))
		val CONNECTION_FAILURE = OBDResponse(intArrayOf(-4))
		val STOPPED = OBDResponse(intArrayOf(-3))
	}

	fun isValid(): Boolean {
		return this != OK && this != QUESTION_MARK && this != NO_DATA && this != ERROR && this != STOPPED
	}
}

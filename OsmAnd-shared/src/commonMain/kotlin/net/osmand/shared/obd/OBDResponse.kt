package net.osmand.shared.obd

data class OBDResponse(val result: IntArray){
	companion object{
		val OK = OBDResponse(intArrayOf(1))
		val QUESTION_MARK = OBDResponse(intArrayOf(1))
		val NO_DATA = OBDResponse(intArrayOf())
	}

	fun isValid(): Boolean {
		return this != OK && this != QUESTION_MARK && this != NO_DATA
	}
}

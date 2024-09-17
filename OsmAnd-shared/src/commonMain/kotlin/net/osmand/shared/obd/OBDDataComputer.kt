package net.osmand.shared.obd

object OBDDataComputer {
	private val sensorDataCache = HashMap<OBDCommand, OBDDataField?>()


	fun addDataField(command: OBDCommand, dataField: OBDDataField, timeStamp: Long) {
		sensorDataCache[command] = dataField
	}


}
package net.osmand.shared.obd

interface OBDResponseListener {
	fun onCommandResponse(command: OBDCommand, result: String)
}
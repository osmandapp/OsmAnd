package net.osmand.shared.obd

import okio.Sink
import okio.Source

interface OBDConnector {
	fun connect(): UnderlyingTransport?
	fun disconnect()
	fun onConnectionSuccess()
	fun onConnectionFailed()
}
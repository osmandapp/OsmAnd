package net.osmand.shared.obd

import okio.Sink
import okio.Source

interface OBDConnector {
	fun connect(): Pair<Source, Sink>?
	fun disconnect()
	fun onConnectionSuccess()
	fun onConnectionFailed()
}
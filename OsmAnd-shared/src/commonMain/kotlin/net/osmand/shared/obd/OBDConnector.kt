package net.osmand.shared.obd

import okio.Sink
import okio.Source

interface OBDConnector {
	fun connect(): Pair<Source, Sink>?
	fun onConnectionSuccess()
	fun onConnectionFailed()
}
package net.osmand.shared.aistracker

/**
 * Reports what the network listener is doing, so the UI can show the connection state in words
 * instead of guessing it from the arrival of AIS messages.
 */
interface AisConnectionListener {

	fun onAisConnecting()

	fun onAisConnected()

	/** Called when the socket is closed after an error. [message] is the reason, if any. */
	fun onAisConnectionFailed(message: String?)
}

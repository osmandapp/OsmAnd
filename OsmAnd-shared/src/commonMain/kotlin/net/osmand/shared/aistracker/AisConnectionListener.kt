package net.osmand.shared.aistracker

/**
 * Reports what the network listener is doing, so the UI can show the connection state in words
 * instead of guessing it from the arrival of AIS messages.
 */
interface AisConnectionListener {

	fun onAisConnecting()

	fun onAisConnected()

	/**
	 * Called when the socket is closed before [stopListener][AisMessageListener.stopListener] -
	 * after an error, a connect timeout or because the server closed the stream. [message] is the
	 * reason, if any. The listener retries after a delay and reports [onAisConnecting] again.
	 */
	fun onAisConnectionFailed(message: String?)
}

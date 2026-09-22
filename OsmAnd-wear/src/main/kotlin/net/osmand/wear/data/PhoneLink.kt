package net.osmand.wear.data

import net.osmand.wear.api.PhoneState

/** What the watch currently knows about the phone. Every screen renders from this. */
sealed interface PhoneLink {

	/** Initial state and the state while a capability lookup is in flight. */
	data object Connecting : PhoneLink

	/** No phone advertising the OsmAnd capability is reachable. */
	data object NotConnected : PhoneLink

	/** Bytes arrived but could not be read — the two APKs are out of step. */
	data object ProtocolMismatch : PhoneLink

	data class Connected(val state: PhoneState) : PhoneLink
}

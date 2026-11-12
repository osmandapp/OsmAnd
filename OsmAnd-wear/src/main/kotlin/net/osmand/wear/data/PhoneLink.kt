package net.osmand.wear.data

import androidx.compose.ui.graphics.ImageBitmap

import net.osmand.wear.api.PhoneState

/** A snapshot together with the manoeuvre arrows the phone drew for it, keyed by icon key. */
data class Snapshot(
	val state: PhoneState,
	val icons: Map<String, ImageBitmap> = emptyMap()
)

/** What the watch currently knows about the phone. Every screen renders from this. */
sealed interface PhoneLink {

	/** Initial state and the state while a capability lookup is in flight. */
	data object Connecting : PhoneLink

	/** No phone advertising the OsmAnd capability is reachable. */
	data object NotConnected : PhoneLink

	/** A payload arrived but could not be read — the two APKs are out of step. */
	data object ProtocolMismatch : PhoneLink

	data class Connected(val snapshot: Snapshot) : PhoneLink
}

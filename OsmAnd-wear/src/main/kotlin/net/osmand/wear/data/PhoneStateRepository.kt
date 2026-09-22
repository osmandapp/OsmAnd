package net.osmand.wear.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.osmand.wear.api.PhoneState
import net.osmand.wear.api.WearCodec

/**
 * Single source of truth on the watch side.
 *
 * A process-wide object rather than a ViewModel because [WearDataLayerService] delivers
 * updates outside of any Activity lifecycle — the service may well run while no screen exists.
 */
object PhoneStateRepository {

	private val _link = MutableStateFlow<PhoneLink>(PhoneLink.Connecting)
	val link: StateFlow<PhoneLink> = _link.asStateFlow()

	/** Called with the raw Data Layer payload; decoding failures are surfaced, not swallowed. */
	fun onStateBytes(bytes: ByteArray?) {
		if (bytes == null) {
			return
		}
		val state = WearCodec.decodeState(bytes)
		_link.value = if (state != null) PhoneLink.Connected(state) else PhoneLink.ProtocolMismatch
	}

	fun onState(state: PhoneState) {
		_link.value = PhoneLink.Connected(state)
	}

	fun onPhoneUnreachable() {
		// Keep showing the last known state if we ever had one: a short Bluetooth drop should
		// not blank the screen mid-manoeuvre. Only report loss when nothing was received yet.
		if (_link.value !is PhoneLink.Connected) {
			_link.value = PhoneLink.NotConnected
		}
	}

	fun onConnecting() {
		if (_link.value !is PhoneLink.Connected) {
			_link.value = PhoneLink.Connecting
		}
	}
}

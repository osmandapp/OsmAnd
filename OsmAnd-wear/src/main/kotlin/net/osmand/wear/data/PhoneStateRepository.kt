package net.osmand.wear.data

import android.util.Log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth on the watch side.
 *
 * A process-wide object rather than a ViewModel because [WearDataLayerService] delivers updates
 * outside of any Activity lifecycle — the service may well run while no screen exists.
 */
object PhoneStateRepository {

	private val _link = MutableStateFlow<PhoneLink>(PhoneLink.Connecting)
	val link: StateFlow<PhoneLink> = _link.asStateFlow()

	fun onSnapshot(snapshot: Snapshot?) {
		if (snapshot == null) {
			Log.d(TAG, "Dropping an unreadable snapshot from the phone")
			_link.value = PhoneLink.ProtocolMismatch
			return
		}
		_link.value = PhoneLink.Connected(snapshot)
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

	private const val TAG = "OsmAndWear"
}

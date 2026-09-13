package net.osmand.plus.plugins.aistracker

import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import net.osmand.plus.R

/**
 * State of the NMEA connection of the Vessel tracker (AIS) plugin.
 *
 * The colour is only a second channel for the state - [titleId] always states it in words,
 * so the row stays readable for a colour blind user and for TalkBack.
 */
enum class AisConnectionState(
	@field:StringRes @get:StringRes val titleId: Int,
	@field:StringRes @get:StringRes val actionId: Int,
	@field:AttrRes @get:AttrRes val iconColorAttr: Int,
	val filledTonalAction: Boolean
) {
	NOT_SET_UP(
		R.string.ais_connection_not_set_up,
		R.string.ais_connection_set_up,
		R.attr.colorOnSurfaceVariant,
		true),
	NOT_CONNECTED(
		R.string.ais_connection_not_connected,
		R.string.external_device_details_connect,
		R.attr.colorOnSurfaceVariant,
		true),
	CONNECTING(
		R.string.ais_connection_connecting,
		R.string.shared_string_cancel,
		R.attr.colorOnSurfaceVariant,
		false),
	CONNECTED(
		R.string.external_device_connected,
		R.string.external_device_details_disconnect,
		R.attr.colorPrimary,
		false),
	NO_DATA(
		R.string.ais_connection_no_data,
		R.string.reconnect,
		R.attr.colorWarningIcon,
		true),
	FAILED(
		R.string.ais_connection_failed,
		R.string.external_device_details_connect,
		R.attr.colorError,
		true);

	val connectionActive: Boolean
		get() = this == CONNECTING || this == CONNECTED || this == NO_DATA
}

package net.osmand.shared.data

import kotlinx.serialization.Serializable
import net.osmand.shared.util.Localization

@Serializable
data class BTDeviceInfo(val name: String, val address: String) {
	companion object {
		val UNKNOWN_DEVICE = BTDeviceInfo(Localization.getString("unknown_bt_device"), "")
	}
}
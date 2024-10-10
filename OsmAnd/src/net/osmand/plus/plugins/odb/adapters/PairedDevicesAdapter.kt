package net.osmand.plus.plugins.odb.adapters

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.util.Algorithms

class PairedDevicesAdapter(
	app: OsmandApplication, nightMode: Boolean,
	private val deviceMenuClickListener: FoundDevicesMenuListener) :
	FoundDevicesAdapter(app, nightMode, null) {
	override fun onBindViewHolder(holder: FoundDeviceViewHolder, position: Int) {
		super.onBindViewHolder(holder, position)
		val device = items[position] as BTDeviceInfo
		val connectedDevice = plugin?.getConnectedDeviceInfo()
		val isConnected = connectedDevice != null && Algorithms.stringsEqual(
			device.address,
			connectedDevice.address)
		holder.icon.setImageDrawable(app.uiUtilities.getIcon(if (isConnected) R.drawable.widget_obd_car_day else R.drawable.ic_action_car_obd2, nightMode))
		holder.menuIcon.visibility = View.VISIBLE
		holder.menuIcon.setOnClickListener { v: View -> showOptionsMenu(v, device) }
		holder.itemView.setOnClickListener { v: View? ->
			if (plugin != null /* && plugin.isDevicePaired(device)*/) {
				deviceMenuClickListener.onSettings(device)
			}
		}
		holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE
	}

	private fun showOptionsMenu(view: View, device: BTDeviceInfo) {
		val optionsMenu = PopupMenu(view.context, view)
		(optionsMenu.menu as MenuBuilder).setOptionalIconsVisible(true)
		MenuCompat.setGroupDividerEnabled(optionsMenu.menu, true)
		val connectedDevice = plugin?.getConnectedDeviceInfo()
		val isConnected = connectedDevice != null && Algorithms.stringsEqual(
			connectedDevice.address,
			device.address)
		val enableDisableItem = optionsMenu.menu.add(
			1, 1, Menu.NONE,
			if (isConnected) R.string.external_device_details_disconnect else R.string.external_device_details_connect)
		enableDisableItem.setIcon(
			app.uiUtilities.getIcon(
				if (isConnected) R.drawable.ic_action_obd2_connector_disconnect else R.drawable.ic_action_obd2_connector_disable,
				menuIconColor))
		enableDisableItem.setOnMenuItemClickListener { item: MenuItem? ->
			if (isConnected) {
				deviceMenuClickListener.onDisconnect(device)
			} else {
				deviceMenuClickListener.onConnect(device)
			}
			optionsMenu.dismiss()
			true
		}
		val settingsItem = optionsMenu.menu.add(
			1, 2, Menu.NONE,
			R.string.shared_string_settings)
		settingsItem.setIcon(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_settings_outlined,
				menuIconColor))
		settingsItem.setOnMenuItemClickListener { item: MenuItem? ->
			deviceMenuClickListener.onSettings(device)
			optionsMenu.dismiss()
			true
		}
		val renameItem = optionsMenu.menu.add(
			1, 3, Menu.NONE,
			R.string.shared_string_rename)
		renameItem.setIcon(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_edit_outlined,
				menuIconColor))
		renameItem.setOnMenuItemClickListener { item: MenuItem? ->
			deviceMenuClickListener.onRename(device)
			optionsMenu.dismiss()
			true
		}
		val forgetItem = optionsMenu.menu.add(
			2, 4, Menu.NONE,
			R.string.external_device_menu_forget)
		forgetItem.setIcon(R.drawable.ic_action_sensor_remove)
		forgetItem.setIcon(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_sensor_remove,
				menuIconColor))
		forgetItem.setOnMenuItemClickListener { item: MenuItem? ->
			deviceMenuClickListener.onForget(device)
			optionsMenu.dismiss()
			true
		}
		optionsMenu.show()
	}

	@get:ColorRes
	private val menuIconColor: Int
		private get() = if (nightMode) R.color.icon_color_secondary_light else R.color.icon_color_secondary_dark

	interface FoundDevicesMenuListener {
		fun onDisconnect(device: BTDeviceInfo)
		fun onConnect(device: BTDeviceInfo)
		fun onSettings(device: BTDeviceInfo)
		fun onRename(device: BTDeviceInfo)
		fun onForget(device: BTDeviceInfo)
	}
}
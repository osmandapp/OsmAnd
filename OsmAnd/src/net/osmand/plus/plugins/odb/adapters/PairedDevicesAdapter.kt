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
	private val deviceMenuClickListener: PairedDevicesMenuListener) :
	OBDDevicesAdapter(app, nightMode, null) {
	override fun onBindViewHolder(holder: FoundDeviceViewHolder, position: Int) {
		super.onBindViewHolder(holder, position)
		val device = items[position] as BTDeviceInfo
		val connectedDevice = plugin?.getConnectedDeviceInfo()
		val isConnected = connectedDevice != null && Algorithms.stringsEqual(
			device.address,
			connectedDevice.address)
		holder.icon.setImageDrawable(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_car_obd2,
				nightMode))
		holder.menuIcon.visibility = View.VISIBLE
		holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE
		holder.itemView.setOnClickListener { v: View? ->
			if (plugin != null) {
				deviceMenuClickListener.onConnect(device)
			}
		}
	}

	override fun showOptionsMenu(view: View, device: BTDeviceInfo) {
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
			app.uiUtilities.getIcon(R.drawable.ic_action_obd2_connector, menuIconColor))
		enableDisableItem.setOnMenuItemClickListener { item: MenuItem? ->
			deviceMenuClickListener.onConnect(device)
			optionsMenu.dismiss()
			true
		}
		optionsMenu.show()
	}


	interface PairedDevicesMenuListener {
		fun onConnect(device: BTDeviceInfo)
	}
}
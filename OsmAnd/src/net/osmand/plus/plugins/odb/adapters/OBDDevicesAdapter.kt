package net.osmand.plus.plugins.odb.adapters

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.util.Algorithms

open class OBDDevicesAdapter(
	protected val app: OsmandApplication,
	protected val nightMode: Boolean,
	private var deviceClickListener: OBDDeviceItemListener?) :
	RecyclerView.Adapter<FoundDeviceViewHolder>() {
	protected val plugin = PluginsHelper.getPlugin(
		VehicleMetricsPlugin::class.java)
	var items: List<BTDeviceInfo> = listOf()
		@SuppressLint("NotifyDataSetChanged")
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoundDeviceViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)
		val view = inflater.inflate(R.layout.item_external_device, parent, false)
		return FoundDeviceViewHolder(view)
	}

	override fun onBindViewHolder(holder: FoundDeviceViewHolder, position: Int) {
		holder.menuIcon.visibility = View.VISIBLE
		val device = items[position] as BTDeviceInfo
		val connectedDevice = plugin?.getConnectedDeviceInfo()
		val isConnected = connectedDevice?.address == device.address

		holder.name.text = device.name
		holder.icon.setImageResource(if (isConnected) R.drawable.ic_action_car_obd2 else if (nightMode) R.drawable.widget_obd_car_day else R.drawable.widget_obd_car_night)
		holder.description.visibility = View.VISIBLE
		holder.description.text = device.address
		holder.description.gravity = Gravity.CENTER_VERTICAL
		holder.itemView.setOnClickListener { _: View? ->
			deviceClickListener?.onDeviceClicked(device)
		}
		holder.icon.setImageDrawable(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_car_obd2,
				nightMode))
		holder.menuIcon.visibility = View.VISIBLE
		holder.menuIcon.setOnClickListener { v: View -> showOptionsMenu(v, device) }
		holder.itemView.setOnClickListener { _: View? ->
			if (plugin != null /* && plugin.isDevicePaired(device)*/) {
				deviceClickListener?.onSettings(device)
			}
		}
		holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE

	}

	protected open fun showOptionsMenu(view: View, device: BTDeviceInfo) {
		val optionsMenu = PopupMenu(view.context, view)
		optionsMenu.setForceShowIcon(true)
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
		enableDisableItem.setOnMenuItemClickListener { _: MenuItem? ->
			if (isConnected) {
				deviceClickListener?.onDisconnect(device)
			} else {
				deviceClickListener?.onConnect(device)
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
		settingsItem.setOnMenuItemClickListener { _: MenuItem? ->
			deviceClickListener?.onSettings(device)
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
			deviceClickListener?.onRename(device)
			optionsMenu.dismiss()
			true
		}
		val forgetItem = optionsMenu.menu.add(
			2, 4, Menu.NONE,
			R.string.external_device_menu_forget)
		forgetItem.setIcon(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_obd2_connector_disconnect,
				menuIconColor))
		forgetItem.setOnMenuItemClickListener { _: MenuItem? ->
			deviceClickListener?.onForget(device)
			optionsMenu.dismiss()
			true
		}
		optionsMenu.show()

	}

	override fun getItemCount(): Int {
		return items.size
	}

	@get:ColorRes
	protected val menuIconColor: Int
		get() = if (nightMode) R.color.icon_color_secondary_light else R.color.icon_color_secondary_dark

	interface OBDDeviceItemListener {
		fun onDeviceClicked(device: BTDeviceInfo)
		fun onDisconnect(device: BTDeviceInfo)
		fun onConnect(device: BTDeviceInfo)
		fun onSettings(device: BTDeviceInfo)
		fun onRename(device: BTDeviceInfo)
		fun onForget(device: BTDeviceInfo)
	}
}
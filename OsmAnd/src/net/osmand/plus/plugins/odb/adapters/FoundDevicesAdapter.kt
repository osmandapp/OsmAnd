package net.osmand.plus.plugins.odb.adapters

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.data.BTDeviceInfo

open class FoundDevicesAdapter(
	protected val app: OsmandApplication,
	protected val nightMode: Boolean,
	protected var deviceClickListener: DeviceClickListener?) :
	RecyclerView.Adapter<FoundDeviceViewHolder>() {
	protected val plugin = PluginsHelper.getPlugin(
		VehicleMetricsPlugin::class.java)
	var items: List<Any> = ArrayList()
		@SuppressLint("NotifyDataSetChanged")
		set(value) {
			field = value
			notifyDataSetChanged()

		}
	protected var uiUtils: UiUtilities = app.uiUtilities

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

//		DeviceType deviceType = device.getDeviceType();
		holder.name.text = device.name
		holder.icon.setImageResource(if (isConnected) R.drawable.ic_action_car_obd2 else if (nightMode) R.drawable.widget_obd_car_day else R.drawable.widget_obd_car_night)
//		int rssi = device.getRssi();
//		Drawable signalLevelIcon;
//		UiUtilities uiUtils = app.getUIUtilities();
//		if (!device.isConnected()) {
//			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_not_found, nightMode);
//		} else if (rssi > -50) {
//			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_high);
//		} else if (rssi > -70) {
//			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_middle);
//		} else {
//			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_low);
//		}
		holder.description.visibility = View.VISIBLE
		//		boolean isBle = device instanceof BLEAbstractDevice;
		val bleTextMarker = app.getString(R.string.external_device_ble)
		val antTextMarker = app.getString(R.string.external_device_ant)
		var connectedTextId: Int
		//		if (device.isConnected()) {
//			connectedTextId = R.string.external_device_connected;
//		} else {
//			connectedTextId = R.string.external_device_disconnected;
//		}
		holder.description.text = device.address
		//		holder.description.setCompoundDrawablesRelativeWithIntrinsicBounds(signalLevelIcon, null, null, null);
		holder.description.gravity = Gravity.CENTER_VERTICAL
		holder.itemView.setOnClickListener { v: View? ->
			if (deviceClickListener != null) {
				deviceClickListener!!.onDeviceClicked(device)
			}
		}
	}

	override fun getItemCount(): Int {
		return items.size
	}

//	@SuppressLint("NotifyDataSetChanged")
//	fun setItems(items: List<Any>) {
//		this.items = items
//		notifyDataSetChanged()
//	}

	interface DeviceClickListener {
		fun onDeviceClicked(device: BTDeviceInfo)
	}
}
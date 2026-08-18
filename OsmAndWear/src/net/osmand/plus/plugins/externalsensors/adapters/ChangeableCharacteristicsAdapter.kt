package net.osmand.plus.plugins.externalsensors.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty
import net.osmand.plus.plugins.externalsensors.viewholders.DeviceCharacteristicsViewHolder
import net.osmand.plus.utils.UiUtilities

class ChangeableCharacteristicsAdapter(
	private val app: OsmandApplication,
	private val nightMode: Boolean,
	private val device: AbstractDevice<*>,
	private val propertyClickedListener: OnPropertyClickedListener) :
	RecyclerView.Adapter<DeviceCharacteristicsViewHolder>() {
	private val items: List<DeviceChangeableProperty> = device.changeableProperties
	private val plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin::class.java)

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int): DeviceCharacteristicsViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)
		val view = inflater.inflate(R.layout.device_changeable_characteristic_item, parent, false)
		return DeviceCharacteristicsViewHolder(view, nightMode)
	}

	override fun onBindViewHolder(holder: DeviceCharacteristicsViewHolder, position: Int) {
		val property = items[position]
		holder.name.text = app.getString(property.displayNameResId)
		AndroidUiHelper.updateVisibility(holder.divider, true)
		val value = plugin?.getFormattedDevicePropertyValue(device, property)
		val unitsId = property.getUnitsResId(app, true)
		if (unitsId == 0) {
			holder.value.text = value
		} else {
			val unitsStr = app.getString(unitsId)
			holder.value.text =
				app.getString(R.string.ltr_or_rtl_combine_via_space, value, unitsStr)
		}
		holder.itemView.setOnClickListener { propertyClickedListener.onPropertyClicked(property) }
	}

	override fun getItemCount(): Int {
		return items.size
	}

	interface OnPropertyClickedListener {
		fun onPropertyClicked(property: DeviceChangeableProperty)
	}
}
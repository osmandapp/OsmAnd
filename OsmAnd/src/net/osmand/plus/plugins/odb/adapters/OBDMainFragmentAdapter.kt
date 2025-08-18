package net.osmand.plus.plugins.odb.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VISIBLE
import android.widget.LinearLayout.GONE
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.odb.dialogs.ForgetOBDDeviceDialog
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment.OBDDataItem
import net.osmand.plus.plugins.odb.dialogs.RenameOBDDialog
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.util.Algorithms

class OBDMainFragmentAdapter(
	private val app: OsmandApplication,
	var nightMode: Boolean,
	private var mapActivity: MapActivity,
	private val device: BTDeviceInfo,
	private val obdMainFragment: OBDMainFragment
) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val plugin = PluginsHelper.getPlugin(
		VehicleMetricsPlugin::class.java
	)
	val lastSavedValueMap = HashMap<OBDComputerWidget, Pair<String, String?>>()
	var items: List<Any> = listOf()
		@SuppressLint("NotifyDataSetChanged")
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	companion object {
		const val TITLE_VEHICLE_TYPE: Int = 0
		const val TITLE_RECEIVED_TYPE: Int = 1
		const val DATA_TYPE: Int = 2
		const val ITEM_DIVIDER: Int = 3
		const val TITLE_SETTINGS_TYPE: Int = 4
		const val NAME_ITEM_TYPE: Int = 5
		const val FORGET_SENSOR_TYPE: Int = 6
		const val UPDATE_VALUE_PAYLOAD_TYPE: Int = 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)
		val itemView: View
		return when (viewType) {
			TITLE_VEHICLE_TYPE, TITLE_RECEIVED_TYPE, TITLE_SETTINGS_TYPE -> {
				itemView = inflater.inflate(R.layout.obd_title_item, parent, false)
				TitleHolder(itemView)
			}

			DATA_TYPE -> {
				itemView = inflater.inflate(R.layout.device_characteristic_item, parent, false)
				CharacteristicHolder(itemView, plugin)
			}

			ITEM_DIVIDER -> {
				itemView = inflater.inflate(R.layout.list_item_divider, parent, false)
				DividerHolder(itemView)
			}

			NAME_ITEM_TYPE -> {
				itemView = inflater.inflate(R.layout.device_property_item, parent, false)
				DeviceNameHolder(itemView)
			}

			FORGET_SENSOR_TYPE -> {
				itemView = inflater.inflate(R.layout.forget_sensor_item, parent, false)
				ForgetSensorHolder(itemView)
			}

			else -> throw IllegalArgumentException("Unsupported view type")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]

		if (holder is TitleHolder) {
			var title: String? = null
			when (item) {
				TITLE_VEHICLE_TYPE -> {
					title = app.getString(R.string.obd_vehicle_info)
				}

				TITLE_RECEIVED_TYPE -> {
					title = app.getString(R.string.external_device_details_received_data)
				}

				TITLE_SETTINGS_TYPE -> {
					title = app.getString(R.string.shared_string_settings)
				}
			}
			holder.bindView(title)
		} else if (holder is CharacteristicHolder && item is OBDDataItem) {
			var showDivider = false
			if (items.size > position + 1 && items[position + 1] is OBDDataItem) {
				showDivider = true
			}
			holder.bindView(item, showDivider)
		} else if (holder is DividerHolder) {
			holder.bindView()
		} else if (holder is DeviceNameHolder) {
			holder.bindView()
		} else if (holder is ForgetSensorHolder) {
			holder.bindView()
		}
	}

	override fun onBindViewHolder(
		holder: RecyclerView.ViewHolder,
		position: Int,
		payloads: List<Any?>
	) {
		val item = items[position]
		if (!Algorithms.isEmpty(payloads) && payloads[0] is Int && item is OBDDataItem) {
			if (holder is CharacteristicHolder && payloads[0] == UPDATE_VALUE_PAYLOAD_TYPE) {
				var showDivider = false
				if (items.size > position + 1 && items[position + 1] is OBDDataItem) {
					showDivider = true
				}
				holder.bindViewValues(item.widget, showDivider)
			}
		} else {
			super.onBindViewHolder(holder, position, payloads)
		}
	}

	override fun getItemViewType(position: Int): Int {
		val item = items[position]
		if (item is Int) {
			return item
		} else if (item is OBDDataItem) {
			return DATA_TYPE
		}
		throw java.lang.IllegalArgumentException("Unsupported view type")
	}

	override fun getItemCount(): Int {
		return items.size
	}

	inner class DividerHolder(itemView: View) :
		RecyclerView.ViewHolder(itemView) {

		fun bindView(
		) {
			val resizableDivider = itemView.findViewById<View>(R.id.resizable_item_divider)
			val params =
				LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(mapActivity, 18f))
			resizableDivider.layoutParams = params
		}
	}

	inner class TitleHolder(itemView: View) :
		RecyclerView.ViewHolder(itemView) {
		private val textView: TextView = itemView.findViewById(R.id.title)
		fun bindView(
			title: String?
		) {
			textView.text = title
		}
	}

	inner class CharacteristicHolder(
		itemView: View,
		val plugin: VehicleMetricsPlugin?
	) :
		RecyclerView.ViewHolder(itemView) {

		private val titleView: TextView = itemView.findViewById(R.id.title)
		private val valueView: TextView = itemView.findViewById(R.id.value)
		private val unitView: TextView = itemView.findViewById(R.id.unit)
		private val divider: View = itemView.findViewById(R.id.divider)
		private val shortDivider: View = itemView.findViewById(R.id.short_divider)
		private val icon: ImageView = itemView.findViewById(R.id.icon)
		private val view: View = itemView

		fun bindView(
			dataItem: OBDDataItem,
			showDivider: Boolean,
		) {
			if (plugin == null) {
				return
			}
			val widget = dataItem.widget
			view.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode))
			titleView.text = widget.type.getTitle()
			icon.visibility = if (dataItem.dataType.icon == null) GONE else VISIBLE
			dataItem.dataType.icon?.let {
				val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
				val paintedIcon = app.uiUtilities.getPaintedIcon(dataItem.dataType.icon, iconColor)
				icon.setImageDrawable(paintedIcon)
			}
			updateValue(widget)
			AndroidUiHelper.updateVisibility(divider, false)
			AndroidUiHelper.updateVisibility(shortDivider, showDivider)
		}

		fun bindViewValues(
			widget: OBDComputerWidget,
			showDivider: Boolean
		) {
			updateValue(widget)
			AndroidUiHelper.updateVisibility(divider, false)
			AndroidUiHelper.updateVisibility(shortDivider, showDivider)
		}

		private fun updateValue(
			widget: OBDComputerWidget,
		) {
			if (plugin == null) {
				return
			}
			val value = plugin.getWidgetValue(widget)
			val unit = plugin.getWidgetUnit(widget)
			val hideUnit = value == "N/A" || value == "-"
			unitView.text = unit
			valueView.text = value
			AndroidUiHelper.updateVisibility(unitView, !hideUnit)

			val valuePair = Pair(value, unit);
			lastSavedValueMap[widget] = valuePair
		}
	}

	inner class DeviceNameHolder(
		itemView: View
	) :
		RecyclerView.ViewHolder(itemView) {

		private val propertyTextView: TextView = itemView.findViewById(R.id.property_name)
		private val divider: View = itemView.findViewById(R.id.divider)
		private val view: View = itemView

		fun bindView() {
			view.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode))
			propertyTextView.text = device.name

			view.setOnClickListener {
				RenameOBDDialog.showInstance(
					mapActivity,
					obdMainFragment,
					device
				)
			}
			AndroidUiHelper.updateVisibility(divider, true)
		}
	}

	inner class ForgetSensorHolder(
		itemView: View
	) :
		RecyclerView.ViewHolder(itemView) {
		private var forgetButton: View? = null
		private var forgetButtonText: TextView? = null
		private var forgetButtonIcon: ImageView? = null
		private val view: View = itemView

		fun bindView() {
			val developmentPlugin = PluginsHelper.getPlugin(
				OsmandDevelopmentPlugin::class.java
			)
			val isDevicePaired =
				(developmentPlugin?.isEnabled == true && app.settings.SIMULATE_OBD_DATA.get())
						|| plugin?.isPaired(mapActivity, device) == true
			forgetButton = view.findViewById(R.id.forget_device_container)
			forgetButtonText = view.findViewById(R.id.forget_btn)
			forgetButtonIcon = view.findViewById(R.id.forget_icon)
			forgetButton?.isEnabled = isDevicePaired

			var forgetDeviceTextColor = R.color.deletion_color_warning
			var forgetDeviceIconColor = R.color.deletion_color_warning
			if (!isDevicePaired) {
				forgetDeviceTextColor =
					if (nightMode) R.color.text_color_tertiary_dark else R.color.text_color_tertiary_light
				forgetDeviceIconColor =
					if (nightMode) R.color.icon_color_secondary_dark else R.color.icon_color_secondary_light
			}

			forgetButtonText?.setTextColor(
				ContextCompat.getColorStateList(
					app,
					forgetDeviceTextColor))
			forgetButtonIcon?.setImageDrawable(
				app.uiUtilities.getIcon(
					R.drawable.ic_action_sensor_remove,
					forgetDeviceIconColor))

			view.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode))
			view.setOnClickListener {
				ForgetOBDDeviceDialog.showInstance(
					mapActivity.supportFragmentManager,
					obdMainFragment,
					device.address,
					device.isBLE
				)
			}
		}
	}
}
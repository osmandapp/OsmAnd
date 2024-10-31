package net.osmand.plus.plugins.odb.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.util.Algorithms

class OBDMainFragmentAdapter(
	private val app: OsmandApplication,
	private val nightMode: Boolean,
	private var mapActivity: MapActivity
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
		const val UPDATE_VALUE_PAYLOAD_TYPE: Int = 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)
		val itemView: View
		return when (viewType) {
			TITLE_VEHICLE_TYPE, TITLE_RECEIVED_TYPE -> {
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

			else -> throw IllegalArgumentException("Unsupported view type")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]

		if (holder is TitleHolder) {
			var title: String? = null
			if (item == TITLE_VEHICLE_TYPE) {
				title = app.getString(R.string.obd_vehicle_info)
			} else if (item == TITLE_RECEIVED_TYPE) {
				title = app.getString(R.string.external_device_details_received_data)
			}
			holder.bindView(title)
		} else if (holder is CharacteristicHolder && item is OBDComputerWidget) {
			var showDivider = false
			if (items.size > position + 1 && items[position + 1] is OBDComputerWidget) {
				showDivider = true
			}
			holder.bindView(item, showDivider)
		} else if (holder is DividerHolder) {
			holder.bindView()
		}
	}

	override fun onBindViewHolder(
		holder: RecyclerView.ViewHolder,
		position: Int,
		payloads: List<Any?>
	) {
		val item = items[position]
		if (!Algorithms.isEmpty(payloads) && payloads[0] is Int && item is OBDComputerWidget) {
			if (holder is CharacteristicHolder && payloads[0] == UPDATE_VALUE_PAYLOAD_TYPE) {
				var showDivider = false
				if (items.size > position + 1 && items[position + 1] is OBDComputerWidget) {
					showDivider = true
				}
				holder.bindViewValues(item, showDivider)
			}
		} else {
			super.onBindViewHolder(holder, position, payloads)
		}
	}

	override fun getItemViewType(position: Int): Int {
		val item = items[position]
		if (item is Int) {
			return item
		} else if (item is OBDComputerWidget) {
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
		private val view: View = itemView

		fun bindView(
			widget: OBDComputerWidget,
			showDivider: Boolean,
		) {
			if (plugin == null) {
				return
			}

			view.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode))
			itemView.findViewById<TextView>(R.id.title).text = widget.type.getTitle()
			titleView.text = widget.type.getTitle()
			updateValue(widget)

			AndroidUiHelper.updateVisibility(divider, showDivider)
		}

		fun bindViewValues(
			widget: OBDComputerWidget,
			showDivider: Boolean
		) {
			updateValue(widget)
			AndroidUiHelper.updateVisibility(divider, showDivider)
		}

		private fun updateValue(
			widget: OBDComputerWidget,
		) {
			if (plugin == null) {
				return
			}
			val value = plugin.getWidgetValue(widget)
			val unit = plugin.getWidgetUnit(widget)
			valueView.text = value
			unitView.text = unit
			val valuePair = Pair(value, unit);
			lastSavedValueMap[widget] = valuePair
		}
	}
}
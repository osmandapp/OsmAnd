package net.osmand.plus.plugins.odb.dialogs

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.data.BTDeviceInfo

class VehicleMetricsSettingsFragment : BaseOsmAndFragment() {
	private var plugin: VehicleMetricsPlugin? = null

	var recyclerView: RecyclerView? = null
	var adapter: DeviceAdapter? = null
	var emptyView: View? = null

	private var items = listOf<BTDeviceInfo>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		plugin = PluginsHelper.getPlugin(
			VehicleMetricsPlugin::class.java)
	}

	@SuppressLint("MissingPermission")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		updateNightMode()
		val view =
			themedInflater.inflate(R.layout.fragment_vehicle_metrics_settings, container, false)
		items = plugin!!.getPairedOBDDevicesList(requireActivity())
		checkIfEmpty()
		setupToolbar(view)
		setupDeviceList(view)
		updateConnectBtn(view)
		//		setupUI(view);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)
		return view
	}

	private fun updateConnectBtn(view: View) {
		val btn = view.findViewById<Button>(R.id.connectBtn)
		val status = view.findViewById<TextView>(R.id.status)
		if (plugin?.isConnected() == true) {
			btn.setText("Disconnect")
			status.setText("Connected to ${plugin?.getConnectedDeviceName()}")
		} else {
			btn.setText("Connect")
			status.setText("Disconnected")
		}
		btn.setOnClickListener { onConnectBtnClicked() }
	}

	private fun onConnectBtnClicked() {
		plugin?.let {
			if (it.getConnectedDeviceName() != null) {
				it.disconnect()
			} else {
				val selectedPosition = adapter?.selectedPosition
				selectedPosition?.let { position ->
					if (position != RecyclerView.NO_POSITION) {
						Thread {
							it.connectToObd(requireActivity(), items[position])
							view?.let { Handler(Looper.getMainLooper()).post { updateConnectBtn(it) } }
						}.start()
					}
				}
			}
		}
	}

	private fun setupDeviceList(view: View) {
		emptyView = view.findViewById(R.id.empty_view)
		recyclerView = view.findViewById(R.id.available_devices_list)
		adapter = DeviceAdapter(items)
		recyclerView?.layoutManager = LinearLayoutManager(activity)
		recyclerView?.adapter = adapter
	}

	private fun setupToolbar(view: View) {
		val appbar = view.findViewById<View>(R.id.appbar)
		ViewCompat.setElevation(appbar, elevation)
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode))
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { v: View? -> requireActivity().onBackPressed() }
	}

	private val elevation = 5.0f

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.status_bar_main_light
	}

	override fun getContentStatusBarNightMode(): Boolean {
		return true
	}

	private fun checkIfEmpty() {
		if (items.isEmpty()) {
			recyclerView?.visibility = View.GONE
			emptyView?.visibility = View.VISIBLE
		} else {
			recyclerView?.visibility = View.VISIBLE
			emptyView?.visibility = View.GONE
		}
	}

	class DeviceAdapter(private val items: List<BTDeviceInfo>) :
		RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
		var selectedPosition: Int = RecyclerView.NO_POSITION

		class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			val textView: TextView = itemView.findViewById(android.R.id.text1)
			val subTextView: TextView = itemView.findViewById(android.R.id.text2)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(android.R.layout.simple_list_item_2, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.textView.text = items[position].name
			holder.subTextView.text = items[position].address
			holder.textView.setTextColor(Color.BLACK)
			holder.subTextView.setTextColor(Color.GRAY)
			if (position == selectedPosition) {
				holder.itemView.setBackgroundColor(Color.LTGRAY)
			} else {
				holder.itemView.setBackgroundColor(Color.TRANSPARENT)
			}
			holder.itemView.setOnClickListener {
				val previousPosition = selectedPosition
				selectedPosition = holder.adapterPosition
				notifyItemChanged(previousPosition)
				notifyItemChanged(selectedPosition)
			}
		}

		override fun getItemCount() = items.size
	}
}
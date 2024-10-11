package net.osmand.plus.plugins.odb.dialogs

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget

class OBDMainFragment : OBDDevicesBaseFragment() {

	private val handler = Handler(Looper.getMainLooper())
	private val widgets = mutableListOf<OBDComputerWidget>()
	private val dataRows = mutableListOf<View>()

	private var updateEnable = false

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.activity_background_color_light
	}

	override val layoutId: Int
		get() = R.layout.fragment_obd_devices_list

	override fun setupToolbar(view: View) {
		view.findViewById<ImageButton>(R.id.close_button).apply {
			setOnClickListener {
				requireActivity().onBackPressed()
			}
			setImageResource(AndroidUtils.getNavigationIconResId(context))
		}
	}

	override fun setupUI(view: View) {
		setupConnectionState(view)
		setupVehicleInfo(view)
		setupReceivedData(view)
	}

	private fun setupConnectionState(view: View) {

	}

	private fun setupVehicleInfo(view: View) {
		view.findViewById<ViewGroup>(R.id.info_container)?.apply {
			removeAllViews()
			createWidgetView(OBDTypeWidget.VIN, this)
		}
	}

	private fun setupReceivedData(view: View) {
		view.findViewById<ViewGroup>(R.id.data_container)?.apply {
			removeAllViews()
			OBDTypeWidget.entries.forEach {
				if (it != OBDTypeWidget.VIN) {
					createWidgetView(it, this)
				}
			}
		}
	}

	private fun createWidgetView(widgetType: OBDTypeWidget, container: ViewGroup) {
		val widget = OBDDataComputer.registerWidget(widgetType, 0)
		widgets.add(widget)

		val itemView = themedInflater.inflate(R.layout.device_characteristic_item, container, false)
		itemView.findViewById<TextView>(R.id.title).text = widget.type.getTitle()
		itemView.tag = widget
		container.addView(itemView)
		dataRows.add(itemView)
	}

	override fun onStart() {
		super.onStart()
		updateWidgets()
	}

	private fun updateWidgetsData(view: View, widget: OBDComputerWidget) {
		val value = if (widget.computeValue() == null) " - " else widget.computeValue().toString()
		view.findViewById<TextView>(R.id.value).apply {
			if (text.toString() != value) {
				text = value
			}
		}
	}

	private fun updateWidgets() {
		dataRows.forEach {
			if (it.tag is OBDComputerWidget) {
				app.runInUIThread { updateWidgetsData(it, it.tag as OBDComputerWidget) }
			}
		}
		handler.postDelayed({ updateWidgets() }, 100)
	}

	override fun onResume() {
		super.onResume()
		updateEnable = true
		startHandler()
	}

	private fun startHandler() {
		handler.postDelayed({
			if (view != null && updateEnable) {
				updateWidgets()
				startHandler()
			}
		}, UPDATE_INTERVAL_MILLIS)
	}

	override fun onPause() {
		super.onPause()
		updateEnable = false
	}

	override fun onDestroy() {
		super.onDestroy()
		widgets.forEach { OBDDataComputer.removeWidget(it) }
	}

	companion object {
		const val TAG: String = "VehicleMetricsFragment"
		const val UPDATE_INTERVAL_MILLIS = 100L

		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDMainFragment()
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}
}
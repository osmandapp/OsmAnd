package net.osmand.plus.plugins.odb.dialogs

//import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_LITERS
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_DISTANCE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_TYPE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.RPM
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.SPEED
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.VIN

class OBDMainFragment : BaseOsmAndFragment() {
	private var deviceName: EditText? = null
	private var fuelLeftDistBtn: Button? = null
	private var fuelLeftLitersBtn: Button? = null
	private var fuelConsumptionBtn: Button? = null
	private var rpmBtn: Button? = null
	private var speedBtn: Button? = null
	private var tempIntakeBtn: Button? = null
	private var tempCoolantBtn: Button? = null
	private var batteryVoltageBtn: Button? = null
	private var fuelTypeBtn: Button? = null
	private var fuelLeftPersBtn: Button? = null
	private var tempAmbientBtn: Button? = null
	private var vinBtn: Button? = null
	private var fuelLeftDistResp: EditText? = null
	private var fuelLeftLitersResp: EditText? = null
	private var fuelConsumptionResp: EditText? = null
	private var rpmResp: EditText? = null
	private var speedResp: EditText? = null
	private var tempIntakeResp: EditText? = null
	private var tempCoolantResp: EditText? = null
	private var vinResp: EditText? = null
	private var fuelTypeResp: EditText? = null
	private var fuelLeftPersResp: EditText? = null
	private var tempAmbientResp: EditText? = null
	private var batteryVoltageResp: EditText? = null

	protected var plugin: VehicleMetricsPlugin? = null
	private val handler = Handler(Looper.getMainLooper())

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		updateNightMode()
		val view = themedInflater.inflate(getLayoutId(), container, false)
		setupUI(view)
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)
		return view
	}

	fun getLayoutId(): Int {
		return R.layout.fragment_obd_main
	}

	private fun setupToolbar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode))
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { v: View? -> requireActivity().onBackPressed() }
	}

	private fun setupUI(view: View) {
		setupToolbar(view)
		fuelLeftDistResp = view.findViewById(R.id.resp1)
		fuelLeftLitersResp = view.findViewById(R.id.resp2)
		fuelConsumptionResp = view.findViewById(R.id.resp3)
		rpmResp = view.findViewById(R.id.resp4)
		speedResp = view.findViewById(R.id.resp5)
		tempIntakeResp = view.findViewById(R.id.resp6)
		tempCoolantResp = view.findViewById(R.id.resp7)
		vinResp = view.findViewById(R.id.resp12)
		batteryVoltageResp = view.findViewById(R.id.resp8)
		fuelTypeResp = view.findViewById(R.id.resp9)
		fuelLeftPersResp = view.findViewById(R.id.resp10)
		tempAmbientResp = view.findViewById(R.id.resp11)
		deviceName = view.findViewById(R.id.device_name)
		fuelLeftDistBtn = view.findViewById(R.id.btn1)
		fuelLeftLitersBtn = view.findViewById(R.id.btn2)
		fuelConsumptionBtn = view.findViewById(R.id.btn3)
		rpmBtn = view.findViewById(R.id.btn4)
		speedBtn = view.findViewById(R.id.btn5)
		tempIntakeBtn = view.findViewById(R.id.btn6)
		tempCoolantBtn = view.findViewById(R.id.btn7)
		batteryVoltageBtn = view.findViewById(R.id.btn8)
		fuelTypeBtn = view.findViewById(R.id.btn9)
		fuelLeftPersBtn = view.findViewById(R.id.btn10)
		tempAmbientBtn = view.findViewById(R.id.btn11)
		vinBtn = view.findViewById(R.id.btn12)
		fuelLeftDistBtn?.text = "fuel left distance"
		fuelLeftLitersBtn?.text = "fuel left liters"
		fuelConsumptionBtn?.text = "fuel consumption"
		rpmBtn?.text = "rpm"
		speedBtn?.text = "speed"
		tempIntakeBtn?.text = "intake air temp"
		tempCoolantBtn?.text = "engine coolant temp"
		batteryVoltageBtn?.text = "battery voltage"
		fuelTypeBtn?.text = "fuel type"
		fuelLeftPersBtn?.text = "fuel left percent"
		tempAmbientBtn?.text = "ambient air temperature"
		vinBtn?.text = "vin"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		plugin = PluginsHelper.getPlugin(
			VehicleMetricsPlugin::class.java)


	}

	companion object {
		val TAG = OBDMainFragment::class.java.simpleName
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

	private val widgets = mutableListOf<OBDDataComputer.OBDComputerWidget>()

	override fun onStart() {
		super.onStart()
		OBDDataComputer.OBDTypeWidget.entries.forEach {
			widgets.add(OBDDataComputer.registerWidget(it, 0))
		}
		updateWidgets()
	}

	private fun updateWidgets() {
		widgets.forEach {
			updateWidgetsData(
				it.type,
				if (it.computeValue() == null) " - " else it.computeValue().toString())
		}
		handler.postDelayed({ updateWidgets() }, 100)
	}

	override fun onStop() {
		super.onStop()
		widgets.forEach { OBDDataComputer.removeWidget(it) }
	}

	private fun updateWidgetsData(widgetType: OBDDataComputer.OBDTypeWidget, result: String) {
		app.runInUIThread {
			when (widgetType) {
				SPEED -> updateWidgetData(speedResp, result)
				RPM -> updateWidgetData(rpmResp, result)
				FUEL_LEFT_DISTANCE -> updateWidgetData(fuelLeftDistResp, result)
//				FUEL_LEFT_LITERS -> updateWidgetData(fuelLeftLitersResp, result)
				FUEL_LEFT_PERCENT -> updateWidgetData(fuelLeftPersResp, result)
				FUEL_CONSUMPTION_RATE -> updateWidgetData(fuelConsumptionResp, result)
				TEMPERATURE_INTAKE -> updateWidgetData(tempIntakeResp, result)
				TEMPERATURE_AMBIENT -> updateWidgetData(tempAmbientResp, result)
				BATTERY_VOLTAGE -> updateWidgetData(batteryVoltageResp, result)
				FUEL_TYPE -> updateWidgetData(fuelTypeResp, result)
				TEMPERATURE_COOLANT -> updateWidgetData(tempCoolantResp, result)
				VIN -> updateWidgetData(vinResp, result)
			}
		}

	}

	private fun updateWidgetData(field: EditText?, result: String) {
		if (field?.text.toString() != result) {
			field?.setText(result)
		}
	}
}
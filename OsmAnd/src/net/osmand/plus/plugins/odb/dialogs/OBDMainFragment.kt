package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_DISTANCE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_LITERS
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_TYPE
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.RPM
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.SPEED
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE

class OBDMainFragment : BaseOsmAndFragment() {
	private var appBar: AppBarLayout? = null
	private var responsesView: EditText? = null
	private var deviceName: EditText? = null
	private var connectBtn: Button? = null
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
	private var fuelLeftDistResp: EditText? = null
	private var fuelLeftLitersResp: EditText? = null
	private var fuelConsumptionResp: EditText? = null
	private var rpmResp: EditText? = null
	private var speedResp: EditText? = null
	private var tempIntakeResp: EditText? = null
	private var tempCoolantResp: EditText? = null
	private var resp8: EditText? = null
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

	private fun setupUI(view: View) {
		appBar = view.findViewById(R.id.appbar)
		responsesView = view.findViewById(R.id.responses)
		fuelLeftDistResp = view.findViewById(R.id.resp1)
		fuelLeftLitersResp = view.findViewById(R.id.resp2)
		fuelConsumptionResp = view.findViewById(R.id.resp3)
		rpmResp = view.findViewById(R.id.resp4)
		speedResp = view.findViewById(R.id.resp5)
		tempIntakeResp = view.findViewById(R.id.resp6)
		tempCoolantResp = view.findViewById(R.id.resp7)
		batteryVoltageResp = view.findViewById(R.id.resp8)
		fuelTypeResp = view.findViewById(R.id.resp9)
		fuelLeftPersResp = view.findViewById(R.id.resp10)
		tempAmbientResp = view.findViewById(R.id.resp11)
		deviceName = view.findViewById(R.id.device_name)
		connectBtn = view.findViewById(R.id.connect)
		connectBtn?.setOnClickListener {
//			val devName = it
			val devName = "Android-Vlink"
			Thread {
				if (plugin?.connectToObd(requireActivity(), devName) == true) {
					addToResponses("Connected to ${plugin?.getConnectedDeviceName()}")
				} else {
					addToResponses("Can't connect to $devName")
				}
			}.start()
		}
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
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		plugin = PluginsHelper.getPlugin(
			VehicleMetricsPlugin::class.java)


	}


	override fun onDestroyView() {
		super.onDestroyView()
		appBar = null
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

	fun addToResponses(msg: String) {
		app.runInUIThread {
			responsesView?.setText("${responsesView?.text}\n***$msg")
		}
	}

	private val widgets = mutableListOf<OBDDataComputer.OBDComputerWidget>()

	override fun onStart() {
		super.onStart()
		OBDDataComputer.OBDTypeWidget.entries.forEach {
			widgets.add(OBDDataComputer.registerWidget(it, 0) )
		}
		updateWidgets()
	}

	private fun updateWidgets(){
		widgets.forEach {
			updateWidgetsData(it.type, if(it.computeValue() == null) " - " else it.computeValue().toString())
		}
		handler.postDelayed({updateWidgets()}, 100)
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
				FUEL_LEFT_LITERS -> updateWidgetData(fuelLeftLitersResp, result)
				FUEL_LEFT_PERCENT -> updateWidgetData(fuelLeftPersResp, result)
				FUEL_CONSUMPTION_RATE -> updateWidgetData(fuelConsumptionResp, result)
				TEMPERATURE_INTAKE -> updateWidgetData(tempIntakeResp, result)
				TEMPERATURE_AMBIENT -> updateWidgetData(tempAmbientResp, result)
				BATTERY_VOLTAGE -> updateWidgetData(batteryVoltageResp, result)
				FUEL_TYPE -> updateWidgetData(fuelTypeResp, result)
				TEMPERATURE_COOLANT -> updateWidgetData(tempCoolantResp, result)
			}
		}

	}

	private fun updateWidgetData(field: EditText?, result: String) {
		if (field?.text.toString() != result) {
			field?.setText(result)
		}
	}
}
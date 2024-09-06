package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.odb.OBDPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.obd.OBDCommand

class OBDMainFragment : BaseOsmAndFragment(), OBDPlugin.OBDResponseListener {
	private var appBar: AppBarLayout? = null
	private var responsesView: EditText? = null
	private var deviceName: EditText? = null
	private var connectBtn: Button? = null
	private var commandBtn1: Button? = null
	private var commandBtn2: Button? = null
	private var commandBtn3: Button? = null
	private var commandBtn4: Button? = null
	private var commandBtn5: Button? = null
	private var commandBtn6: Button? = null
	private var commandBtn7: Button? = null
	private var commandBtn8: Button? = null
	private var commandBtn9: Button? = null
	private var commandBtn10: Button? = null


	protected var plugin: OBDPlugin? = null

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
		deviceName = view.findViewById(R.id.device_name)
		connectBtn = view.findViewById(R.id.connect)
		connectBtn?.setOnClickListener {
			plugin?.connectToObd(requireActivity(), deviceName?.text.toString())
			plugin?.connectToObd(requireActivity(), "Android-Vlink")
			addToResponses("Connected to ${plugin?.getConnectedDeviceName()}")
		}
		commandBtn1 = view.findViewById(R.id.btn1)
		commandBtn2 = view.findViewById(R.id.btn2)
		commandBtn3 = view.findViewById(R.id.btn3)
		commandBtn4 = view.findViewById(R.id.btn4)
		commandBtn5 = view.findViewById(R.id.btn5)
		commandBtn6 = view.findViewById(R.id.btn6)
		commandBtn7 = view.findViewById(R.id.btn7)
		commandBtn8 = view.findViewById(R.id.btn8)
		commandBtn9 = view.findViewById(R.id.btn9)
		commandBtn10 = view.findViewById(R.id.btn10)
		commandBtn1?.text = OBDCommand.OBD_SUPPORTED_LIST1_COMMAND.name
		commandBtn2?.text = OBDCommand.OBD_SUPPORTED_LIST2_COMMAND.name
		commandBtn3?.text = OBDCommand.OBD_SUPPORTED_LIST3_COMMAND.name
		commandBtn4?.text = OBDCommand.OBD_RPM_COMMAND.name
		commandBtn5?.text = OBDCommand.OBD_SPEED_COMMAND.name
		commandBtn6?.text = OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND.name
		commandBtn7?.text = OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND.name
		commandBtn8?.text = OBDCommand.OBD_FUEL_LEVEL_COMMAND.name
		commandBtn9?.text = OBDCommand.OBD_FUEL_TYPE_COMMAND.name
		commandBtn10?.text = OBDCommand.OBD_INTAKE_AIR_TEMP_COMMAND.name

		commandBtn1?.setOnClickListener { sendCommand(OBDCommand.OBD_SUPPORTED_LIST1_COMMAND) }
		commandBtn2?.setOnClickListener { sendCommand(OBDCommand.OBD_SUPPORTED_LIST2_COMMAND) }
		commandBtn3?.setOnClickListener { sendCommand(OBDCommand.OBD_SUPPORTED_LIST3_COMMAND) }
		commandBtn4?.setOnClickListener { sendCommand(OBDCommand.OBD_RPM_COMMAND) }
		commandBtn5?.setOnClickListener { sendCommand(OBDCommand.OBD_SPEED_COMMAND) }
		commandBtn6?.setOnClickListener { sendCommand(OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND) }
		commandBtn7?.setOnClickListener { sendCommand(OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND) }
		commandBtn8?.setOnClickListener { sendCommand(OBDCommand.OBD_FUEL_LEVEL_COMMAND) }
		commandBtn9?.setOnClickListener { sendCommand(OBDCommand.OBD_FUEL_TYPE_COMMAND) }
		commandBtn10?.setOnClickListener { sendCommand(OBDCommand.OBD_INTAKE_AIR_TEMP_COMMAND) }


	}

	private fun sendCommand(command: OBDCommand) {
		plugin?.sendCommand(command)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		plugin = PluginsHelper.getPlugin(
			OBDPlugin::class.java)


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

	override fun onCommandResponse(command: OBDCommand, rawResponse: String, result: String) {
		val responsesTex = responsesView?.text.toString()
		addToResponses("Raw - $rawResponse; Result - $result")
	}

	fun addToResponses(msg: String) {
		app.runInUIThread {
			responsesView?.setText("${responsesView?.text}\n***$msg")
		}

	}
}
package net.osmand.plus.plugins.aistracker.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.R
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP
import net.osmand.plus.widgets.ui.ScreenDescriptionView
import net.osmand.util.Algorithms

/**
 * Connection screen. The values are applied on Save, not while typing: leaving the screen with
 * unsaved changes discards them, the form is short and re-entering it is cheap.
 */
class AisConnectionFragment : AisBaseFragment() {

	private lateinit var toggleGroup: MaterialButtonToggleGroup
	private lateinit var description: ScreenDescriptionView
	private lateinit var hostLayout: TextInputLayout
	private lateinit var hostEdit: TextInputEditText
	private lateinit var portLayout: TextInputLayout
	private lateinit var portEdit: TextInputEditText
	private lateinit var saveButton: MaterialButton

	private var protocol = AIS_NMEA_PROTOCOL_TCP
	private var validationRequested = false

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_ais_connection, container, false)
		setupToolbar(view, R.string.ais_connection, R.string.ais_reset_connection_settings) {
			resetConnectionSettings()
		}

		toggleGroup = view.findViewById(R.id.protocol_toggle)
		description = view.findViewById(R.id.screen_description)
		hostLayout = view.findViewById(R.id.host_layout)
		hostEdit = view.findViewById(R.id.host_edit)
		portLayout = view.findViewById(R.id.port_layout)
		portEdit = view.findViewById(R.id.port_edit)
		saveButton = view.findViewById(R.id.save_button)

		protocol = plugin.AIS_NMEA_PROTOCOL.get()
		toggleGroup.check(if (protocol == AIS_NMEA_PROTOCOL_TCP) R.id.protocol_tcp else R.id.protocol_udp)
		toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
			if (isChecked) {
				protocol = if (checkedId == R.id.protocol_tcp) AIS_NMEA_PROTOCOL_TCP else AIS_NMEA_PROTOCOL_UDP
				updateProtocol()
			}
		}

		val watcher = object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
			override fun afterTextChanged(s: Editable?) = updateSaveState()
		}
		hostEdit.addTextChangedListener(watcher)
		portEdit.addTextChangedListener(watcher)
		hostEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateHost() }
		portEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validatePort() }

		saveButton.setOnClickListener { save() }

		updateProtocol()
		return view
	}

	private fun updateProtocol() {
		val tcp = protocol == AIS_NMEA_PROTOCOL_TCP
		description.setText(
			if (tcp) R.string.ais_connection_tcp_desc else R.string.ais_connection_udp_desc)
		hostLayout.visibility = if (tcp) View.VISIBLE else View.GONE
		hostEdit.setText(plugin.AIS_NMEA_IP_ADDRESS.get())
		portEdit.setText(
			(if (tcp) plugin.AIS_NMEA_TCP_PORT.get() else plugin.AIS_NMEA_UDP_PORT.get()).toString())
		validationRequested = false
		hostLayout.error = null
		portLayout.error = null
		updateSaveState()
	}

	private fun currentHost(): String = hostEdit.text?.toString()?.trim().orEmpty()

	private fun currentPort(): String = portEdit.text?.toString()?.trim().orEmpty()

	private fun isHostValid(): Boolean =
		protocol != AIS_NMEA_PROTOCOL_TCP || !Algorithms.isEmpty(currentHost())

	private fun isPortValid(): Boolean {
		val port = currentPort().toIntOrNull() ?: return false
		return port in 1..65535
	}

	private fun validateHost() {
		hostLayout.error = if (isHostValid()) null else getString(R.string.ais_error_empty_host)
	}

	private fun validatePort() {
		portLayout.error = when {
			currentPort().isEmpty() -> getString(R.string.ais_error_empty_port)
			!isPortValid() -> getString(R.string.ais_error_port_range)
			else -> null
		}
	}

	private fun hasChanges(): Boolean {
		val tcp = protocol == AIS_NMEA_PROTOCOL_TCP
		if (protocol != plugin.AIS_NMEA_PROTOCOL.get()) {
			return true
		}
		if (tcp && currentHost() != plugin.AIS_NMEA_IP_ADDRESS.get()) {
			return true
		}
		val savedPort = if (tcp) plugin.AIS_NMEA_TCP_PORT.get() else plugin.AIS_NMEA_UDP_PORT.get()
		return currentPort() != savedPort.toString()
	}

	private fun updateSaveState() {
		saveButton.isEnabled = isHostValid() && isPortValid() && hasChanges()
		if (validationRequested) {
			validateHost()
			validatePort()
		}
	}

	private fun save() {
		validationRequested = true
		validateHost()
		validatePort()
		if (!isHostValid() || !isPortValid()) {
			return
		}
		val tcp = protocol == AIS_NMEA_PROTOCOL_TCP
		plugin.AIS_NMEA_PROTOCOL.set(protocol)
		if (tcp) {
			plugin.AIS_NMEA_IP_ADDRESS.set(currentHost())
			plugin.AIS_NMEA_TCP_PORT.set(currentPort().toInt())
		} else {
			plugin.AIS_NMEA_UDP_PORT.set(currentPort().toInt())
		}
		/* an open socket would no longer match what the screen shows, so it is dropped and
		 * reopened with the new values */
		if (plugin.connectionState.connectionActive) {
			plugin.connect()
		}
		requireActivity().onBackPressed()
	}

	private fun resetConnectionSettings() {
		plugin.AIS_NMEA_PROTOCOL.resetToDefault()
		plugin.AIS_NMEA_IP_ADDRESS.resetToDefault()
		plugin.AIS_NMEA_TCP_PORT.resetToDefault()
		plugin.AIS_NMEA_UDP_PORT.resetToDefault()
		protocol = plugin.AIS_NMEA_PROTOCOL.get()
		toggleGroup.check(
			if (protocol == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) R.id.protocol_tcp else R.id.protocol_udp)
		updateProtocol()
	}
}

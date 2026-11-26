package net.osmand.aiconnector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import net.osmand.aiconnector.ConnectorSettings.Access
import net.osmand.aiconnector.ConnectorSettings.RunMode

/**
 * An AI assistant on a computer uses OsmAnd on this phone: the MCP server switch, how the computer
 * connects, the settings to copy into the assistant and an example request. Which OsmAnd app and its
 * access are chosen above, on the main screen.
 */
class ComputerFragment : Fragment() {

	companion object {
		private const val CLIENT = "client"
	}

	private lateinit var mainSwitch: MaterialSwitch
	private lateinit var status: TextView
	private lateinit var keepScreenBanner: MaterialCardView
	private lateinit var runModeFooter: TextView
	private lateinit var bridgeRow: View
	private lateinit var bridgeSwitch: MaterialSwitch
	private lateinit var keepScreenOnSwitch: MaterialSwitch
	private lateinit var accessFooter: TextView
	private lateinit var setupCommand: TextView
	private lateinit var copySetup: Button
	private lateinit var clientWhere: TextView
	private lateinit var clientConfig: TextView
	private var client = AssistantClient.CLAUDE_CODE

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		inflater.inflate(R.layout.fragment_computer, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val ctx = requireContext()
		mainSwitch = view.findViewById(R.id.main_switch)
		status = view.findViewById(R.id.status)
		view.findViewById<View>(R.id.main_card).setOnClickListener {
			val enabled = !ConnectorSettings.isEnabled(ctx)
			ConnectorSettings.setEnabled(ctx, enabled)
			if (enabled) ConnectorService.start(ctx) else ConnectorService.stop(ctx)
			refreshLater()
		}
		keepScreenBanner = view.findViewById(R.id.keep_screen_banner)
		runModeFooter = view.findViewById(R.id.run_mode_footer)
		val runToggle: MaterialButtonToggleGroup = view.findViewById(R.id.run_mode_toggle)
		runToggle.check(if (isScreenMode()) R.id.run_while_open else R.id.run_background)
		runToggle.addOnButtonCheckedListener { _, id, checked ->
			if (checked) {
				ConnectorSettings.setRunMode(ctx, if (id == R.id.run_while_open) RunMode.SCREEN else RunMode.BACKGROUND)
				restartServer()
			}
		}
		keepScreenOnSwitch = view.findViewById(R.id.keep_screen_on_switch)
		view.findViewById<View>(R.id.keep_screen_on_row).setOnClickListener {
			ConnectorSettings.setKeepScreenOn(ctx, !ConnectorSettings.keepScreenOn(ctx))
			refresh()
		}
		bridgeRow = view.findViewById(R.id.bridge_row)
		bridgeSwitch = view.findViewById(R.id.bridge_switch)
		bridgeRow.setOnClickListener {
			ConnectorSettings.setUseBridge(ctx, !ConnectorSettings.useBridge(ctx))
			refresh()
		}
		val toggle: MaterialButtonToggleGroup = view.findViewById(R.id.access_toggle)
		toggle.check(if (ConnectorSettings.access(ctx) == Access.WIFI) R.id.access_wifi else R.id.access_usb)
		toggle.addOnButtonCheckedListener { _, id, checked ->
			if (checked) {
				ConnectorSettings.setAccess(ctx, if (id == R.id.access_wifi) Access.WIFI else Access.USB)
				restartServer()
			}
		}
		accessFooter = view.findViewById(R.id.access_footer)
		setupCommand = view.findViewById(R.id.setup_command)
		copySetup = view.findViewById(R.id.copy_setup)
		copySetup.setOnClickListener { copy(setupCommand.text) }

		clientWhere = view.findViewById(R.id.client_where)
		clientConfig = view.findViewById(R.id.client_config)
		client = AssistantClient.entries.firstOrNull { it.name == prefs().getString(CLIENT, null) } ?: client
		val chips: ChipGroup = view.findViewById(R.id.clients)
		for (c in AssistantClient.entries) {
			chips.addView(Chip(ctx).apply {
				id = View.generateViewId()
				setText(c.titleId)
				isCheckable = true
				isChecked = c == client
				setOnClickListener {
					client = c
					prefs().edit().putString(CLIENT, c.name).apply()
					refresh()
				}
			})
		}
		view.findViewById<Button>(R.id.copy).setOnClickListener { copy(clientConfig.text) }
		view.findViewById<Button>(R.id.copy_example).setOnClickListener { copy(getString(R.string.try_example)) }
	}

	/* screen mode: no foreground service, the connector runs while this panel is on the screen */
	override fun onStart() {
		super.onStart()
		if (ConnectorSettings.isEnabled(requireContext())) ConnectorService.start(requireContext())
	}

	override fun onStop() {
		super.onStop()
		if (isScreenMode() && !requireActivity().isChangingConfigurations) ConnectorService.stop(requireContext())
		requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	override fun onResume() {
		super.onResume()
		refreshLater()
	}

	private fun isScreenMode() = ConnectorSettings.runMode(requireContext()) == RunMode.SCREEN

	private fun refresh() {
		if (!isAdded) return
		val ctx = requireContext()
		val enabled = ConnectorSettings.isEnabled(ctx)
		val runningUrl = ConnectorService.runningUrl
		mainSwitch.isChecked = enabled
		status.text = when {
			!enabled -> getString(R.string.status_stopped)
			runningUrl != null -> getString(R.string.status_running, runningUrl.removeSuffix("/mcp").removePrefix("http://"))
			else -> getString(R.string.status_cannot_start, ConnectorSettings.PORT)
		}

		val screenMode = isScreenMode()
		runModeFooter.setText(if (screenMode) R.string.run_screen_footer else R.string.run_background_footer)
		keepScreenBanner.visibility = if (enabled && screenMode) View.VISIBLE else View.GONE
		val window = requireActivity().window
		if (enabled && screenMode) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}

		val usb = ConnectorSettings.access(ctx) == Access.USB
		val bridge = ConnectorSettings.usesBridge(ctx)
		bridgeRow.visibility = if (usb) View.GONE else View.VISIBLE
		bridgeSwitch.isChecked = ConnectorSettings.useBridge(ctx)
		keepScreenOnSwitch.isChecked = ConnectorSettings.keepScreenOn(ctx)
		accessFooter.setText(when {
			usb -> R.string.usb_footer
			bridge -> R.string.wifi_footer
			else -> R.string.wifi_direct_footer
		})
		// USB: forward the port; Wi-Fi with the bridge: download it; Wi-Fi without: nothing to run
		val command = when {
			usb -> "adb forward tcp:${ConnectorSettings.PORT} tcp:${ConnectorSettings.PORT}"
			bridge -> ConnectorSettings.bridgeDownload()
			else -> null
		}
		setupCommand.text = command
		setupCommand.visibility = if (command != null) View.VISIBLE else View.GONE
		copySetup.visibility = setupCommand.visibility

		clientWhere.setText(client.whereId)
		clientConfig.text = client.config(bridge, ConnectorSettings.url(ctx), ConnectorSettings.token(ctx))
	}

	private fun refreshLater() {
		mainSwitch.postDelayed({ refresh() }, 400)
	}

	/** The server serves the OsmAnd app chosen on the main screen: restart it after a change. */
	fun restartServer() {
		val ctx = context ?: return
		if (ConnectorSettings.isEnabled(ctx)) ConnectorService.start(ctx)
		refreshLater()
	}

	fun confirmResetToken() {
		MaterialAlertDialogBuilder(requireContext())
			.setTitle(R.string.reset_token)
			.setMessage(R.string.reset_token_message)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.reset) { _, _ ->
				ConnectorSettings.newToken(requireContext())
				restartServer()
			}
			.show()
	}

	private fun copy(text: CharSequence) {
		val ctx = requireContext()
		ctx.getSystemService(ClipboardManager::class.java)
			.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
		Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
	}

	private fun prefs() = requireContext().getSharedPreferences("connector_ui", Context.MODE_PRIVATE)
}

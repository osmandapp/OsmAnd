package net.osmand.aiconnector

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import net.osmand.aiconnector.ConnectorSettings.Access
import net.osmand.aiconnector.ConnectorSettings.RunMode
import org.json.JSONObject

/**
 * The connector setup in four steps: OsmAnd access, how the computer connects, the settings to copy
 * into the AI assistant, and an example request.
 */
class MainActivity : AppCompatActivity() {

	companion object {
		private const val REQUEST_PERMISSIONS = "net.osmand.aidl.REQUEST_PERMISSIONS"

		/** Groups the assistant asks for: OsmAnd preselects only the safe ones, the user turns on the rest. */
		private val REQUESTED_GROUPS = arrayOf("map", "search", "location", "navigation", "favorites",
			"tracks_view", "tracks_edit", "recording", "screen")
		private const val CLIENT = "client"
		private const val ACCESS_STATUS = "access_status"
	}

	private lateinit var mainSwitch: MaterialSwitch
	private lateinit var status: TextView
	private lateinit var keepScreenBanner: MaterialCardView
	private lateinit var runModeFooter: TextView
	private lateinit var osmandName: TextView
	private lateinit var accessStatus: TextView
	private lateinit var accessFooter: TextView
	private lateinit var bridgeRow: View
	private lateinit var bridgeSwitch: MaterialSwitch
	private lateinit var setupCommand: TextView
	private lateinit var copySetup: Button
	private lateinit var clientWhere: TextView
	private lateinit var clientConfig: TextView
	private var client = AssistantClient.CLAUDE_CODE

	private val permissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		val granted = it.data?.getStringArrayExtra("granted_groups")
		setAccessStatus(if (it.resultCode == RESULT_OK && !granted.isNullOrEmpty()) {
			getString(R.string.access_granted, granted.joinToString())
		} else {
			getString(R.string.access_denied)
		})
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
		}
		bindViews()
		if (ConnectorSettings.isEnabled(this) && !isScreenMode()) {
			ConnectorService.start(this)
		}
	}

	override fun onStart() {
		super.onStart()
		if (ConnectorSettings.isEnabled(this) && isScreenMode()) {
			ConnectorService.start(this)
		}
	}

	/* screen mode: no foreground service, so the connector stops with the screen */
	override fun onStop() {
		super.onStop()
		if (isScreenMode() && !isChangingConfigurations) {
			ConnectorService.stop(this)
		}
	}

	private fun isScreenMode() = ConnectorSettings.runMode(this) == RunMode.SCREEN

	override fun onResume() {
		super.onResume()
		refreshLater()
	}

	private fun bindViews() {
		val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
		toolbar.inflateMenu(R.menu.main)
		toolbar.setOnMenuItemClickListener {
			if (it.itemId == R.id.reset_token) confirmResetToken()
			true
		}

		mainSwitch = findViewById(R.id.main_switch)
		status = findViewById(R.id.status)
		findViewById<View>(R.id.main_card).setOnClickListener {
			val enabled = !ConnectorSettings.isEnabled(this)
			ConnectorSettings.setEnabled(this, enabled)
			if (enabled) ConnectorService.start(this) else ConnectorService.stop(this)
			refreshLater()
		}
		keepScreenBanner = findViewById(R.id.keep_screen_banner)
		runModeFooter = findViewById(R.id.run_mode_footer)
		val runToggle: MaterialButtonToggleGroup = findViewById(R.id.run_mode_toggle)
		runToggle.check(if (isScreenMode()) R.id.run_while_open else R.id.run_background)
		runToggle.addOnButtonCheckedListener { _, id, checked ->
			if (checked) {
				ConnectorSettings.setRunMode(this, if (id == R.id.run_while_open) RunMode.SCREEN else RunMode.BACKGROUND)
				restartServer()
			}
		}

		osmandName = findViewById(R.id.osmand_name)
		findViewById<View>(R.id.osmand_row).setOnClickListener { chooseOsmand() }
		accessStatus = findViewById(R.id.access_status)
		accessStatus.text = prefs().getString(ACCESS_STATUS, null) ?: getString(R.string.access_not_checked)
		findViewById<Button>(R.id.allow_access).setOnClickListener { requestAccess() }
		findViewById<Button>(R.id.check).setOnClickListener { checkConnection() }

		accessFooter = findViewById(R.id.access_footer)
		setupCommand = findViewById(R.id.setup_command)
		copySetup = findViewById(R.id.copy_setup)
		copySetup.setOnClickListener { copy(setupCommand.text) }
		bridgeRow = findViewById(R.id.bridge_row)
		bridgeSwitch = findViewById(R.id.bridge_switch)
		bridgeRow.setOnClickListener {
			ConnectorSettings.setUseBridge(this, !ConnectorSettings.useBridge(this))
			refresh()
		}
		val toggle: MaterialButtonToggleGroup = findViewById(R.id.access_toggle)
		toggle.check(if (ConnectorSettings.access(this) == Access.WIFI) R.id.access_wifi else R.id.access_usb)
		toggle.addOnButtonCheckedListener { _, id, checked ->
			if (checked) {
				ConnectorSettings.setAccess(this, if (id == R.id.access_wifi) Access.WIFI else Access.USB)
				restartServer()
			}
		}

		clientWhere = findViewById(R.id.client_where)
		clientConfig = findViewById(R.id.client_config)
		client = AssistantClient.entries.firstOrNull { it.name == prefs().getString(CLIENT, null) } ?: client
		val chips: ChipGroup = findViewById(R.id.clients)
		for (c in AssistantClient.entries) {
			val chip = Chip(this).apply {
				id = View.generateViewId()
				setText(c.titleId)
				isCheckable = true
				isChecked = c == client
				setOnClickListener {
					client = c
					prefs().edit().putString(CLIENT, c.name).apply()
					refresh()
				}
			}
			chips.addView(chip)
		}
		findViewById<Button>(R.id.copy).setOnClickListener { copy(clientConfig.text) }
		findViewById<Button>(R.id.copy_example).setOnClickListener { copy(getString(R.string.try_example)) }
	}

	private fun refresh() {
		val enabled = ConnectorSettings.isEnabled(this)
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
		if (enabled && screenMode) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}

		val pack = ConnectorSettings.osmandPackage(this)
		osmandName.text = if (ConnectorSettings.installedOsmAnd(this).isEmpty()) {
			getString(R.string.osmand_not_installed)
		} else {
			"${ConnectorSettings.OSMAND_PACKAGES[pack]} ($pack)"
		}

		val usb = ConnectorSettings.access(this) == Access.USB
		val bridge = ConnectorSettings.usesBridge(this)
		bridgeRow.visibility = if (usb) View.GONE else View.VISIBLE
		bridgeSwitch.isChecked = ConnectorSettings.useBridge(this)
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
		clientConfig.text = client.config(bridge, ConnectorSettings.url(this), ConnectorSettings.token(this))
	}

	private fun refreshLater() {
		mainSwitch.postDelayed({ refresh() }, 400)
	}

	private fun restartServer() {
		if (ConnectorSettings.isEnabled(this)) {
			ConnectorService.start(this)
		}
		refreshLater()
	}

	private fun chooseOsmand() {
		val installed = ConnectorSettings.installedOsmAnd(this)
		if (installed.size < 2) return
		val names = installed.map { "${ConnectorSettings.OSMAND_PACKAGES[it]} ($it)" }.toTypedArray()
		val current = installed.indexOf(ConnectorSettings.osmandPackage(this))
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.choose_osmand)
			.setSingleChoiceItems(names, current) { dialog, which ->
				ConnectorSettings.setOsmandPackage(this, installed[which])
				setAccessStatus(getString(R.string.access_not_checked))
				dialog.dismiss()
				restartServer()
			}
			.show()
	}

	private fun requestAccess() {
		val intent = Intent(REQUEST_PERMISSIONS)
			.setPackage(ConnectorSettings.osmandPackage(this))
			.putExtra("groups", REQUESTED_GROUPS)
		try {
			permissionRequest.launch(intent)
		} catch (e: ActivityNotFoundException) {
			setAccessStatus(getString(R.string.access_old_osmand))
		}
	}

	private fun checkConnection() {
		accessStatus.text = "…"
		Thread {
			val message = try {
				val tools = ConnectorService.tools ?: throw IllegalStateException(getString(R.string.status_stopped))
				val res = tools.call("osmand_status", JSONObject())
				val text = res.getJSONArray("content").getJSONObject(0).getString("text")
				if (res.optBoolean("isError")) {
					getString(R.string.check_failed, text)
				} else {
					val o = JSONObject(text)
					if (o.optBoolean("enabled")) {
						getString(R.string.check_ok, o.optString("package"))
					} else {
						getString(R.string.access_denied)
					}
				}
			} catch (e: Exception) {
				getString(R.string.check_failed, e.message ?: e.toString())
			}
			runOnUiThread { setAccessStatus(message) }
		}.start()
	}

	private fun copy(text: CharSequence) {
		val clipboard = getSystemService(ClipboardManager::class.java)
		clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
		Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
	}

	private fun confirmResetToken() {
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.reset_token)
			.setMessage(R.string.reset_token_message)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.reset) { _, _ ->
				ConnectorSettings.newToken(this)
				restartServer()
			}
			.show()
	}

	private fun setAccessStatus(text: String) {
		accessStatus.text = text
		prefs().edit().putString(ACCESS_STATUS, text).apply()
	}

	private fun prefs() = getSharedPreferences("connector_ui", Context.MODE_PRIVATE)
}

package net.osmand.plus.plugins.aistracker.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import net.osmand.plus.R
import net.osmand.plus.plugins.aistracker.AisConnectionState
import net.osmand.plus.plugins.aistracker.AisFormatter
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

/**
 * Root screen of the Vessel tracker (AIS) plugin.
 *
 * This is the first OsmAnd screen built with Material 3 components. Everything on it is a plain
 * view hierarchy - the groups are segmented lists whose row shape depends on the position of the
 * row, so hiding a row keeps the shape of the group correct.
 */
class AisTrackerSettingsFragment : AisBaseFragment(),
	AisTrackerPlugin.AisConnectionStateListener {

	private lateinit var connectionRow: View
	private lateinit var connectionIcon: View
	private lateinit var connectionProgress: CircularProgressIndicator
	private lateinit var connectionAction: MaterialButton
	private lateinit var nmeaLocationRow: SettingRow
	private lateinit var backgroundRow: SettingRow
	private lateinit var objectsVisibilityRow: SettingRow
	private lateinit var collisionWarningRow: SettingRow
	private lateinit var mmsiRow: SettingRow
	private lateinit var showOnMapRow: SettingRow
	private lateinit var connectionFooter: GroupFooterView
	private lateinit var vesselsHeader: View
	private lateinit var connectionGroup: ViewGroup
	private lateinit var vesselsGroup: ViewGroup
	private lateinit var myVesselGroup: ViewGroup

	private val handler = Handler(Looper.getMainLooper())
	private val refreshTask = object : Runnable {
		override fun run() {
			updateConnectionCard()
			handler.postDelayed(this, VESSEL_COUNT_REFRESH_INTERVAL_MS)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_ais_settings, container, false)
		setupToolbar(view, R.string.plugin_ais_tracker_name, R.string.ais_reset_plugin_settings) {
			confirmResetPluginSettings()
		}
		bindViews(view)
		return view
	}

	private fun bindViews(view: View) {
		connectionGroup = view.findViewById(R.id.connection_group)
		vesselsGroup = view.findViewById(R.id.vessels_group)
		myVesselGroup = view.findViewById(R.id.my_vessel_group)
		connectionFooter = view.findViewById(R.id.connection_footer)
		vesselsHeader = view.findViewById(R.id.vessels_header)

		connectionRow = view.findViewById(R.id.connection_row)
		connectionIcon = connectionRow.findViewById(R.id.icon)
		connectionProgress = connectionRow.findViewById(R.id.progress)
		connectionAction = connectionRow.findViewById(R.id.connection_action)
		connectionRow.findViewById<View>(R.id.connection_content).setOnClickListener {
			showFragment(AisConnectionFragment())
		}

		nmeaLocationRow = SettingRow(view.findViewById(R.id.nmea_location_row))
		nmeaLocationRow.setIcon(null)
		nmeaLocationRow.setTitle(R.string.ais_use_nmea_location)
		nmeaLocationRow.setSubtitle(getString(R.string.ais_use_nmea_location_desc))
		nmeaLocationRow.setOnClickListener {
			plugin.AIS_USE_NMEA_LOCATION.setModeValue(appMode, !plugin.AIS_USE_NMEA_LOCATION.getModeValue(appMode))
			updateContent()
		}

		backgroundRow = SettingRow(view.findViewById(R.id.background_row))
		backgroundRow.setIcon(null)
		backgroundRow.setTitle(R.string.ais_run_in_background)
		backgroundRow.setSubtitle(getString(R.string.ais_run_in_background_desc))
		backgroundRow.setOnClickListener {
			plugin.AIS_RECEIVE_IN_BACKGROUND.setModeValue(appMode, !plugin.AIS_RECEIVE_IN_BACKGROUND.getModeValue(appMode))
			updateContent()
		}

		objectsVisibilityRow = SettingRow(view.findViewById(R.id.objects_visibility_row))
		objectsVisibilityRow.setIcon(R.drawable.ic_action_ais_object_visibility,
			themedColor(view, R.attr.colorOnSurfaceVariant))
		objectsVisibilityRow.setTitle(R.string.ais_objects_visibility)
		objectsVisibilityRow.hideSwitch()
		objectsVisibilityRow.setOnClickListener { showFragment(AisObjectsVisibilityFragment()) }

		collisionWarningRow = SettingRow(view.findViewById(R.id.collision_warning_row))
		collisionWarningRow.setIcon(R.drawable.ic_action_ais_cpa,
			themedColor(view, R.attr.colorOnSurfaceVariant))
		collisionWarningRow.setTitle(R.string.ais_collision_warning)
		collisionWarningRow.hideSwitch()
		collisionWarningRow.setOnClickListener { showFragment(AisCollisionWarningFragment()) }

		mmsiRow = SettingRow(view.findViewById(R.id.mmsi_row))
		mmsiRow.setIcon(null)
		mmsiRow.setTitle(R.string.ais_mmsi)
		mmsiRow.hideSwitch()
		mmsiRow.setOnClickListener {
			AisMmsiDialog.show(this) { updateContent() }
		}

		showOnMapRow = SettingRow(view.findViewById(R.id.show_on_map_row))
		showOnMapRow.setIcon(null)
		showOnMapRow.setTitle(R.string.ais_show_on_the_map)
		showOnMapRow.setOnClickListener {
			plugin.AIS_DISPLAY_OWN_POSITION.setModeValue(appMode, !plugin.AIS_DISPLAY_OWN_POSITION.getModeValue(appMode))
			plugin.layer?.refreshOwnObjectVisibility()
			updateContent()
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		updateContent()
	}

	override fun onResume() {
		super.onResume()
		plugin.addConnectionStateListener(this)
		updateContent()
		handler.postDelayed(refreshTask, VESSEL_COUNT_REFRESH_INTERVAL_MS)
	}

	override fun onPause() {
		super.onPause()
		plugin.removeConnectionStateListener(this)
		handler.removeCallbacks(refreshTask)
	}

	override fun onAisConnectionStateChanged(state: AisConnectionState) {
		if (isAdded) {
			updateConnectionCard()
		}
	}

	private fun updateContent() {
		updateConnectionCard()

		nmeaLocationRow.setChecked(plugin.AIS_USE_NMEA_LOCATION.getModeValue(appMode))
		backgroundRow.setChecked(plugin.AIS_RECEIVE_IN_BACKGROUND.getModeValue(appMode))

		objectsVisibilityRow.setSubtitle(
			getString(R.string.ais_objects_visibility_summary,
				AisFormatter.formatMinutes(osmandApp, plugin.AIS_OBJ_LOST_TIMEOUT.getModeValue(appMode))))

		collisionWarningRow.setSubtitle(
			if (plugin.AIS_CPA_ENABLED.getModeValue(appMode)) {
				getString(R.string.ltr_or_rtl_combine_via_bold_point,
					AisFormatter.formatMinutes(osmandApp, plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode)),
					AisFormatter.formatNauticalMiles(osmandApp, plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode), appMode))
			} else {
				getString(R.string.shared_string_off)
			})

		val mmsi = plugin.AIS_OWN_MMSI.getModeValue(appMode)
		val mmsiSet = mmsi != 0
		mmsiRow.setSubtitle(
			if (mmsiSet) AisFormatter.formatMmsi(mmsi) else getString(R.string.ais_mmsi_not_set))

		showOnMapRow.setChecked(mmsiSet && plugin.AIS_DISPLAY_OWN_POSITION.getModeValue(appMode))
		showOnMapRow.setRowEnabled(mmsiSet)
		view?.let {
			showOnMapRow.setTitleColor(themedColor(it,
				if (mmsiSet) R.attr.colorOnSurface else R.attr.colorOnSurfaceDisabled))
		}

		SegmentedList.apply(connectionGroup)
		SegmentedList.apply(vesselsGroup)
		SegmentedList.apply(myVesselGroup)
	}

	private fun updateConnectionCard() {
		val context = view?.context ?: return
		val state = plugin.getConnectionState(appMode)

		/* the connection row carries an action button, so it has its own layout and is bound here
		 * instead of through SettingRow */
		val subtitle: TextView = connectionRow.findViewById(R.id.subtitle)
		val status = getString(state.titleId)
		/* only the status word takes the state colour, the protocol stays in the default one */
		subtitle.text = UiUtilities.createColorSpannable(
			getString(R.string.ltr_or_rtl_combine_via_bold_point, status, protocolName()),
			AndroidUtils.getColorFromAttr(context, subtitleColorAttr(state)), status)

		val connecting = state == AisConnectionState.CONNECTING
		connectionIcon.visibility = if (connecting) View.GONE else View.VISIBLE
		connectionProgress.visibility = if (connecting) View.VISIBLE else View.GONE
		if (!connecting) {
			(connectionIcon as ImageView).setColorFilter(
				AndroidUtils.getColorFromAttr(context, state.iconColorAttr))
		}

		connectionAction.setText(state.actionId)
		applyButtonStyle(connectionAction, state.filledTonalAction)
		connectionAction.setOnClickListener { onConnectionAction(state) }
		/* only the active profile has a socket: for another profile there is nothing to connect
		 * or disconnect, only a connection to set up */
		val actionShown = plugin.isActiveMode(appMode) || state == AisConnectionState.NOT_SET_UP
		connectionAction.visibility = if (actionShown) View.VISIBLE else View.GONE
		/* the bottom padding of the row belongs to the button */
		connectionRow.setPaddingRelative(connectionRow.paddingStart, connectionRow.paddingTop,
			connectionRow.paddingEnd, if (actionShown) dp(16) else 0)

		val connected = state == AisConnectionState.CONNECTED
		connectionFooter.visibility = if (connected) View.VISIBLE else View.GONE
		/* the 16dp gap to the next group comes from the footer while it is shown; without the
		 * footer the header of the next group has to bring it itself */
		(vesselsHeader.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
			if (connected) 0 else resources.getDimensionPixelSize(R.dimen.ui_group_gap)
		vesselsHeader.requestLayout()
		if (connected) {
			val vessels = getString(R.string.ais_vessels_on_the_map, plugin.vesselsCount)
			/* the position half is only meaningful while the stream is the location source */
			connectionFooter.setText(
				if (plugin.AIS_USE_NMEA_LOCATION.getModeValue(appMode)) {
					getString(R.string.ltr_or_rtl_combine_via_bold_point, vessels,
						getString(
							if (plugin.isReceivingPosition) R.string.ais_receiving_position_data
							else R.string.ais_no_position_data))
				} else {
					vessels
				})
		}
	}

	private fun onConnectionAction(state: AisConnectionState) {
		when (state) {
			AisConnectionState.NOT_SET_UP -> showFragment(AisConnectionFragment())
			AisConnectionState.CONNECTING, AisConnectionState.CONNECTED -> plugin.disconnect(appMode)
			else -> plugin.connect(appMode)
		}
		updateConnectionCard()
	}

	private fun applyButtonStyle(button: MaterialButton, filledTonal: Boolean) {
		val context = button.context
		if (filledTonal) {
			button.setBackgroundColor(
				AndroidUtils.getColorFromAttr(context, R.attr.colorPrimaryContainer))
			button.strokeWidth = 0
		} else {
			button.setBackgroundColor(ContextCompat.getColor(context, R.color.color_transparent))
			button.strokeWidth = dp(1)
			button.strokeColor = ColorStateList.valueOf(
				AndroidUtils.getColorFromAttr(context, R.attr.colorOutline))
		}
		button.setTextColor(AndroidUtils.getColorFromAttr(context, R.attr.colorPrimary))
	}

	private fun subtitleColorAttr(state: AisConnectionState): Int = when (state) {
		AisConnectionState.FAILED -> R.attr.colorError
		AisConnectionState.NO_DATA -> R.attr.colorWarningIcon
		else -> R.attr.colorOnSurfaceVariant
	}

	private fun protocolName(): String = getString(
		if (plugin.AIS_NMEA_PROTOCOL.getModeValue(appMode) == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) {
			R.string.ais_protocol_tcp
		} else {
			R.string.ais_protocol_udp
		})

	/**
	 * Resets the host and the port as well, which are not cheap to type again - unlike the
	 * sliders of the other screens, which reset without asking.
	 */
	private fun confirmResetPluginSettings() {
		MaterialAlertDialogBuilder(materialContext())
			.setTitle(R.string.ais_reset_plugin_settings)
			.setMessage(getString(R.string.reset_confirmation_descr,
				getString(R.string.shared_string_reset)))
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_reset) { _, _ -> resetPluginSettings() }
			.show()
	}

	private fun resetPluginSettings() {
		plugin.resetSettings(appMode)
		updateContent()
	}

	companion object {
		/** The vessel count changes constantly - refreshing it faster than this is just noise. */
		private const val VESSEL_COUNT_REFRESH_INTERVAL_MS = 5000L
	}
}

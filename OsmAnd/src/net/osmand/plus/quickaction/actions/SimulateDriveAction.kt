package net.osmand.plus.quickaction.actions

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin
import net.osmand.plus.quickaction.QuickAction
import net.osmand.plus.quickaction.QuickActionIds.SIMULATE_DRIVE_ACTION_ID
import net.osmand.plus.quickaction.QuickActionType
import net.osmand.plus.utils.UiUtilities

/**
 * Turns on and off the manually driven location simulation. Available with the enabled
 * OsmAnd development plugin only.
 */
class SimulateDriveAction : QuickAction {

	constructor() : super(TYPE)

	constructor(quickAction: QuickAction) : super(quickAction)

	override fun execute(mapActivity: MapActivity, params: Bundle?) {
		if (PluginsHelper.getActivePlugin(OsmandDevelopmentPlugin::class.java) == null) {
			return
		}
		val app = mapActivity.app
		val simulation = app.locationProvider.locationSimulation
		if (simulation.isDriveSimulationActive) {
			simulation.stopDriveSimulation()
		} else {
			simulation.startDriveSimulation(null)
		}
	}

	override fun drawUI(parent: ViewGroup, mapActivity: MapActivity, nightMode: Boolean) {
		val view = UiUtilities.inflate(parent.context, nightMode, R.layout.quick_action_with_text, parent, false)
		view.findViewById<TextView>(R.id.text).setText(R.string.drive_simulation_descr)
		parent.addView(view)
	}

	override fun isActionWithSlash(app: OsmandApplication): Boolean {
		return app.locationProvider.locationSimulation.isDriveSimulationActive
	}

	override fun getActionText(app: OsmandApplication): String {
		return app.getString(if (app.locationProvider.locationSimulation.isDriveSimulationActive) {
			R.string.shared_string_control_stop
		} else {
			R.string.drive_simulation
		})
	}

	companion object {
		@JvmField
		val TYPE: QuickActionType = QuickActionType(SIMULATE_DRIVE_ACTION_ID,
			"simulate.drive", SimulateDriveAction::class.java)
			.nameRes(R.string.drive_simulation)
			.iconRes(R.drawable.ic_action_simulate_position)
			.category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.quick_action_verb_turn_on_off)
			.nonEditable()
	}
}

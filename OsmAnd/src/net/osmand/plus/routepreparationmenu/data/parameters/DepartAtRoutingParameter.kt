package net.osmand.plus.routepreparationmenu.data.parameters

import net.osmand.plus.R

/**
 * The "Depart at" item of the public transport options. It is only offered while a transport route
 * provider is registered, because the OBF based planner has no timetable to apply it to.
 */
class DepartAtRoutingParameter : LocalRoutingParameter(null) {

	override fun getKey(): String = KEY

	override fun canAddToRouteMenu(): Boolean = false

	override fun getActiveIconId(): Int = R.drawable.ic_action_time_of_day

	override fun getDisabledIconId(): Int = R.drawable.ic_action_time_of_day

	companion object {
		const val KEY = "depart_at_routing_parameter"
	}
}

package net.osmand.plus.plugins.drone.menu

import net.osmand.NativeLibrary.RenderedObject
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.controllers.RenderedObjectMenuController
import net.osmand.plus.plugins.drone.DroneZoneRenderedObjectParser
import net.osmand.plus.plugins.drone.model.RawValue

class DroneZoneMenuController(
	private val activity: MapActivity,
	pointDescription: PointDescription,
	renderedObject: RenderedObject,
) : RenderedObjectMenuController(activity, pointDescription, renderedObject) {
	private val zone = DroneZoneRenderedObjectParser.parse(renderedObject, preferredMapLangLC)

	override fun getTypeStr(): String = activity.getString(R.string.drone_zones)

	override fun getRightIconId(): Int = R.drawable.ic_action_layers

	override fun addPlainMenuItems(typeStr: String?, pointDescription: PointDescription?, latLon: LatLon?) {
		row(R.drawable.ic_action_info_dark, R.string.drone_zone_restriction, restrictionText(zone.restriction))
		zone.typeCode?.let { row(R.drawable.ic_action_info_dark, R.string.drone_zone_category, it.raw) }
		if (zone.reasons.isNotEmpty()) {
			row(R.drawable.ic_action_info_dark, R.string.drone_zone_reasons, zone.reasons.joinToString(", ") { it.raw })
		}
		zone.lowerLimit?.let { row(R.drawable.ic_action_altitude_min, R.string.drone_zone_lower_limit, it.format()) }
		zone.upperLimit?.let { row(R.drawable.ic_action_altitude_max, R.string.drone_zone_upper_limit, it.format()) }
		if (zone.legalTexts.isNotEmpty()) {
			row(R.drawable.ic_action_note_dark, R.string.drone_zone_legal_basis, zone.legalTexts.joinToString("\n"))
		}
		zone.links.forEach { link ->
			addPlainMenuItem(R.drawable.ic_action_link, null, link.url, link.text, true, true, null)
		}
		val source = listOfNotNull(zone.dataset.provider, zone.dataset.source).filter { it.isNotBlank() }.joinToString(" · ")
		row(R.drawable.ic_action_data, R.string.drone_zone_source, source)
		zone.dataset.id?.let { row(R.drawable.ic_action_data, R.string.drone_zone_dataset, it) }
		val validFrom = zone.dataset.validFrom
		val validTo = zone.dataset.validTo
		if (validFrom != null && validTo != null) {
			row(R.drawable.ic_action_time_span, R.string.drone_zone_validity, activity.getString(R.string.drone_zone_validity_value, validFrom, validTo))
		}
		if (zone.dataset.expired) {
			row(R.drawable.ic_action_warning_colored, 0, activity.getString(R.string.drone_zone_expired_warning))
		}
		row(R.drawable.ic_action_info_outlined, 0, activity.getString(R.string.drone_zone_disclaimer))
	}

	private fun restrictionText(value: RawValue): String {
		return when (value.knownSemantic) {
			"REQ_AUTHORIZATION" -> activity.getString(R.string.drone_zone_restriction_req_authorization)
			else -> value.raw
		}
	}

	private fun row(icon: Int, labelRes: Int, value: String) {
		if (value.isNotBlank()) {
			addPlainMenuItem(icon, null, value, if (labelRes == 0) null else activity.getString(labelRes), false, false, null)
		}
	}
}

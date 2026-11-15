package net.osmand.plus.auto

import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.Maneuver
import java.lang.reflect.Modifier

/**
 * Developer tool for checking how a car draws each maneuver type on the cluster and HUD:
 * every call of [next] replaces the maneuver sent to the car with the next type, then turns the replacement off.
 */
class ManeuverTester {

	private var index = OFF

	val isActive: Boolean
		get() = index != OFF

	fun next() {
		index = if (index + 1 < TYPES.size) index + 1 else OFF
	}

	fun getTitle(): String {
		return if (isActive) {
			val (type, name) = TYPES[index]
			"$type $name"
		} else {
			"Maneuvers"
		}
	}

	fun buildManeuver(icon: CarIcon?): Maneuver {
		val type = TYPES[index].first
		val builder = Maneuver.Builder(type)
		if (type in ROUNDABOUT_TYPES) {
			builder.setRoundaboutExitNumber(ROUNDABOUT_EXIT_NUMBER)
		}
		if (type in ROUNDABOUT_ANGLE_TYPES) {
			builder.setRoundaboutExitAngle(ROUNDABOUT_EXIT_ANGLE)
		}
		if (icon != null) {
			builder.setIcon(icon)
		}
		return builder.build()
	}

	companion object {
		private const val OFF = -1
		private const val ROUNDABOUT_EXIT_NUMBER = 2
		private const val ROUNDABOUT_EXIT_ANGLE = 90

		private val ROUNDABOUT_ANGLE_TYPES = setOf(
			Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE,
			Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE
		)
		private val ROUNDABOUT_TYPES = ROUNDABOUT_ANGLE_TYPES + setOf(
			Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
			Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
		)

		private val TYPES: List<Pair<Int, String>> = Maneuver::class.java.fields
			.filter {
				Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType
						&& it.name.startsWith("TYPE_") && it.name != "TYPE_UNKNOWN"
			}
			.map { it.getInt(null) to it.name.removePrefix("TYPE_") }
			.sortedBy { it.first }
	}
}
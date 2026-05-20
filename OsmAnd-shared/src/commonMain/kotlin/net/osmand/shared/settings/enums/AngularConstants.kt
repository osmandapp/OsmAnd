package net.osmand.shared.settings.enums

import net.osmand.shared.units.AngleUnits

enum class AngularConstants {

	DEGREES, DEGREES360, MILLIRADS;

	fun toHumanString() = toUnit().getName()

	fun getUnitSymbol() = toUnit().getSymbol()

	fun toUnit(): AngleUnits {
		return when(this) {
			DEGREES, DEGREES360 -> AngleUnits.DEGREES
			MILLIRADS -> AngleUnits.MILLIRADIANS
		}
	}
}
package net.osmand.shared.vehicle.specification.domain.util

import net.osmand.shared.units.MeasurementUnit

data class UnitValues(
	val units: MeasurementUnit<*>,
	val values: List<Float>
)
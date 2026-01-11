package net.osmand.shared.gpx.filters

import net.osmand.shared.settings.enums.AltitudeMetrics
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.units.HeartRateUnits
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.NoUnit
import net.osmand.shared.units.PowerUnits
import net.osmand.shared.units.RotationUnits
import net.osmand.shared.units.SpeedUnits
import net.osmand.shared.units.TemperatureUnits
import net.osmand.shared.units.TimeUnits
import net.osmand.shared.util.PlatformUtil

enum class MeasureUnitType {
	TIME_DURATION,
	DATE,
	SPEED,
	ALTITUDE,
	DISTANCE,
	ROTATIONS,
	POWER,
	TEMPERATURE,
	BPM,
	NONE;

	fun getFilterUnitText(mc: MetricsConstants, am: AltitudeMetrics) = getUnit(mc, am).getSymbol()

	fun getBaseValueFromFormatted(value: String) = getUnit().toBase(value.toDouble())

	fun getUnit(
		mc: MetricsConstants? = PlatformUtil.getOsmAndContext().getMetricSystem(),
		am: AltitudeMetrics? = PlatformUtil.getOsmAndContext().getAltitudeMetric(),
		sc: SpeedConstants? = PlatformUtil.getOsmAndContext().getSpeedSystem()
	): MeasurementUnit<*> {
		return when (this) {
			DISTANCE -> mc?.getDistanceUnit() ?: LengthUnits.METERS
			ALTITUDE -> am?.getUnits() ?: LengthUnits.METERS
			SPEED -> sc?.toUnits() ?: mc?.getSpeedUnit() ?: SpeedUnits.KILOMETERS_PER_HOUR // TODO: we should use only one point of the truth
			TEMPERATURE -> TemperatureUnits.CELSIUS
			TIME_DURATION -> TimeUnits.MINUTES
			ROTATIONS -> RotationUnits.RPM
			POWER -> PowerUnits.WATTS
			BPM -> HeartRateUnits.BPM
			else -> NoUnit
		}
	}
}
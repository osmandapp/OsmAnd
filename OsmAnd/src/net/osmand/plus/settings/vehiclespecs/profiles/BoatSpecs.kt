package net.osmand.plus.settings.vehiclespecs.profiles

import net.osmand.plus.R
import net.osmand.plus.settings.enums.LengthUnits
import net.osmand.plus.settings.vehiclespecs.SpecificationType

class BoatSpecs : VehicleSpecs() {

    override fun collectSpecs() {
        add(
            type = SpecificationType.WIDTH,
            iconDayId = R.drawable.img_help_vessel_width_day,
            iconNightId = R.drawable.img_help_vessel_width_night,
            descriptionId = R.string.vessel_width_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 10f, 13f, 16f, 19.5f, 23f, 26f, 29f, 33f, 35f, 39f, 42f, 46f, 49f)
        )

        add(
            type = SpecificationType.HEIGHT,
            iconDayId = R.drawable.img_help_vessel_height_day,
            iconNightId = R.drawable.img_help_vessel_height_night,
            descriptionId = R.string.vessel_height_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f, 22.0f, 24.0f, 26.0f, 28.0f, 30.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 13f, 19f, 26f, 33f, 40f, 46f, 52f, 59f, 65f, 72f, 79f, 85f, 92f, 98f)
        )
    }
}
package net.osmand.plus.settings.vehiclespecs.profiles

import net.osmand.plus.R
import net.osmand.plus.settings.enums.LengthUnits
import net.osmand.plus.settings.vehiclespecs.SpecificationType

class BicycleSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to createSpecification(
            iconDayId = R.drawable.img_help_cycleway_width_day,
            iconNightId = R.drawable.img_help_cycleway_width_night,
            descriptionId = R.string.bicycle_width_limit_description,
            metricUnits = LengthUnits.CENTIMETERS,
            metricValues = listOf(30f, 40f, 50f, 60f, 70f, 80f, 100f, 115f),
            imperialUnits = LengthUnits.INCHES,
            imperialValues = listOf(12f, 16f, 20f, 24f, 28f, 32f, 40f, 46f)
        )
    )
)
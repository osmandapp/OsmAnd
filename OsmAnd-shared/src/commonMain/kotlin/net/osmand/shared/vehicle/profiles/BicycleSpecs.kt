package net.osmand.shared.vehicle.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.vehicle.SpecificationType

class BicycleSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to createSpecification(
            iconDayResName = "img_help_cycleway_width_day",
            iconNightResName = "img_help_cycleway_width_night",
            descriptionResName = "bicycle_width_limit_description",
            metricUnits = LengthUnits.CENTIMETERS,
            metricValues = listOf(30f, 40f, 50f, 60f, 70f, 80f, 100f, 115f),
            imperialUnits = LengthUnits.INCHES,
            imperialValues = listOf(12f, 16f, 20f, 24f, 28f, 32f, 40f, 46f)
        )
    )
)
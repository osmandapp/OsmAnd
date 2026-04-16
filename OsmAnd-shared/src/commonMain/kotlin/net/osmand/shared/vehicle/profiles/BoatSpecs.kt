package net.osmand.shared.vehicle.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.vehicle.SpecificationType

class BoatSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to createSpecification(
            iconDayResName = "img_help_vessel_width_day",
            iconNightResName = "img_help_vessel_width_night",
            descriptionResName = "vessel_width_limit_description",
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 10f, 13f, 16f, 19.5f, 23f, 26f, 29f, 33f, 35f, 39f, 42f, 46f, 49f)
        ),

        SpecificationType.HEIGHT to createSpecification(
            iconDayResName = "img_help_vessel_height_day",
            iconNightResName = "img_help_vessel_height_night",
            descriptionResName = "vessel_height_limit_description",
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f, 22.0f, 24.0f, 26.0f, 28.0f, 30.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 13f, 19f, 26f, 33f, 40f, 46f, 52f, 59f, 65f, 72f, 79f, 85f, 92f, 98f)
        )
    )
)
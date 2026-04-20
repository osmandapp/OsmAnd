package net.osmand.shared.vehicle.specification.domain.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.vehicle.specification.domain.Specification
import net.osmand.shared.vehicle.specification.domain.SpecificationType
import net.osmand.shared.vehicle.specification.domain.util.Assets
import net.osmand.shared.vehicle.specification.domain.util.UnitValues

class BicycleSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to Specification(
            assets = Assets(
                iconDayName = "img_help_cycleway_width_day",
                iconNightName = "img_help_cycleway_width_night",
                summaryName = "bicycle_width_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.CENTIMETERS,
                values = listOf(30f, 40f, 50f, 60f, 70f, 80f, 100f, 115f)
            ),
            imperial = UnitValues(
                units = LengthUnits.INCHES,
                values = listOf(12f, 16f, 20f, 24f, 28f, 32f, 40f, 46f)
            )
        )
    )
)
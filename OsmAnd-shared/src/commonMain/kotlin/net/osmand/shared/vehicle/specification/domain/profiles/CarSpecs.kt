package net.osmand.shared.vehicle.specification.domain.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.WeightUnits
import net.osmand.shared.vehicle.specification.domain.Specification
import net.osmand.shared.vehicle.specification.domain.SpecificationType
import net.osmand.shared.vehicle.specification.domain.util.Assets
import net.osmand.shared.vehicle.specification.domain.util.UnitValues

class CarSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to Specification(
            assets = Assets(
                iconDayName = "img_help_width_limit_day",
                iconNightName = "img_help_width_limit_night",
                summaryName = "width_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.METERS,
                values = listOf(1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f)
            ),
            imperial = UnitValues(
                units = LengthUnits.INCHES,
                values = listOf(60f, 64f, 68f, 72f, 76f, 80f)
            )
        ),

        SpecificationType.HEIGHT to Specification(
            assets = Assets(
                iconDayName = "img_help_height_limit_day",
                iconNightName = "img_help_height_limit_night",
                summaryName = "height_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.METERS,
                values = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f)
            ),
            imperial = UnitValues(
                units = LengthUnits.FEET,
                values = listOf(5f, 6.5f, 8f, 10f, 11.5f, 13f, 14.5f)
            )
        ),

        SpecificationType.LENGTH to Specification(
            assets = Assets(
                iconDayName = "img_help_length_limit_day",
                iconNightName = "img_help_length_limit_night",
                summaryName = "lenght_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.METERS,
                values = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f, 5.5f, 6.0f)
            ),
            imperial = UnitValues(
                units = LengthUnits.FEET,
                values = listOf(5f, 6.5f, 8f, 10f, 11.5f, 13f, 15f, 16.5f, 18f, 20f)
            )
        ),

        SpecificationType.WEIGHT to Specification(
            assets = Assets(
                iconDayName = "img_help_weight_limit_day",
                iconNightName = "img_help_weight_limit_night",
                summaryName = "weight_limit_description"
            ),
            metric = UnitValues(
                units = WeightUnits.TONS,
                values = listOf(0.7f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f)
            ),
            imperial = UnitValues(
                units = WeightUnits.POUNDS,
                values = listOf(1500f, 2200f, 3500f, 4500f, 5500f, 6500f, 7500f)
            )
        )
    )
)
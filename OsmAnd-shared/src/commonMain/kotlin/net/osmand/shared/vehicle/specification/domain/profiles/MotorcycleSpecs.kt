package net.osmand.shared.vehicle.specification.domain.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.WeightUnits
import net.osmand.shared.vehicle.specification.domain.Specification
import net.osmand.shared.vehicle.specification.domain.SpecificationType
import net.osmand.shared.vehicle.specification.domain.util.Assets
import net.osmand.shared.vehicle.specification.domain.util.UnitValues

class MotorcycleSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to Specification(
            assets = Assets(
                iconDayName = "img_help_width_limit_day",
                iconNightName = "img_help_width_limit_night",
                summaryName = "width_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.METERS,
                values = listOf(0.7f, 0.8f, 0.9f, 1.0f)
            ),
            imperial = UnitValues(
                units = LengthUnits.INCHES,
                values = listOf(28f, 32f, 36f, 40f)
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
                values = listOf(0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f)
            ),
            imperial = UnitValues(
                units = LengthUnits.FEET,
                values = listOf(2f, 2.5f, 3.3f, 4f, 4.5f, 5.2f, 6f, 6.5f)
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
                values = listOf(1.5f, 1.7f, 1.9f, 2.1f, 2.3f, 2.5f)
            ),
            imperial = UnitValues(
                units = LengthUnits.FEET,
                values = listOf(5f, 5.5f, 6.2f, 7f, 7.5f, 8.2f)
            )
        ),

        SpecificationType.WEIGHT to Specification(
            assets = Assets(
                iconDayName = "img_help_weight_limit_day",
                iconNightName = "img_help_weight_limit_night",
                summaryName = "weight_limit_description"
            ),
            metric = UnitValues(
                units = WeightUnits.KILOGRAMS,
                values = listOf(60f, 100f, 150f, 200f, 250f, 300f)
            ),
            imperial = UnitValues(
                units = WeightUnits.POUNDS,
                values = listOf(130f, 220f, 330f, 440f, 550f, 660f)
            )
        )
    )
)
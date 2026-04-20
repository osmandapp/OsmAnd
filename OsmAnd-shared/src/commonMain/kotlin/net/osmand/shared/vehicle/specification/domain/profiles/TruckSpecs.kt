package net.osmand.shared.vehicle.specification.domain.profiles

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.WeightUnits
import net.osmand.shared.vehicle.specification.data.validator.TruckWeightValidator
import net.osmand.shared.vehicle.specification.domain.Specification
import net.osmand.shared.vehicle.specification.domain.SpecificationType
import net.osmand.shared.vehicle.specification.domain.util.Assets
import net.osmand.shared.vehicle.specification.domain.util.UnitValues

class TruckSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to Specification(
            assets = Assets(
                iconDayName = "img_help_width_limit_day",
                iconNightName = "img_help_width_limit_night",
                summaryName = "width_limit_description"
            ),
            metric = UnitValues(
                units = LengthUnits.METERS,
                values = listOf(1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.2f, 2.3f, 2.4f, 2.5f)
            ),
            imperial = UnitValues(
                units = LengthUnits.INCHES,
                values = listOf(68f, 72f, 76f, 80f, 84f, 88f, 92f, 96f, 100f)
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
                values = listOf(4.5f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f)
            ),
            imperial = UnitValues(
                units = LengthUnits.FEET,
                values = listOf(15f, 16f, 20f, 24f, 26f, 30f, 33f, 36f, 39f)
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
                values = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f, 22.0f, 24.0f, 26.0f, 30.0f, 36.0f, 40.0f)
            ),
            imperial = UnitValues(
                units = WeightUnits.POUNDS,
                values = listOf(7500f, 8500f, 13000f, 17500f, 22000f, 26000f, 30000f, 35000f, 40000f, 44000f, 48000f, 52000f, 58000f, 66000f, 80000f, 88000f)
            ),
            validatorFactory = ::TruckWeightValidator
        ),

        SpecificationType.AXLE_LOAD to Specification(
            assets = Assets(
                iconDayName = "img_help_weight_limit_day",
                iconNightName = "img_help_weight_limit_night",
                summaryName = "max_axle_load_description"
            ),
            metric = UnitValues(
                units = WeightUnits.TONS,
                values = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f)
            ),
            imperial = UnitValues(
                units = WeightUnits.POUNDS,
                values = listOf(7500f, 8500f, 13000f, 17500f, 20000f, 26000f, 30000f, 34000f)
            )
        ),

        SpecificationType.WEIGHT_FULL_LOAD to Specification(
            assets = Assets(
                iconDayName = "img_help_weight_limit_day",
                iconNightName = "img_help_weight_limit_night",
                summaryName = "max_weight_at_full_load_description"
            ),
            metric = UnitValues(
                units = WeightUnits.TONS,
                values = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f)
            ),
            imperial = UnitValues(
                units = WeightUnits.POUNDS,
                values = listOf(7500f, 8500f, 13000f, 17500f, 20000f, 26000f, 30000f, 34000f)
            )
        )
    )
)
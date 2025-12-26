package net.osmand.plus.settings.vehiclespecs.profiles

import android.content.Context
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.LengthUnits
import net.osmand.plus.settings.enums.WeightUnits
import net.osmand.plus.settings.vehiclespecs.SpecificationType

class TruckSpecs : VehicleSpecs(
    mapOf(
        SpecificationType.WIDTH to createSpecification(
            iconDayId = R.drawable.img_help_width_limit_day,
            iconNightId = R.drawable.img_help_width_limit_night,
            descriptionId = R.string.width_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.2f, 2.3f, 2.4f, 2.5f),
            imperialUnits = LengthUnits.INCHES,
            imperialValues = listOf(68f, 72f, 76f, 80f, 84f, 88f, 92f, 96f, 100f)
        ),

        SpecificationType.HEIGHT to createSpecification(
            iconDayId = R.drawable.img_help_height_limit_day,
            iconNightId = R.drawable.img_help_height_limit_night,
            descriptionId = R.string.height_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 8f, 10f, 11.5f, 13f, 14.5f)
        ),

        SpecificationType.LENGTH to createSpecification(
            iconDayId = R.drawable.img_help_length_limit_day,
            iconNightId = R.drawable.img_help_length_limit_night,
            descriptionId = R.string.lenght_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(4.5f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(15f, 16f, 20f, 24f, 26f, 30f, 33f, 36f, 39f)
        ),

        SpecificationType.WEIGHT to createSpecification(
            iconDayId = R.drawable.img_help_weight_limit_day,
            iconNightId = R.drawable.img_help_weight_limit_night,
            descriptionId = R.string.weight_limit_description,
            metricUnits = WeightUnits.TONES,
            metricValues = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f, 22.0f, 24.0f, 26.0f, 30.0f, 36.0f, 40.0f),
            imperialUnits = WeightUnits.POUNDS,
            imperialValues = listOf(7500f, 8500f, 13000f, 17500f, 22000f, 26000f, 30000f, 35000f, 40000f, 44000f, 48000f, 52000f, 58000f, 66000f, 80000f, 88000f )
        ),

        SpecificationType.AXLE_LOAD to createSpecification(
            iconDayId = R.drawable.img_help_weight_limit_day,
            iconNightId = R.drawable.img_help_weight_limit_night,
            descriptionId = R.string.max_axle_load_description,
            metricUnits = WeightUnits.TONES,
            metricValues = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f),
            imperialUnits = WeightUnits.POUNDS,
            imperialValues = listOf(7500f, 8500f, 13000f, 17500f, 20000f, 26000f, 30000f, 34000f)
        ),

        SpecificationType.WEIGHT_FULL_LOAD to createSpecification(
            iconDayId = R.drawable.img_help_weight_limit_day,
            iconNightId = R.drawable.img_help_weight_limit_night,
            descriptionId = R.string.max_weight_at_full_load_description,
            metricUnits = WeightUnits.TONES,
            metricValues = listOf(3.5f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f),
            imperialUnits = WeightUnits.POUNDS,
            imperialValues = listOf(7500f, 8500f, 13000f, 17500f, 20000f, 26000f, 30000f, 34000f)
        )
    )
) {

    override fun checkValue(
        context: Context,
        type: SpecificationType,
        useMetricSystem: Boolean,
        value: Float
    ): String {
        if (type == SpecificationType.WEIGHT) {
            val data = getSpecsData(type)
            val units = getMeasurementUnits(SpecificationType.WEIGHT, useMetricSystem)

            val min = data.getPredefinedValues(useMetricSystem).firstOrNull() ?: 0f
            if (value < min) {
                val errorPattern = context.getString(R.string.common_weight_limit_error)
                val minFormatted = units.formatValue(min)
                val unitsName = context.getString(units.getNameResId())
                val profileName = context.getString(ApplicationMode.CAR.nameKeyResource)
                return errorPattern.format(minFormatted, unitsName, profileName)
            }
        }
        return super.checkValue(context, type, useMetricSystem, value)
    }
}
package net.osmand.plus.settings.vehiclespecs.profiles

import net.osmand.plus.R
import net.osmand.plus.settings.enums.LengthUnits
import net.osmand.plus.settings.enums.WeightUnits
import net.osmand.plus.settings.vehiclespecs.SpecificationType

class MotorcycleSpecs : VehicleSpecs() {

    override fun collectSpecs() {
        add(
            type = SpecificationType.WIDTH,
            iconDayId = R.drawable.img_help_width_limit_day,
            iconNightId = R.drawable.img_help_width_limit_night,
            descriptionId = R.string.width_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(0.7f, 0.8f, 0.9f, 1.0f),
            imperialUnits = LengthUnits.INCHES,
            imperialValues = listOf(28f, 32f, 36f, 40f)
        )

        add(
            type = SpecificationType.HEIGHT,
            iconDayId = R.drawable.img_help_height_limit_day,
            iconNightId = R.drawable.img_help_height_limit_night,
            descriptionId = R.string.height_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(2f, 2.5f, 3.3f, 4f, 4.5f, 5.2f, 6f, 6.5f)
        )

        add(
            type = SpecificationType.LENGTH,
            iconDayId = R.drawable.img_help_length_limit_day,
            iconNightId = R.drawable.img_help_length_limit_night,
            descriptionId = R.string.lenght_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 1.7f, 1.9f, 2.1f, 2.3f, 2.5f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 5.5f, 6.2f, 7f, 7.5f, 8.2f)
        )

        add(
            type = SpecificationType.WEIGHT,
            iconDayId = R.drawable.img_help_weight_limit_day,
            iconNightId = R.drawable.img_help_weight_limit_night,
            descriptionId = R.string.weight_limit_description,
            metricUnits = WeightUnits.KILOGRAMS,
            metricValues = listOf(60f, 100f, 150f, 200f, 250f, 300f),
            imperialUnits = WeightUnits.POUNDS,
            imperialValues = listOf(130f, 220f, 330f, 440f, 550f, 660f)
        )
    }
}
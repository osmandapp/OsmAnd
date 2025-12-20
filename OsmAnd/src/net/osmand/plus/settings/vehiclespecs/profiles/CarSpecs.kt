package net.osmand.plus.settings.vehiclespecs.profiles

import net.osmand.plus.R
import net.osmand.plus.settings.enums.LengthUnits
import net.osmand.plus.settings.enums.WeightUnits
import net.osmand.plus.settings.vehiclespecs.SpecificationType

class CarSpecs : VehicleSpecs() {

    override fun collectSpecs() {
        add(
            type = SpecificationType.WIDTH,
            iconDayId = R.drawable.img_help_width_limit_day,
            iconNightId = R.drawable.img_help_width_limit_night,
            descriptionId = R.string.width_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f),
            imperialUnits = LengthUnits.INCHES,
            imperialValues = listOf(60f, 64f, 68f, 72f, 76f, 80f)
        )

        add(
            type = SpecificationType.HEIGHT,
            iconDayId = R.drawable.img_help_height_limit_day,
            iconNightId = R.drawable.img_help_height_limit_night,
            descriptionId = R.string.height_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 8f, 10f, 11.5f, 13f, 14.5f)
        )

        add(
            type = SpecificationType.LENGTH,
            iconDayId = R.drawable.img_help_length_limit_day,
            iconNightId = R.drawable.img_help_length_limit_night,
            descriptionId = R.string.lenght_limit_description,
            metricUnits = LengthUnits.METERS,
            metricValues = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f, 5.5f, 6.0f),
            imperialUnits = LengthUnits.FEET,
            imperialValues = listOf(5f, 6.5f, 8f, 10f, 11.5f, 13f, 15f, 16.5f, 18f, 20f)
        )

        add(
            type = SpecificationType.WEIGHT,
            iconDayId = R.drawable.img_help_weight_limit_day,
            iconNightId = R.drawable.img_help_weight_limit_night,
            descriptionId = R.string.weight_limit_description,
            metricUnits = WeightUnits.TONES,
            metricValues = listOf(0.7f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f),
            imperialUnits = WeightUnits.POUNDS,
            imperialValues = listOf(1500f, 2200f, 3500f, 4500f, 5500f, 6500f, 7500f)
        )
    }
}
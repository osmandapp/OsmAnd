package net.osmand.plus.settings.vehiclesize;

import static net.osmand.plus.settings.vehiclesize.SizeType.HEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.LENGTH;
import static net.osmand.plus.settings.vehiclesize.SizeType.WEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.WIDTH;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.plus.settings.vehiclesize.containers.Metric;

public class TruckSizes extends VehicleSizes {

	@Override
	protected void collectSizesData() {
		ThemedIconId icon = new ThemedIconId(R.drawable.img_help_width_limit_day, R.drawable.img_help_width_limit_night);
		Assets assets = new Assets(icon, R.string.width_limit_description);
		Limits limits = new Limits(1.7f, 2.5f);
		add(WIDTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_height_limit_day, R.drawable.img_help_height_limit_night);
		assets = new Assets(icon, R.string.height_limit_description);
		limits = new Limits(1.5f, 4.5f);
		add(HEIGHT, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_length_limit_day, R.drawable.img_help_length_limit_night);
		assets = new Assets(icon, R.string.lenght_limit_description);
		limits = new Limits(4.5f, 12f);
		add(LENGTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_weight_limit_day, R.drawable.img_help_weight_limit_night);
		assets = new Assets(icon, R.string.weight_limit_description);
		limits = new Limits(3.5f, 16f);
		add(WEIGHT, assets, limits);
	}

	@Override
	public boolean verifyValue(@NonNull Context ctx, @NonNull SizeType type, @NonNull Metric metric,
	                           float value, @NonNull StringBuilder error) {
		if (type == WEIGHT) {
			SizeData data = getSizeData(type);
			Limits limits = VehicleAlgorithms.convertWeightLimitsByMetricSystem(
					data.getLimits(), metric.getWeightMetric(), useKilogramsInsteadOfTons());
			float min = limits.getMin();
			if (value < min) {
				String errorMessagePattern = ctx.getString(R.string.common_weight_limit_error);
				String minWeightFormatted = formatValue(min);
				String metricStr = ctx.getString(getMetricStringId(type, metric));
				String drivingProfileName = ctx.getString(ApplicationMode.CAR.getNameKeyResource());
				String message = String.format(errorMessagePattern, minWeightFormatted, metricStr, drivingProfileName);
				error.append(message);
				return false;
			}
		}
		return true;
	}
}

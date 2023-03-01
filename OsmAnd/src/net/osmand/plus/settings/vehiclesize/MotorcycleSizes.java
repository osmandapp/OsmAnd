package net.osmand.plus.settings.vehiclesize;

import static net.osmand.plus.settings.vehiclesize.SizeType.HEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.LENGTH;
import static net.osmand.plus.settings.vehiclesize.SizeType.WEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.WIDTH;
import static net.osmand.plus.utils.OsmAndFormatter.KILOGRAMS_IN_ONE_TON;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.util.Algorithms;

public class MotorcycleSizes extends VehicleSizes {

	@Override
	protected void collectSizesData() {
		ThemedIconId icon = new ThemedIconId(R.drawable.img_help_width_limit_day, R.drawable.img_help_width_limit_night);
		Assets assets = new Assets(icon, R.string.width_limit_description);
		Limits limits = new Limits(0.7f, 1f);
		add(WIDTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_height_limit_day, R.drawable.img_help_height_limit_night);
		assets = new Assets(icon, R.string.height_limit_description);
		limits = new Limits(0.6f, 2f); // 0.9 -> 2
		add(HEIGHT, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_length_limit_day, R.drawable.img_help_length_limit_night);
		assets = new Assets(icon, R.string.lenght_limit_description);
		limits = new Limits(1.5f, 2.5f); // 2.3 -> 2.5
		add(LENGTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_weight_limit_day, R.drawable.img_help_weight_limit_night);
		assets = new Assets(icon, R.string.weight_limit_description);
		limits = new Limits(60.0f, 300.0f); // in kilograms
		add(WEIGHT, assets, limits);
	}

	@Override
	public int getMetricStringId(@NonNull SizeType type, @NonNull MetricsConstants mc) {
		return type == WEIGHT ? R.string.shared_string_kilograms : super.getMetricStringId(type, mc);
	}

	@Override
	public int getMetricShortStringId(@NonNull SizeType type, @NonNull MetricsConstants mc) {
		return type == WEIGHT ? R.string.kg : super.getMetricShortStringId(type, mc);
	}

	@Override
	public float readSavedValue(@NonNull SizePreference preference) {
		if (preference.getSizeType() == WEIGHT) {
			float value = (float) Algorithms.parseDoubleSilently(preference.getValue(), 0.0f);
			if (value != 0.0f) {
				value += 0.001f;
			}
			value *= KILOGRAMS_IN_ONE_TON;
			return value;
		}
		return super.readSavedValue(preference);
	}

	@Override
	public float prepareValueToSave(@NonNull SizePreference preference, float value) {
		if (preference.getSizeType() == WEIGHT) {
			value /= KILOGRAMS_IN_ONE_TON;
			if (value != 0.0f) {
				value -= 0.001f;
			}
			return value;
		}
		return super.prepareValueToSave(preference, value);
	}
}

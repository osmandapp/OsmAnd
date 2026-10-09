package net.osmand.plus.settings.vehiclesize;

import static net.osmand.plus.settings.vehiclesize.SizeType.*;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;

public class CarSizes extends VehicleSizes {

	@Override
	protected void collectSizesData() {
		ThemedIconId icon = new ThemedIconId(R.drawable.img_help_width_limit_day, R.drawable.img_help_width_limit_night);
		Assets assets = new Assets(icon, R.string.width_limit_description);
		Limits limits = new Limits(1.5f, 2f); // 1.7 -> 2
		add(WIDTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_height_limit_day, R.drawable.img_help_height_limit_night);
		assets = new Assets(icon, R.string.height_limit_description);
		limits = new Limits(1.5f, 4.3f);
		add(HEIGHT, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_length_limit_day, R.drawable.img_help_length_limit_night);
		assets = new Assets(icon, R.string.lenght_limit_description);
		limits = new Limits(1.5f, 6f); // 4.5 -> 6
		add(LENGTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_weight_limit_day, R.drawable.img_help_weight_limit_night);
		assets = new Assets(icon, R.string.weight_limit_description);
		limits = new Limits(0.7f, 3.5f); // 0.75 -> 3.5
		add(WEIGHT, assets, limits);
	}

}

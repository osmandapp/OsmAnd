package net.osmand.plus.settings.vehiclesize;

import static net.osmand.plus.settings.vehiclesize.SizeType.HEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.LENGTH;
import static net.osmand.plus.settings.vehiclesize.SizeType.WEIGHT;
import static net.osmand.plus.settings.vehiclesize.SizeType.WIDTH;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;

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
		limits = new Limits(0.06f, 0.3f);
		add(WEIGHT, assets, limits);
	}

	@Override
	public boolean useKilogramsInsteadOfTons() {
		return true;
	}
}

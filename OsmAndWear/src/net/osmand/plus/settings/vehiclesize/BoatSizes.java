package net.osmand.plus.settings.vehiclesize;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;

public class BoatSizes extends VehicleSizes {

	private static final int EXTENDED_PROPOSED_VALUES_COUNT = 12;

	@Override
	protected void collectSizesData() {
		ThemedIconId icon = new ThemedIconId(R.drawable.img_help_vessel_width_day, R.drawable.img_help_vessel_width_night);
		Assets assets = new Assets(icon, R.string.vessel_width_limit_description);
		Limits limits = new Limits(1.5f, 15f);
		add(SizeType.WIDTH, assets, limits);

		icon = new ThemedIconId(R.drawable.img_help_vessel_height_day, R.drawable.img_help_vessel_height_night);
		assets = new Assets(icon, R.string.vessel_height_limit_description);
		limits = new Limits(1.5f, 30f);
		add(SizeType.HEIGHT, assets, limits);
	}

	@Override
	protected int getMinProposedValuesCount() {
		return EXTENDED_PROPOSED_VALUES_COUNT;
	}

	@Override
	protected boolean useInchesInsteadOfFeet() {
		return false;
	}
}

package net.osmand.plus.settings.vehiclesize;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;

public class BicycleSizes extends VehicleSizes {

	@Override
	protected void collectSizesData() {
		ThemedIconId icon = new ThemedIconId(R.drawable.img_help_cycleway_width_day, R.drawable.img_help_cycleway_width_night);
		Assets assets = new Assets(icon, R.string.bicycle_width_limit_description);
		Limits<Float> limits = new Limits<>(0.3f, 1.15f);
		add(SizeType.WIDTH, assets, limits);
	}

	@Override
	protected boolean useCentimetersInsteadOfMeters() {
		return true;
	}

	@Override
	protected boolean useRoundedProposedLimits() {
		return false;
	}
}

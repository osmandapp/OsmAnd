package net.osmand.plus.settings.vehiclesize;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.shared.settings.enums.MetricsConstants;

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

	/**
	 * Rounds proposed UI values for all size types, except WIDTH in metric systems.
	 *
	 * Reason:
	 * - WIDTH in metric units (e.g. bicycle width 1.15 m → 115 cm) must remain exact.
	 * - All other combinations (imperial units or other size types) benefit from rounding,
	 *   since converted values may look unnatural (e.g. 1.15 m → 45.2756 in).
	 */
	@Override
	protected boolean useRoundedProposedLimits(@NonNull SizeType sizeType,
	                                           @NonNull MetricsConstants metricSystem) {
		return sizeType != SizeType.WIDTH || metricSystem != MetricsConstants.KILOMETERS_AND_METERS;
	}
}

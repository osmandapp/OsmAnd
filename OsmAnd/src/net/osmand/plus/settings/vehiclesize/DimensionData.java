package net.osmand.plus.settings.vehiclesize;

import androidx.annotation.NonNull;

import net.osmand.plus.base.wrapper.Assets;
import net.osmand.plus.base.wrapper.Limits;

public class DimensionData {
	private Assets assets;
	private Limits limits;

	public DimensionData(@NonNull Assets assets,
	                     @NonNull Limits limits) {
		this.assets = assets;
		this.limits = limits;
	}

	public Assets getAssets() {
		return assets;
	}

	public Limits getLimits() {
		return limits;
	}
}

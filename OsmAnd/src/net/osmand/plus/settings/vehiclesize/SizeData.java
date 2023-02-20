package net.osmand.plus.settings.vehiclesize;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.plus.base.containers.Limits;

public class SizeData {

	private Assets assets;
	private Limits limits;

	public SizeData(@NonNull Assets assets,
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

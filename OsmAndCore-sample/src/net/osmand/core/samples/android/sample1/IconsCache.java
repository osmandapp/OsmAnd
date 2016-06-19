package net.osmand.core.samples.android.sample1;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.core.CoreResourcesFromAndroidAssetsCustom;

public class IconsCache {

	private CoreResourcesFromAndroidAssetsCustom assets;
	private float displayDensityFactor;

	public IconsCache(CoreResourcesFromAndroidAssetsCustom assets) {
		this.assets = assets;
	}

	public float getDisplayDensityFactor() {
		return displayDensityFactor;
	}

	public void setDisplayDensityFactor(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
	}

	public Drawable getIcon(String name) {
		return assets.getIcon("map/icons/" + name + ".png", displayDensityFactor);
	}
}

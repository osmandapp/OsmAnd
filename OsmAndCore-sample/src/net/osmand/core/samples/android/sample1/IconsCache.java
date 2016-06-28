package net.osmand.core.samples.android.sample1;

import android.graphics.drawable.Drawable;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;

public class IconsCache {

	private CoreResourcesFromAndroidAssets assets;
	private float displayDensityFactor;

	public IconsCache(CoreResourcesFromAndroidAssets assets) {
		this.assets = assets;
	}

	public float getDisplayDensityFactor() {
		return displayDensityFactor;
	}

	public void setDisplayDensityFactor(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
	}

	public Drawable getMapIcon(String name) {
		return assets.getIcon("map/icons/" + name + ".png", displayDensityFactor);
	}
}

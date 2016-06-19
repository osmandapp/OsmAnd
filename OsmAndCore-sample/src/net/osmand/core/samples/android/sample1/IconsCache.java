package net.osmand.core.samples.android.sample1;

import android.graphics.drawable.Drawable;

import net.osmand.core.jni.OsmAndCore;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;

public class IconsCache {

	private float displayDensityFactor;

	public IconsCache(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
	}

	public float getDisplayDensityFactor() {
		return displayDensityFactor;
	}

	public void setDisplayDensityFactor(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
	}

	public Drawable getIcon(String name) {
		/*
		SWIGTYPE_p_QByteArray byteArray =
				OsmAndCore.getCoreResourcesProvider().getResource(name, displayDensityFactor);
		String s = SwigUtilities.getDataFromQByteArray(byteArray);
		*/

		return null;
	}
}

package net.osmand.plus;

import android.graphics.Color;

import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GPXExtensions {
	Map<String, String> extensions = null;

	public Map<String, String> getExtensionsToRead() {
		if (extensions == null) {
			return Collections.emptyMap();
		}
		return extensions;
	}

	private boolean customZoom = false;
	public boolean hasCustomZoom() {
		customZoom = customZoom | (extensions != null && extensions.containsKey("zoom"));
		return customZoom;
	}
	public void setCustomZoom(boolean zoom) {
		customZoom = zoom;
	}

	public float getGpxZoom(float defaultGpxZoom) {
		if(extensions != null && extensions.containsKey("zoom")) {
			try {
				defaultGpxZoom = Float.parseFloat(extensions.get("zoom"));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return defaultGpxZoom;
	}

	public void setGpxZoom(float gpxZoom) {
		getExtensionsToWrite().put("zoom", Float.toString(gpxZoom));
	}

	public int getColor(int defColor) {
		if(extensions != null && extensions.containsKey("color")) {
			try {
				return Color.parseColor(extensions.get("color").toUpperCase());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return defColor;
	}

	public void setColor(int color) {
		getExtensionsToWrite().put("color", Algorithms.colorToString(color));
	}

	public Map<String, String> getExtensionsToWrite() {
		if (extensions == null) {
			extensions = new LinkedHashMap<String, String>();
		}
		return extensions;
	}

}

package net.osmand.plus.plugins.weather;

import static net.osmand.map.WorldRegion.WORLD;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.core.jni.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.map.WorldRegion;

public class WeatherUtils {

	@NonNull
	public static String checkAndGetRegionId(@NonNull WorldRegion region) {
		return region.getRegionId();
	}

	@NonNull
	public static Pair<LatLon, LatLon> getRectanglePoints(@NonNull WorldRegion region) {
		LatLon topLeft;
		LatLon bottomRight;

		if (isEntireWorld(region)) {
			topLeft = new LatLon(90, -180);
			bottomRight = new LatLon(-90, 180);
		} else {
			QuadRect rect = region.getBoundingBox();
			topLeft = new LatLon(rect.top, rect.left);
			bottomRight = new LatLon(rect.bottom, rect.right);
		}
		return new Pair<>(topLeft, bottomRight);
	}

	public static boolean isEntireWorld(@NonNull WorldRegion region) {
		return isEntireWorld(checkAndGetRegionId(region));
	}

	public static boolean isEntireWorld(@NonNull String regionId) {
		return WORLD.equals(regionId);
	}

}

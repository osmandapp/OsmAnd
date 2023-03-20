package net.osmand.plus.plugins.weather;

import static net.osmand.map.WorldRegion.WORLD;

import androidx.annotation.NonNull;

import net.osmand.data.QuadRect;
import net.osmand.map.WorldRegion;

public class WeatherUtils {

	@NonNull
	public static QuadRect getRegionBounds(@NonNull WorldRegion region) {
		return WORLD.equals(region.getRegionId())
				? new QuadRect(-180, 90, 180, -90)
				: region.getBoundingBox();
	}
}

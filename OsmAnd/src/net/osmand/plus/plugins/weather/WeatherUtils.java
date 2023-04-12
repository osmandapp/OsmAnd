package net.osmand.plus.plugins.weather;

import static net.osmand.map.WorldRegion.WORLD;

import androidx.annotation.NonNull;

import net.osmand.data.QuadRect;
import net.osmand.map.WorldRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherUtils {

	@NonNull
	public static List<QuadRect> getRegionBounds(@NonNull WorldRegion region) {
		if (WORLD.equals(region.getRegionId())) {
			return Collections.singletonList(new QuadRect(-180, 90, 180, -90));
		} else {
			List<QuadRect> regionBounds = new ArrayList<>();

			QuadRect regionRect = region.getBoundingBox();
			boolean regionRectAvailable = !regionRect.hasInitialState();
			if (regionRectAvailable) {
				regionBounds.add(regionRect);
			}
			for (WorldRegion subregion : region.getSubregions()) {
				QuadRect subregionRect = subregion.getBoundingBox();
				if (!regionRectAvailable || !regionRect.contains(subregionRect)) {
					regionBounds.add(subregionRect);
				}
			}
			return regionBounds;
		}
	}
}

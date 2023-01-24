package net.osmand.plus.plugins.weather.indexitem;

import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.plugins.weather.WeatherUtils.checkAndGetRegionId;
import static net.osmand.plus.plugins.weather.WeatherUtils.isEntireWorld;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.IndexItem;

public class WeatherIndexItem extends IndexItem {

	private WorldRegion region;

	public WeatherIndexItem(@NonNull WorldRegion region,
	                        long timestamp,
	                        @NonNull String size,
	                        long contentSize,
	                        long containerSize) {
		super(checkAndGetRegionId(region) + WEATHER_EXT, "", timestamp, size, contentSize, containerSize, WEATHER_FORECAST);
		this.region = region;
	}

	public WorldRegion getRegion() {
		return region;
	}

	public String getRegionId() {
		return checkAndGetRegionId(region);
	}

	public boolean isWorldMap() {
		return isEntireWorld(region);
	}

	@Override
	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		return new DownloadEntry(region);
	}

	@Override
	public long getExistingFileSize(@NonNull OsmandApplication ctx) {
		// todo implement
		return 0;
	}
}

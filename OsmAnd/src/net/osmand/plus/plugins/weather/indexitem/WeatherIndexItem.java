package net.osmand.plus.plugins.weather.indexitem;

import static android.text.format.DateUtils.WEEK_IN_MILLIS;
import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.IndexItem;

public class WeatherIndexItem extends IndexItem {

	private final WorldRegion region;

	public WeatherIndexItem(@NonNull WorldRegion region, long timestamp,
	                        @NonNull String size, long contentSize, long containerSize) {
		super(region.getRegionId() + WEATHER_EXT, "", timestamp,
				size, contentSize, containerSize, WEATHER_FORECAST, false, null);
		this.region = region;
	}

	@NonNull
	public WorldRegion getRegion() {
		return region;
	}

	@NonNull
	public String getRegionId() {
		return getRegion().getRegionId();
	}

	@Override
	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		return new DownloadEntry(region);
	}

	@Override
	public long getExistingFileSize(@NonNull OsmandApplication ctx) {
		return getSize();
	}

	public long getDataExpireTime() {
		long downloadTime = getLocalTimestamp();
		if (!isDownloaded() || downloadTime == 0) {
			downloadTime = getTimestamp();
		}
		return downloadTime + WEEK_IN_MILLIS;
	}
}

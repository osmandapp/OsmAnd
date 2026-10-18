package net.osmand.plus.download;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Free version: all maps of one region and their updates are free.
 * <p>
 * The free region is a country - the region right under a continent, so Germany with all its Länder
 * is one region. In the big federations it is a first-level region (a US state), otherwise one free
 * region would be tens of GB.
 * <p>
 * Only new downloads of standard and roads-only maps are counted; updates of maps on the device are
 * always free. The world overview map, seamarks and the maps of plugins - contour lines, terrain,
 * depth, Wikipedia, travel guides - are never counted and never deleted when the free region is
 * replaced.
 */
public class FreeRegionHelper {

	private static final Log LOG = PlatformUtil.getLog(FreeRegionHelper.class);

	private static final Set<String> FEDERATIONS = new HashSet<>(Arrays.asList(
			"northamerica_us", "northamerica_canada", "russia", "southamerica_brazil",
			"asia_india", "asia_china", "australia-oceania-all_australia-oceania"));

	/**
	 * Own preferences file, not OsmandSettings: OsmandBackupAgent backs up the OsmandSettings files and
	 * the free region must not come back from a backup or travel to another device with Cloud.
	 */
	static final String PREFERENCES_NAME = "net.osmand.free_region";
	private static final String REGION = "region";
	private static final String MIGRATED = "migrated";
	private static final String UPDATE_WARNING_SHOWN = "update_warning_shown";

	/** Maps built before 01.07.2026 do not work with the new search. */
	private static final long NEW_SEARCH_MAPS_DATE = 1782864000000L;

	private final OsmandApplication app;
	private final SharedPreferences preferences;
	private final OsmandRegions regions;

	public FreeRegionHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.preferences = getPreferences(app);
		this.regions = app.getRegions();
	}

	@NonNull
	static SharedPreferences getPreferences(@NonNull OsmandApplication app) {
		return app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
	}

	public boolean isFreeVersion() {
		return !Version.isPaidVersion(app);
	}

	@Nullable
	public WorldRegion getFreeRegion() {
		migrateIfNeeded();
		String id = preferences.getString(REGION, null);
		return Algorithms.isEmpty(id) ? null : regions.getRegionData(id);
	}

	public void setFreeRegion(@NonNull WorldRegion region) {
		preferences.edit().putString(REGION, region.getRegionId()).apply();
	}

	/**
	 * @return the free-version region the item counts against, or null when the item is free for
	 * everyone (world maps, plugin maps, voice, fonts...).
	 */
	@Nullable
	public WorldRegion getRegion(@NonNull IndexItem item) {
		if (!DownloadActivityType.isCountedInDownloads(item)) {
			return null;
		}
		return getRegionByDownloadName(item.getBasename());
	}

	@Nullable
	private WorldRegion getRegionByDownloadName(@NonNull String downloadName) {
		String name = downloadName.toLowerCase();
		if (FileNameTranslationHelper.getStandardMapName(app, name) != null) {
			return null;
		}
		WorldRegion region = regions.getRegionDataByDownloadName(name);
		return region != null ? getRegion(region) : null;
	}

	@Nullable
	public static WorldRegion getRegion(@NonNull WorldRegion region) {
		WorldRegion country = region.getCountryRegion();
		if (country != null && FEDERATIONS.contains(country.getRegionId()) && !country.equals(region)) {
			WorldRegion state = region;
			while (state.getSuperregion() != null && !country.equals(state.getSuperregion())) {
				state = state.getSuperregion();
			}
			return state;
		}
		return country;
	}

	public boolean isFree(@NonNull IndexItem item, @Nullable WorldRegion freeRegion) {
		if (!isFreeVersion()) {
			return true;
		}
		WorldRegion region = getRegion(item);
		return region == null || region.equals(freeRegion) || item.isDownloaded();
	}

	/** "Ukraine_kyiv-city_europe_2.obf" -> "ukraine_kyiv-city_europe", the download name of its region. */
	@NonNull
	private static String getBasename(@NonNull String fileName) {
		int extension = fileName.indexOf('.');
		String basename = extension > 0 ? fileName.substring(0, extension) : fileName;
		return Algorithms.removeFileVersionSuffix(basename).toLowerCase();
	}

	/**
	 * The "update your maps" warning is shown once, after the update of the app, when there are maps
	 * older than the new search. The date is the one of the map data, as for the outdated maps check.
	 */
	public boolean shouldShowUpdateWarning() {
		if (preferences.getBoolean(UPDATE_WARNING_SHOWN, false)) {
			return false;
		}
		ResourceManager resourceManager = app.getResourceManager();
		Map<String, String> dates = resourceManager.getIndexFileNames();
		DateFormat format = resourceManager.getDateFormat();
		for (List<LocalItem> items : collectLocalMaps().values()) {
			for (LocalItem item : items) {
				long date = item.getLastModified();
				String mapDate = dates.get(item.getFileName());
				if (mapDate != null) {
					try {
						date = format.parse(mapDate).getTime();
					} catch (ParseException e) {
						LOG.error(e);
					}
				}
				if (date < NEW_SEARCH_MAPS_DATE) {
					return true;
				}
			}
		}
		return false;
	}

	public void setUpdateWarningShown() {
		preferences.edit().putBoolean(UPDATE_WARNING_SHOWN, true).apply();
	}

	/**
	 * Standard and roads-only map files on the device, by region. Files of no region (world maps,
	 * custom maps) are left out.
	 */
	@NonNull
	public Map<WorldRegion, List<LocalItem>> collectLocalMaps() {
		Map<WorldRegion, List<LocalItem>> result = new LinkedHashMap<>();
		File[] dirs = {app.getAppPath(IndexConstants.MAPS_PATH), app.getAppPath(IndexConstants.ROADS_INDEX_DIR)};
		for (File dir : dirs) {
			File[] files = dir.listFiles();
			if (files == null) {
				continue;
			}
			for (File file : files) {
				if (!file.isFile()) {
					continue;
				}
				LocalItemType type = LocalItemUtils.getItemType(app, file);
				if (type != LocalItemType.MAP_DATA && type != LocalItemType.ROAD_DATA) {
					continue;
				}
				WorldRegion region = getRegionByDownloadName(getBasename(file.getName()));
				if (region != null) {
					List<LocalItem> items = result.get(region);
					if (items == null) {
						items = new ArrayList<>();
						result.put(region, items);
					}
					items.add(new LocalItem(file, type));
				}
			}
		}
		return result;
	}

	/** Maps deleted when the free region is replaced by {@code newRegion}: maps of every other region. */
	@NonNull
	public Map<WorldRegion, List<LocalItem>> collectMapsToDelete(@NonNull WorldRegion newRegion) {
		Map<WorldRegion, List<LocalItem>> maps = collectLocalMaps();
		maps.remove(newRegion);
		return maps;
	}

	public static long getSize(@NonNull List<LocalItem> items) {
		long size = 0;
		for (LocalItem item : items) {
			size += item.getSize();
		}
		return size;
	}

	/**
	 * The old free version counted 7 downloads of any region. An existing user gets the region of
	 * their current location if they have its maps, otherwise the region with the most map data.
	 * Everything already downloaded stays and keeps free updates; a wrong choice is fixed by a replace.
	 */
	private void migrateIfNeeded() {
		// the region tree is loaded in the background on app start - without it no map has a region
		if (preferences.getBoolean(MIGRATED, false) || !isFreeVersion() || !regions.isInitialized()) {
			return;
		}
		preferences.edit().putBoolean(MIGRATED, true).apply();
		if (!Algorithms.isEmpty(preferences.getString(REGION, null))) {
			return;
		}
		Map<WorldRegion, List<LocalItem>> maps = collectLocalMaps();
		if (maps.isEmpty()) {
			return;
		}
		WorldRegion region = getLocationRegion(maps.keySet());
		if (region == null) {
			long maxSize = -1;
			for (Map.Entry<WorldRegion, List<LocalItem>> entry : maps.entrySet()) {
				long size = getSize(entry.getValue());
				if (size > maxSize) {
					maxSize = size;
					region = entry.getKey();
				}
			}
		}
		if (region != null) {
			setFreeRegion(region);
			LOG.info("Free region migrated: " + region.getRegionId() + " of " + maps.size() + " regions");
		}
	}

	@Nullable
	private WorldRegion getLocationRegion(@NonNull Set<WorldRegion> candidates) {
		Location location = app.getLocationProvider().getLastKnownLocation();
		if (location == null) {
			return null;
		}
		try {
			LatLon latLon = new LatLon(location.getLatitude(), location.getLongitude());
			for (WorldRegion region : regions.getWorldRegionsAt(latLon)) {
				WorldRegion freeRegion = getRegion(region);
				if (freeRegion != null && candidates.contains(freeRegion)) {
					return freeRegion;
				}
			}
		} catch (IOException e) {
			LOG.error(e);
		}
		return null;
	}
}

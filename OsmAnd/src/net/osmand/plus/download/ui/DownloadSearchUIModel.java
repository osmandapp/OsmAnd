package net.osmand.plus.download.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Content of the "Maps & Resources" search results screen: rows are split into the "Regions" and
 * "Maps" sections, and every row may have a second line with the region it belongs to.
 */
public class DownloadSearchUIModel {

	private static final Log LOG = PlatformUtil.getLog(DownloadSearchUIModel.class);

	private static final String CITY_SUBTYPE = "city";
	private static final String TOWN_SUBTYPE = "town";
	private static final List<String> CITY_SUBTYPES = Arrays.asList(CITY_SUBTYPE, TOWN_SUBTYPE);
	private static final int SEARCH_CITY_LIMIT = 10000;

	/** Non-clickable row that separates the sections of the search results list. */
	public static class SectionHeader {

		@StringRes
		private final int titleId;

		public SectionHeader(@StringRes int titleId) {
			this.titleId = titleId;
		}

		@StringRes
		public int getTitleId() {
			return titleId;
		}

		@NonNull
		public String getTitle(@NonNull Context ctx) {
			return ctx.getString(titleId);
		}
	}

	private final Context ctx;
	private final OsmandApplication app;
	private final OsmandRegions osmandRegions;
	private final boolean showGroup;
	private final List<String> downloadTypesToShow;

	private SearchRequest<Amenity> cityRequest;

	public DownloadSearchUIModel(@NonNull Context ctx, @NonNull OsmandRegions osmandRegions,
	                             boolean showGroup, @NonNull List<String> downloadTypesToShow) {
		this.ctx = ctx;
		this.app = (OsmandApplication) ctx.getApplicationContext();
		this.osmandRegions = osmandRegions;
		this.showGroup = showGroup;
		this.downloadTypesToShow = downloadTypesToShow;
	}

	/**
	 * Looks up the cities named like the request in the world basemap, so that a city can be found
	 * even when the map covering it is named after the region it belongs to.
	 *
	 * @return cities matching the request, with no map resolved yet.
	 */
	@NonNull
	public List<CityItem> searchCities(@NonNull String text) throws IOException {
		File obf = getWorldBaseMapObf();
		if (obf == null) {
			obf = getWorldBaseMapMiniObf();
		}
		if (obf == null) {
			return new ArrayList<>();
		}
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		SearchSettings searchSettings = searchUICore.getSearchSettings();
		SearchPhrase searchPhrase = searchUICore.getPhrase().generateNewPhrase(text, searchSettings);
		NameStringMatcher matcher = searchPhrase.getFirstUnknownNameStringMatcher();

		String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean translit = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		List<Amenity> amenities = new ArrayList<>();
		SearchRequest<Amenity> request = BinaryMapIndexReader.buildSearchPoiRequest(
				0, 0,
				text,
				Integer.MIN_VALUE, Integer.MAX_VALUE,
				Integer.MIN_VALUE, Integer.MAX_VALUE,
				new ResultMatcher<Amenity>() {
					int count;

					@Override
					public boolean publish(Amenity amenity) {
						if (count++ > SEARCH_CITY_LIMIT) {
							return false;
						}
						List<String> otherNames = amenity.getOtherNames(true);
						String localeName = amenity.getName(lang, translit);
						String subType = amenity.getSubType();
						if (!CITY_SUBTYPES.contains(subType)
								|| (!matcher.matches(localeName) && !matcher.matches(otherNames))) {
							return false;
						}
						amenities.add(amenity);
						return false;
					}

					@Override
					public boolean isCancelled() {
						return count > SEARCH_CITY_LIMIT;
					}
				});

		cityRequest = request;
		BinaryMapIndexReader baseMapReader = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
		try {
			baseMapReader.searchPoiByName(request);
		} finally {
			try {
				baseMapReader.close();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		List<CityItem> cities = new ArrayList<>();
		for (Amenity amenity : amenities) {
			// the request is matched against the localized name, so show that one in the row
			cities.add(new CityItem(amenity.getName(lang, translit), amenity, null));
		}
		return cities;
	}

	/** Stops the running city search, the results of an outdated request are not needed. */
	public void cancelCitySearch() {
		if (cityRequest != null) {
			cityRequest.setInterrupted(true);
		}
	}

	@Nullable
	private File getWorldBaseMapObf() {
		IndexItem worldBaseMapItem = app.getDownloadThread().getIndexes().getWorldBaseMapItem();
		if (worldBaseMapItem != null && worldBaseMapItem.isDownloaded()) {
			File obf = worldBaseMapItem.getTargetFile(app);
			if (obf.exists()) {
				return obf;
			}
		}
		return null;
	}

	@Nullable
	private File getWorldBaseMapMiniObf() {
		File mapsPath = app.getAppPath(IndexConstants.MAPS_PATH);
		String fileName = WorldRegion.WORLD_BASEMAP_MINI + IndexConstants.BINARY_MAP_INDEX_EXT;
		File obf = new File(mapsPath, fileName);
		return obf.exists() ? obf : null;
	}

	/**
	 * @param indexes       all available resources.
	 * @param searchRequest text typed by the user.
	 * @param cities        cities found in the world basemap, already matched by the caller.
	 * @return rows to be shown on the screen: section headers, regions and maps.
	 */
	@NonNull
	public List<Object> search(@NonNull DownloadResources indexes, @NonNull String searchRequest,
	                           @NonNull List<CityItem> cities) {
		List<Object> found = new ArrayList<>();
		processGroup(indexes, found, parseConditions(searchRequest));
		found.addAll(0, filterDuplicateCities(indexes, cities, found));
		return groupBySections(found);
	}

	/** Sorts the regions and the maps separately and puts a header in front of each of them. */
	@NonNull
	private List<Object> groupBySections(@NonNull List<Object> found) {
		List<Object> regions = new ArrayList<>();
		List<Object> maps = new ArrayList<>();
		for (Object row : found) {
			(isRegion(row) ? regions : maps).add(row);
		}
		Collator collator = OsmAndCollator.primaryCollator();
		Comparator<Object> comparator = (row1, row2) -> collator.compare(getSortName(row1), getSortName(row2));
		Collections.sort(regions, comparator);
		Collections.sort(maps, comparator);

		List<Object> result = new ArrayList<>();
		addSection(result, R.string.regions, regions);
		addSection(result, R.string.shared_string_maps, maps);
		return result;
	}

	private void addSection(@NonNull List<Object> result, @StringRes int titleId, @NonNull List<Object> rows) {
		if (!rows.isEmpty()) {
			result.add(new SectionHeader(titleId));
			result.addAll(rows);
		}
	}

	public static boolean isRegion(@NonNull Object row) {
		return row instanceof DownloadResourceGroup;
	}

	/** @return name shown in the first line of the row. */
	@NonNull
	public String getTitle(@NonNull Object row) {
		if (row instanceof DownloadResourceGroup group) {
			return group.getName(ctx);
		}
		if (row instanceof CityItem city) {
			return city.getName();
		}
		return ((DownloadItem) row).getVisibleName(ctx, osmandRegions, false);
	}

	/**
	 * @return name of the region the row belongs to, shown in the second line: the country for
	 * regions and maps and the covering map for cities, or {@code null} when the first line
	 * already says it.
	 */
	@Nullable
	public String getSubtitle(@NonNull Object row) {
		String subtitle = null;
		if (row instanceof DownloadResourceGroup group) {
			subtitle = getCountryName(group.getRegion());
		} else if (row instanceof CityItem city) {
			IndexItem indexItem = city.getIndexItem();
			if (indexItem != null) {
				subtitle = indexItem.getVisibleName(ctx, osmandRegions, false);
				if (Algorithms.isEmpty(subtitle) || subtitle.equals(city.getName())) {
					// the map is named after the city itself, the country says more
					subtitle = getCountryName(getItemRegion(indexItem));
				}
			}
		} else if (row instanceof DownloadItem item) {
			subtitle = getCountryName(getItemRegion(item));
		}
		return Algorithms.isEmpty(subtitle) || subtitle.equals(getTitle(row)) ? null : subtitle;
	}

	@Nullable
	private String getCountryName(@Nullable WorldRegion region) {
		WorldRegion country = region != null ? region.getCountryRegion() : null;
		return country != null ? country.getLocaleName() : null;
	}

	@Nullable
	private WorldRegion getItemRegion(@NonNull DownloadItem item) {
		WorldRegion region = DownloadResourceGroup.getRegion(item.getRelatedGroup());
		return region != null ? region : osmandRegions.getRegionDataByDownloadName(item.getBasename());
	}

	/**
	 * A city is shown as a shortcut to the map that covers it, so it is dropped when that map is
	 * already listed - otherwise the same file is offered twice - and when another city already
	 * points to the same map.
	 */
	@NonNull
	private List<CityItem> filterDuplicateCities(@NonNull DownloadResources indexes,
	                                             @NonNull List<CityItem> cities, @NonNull List<Object> matched) {
		Set<IndexItem> shownMaps = new HashSet<>();
		for (Object row : matched) {
			if (row instanceof IndexItem item) {
				shownMaps.add(item);
			}
		}
		List<CityItem> result = new ArrayList<>();
		for (CityItem city : cities) {
			if (city.getIndexItem() == null) {
				city.setIndexItem(resolveCityMap(indexes, city));
			}
			IndexItem item = city.getIndexItem();
			if (item == null || shownMaps.add(item)) {
				result.add(city);
			}
		}
		return result;
	}

	/**
	 * @return the standard map covering the city, the same one that is downloaded when the city is
	 * selected in the search results.
	 */
	@Nullable
	private IndexItem resolveCityMap(@NonNull DownloadResources indexes, @NonNull CityItem city) {
		Amenity amenity = city.getAmenity();
		LatLon location = amenity != null ? amenity.getLocation() : null;
		// the region boundaries are kept in memory, so the hundreds of cities a search finds are
		// resolved without reading the region file once
		return location != null ? findMapAt(indexes, osmandRegions.getWorldRegion(), location) : null;
	}

	/**
	 * Walks the region tree down to the smallest region containing the point and returns the map of
	 * the first region on the way back that has one.
	 */
	@Nullable
	private IndexItem findMapAt(@NonNull DownloadResources indexes, @NonNull WorldRegion region,
	                            @NonNull LatLon location) {
		for (WorldRegion subregion : region.getSubregions()) {
			// a region without boundaries only groups its subregions, look inside it anyway
			boolean grouping = !subregion.hasBoundaries();
			if (grouping || subregion.containsPoint(location)) {
				IndexItem item = findMapAt(indexes, subregion, location);
				if (item == null && !grouping) {
					// the subregion is split into parts that have no map of their own
					item = getStandardMap(indexes, subregion);
				}
				if (item != null) {
					return item;
				}
			}
		}
		return null;
	}

	@Nullable
	private IndexItem getStandardMap(@NonNull DownloadResources indexes, @NonNull WorldRegion region) {
		for (IndexItem item : indexes.getIndexItems(region)) {
			if (item.getType() == DownloadActivityType.NORMAL_FILE) {
				return item;
			}
		}
		return null;
	}

	private void processGroup(@NonNull DownloadResourceGroup group, @NonNull List<Object> filter,
	                          @NonNull List<List<String>> conds) {
		String name = null;
		if (group.getRegion() != null && group.getRegion().getRegionSearchText() != null) {
			name = group.getRegion().getRegionSearchText().toLowerCase(Locale.US);
		}
		if (name == null) {
			name = group.getName(ctx).toLowerCase(Locale.US);
		}
		if (group.getType().isScreen() && group.getParentGroup() != null && group.getParentGroup().getParentGroup() != null
				&& group.getParentGroup().getParentGroup().getType() != DownloadResourceGroupType.WORLD
				&& isMatch(conds, false, name)) {

			if (showGroup) {
				filter.add(group);
			}

			for (DownloadResourceGroup g : group.getGroups()) {
				if (g.getType() == DownloadResourceGroupType.REGION_MAPS) {
					if (g.getIndividualResources() != null) {
						for (IndexItem item : g.getIndividualResources()) {
							for (String fileTypeTag : downloadTypesToShow) {
								DownloadActivityType type = DownloadActivityType.getIndexType(fileTypeTag);
								if (type != null && type == item.getType()) {
									filter.add(item);
								}
							}
						}
					}
					break;
				}
			}
		}

		// process other maps & voice prompts & astronomy maps
		if (group.getType() == DownloadResourceGroupType.OTHER_MAPS_HEADER
				|| group.getType() == DownloadResourceGroupType.ASTRONOMY_HEADER
				|| group.getType() == DownloadResourceGroupType.VOICE_HEADER_REC
				|| group.getType() == DownloadResourceGroupType.VOICE_HEADER_TTS
				|| group.getType() == DownloadResourceGroupType.FONTS_HEADER) {
			if (group.getIndividualResources() != null) {
				for (IndexItem item : group.getIndividualResources()) {
					name = item.getVisibleName(ctx, osmandRegions, false).toLowerCase(Locale.US);
					if (isMatch(conds, false, name)) {
						filter.add(item);
						break;
					}
				}
			}
		}

		if (group.getGroups() != null) {
			for (DownloadResourceGroup g : group.getGroups()) {
				processGroup(g, filter, conds);
			}
		}
	}

	/**
	 * Splits the search request into a list of OR-ed conditions, each of them being a list of AND-ed
	 * terms: "germany berlin, brandenburg" matches either an item containing both "germany" and
	 * "berlin" or an item containing "brandenburg".
	 */
	@NonNull
	public static List<List<String>> parseConditions(@NonNull String searchRequest) {
		List<List<String>> conds = new ArrayList<>();
		for (String or : searchRequest.split(",")) {
			List<String> cond = new ArrayList<>();
			for (String term : or.split("\\s")) {
				String t = term.trim().toLowerCase(Locale.US);
				if (!t.isEmpty()) {
					cond.add(t);
				}
			}
			if (!cond.isEmpty()) {
				conds.add(cond);
			}
		}
		return conds;
	}

	public static boolean isMatch(@NonNull List<List<String>> conditions, boolean matchByDefault, @NonNull String text) {
		if (conditions.isEmpty()) {
			return matchByDefault;
		}
		for (List<String> terms : conditions) {
			if (containsAll(text, terms)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsAll(@NonNull String text, @NonNull List<String> terms) {
		for (String term : terms) {
			if (!text.contains(term)) {
				return false;
			}
		}
		return true;
	}

	@NonNull
	private String getSortName(@NonNull Object row) {
		if (row instanceof CityItem city) {
			Amenity amenity = city.getAmenity();
			// cities are shown before towns
			boolean isCity = amenity != null && CITY_SUBTYPE.equals(amenity.getSubType());
			return isCity ? "!" + city.getName() : city.getName();
		}
		return getTitle(row);
	}
}

package net.osmand.plus.download.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Content of the "Maps & Resources" search results screen: rows are split into the "Regions" and
 * "Maps" sections, and every row may have a second line with the region it belongs to.
 */
public class DownloadSearchUIModel {

	private static final Log LOG = PlatformUtil.getLog(DownloadSearchUIModel.class);

	private static final String CITY_SUBTYPE = "city";

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
	private final OsmandRegions osmandRegions;
	private final boolean showGroup;
	private final List<String> downloadTypesToShow;

	public DownloadSearchUIModel(@NonNull Context ctx, @NonNull OsmandRegions osmandRegions,
	                             boolean showGroup, @NonNull List<String> downloadTypesToShow) {
		this.ctx = ctx;
		this.osmandRegions = osmandRegions;
		this.showGroup = showGroup;
		this.downloadTypesToShow = downloadTypesToShow;
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
			subtitle = indexItem != null ? indexItem.getVisibleName(ctx, osmandRegions, false) : null;
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
		List<WorldRegion> matchedRegions = new ArrayList<>();
		for (Object row : matched) {
			if (row instanceof IndexItem item) {
				shownMaps.add(item);
				WorldRegion region = DownloadResourceGroup.getRegion(item.getRelatedGroup());
				if (region != null) {
					matchedRegions.add(region);
				}
			}
		}
		List<CityItem> result = new ArrayList<>();
		for (CityItem city : cities) {
			// resolving a city reads the region file, so it is only done for the cities that could
			// be covered by an already listed map - the rest are resolved when they are displayed
			if (city.getIndexItem() == null && isCoveredByAny(matchedRegions, city)) {
				city.setIndexItem(resolveCityMap(indexes, city));
			}
			IndexItem item = city.getIndexItem();
			if (item == null || shownMaps.add(item)) {
				result.add(city);
			}
		}
		return result;
	}

	private static boolean isCoveredByAny(@NonNull List<WorldRegion> regions, @NonNull CityItem city) {
		Amenity amenity = city.getAmenity();
		LatLon location = amenity != null ? amenity.getLocation() : null;
		if (location == null) {
			return false;
		}
		for (WorldRegion region : regions) {
			if (region.containsPoint(location)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the standard map covering the city, the same one that is downloaded when the city is
	 * selected in the search results.
	 */
	@Nullable
	private IndexItem resolveCityMap(@NonNull DownloadResources indexes, @NonNull CityItem city) {
		Amenity amenity = city.getAmenity();
		if (amenity == null) {
			return null;
		}
		WorldRegion region = null;
		try {
			LatLon location = amenity.getLocation();
			Map.Entry<WorldRegion, BinaryMapDataObject> entry = osmandRegions.getSmallestBinaryMapDataObjectAt(location);
			if (entry != null) {
				region = entry.getKey();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		if (region != null) {
			for (IndexItem item : indexes.getIndexItems(region)) {
				if (item.getType() == DownloadActivityType.NORMAL_FILE) {
					return item;
				}
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

package net.osmand.plus.download;

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
 * Builds the content of the "Maps & Resources" search results screen.
 * <p>
 * Results are split into two sections - {@link SearchSection#REGIONS} and {@link SearchSection#MAPS} -
 * and every row may have a secondary line with the region it belongs to (see {@link #getSubtitle(Object)}).
 */
public class DownloadSearchHelper {

	private static final Log LOG = PlatformUtil.getLog(DownloadSearchHelper.class);

	private static final String CITY_SUBTYPE = "city";

	public enum SearchSection {

		REGIONS(R.string.regions),
		MAPS(R.string.shared_string_maps);

		@StringRes
		private final int titleId;

		SearchSection(@StringRes int titleId) {
			this.titleId = titleId;
		}

		@StringRes
		public int getTitleId() {
			return titleId;
		}
	}

	/**
	 * Non-clickable header row separating {@link SearchSection}s of the search results list.
	 */
	public static class SectionHeader {

		private final SearchSection section;

		public SectionHeader(@NonNull SearchSection section) {
			this.section = section;
		}

		@NonNull
		public SearchSection getSection() {
			return section;
		}

		@NonNull
		public String getTitle(@NonNull Context ctx) {
			return ctx.getString(section.getTitleId());
		}
	}

	private final Context ctx;
	private final OsmandRegions osmandRegions;
	private final boolean showGroup;
	private final List<String> downloadTypesToShow;

	public DownloadSearchHelper(@NonNull Context ctx, @NonNull OsmandRegions osmandRegions,
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
		List<Object> matched = new ArrayList<>();
		processGroup(indexes, matched, parseConditions(searchRequest));
		List<Object> found = new ArrayList<>(filterDuplicateCities(indexes, cities, matched));
		found.addAll(matched);
		return groupBySections(found);
	}

	/**
	 * A city is shown as a shortcut to the map that covers it, so it is dropped when that map is
	 * already listed - otherwise the same file is offered twice - and when another city already
	 * points to the same map.
	 */
	@NonNull
	private List<CityItem> filterDuplicateCities(@NonNull DownloadResources indexes,
	                                             @NonNull List<CityItem> cities,
	                                             @NonNull List<Object> matched) {
		Set<IndexItem> shownMaps = new HashSet<>();
		List<WorldRegion> matchedRegions = new ArrayList<>();
		for (Object object : matched) {
			if (object instanceof IndexItem item) {
				shownMaps.add(item);
				WorldRegion region = DownloadResourceGroup.getRegion(item.getRelatedGroup());
				if (region != null) {
					matchedRegions.add(region);
				}
			}
		}
		List<CityItem> result = new ArrayList<>();
		for (CityItem city : cities) {
			// resolving a city reads the region file, so it is only done for the cities that
			// could be covered by an already listed map - the rest are resolved when displayed
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
			Map.Entry<WorldRegion, BinaryMapDataObject> entry =
					osmandRegions.getSmallestBinaryMapDataObjectAt(amenity.getLocation());
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

	/**
	 * Splits found items into the "Regions" and "Maps" sections, sorts each of them
	 * and inserts a {@link SectionHeader} before every non-empty section.
	 */
	@NonNull
	public List<Object> groupBySections(@NonNull List<Object> found) {
		List<Object> regions = new ArrayList<>();
		List<Object> maps = new ArrayList<>();
		for (Object object : found) {
			if (getSection(object) == SearchSection.REGIONS) {
				regions.add(object);
			} else {
				maps.add(object);
			}
		}
		Comparator<Object> comparator = getComparator();
		Collections.sort(regions, comparator);
		Collections.sort(maps, comparator);

		List<Object> result = new ArrayList<>();
		if (!regions.isEmpty()) {
			result.add(new SectionHeader(SearchSection.REGIONS));
			result.addAll(regions);
		}
		if (!maps.isEmpty()) {
			result.add(new SectionHeader(SearchSection.MAPS));
			result.addAll(maps);
		}
		return result;
	}

	@NonNull
	public static SearchSection getSection(@NonNull Object object) {
		return object instanceof DownloadResourceGroup ? SearchSection.REGIONS : SearchSection.MAPS;
	}

	/**
	 * @return name shown in the first line of the row.
	 */
	@NonNull
	public String getTitle(@NonNull Object object) {
		if (object instanceof DownloadResourceGroup group) {
			return group.getName(ctx);
		}
		if (object instanceof CityItem city) {
			return city.getName();
		}
		DownloadItem item = (DownloadItem) object;
		return item.getVisibleName(ctx, osmandRegions, false);
	}

	/**
	 * @return name of the region the row belongs to, shown in the second line of the row:
	 * the country for regions and maps and the map region for cities.
	 * {@code null} when there is nothing to add to the first line.
	 */
	@Nullable
	public String getSubtitle(@NonNull Object object) {
		String subtitle = null;
		if (object instanceof DownloadResourceGroup group) {
			subtitle = getCountryName(group.getRegion());
		} else if (object instanceof CityItem city) {
			IndexItem indexItem = city.getIndexItem();
			subtitle = indexItem != null ? indexItem.getVisibleName(ctx, osmandRegions, false) : null;
		} else if (object instanceof DownloadItem item) {
			subtitle = getCountryName(getItemRegion(item));
		}
		return Algorithms.isEmpty(subtitle) || subtitle.equals(getTitle(object)) ? null : subtitle;
	}

	@Nullable
	private WorldRegion getItemRegion(@NonNull DownloadItem item) {
		WorldRegion region = DownloadResourceGroup.getRegion(item.getRelatedGroup());
		return region != null ? region : osmandRegions.getRegionDataByDownloadName(item.getBasename());
	}

	/**
	 * @return localized name of the country the region belongs to. Continents and the world itself
	 * have no country, for them {@code null} is returned.
	 */
	@Nullable
	public static String getCountryName(@Nullable WorldRegion region) {
		WorldRegion country = getCountryRegion(region);
		return country != null ? country.getLocaleName() : null;
	}

	/**
	 * Countries are the regions directly under a continent. Regions that are not placed under any
	 * continent (Russia and its subregions) are represented by their topmost non-world parent.
	 */
	@Nullable
	public static WorldRegion getCountryRegion(@Nullable WorldRegion region) {
		if (region == null) {
			return null;
		}
		WorldRegion country = region.getCountryRegion();
		if (country != null) {
			return country;
		}
		WorldRegion result = region;
		while (result.getSuperregion() != null
				&& !WorldRegion.WORLD.equals(result.getSuperregion().getRegionId())) {
			result = result.getSuperregion();
		}
		return WorldRegion.WORLD.equals(result.getRegionId()) ? null : result;
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
		if (group.getType().isScreen() && group.getParentGroup() != null
				&& group.getParentGroup().getParentGroup() != null
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
	 * Splits the search request into a list of OR-ed conditions, each of them being
	 * a list of AND-ed terms: "germany berlin, brandenburg" matches either an item
	 * containing both "germany" and "berlin" or an item containing "brandenburg".
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

	public static boolean isMatch(@NonNull List<List<String>> conditions, boolean matchByDefault,
	                              @NonNull String text) {
		boolean res = matchByDefault;
		for (List<String> or : conditions) {
			boolean tadd = true;
			for (String var : or) {
				if (!text.contains(var)) {
					tadd = false;
					break;
				}
			}
			if (!tadd) {
				res = false;
			} else {
				res = true;
				break;
			}
		}
		return res;
	}

	@NonNull
	private Comparator<Object> getComparator() {
		Collator collator = OsmAndCollator.primaryCollator();
		return (obj1, obj2) -> collator.compare(getSortName(obj1), getSortName(obj2));
	}

	@NonNull
	private String getSortName(@NonNull Object object) {
		if (object instanceof CityItem city) {
			Amenity amenity = city.getAmenity();
			// cities are shown before towns
			boolean isCity = amenity != null && CITY_SUBTYPE.equals(amenity.getSubType());
			return isCity ? "!" + city.getName() : city.getName();
		}
		return getTitle(object);
	}
}

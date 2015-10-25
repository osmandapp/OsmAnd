package net.osmand.plus;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.util.Algorithms;
import android.content.res.Resources;

public class WorldRegion {

	public static final String AFRICA_REGION_ID = "africa";
	public static final String ASIA_REGION_ID = "asia";
	public static final String AUSTRALIA_AND_OCEANIA_REGION_ID = "australia-oceania";
	public static final String CENTRAL_AMERICA_REGION_ID = "centralamerica";
	public static final String EUROPE_REGION_ID = "europe";
	public static final String NORTH_AMERICA_REGION_ID = "northamerica";
	public static final String RUSSIA_REGION_ID = "russia";
	public static final String SOUTH_AMERICA_REGION_ID = "southamerica";
	public static final String WORLD = "world";

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(WorldRegion.class);

	// Region data
	private String regionId;
	private String downloadsId;
	private String name;
	private String searchText;
	private LatLon center;

	// Hierarchy
	private WorldRegion superregion;
	private List<WorldRegion> subregions;
	private List<WorldRegion> flattenedSubregions;

	public String getLang(OsmandRegions osmandRegions) {
		return osmandRegions.getLang(regionId);
	}

	public String getMetric(OsmandRegions osmandRegions) {
		return osmandRegions.getMetric(regionId);
	}

	public String getLeftHandDriving(OsmandRegions osmandRegions) {
		return osmandRegions.getLeftHandDriving(regionId);
	}

	public String getRoadSigns(OsmandRegions osmandRegions) {
		return osmandRegions.getRoadSigns(regionId);
	}

	public String getRegionId() {
		return regionId;
	}

	public String getDownloadsId() {
		return downloadsId;
	}

	public String getName() {
		return name;
	}

	public WorldRegion getSuperregion() {
		return superregion;
	}

	public List<WorldRegion> getSubregions() {
		return subregions;
	}

	public List<WorldRegion> getFlattenedSubregions() {
		return flattenedSubregions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorldRegion that = (WorldRegion) o;

		return !(name != null ? !name.toLowerCase().equals(that.name.toLowerCase()) : that.name != null);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	public WorldRegion() {
		superregion = null;
		subregions = new LinkedList<>();
		flattenedSubregions = new LinkedList<>();
	}

	public void initWorld() {
		regionId = "";
		downloadsId = WORLD;
		name = "";
		superregion = null;
	}

	private WorldRegion init(String regionId, OsmandRegions osmandRegions, String name) {
		this.regionId = regionId;
		String downloadName = osmandRegions.getDownloadName(regionId);
		if (downloadName != null) {
			this.searchText = osmandRegions.getDownloadNameIndexLowercase(downloadName);
			downloadsId = downloadName.toLowerCase();
		} else {
			downloadsId = regionId.toLowerCase();
		}
		if (name != null) {
			this.name = name;
		} else {
			this.name = osmandRegions.getLocaleNameByFullName(regionId, false);
			if (this.name == null) {
				this.name = capitalize(regionId.replace('_', ' '));
			}
		}
		return this;
	}
	
	public String getSearchText() {
		return searchText;
	}

	private void addSubregion(WorldRegion subregion, WorldRegion world) {
		subregion.superregion = this;
		subregions.add(subregion);
		world.flattenedSubregions.add(subregion);
	}

	public void loadWorldRegions(OsmandApplication app) {
		OsmandRegions osmandRegions = app.getRegions();
		Map<String, String> loadedItems = osmandRegions.getFullNamesToLowercaseCopy();
		if (loadedItems.size() == 0) {
			return;
		}
		HashMap<String, WorldRegion> regionsLookupTable = new HashMap<>(loadedItems.size());
		// Create main regions
		Resources res = app.getResources();

		WorldRegion africaRegion = createRegionAs(AFRICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_africa));
		addSubregion(africaRegion, this);
		regionsLookupTable.put(africaRegion.regionId, africaRegion);

		WorldRegion asiaRegion = createRegionAs(ASIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_asia));
		addSubregion(asiaRegion, this);
		regionsLookupTable.put(asiaRegion.regionId, asiaRegion);

		WorldRegion australiaAndOceaniaRegion = createRegionAs(AUSTRALIA_AND_OCEANIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_oceania));
		addSubregion(australiaAndOceaniaRegion, this);
		regionsLookupTable.put(australiaAndOceaniaRegion.regionId, australiaAndOceaniaRegion);

		WorldRegion centralAmericaRegion = createRegionAs(CENTRAL_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_central_america));
		addSubregion(centralAmericaRegion, this);
		regionsLookupTable.put(centralAmericaRegion.regionId, centralAmericaRegion);

		WorldRegion europeRegion = createRegionAs(EUROPE_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_europe));
		addSubregion(europeRegion, this);
		regionsLookupTable.put(europeRegion.regionId, europeRegion);

		WorldRegion northAmericaRegion = createRegionAs(NORTH_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_north_america));
		addSubregion(northAmericaRegion, this);
		regionsLookupTable.put(northAmericaRegion.regionId, northAmericaRegion);

		WorldRegion russiaRegion = createRegionAs(RUSSIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_russia));
		addSubregion(russiaRegion, this);
		regionsLookupTable.put(russiaRegion.regionId, russiaRegion);

		WorldRegion southAmericaRegion = createRegionAs(SOUTH_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_south_america));
		addSubregion(southAmericaRegion, this);
		regionsLookupTable.put(southAmericaRegion.regionId, southAmericaRegion);

		// Process all regions
		for (; ; ) {
			int processedRegions = 0;
			Iterator<Entry<String, String>> iterator = loadedItems.entrySet().iterator();
			while (iterator.hasNext()) {
				String regionId = iterator.next().getKey();
				String parentRegionId = osmandRegions.getParentFullName(regionId);
				if (parentRegionId == null) {
					continue;
				}

				// Try to find parent of this region
				WorldRegion parentRegion = regionsLookupTable.get(parentRegionId);
				if (parentRegion == null) {
					continue;
				}

				WorldRegion newRegion = new WorldRegion().init(regionId, osmandRegions, null);
				parentRegion.addSubregion(newRegion, this);
				regionsLookupTable.put(newRegion.regionId, newRegion);

				// Remove
				processedRegions++;
				iterator.remove();
			}

			// If all remaining are orphans, that's all
			if (processedRegions == 0)
				break;
		}

		Comparator<WorldRegion> nameComparator = new Comparator<WorldRegion>() {
			@Override
			public int compare(WorldRegion w1, WorldRegion w2) {
				return w1.getName().compareTo(w2.getName());
			}
		};
		sortSubregions(this, nameComparator);

		if (loadedItems.size() > 0) {
			LOG.warn("Found orphaned regions: " + loadedItems.size());
			for (String regionId : loadedItems.keySet()) {
				LOG.warn("FullName = " + regionId + " parent=" + osmandRegions.getParentFullName(regionId));
			}
		}
	}

	private void sortSubregions(WorldRegion region, Comparator<WorldRegion> comparator) {
		Collections.sort(region.subregions, comparator);
		for (WorldRegion r : region.subregions) {
			if (r.subregions.size() > 0) {
				sortSubregions(r, comparator);
			}
		}
	}

	private static WorldRegion createRegionAs(String regionId, Map<String, String> loadedItems,
			OsmandRegions osmandRegions, String localizedName) {
		WorldRegion worldRegion = new WorldRegion().init(regionId, osmandRegions, localizedName);
		loadedItems.remove(regionId);
		return worldRegion;
	}
	
	public LatLon getCenter() {
		// TODO
		return center;
	}

	private String capitalize(String s) {
		String[] words = s.split(" ");
		if (words[0].length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(Algorithms.capitalizeFirstLetterAndLowercase(words[0]));
			for (int i = 1; i < words.length; i++) {
				sb.append(" ");
				sb.append(Algorithms.capitalizeFirstLetterAndLowercase(words[i]));
			}
			return sb.toString();
		} else {
			return s;
		}
	}

	public WorldRegion getRegionById(String regionId) {
		if (regionId.length() == 0) {
			return this;
		} else {
			for (WorldRegion region : flattenedSubregions) {
				if (region != null && region.getRegionId().equals(regionId)) {
					return region;
				}
			}
		}
		return null;
	}

	
}
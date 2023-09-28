package net.osmand.map;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WorldRegion implements Serializable {

	public static final String WORLD_BASEMAP = "world_basemap";
	public static final String WORLD_BASEMAP_MINI = "world_basemap_mini";
	public static final String ANTARCTICA_REGION_ID = "antarctica";
	public static final String AFRICA_REGION_ID = "africa";
	public static final String ASIA_REGION_ID = "asia";
	public static final String AUSTRALIA_AND_OCEANIA_REGION_ID = "australia-oceania-all";
	public static final String CENTRAL_AMERICA_REGION_ID = "centralamerica";
	public static final String EUROPE_REGION_ID = "europe";
	public static final String NORTH_AMERICA_REGION_ID = "northamerica";
	public static final String RUSSIA_REGION_ID = "russia";
	public static final String JAPAN_REGION_ID = "japan_asia";
	public static final String GERMANY_REGION_ID = "europe_germany";
	public static final String FRANCE_REGION_ID = "europe_france";
	public static final String SOUTH_AMERICA_REGION_ID = "southamerica";
	public static final String WORLD = "world";
	public static final String UNITED_KINGDOM_REGION_ID = "europe_gb";

	// Hierarchy
	protected WorldRegion superregion;
	protected List<WorldRegion> subregions;

	// filled by osmand regions
	protected RegionParams params = new RegionParams();
	protected String regionFullName;
	protected String regionParentFullName;
	protected String regionName;
	protected String regionNameEn;
	protected String regionNameLocale;
	protected String regionSearchText;
	protected String regionDownloadName;
	protected boolean regionMapDownload;
	protected boolean regionRoadsDownload;
	protected LatLon regionCenter;
	protected QuadRect boundingBox;
	protected List<LatLon> polygon;
	protected List<List<LatLon>> additionalPolygons = new ArrayList<>();

	public static class RegionParams {
		protected String regionLeftHandDriving;
		protected String regionLang;
		protected String regionMetric;
		protected String regionRoadSigns;
		protected String wikiLink;
		protected String population;

		public String getRegionLeftHandDriving() {
			return regionLeftHandDriving;
		}

		public String getRegionLang() {
			return regionLang;
		}

		public String getRegionMetric() {
			return regionMetric;
		}

		public String getRegionRoadSigns() {
			return regionRoadSigns;
		}

		public String getWikiLink() {
			return wikiLink;
		}

		public String getPopulation() {
			return population;
		}
	}

	public boolean isRegionMapDownload() {
		return regionMapDownload;
	}

	public boolean isRegionRoadsDownload() {
		return regionRoadsDownload;
	}

	public String getLocaleName() {
		if (!Algorithms.isEmpty(regionNameLocale)) {
			return regionNameLocale;
		}
		if (!Algorithms.isEmpty(regionNameEn)) {
			return regionNameEn;
		}
		if (!Algorithms.isEmpty(regionName)) {
			return regionName;
		}

		return capitalize(regionFullName.replace('_', ' '));
	}

	public String getRegionDownloadName() {
		return regionDownloadName;
	}

	public String getRegionDownloadNameLC() {
		return regionDownloadName == null ? null : regionDownloadName.toLowerCase();
	}

	public RegionParams getParams() {
		return params;
	}

	public LatLon getRegionCenter() {
		return regionCenter;
	}

	public String getRegionSearchText() {
		return regionSearchText;
	}

	public WorldRegion getSuperregion() {
		return superregion;
	}

	public List<WorldRegion> getSuperRegions() {
		List<WorldRegion> regions = new ArrayList<>();
		collectSuperRegions(regions, superregion);
		return regions;
	}

	private void collectSuperRegions(List<WorldRegion> regions, WorldRegion region) {
		if (region != null) {
			regions.add(region);
			collectSuperRegions(regions, region.getSuperregion());
		}
	}

	public List<WorldRegion> getSubregions() {
		return subregions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorldRegion that = (WorldRegion) o;

		return !(regionFullName != null ? !regionFullName.equalsIgnoreCase(that.regionFullName) : that.regionFullName != null);
	}

	@Override
	public int hashCode() {
		return regionFullName != null ? regionFullName.hashCode() : 0;
	}

	public WorldRegion(String regionFullName, String downloadName) {
		this.regionFullName = regionFullName;
		this.regionDownloadName = downloadName;
		superregion = null;
		subregions = new LinkedList<>();

	}

	public WorldRegion(String id) {
		this(id, null);
	}

	public String getRegionId() {
		return regionFullName;
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

	public void addSubregion(WorldRegion rd) {
		subregions.add(rd);
		rd.superregion = this;
	}

	public int getLevel() {
		int res = 0;
		WorldRegion parent = superregion;
		while (parent != null) {
			parent = parent.superregion;
			res++;
		}
		return res;
	}

	public boolean containsRegion(WorldRegion another) {
		// Firstly check rectangles for greater efficiency
		if (!containsBoundingBox(another.boundingBox)) {
			return false;
		}

		// Secondly check whole polygons
		if (!containsPolygon(another.polygon)) {
			return false;
		}

		// Finally check inner point
		boolean isInnerPoint = another.containsPoint(another.regionCenter);
		if (isInnerPoint) {
			return containsPoint(another.regionCenter);
		} else {
			// in this case we should find real inner point and check it
		}
		return true;
	}

	public boolean containsBoundingBox(QuadRect rectangle) {
		return (boundingBox != null && rectangle != null) &&
				boundingBox.contains(rectangle);
	}

	private boolean containsPolygon(List<LatLon> another) {
		return (polygon != null && another != null) &&
				Algorithms.isFirstPolygonInsideSecond(another, polygon);
	}

	public boolean containsPoint(LatLon latLon) {
		return polygon != null && Algorithms.isPointInsidePolygon(latLon, polygon);
	}

	public boolean isContinent() {
		if (superregion != null) {
			String superRegionId = superregion.getRegionId();
			String thisRegionId = getRegionId();
			return WORLD.equals(superRegionId) && !RUSSIA_REGION_ID.equals(thisRegionId);
		}
		return false;
	}

	public static List<WorldRegion> removeDuplicates(List<WorldRegion> regions) {
		List<WorldRegion> copy = new ArrayList<>(regions);
		Set<WorldRegion> duplicates = new HashSet<>();
		for (int i = 0; i < copy.size() - 1; i++) {
			WorldRegion r1 = copy.get(i);
			for (int j = i + 1; j < copy.size(); j++) {
				WorldRegion r2 = copy.get(j);
				if (r1.containsRegion(r2)) {
					duplicates.add(r2);
				} else if (r2.containsRegion(r1)) {
					duplicates.add(r1);
				}
			}
		}
		copy.removeAll(duplicates);
		return copy;
	}

	public String getObfFileName() {
		return getObfFileName(regionDownloadName);
	}

	public String getRoadObfFileName() {
		return getRoadObfFileName(regionDownloadName);
	}

	public static String getObfFileName(String regionDownloadName) {
		return Algorithms.capitalizeFirstLetterAndLowercase(regionDownloadName) + IndexConstants.BINARY_MAP_INDEX_EXT;
	}

	public static String getRoadObfFileName(String regionDownloadName) {
		return Algorithms.capitalizeFirstLetterAndLowercase(regionDownloadName) + ".road" + IndexConstants.BINARY_MAP_INDEX_EXT;
	}

	public static String getRegionDownloadName(String obfFileName) {
		String obfExt = IndexConstants.BINARY_MAP_INDEX_EXT;
		String roadObfExt = ".road" + IndexConstants.BINARY_MAP_INDEX_EXT;
		if (obfFileName.endsWith(roadObfExt)) {
			return obfFileName.toLowerCase().substring(0, obfFileName.length() - roadObfExt.length());
		} else if (obfFileName.endsWith(obfExt)) {
			return obfFileName.toLowerCase().substring(0, obfFileName.length() - obfExt.length());
		} else {
			return obfFileName.toLowerCase();
		}
	}

	public QuadRect getBoundingBox() {
		return boundingBox;
	}

	public List<List<LatLon>> getPolygons() {
		List<List<LatLon>> polygons = new ArrayList<>();
		polygons.add(polygon);
		polygons.addAll(additionalPolygons);
		return polygons;
	}

	@Override
	public String toString() {
		return getRegionId();
	}
}
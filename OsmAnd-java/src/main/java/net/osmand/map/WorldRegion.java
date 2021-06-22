package net.osmand.map;

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
	public static final String ANTARCTICA_REGION_ID = "antarctica";
	public static final String AFRICA_REGION_ID = "africa";
	public static final String ASIA_REGION_ID = "asia";
	public static final String AUSTRALIA_AND_OCEANIA_REGION_ID = "australia-oceania";
	public static final String CENTRAL_AMERICA_REGION_ID = "centralamerica";
	public static final String EUROPE_REGION_ID = "europe";
	public static final String NORTH_AMERICA_REGION_ID = "northamerica";
	public static final String RUSSIA_REGION_ID = "russia";
	public static final String JAPAN_REGION_ID = "japan_asia";
	public static final String SOUTH_AMERICA_REGION_ID = "southamerica";
	protected static final String WORLD = "world";

	// Just a string constant
	public static final String UNITED_KINGDOM_REGION_ID = "gb_europe";

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
	protected LatLon regionCenter;
	protected QuadRect boundingBox;
	protected List<LatLon> polygon;

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

	public String getLocaleName() {
		if(!Algorithms.isEmpty(regionNameLocale)) {
			return regionNameLocale;
		}
		if(!Algorithms.isEmpty(regionNameEn)) {
			return regionNameEn;
		}
		if(!Algorithms.isEmpty(regionName)) {
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

	public List<WorldRegion> getSubregions() {
		return subregions;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorldRegion that = (WorldRegion) o;

		return !(regionFullName != null ? !regionFullName.toLowerCase().equals(that.regionFullName.toLowerCase()) : that.regionFullName != null);
	}

	@Override
	public int hashCode() {
		return regionFullName != null ? regionFullName.hashCode() : 0;
	}

	public WorldRegion(String regionFullName, String downloadName) {
		this.regionFullName = regionFullName;
		this.regionDownloadName = downloadName;
		superregion = null;
		subregions = new LinkedList<WorldRegion>();

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

	public boolean containsRegion(WorldRegion region) {
		if (containsBoundingBox(region.boundingBox)) {
			// check polygon only if bounding box match
			return containsPolygon(region.polygon);
		}
		return false;
	}

	private boolean containsBoundingBox(QuadRect rectangle) {
		return (boundingBox != null && rectangle != null) &&
				boundingBox.contains(rectangle);
	}

	private boolean containsPolygon(List<LatLon> another) {
		return (polygon != null && another != null) &&
				Algorithms.isFirstPolygonInsideSecond(another, polygon);
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
}
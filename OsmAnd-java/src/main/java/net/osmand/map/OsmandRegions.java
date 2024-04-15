package net.osmand.map;

import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;

public class OsmandRegions {

	public static final String MAP_TYPE = "region_map";
	public static final String ROADS_TYPE = "region_roads";
	public static final String MAP_JOIN_TYPE = "region_join_map";
	public static final String ROADS_JOIN_TYPE = "region_join_roads";

	public static final String FIELD_DOWNLOAD_NAME = "download_name";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_NAME_EN = "name:en";
	public static final String FIELD_REGION_PARENT_NAME = "region_parent_name";
	public static final String FIELD_REGION_FULL_NAME = "region_full_name";
	public static final String FIELD_LANG = "region_lang";
	public static final String FIELD_METRIC = "region_metric";
	public static final String FIELD_ROAD_SIGNS = "region_road_signs";
	public static final String FIELD_LEFT_HAND_DRIVING = "region_left_hand_navigation";
	public static final String FIELD_WIKI_LINK = "region_wiki_link";
	public static final String FIELD_POPULATION = "region_population";
	public static final String LOCALE_NAME_DEFAULT_FORMAT = "%1$s %2$s";
	public static final String LOCALE_NAME_REVERSED_FORMAT = "%2$s, %1$s";

	private BinaryMapIndexReader reader;
	private String locale = "en";
	// locale including region
	private String locale2 = null;
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandRegions.class);

	WorldRegion worldRegion = new WorldRegion(WorldRegion.WORLD);
	Map<String, WorldRegion> fullNamesToRegionData = new HashMap<>();
	Map<String, String> downloadNamesToFullNames = new HashMap<>();
	Map<String, LinkedList<BinaryMapDataObject>> countriesByDownloadName = new HashMap<>();


	QuadTree<String> quadTree;
	MapIndexFields mapIndexFields;
	RegionTranslation translator;

	private static class MapIndexFields {

		Integer parentFullName = null;
		Integer fullNameType = null;
		Integer downloadNameType = null;
		Integer nameEnType = null;
		Integer nameType = null;
		Integer nameLocaleType = null;
		Integer nameLocale2Type = null;
		Integer langType = null;
		Integer metricType = null;
		Integer leftHandDrivingType = null;
		Integer roadSignsType = null;
		Integer wikiLinkType = null;
		Integer populationType = null;

		public String get(Integer tp, BinaryMapDataObject object) {
			if (tp == null) {
				return null;
			}
			return object.getNameByType(tp);
		}
	}

	public void setTranslator(RegionTranslation translator) {
		this.translator = translator;
	}

	public BinaryMapIndexReader prepareFile() throws IOException {
		File regions = new File("regions.ocbf");
		// internal version could be updated
//		if (!regions.exists()) {
			InputStream is = OsmandRegions.class.getResourceAsStream("regions.ocbf");
			FileOutputStream fous = new FileOutputStream(regions);
			Algorithms.streamCopy(is, fous);
			fous.close();
//		}
		return prepareFile(regions.getAbsolutePath());
	}
	
	public BinaryMapIndexReader prepareFile(String fileName) throws IOException {
		reader = new BinaryMapIndexReader(new RandomAccessFile(fileName, "r"), new File(fileName));
//		final Collator clt = OsmAndCollator.primaryCollator();
		final Map<String, String> parentRelations = new LinkedHashMap<>();
		final Map<String, List<BinaryMapDataObject>> unattachedBoundaryMapObjectsByRegions = new HashMap<>();
		final ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {

			@Override
			public boolean publish(BinaryMapDataObject object) {
				initTypes(object);

				boolean boundary = false;
				int[] types = object.getTypes();
				for (int type : types) {
					TagValuePair tp = object.getMapIndex().decodeType(type);
					if ("boundary".equals(tp.value)) {
						boundary = true;
						break;
					}
				}

				if (boundary) {
					String fullRegionName = getFullName(object);
					WorldRegion region = fullNamesToRegionData.get(fullRegionName);
					if (region != null) {
						addPolygonToRegionIfValid(object, region);
					} else {
						List<BinaryMapDataObject> unattachedMapObjects = unattachedBoundaryMapObjectsByRegions.get(fullRegionName);
						if (unattachedMapObjects == null) {
							unattachedMapObjects = new ArrayList<>();
							unattachedBoundaryMapObjectsByRegions.put(fullRegionName, unattachedMapObjects);
						}
						unattachedMapObjects.add(object);
					}
					return false;
				}

				WorldRegion region = initRegionData(parentRelations, object);
				if (region == null) {
					return false;
				}

				List<BinaryMapDataObject> unattachedMapObjects = unattachedBoundaryMapObjectsByRegions.get(region.regionFullName);
				if (unattachedMapObjects != null) {
					for (BinaryMapDataObject mapObject : unattachedMapObjects) {
						addPolygonToRegionIfValid(mapObject, region);
					}
					unattachedBoundaryMapObjectsByRegions.remove(region.regionFullName);
				}

				if (region.regionDownloadName != null) {
					downloadNamesToFullNames.put(region.regionDownloadName, region.regionFullName);
				}
				fullNamesToRegionData.put(region.regionFullName, region);
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		iterateOverAllObjects(resultMatcher);
		// post process download names
		for (Map.Entry<String, String> e : parentRelations.entrySet()) {
			String fullName = e.getKey();
			String parentFullName = e.getValue();
			// String parentParentFulName = parentRelations.get(parentFullName); // could be used for japan/russia
			WorldRegion rd = fullNamesToRegionData.get(fullName);
			WorldRegion parent = fullNamesToRegionData.get(parentFullName);
			if (parent != null && rd != null) {
				parent.addSubregion(rd);
			}
		}
		structureWorldRegions(new ArrayList<>(fullNamesToRegionData.values()));
		return reader;
	}

	public boolean containsCountry(String name) {
		return countriesByDownloadName.containsKey(name);
	}


	public String getLocaleName(String downloadName, boolean includingParent) {
		return getLocaleName(downloadName, includingParent, false);
	}

	public String getLocaleName(String downloadName, boolean includingParent, boolean reversed) {
		String divider = reversed ? ", " : " ";
		return getLocaleName(downloadName, divider, includingParent, reversed);
	}

	public String getLocaleName(String downloadName, String divider, boolean includingParent, boolean reversed) {
		final String lc = downloadName.toLowerCase();
		if (downloadNamesToFullNames.containsKey(lc)) {
			String fullName = downloadNamesToFullNames.get(lc);
			return getLocaleNameByFullName(fullName, divider, includingParent, reversed);
		}
		return downloadName.replace('_', ' ');
	}

	public String getLocaleNameByFullName(String fullName, String divider, boolean includingParent, boolean reversed) {
		WorldRegion region = fullNamesToRegionData.get(fullName);
		if (region == null) {
			return fullName.replace('_', ' ');
		}
		String regionName = region.getLocaleName();
		if (includingParent && region.getSuperregion() != null) {
			WorldRegion parent = region.getSuperregion();
			WorldRegion parentParent = parent.getSuperregion();

			if (parentParent != null) {
				String parentParentId = parentParent.getRegionId();
				if (WorldRegion.WORLD.equals(parentParentId) &&
						!parent.getRegionId().equals(WorldRegion.RUSSIA_REGION_ID)) {
					return regionName;
				}
				if (WorldRegion.RUSSIA_REGION_ID.equals(parentParentId) || WorldRegion.JAPAN_REGION_ID.equals(parentParentId)) {
					String format = reversed ? LOCALE_NAME_REVERSED_FORMAT : LOCALE_NAME_DEFAULT_FORMAT;
					return String.format(format, parentParent.getLocaleName(), regionName);
				}
			}
			List<WorldRegion> superRegions = region.getSuperRegions();
			if (!Algorithms.isEmpty(superRegions)) {
				return getLocaleNameWithParent(superRegions, regionName, divider, reversed);
			}
		}
		return regionName;
	}

	private String getLocaleNameWithParent(List<WorldRegion> superRegions, String regionName, String divider, boolean reversed) {
		StringBuilder builder = new StringBuilder();
		List<String> topRegionsIds = getTopRegionsIds();
		if (reversed) {
			builder.append(regionName);
			for (WorldRegion region : superRegions) {
				String regionId = region.getRegionId();
				if ((!topRegionsIds.contains(regionId) || WorldRegion.RUSSIA_REGION_ID.equals(regionId))
						&& (!WorldRegion.WORLD.equals(regionId))) {
					builder.append(divider).append(region.getLocaleName());
				}
			}
		} else {
			ListIterator<WorldRegion> iterator = superRegions.listIterator(superRegions.size());
			while (iterator.hasPrevious()) {
				WorldRegion region = iterator.previous();
				String regionId = region.getRegionId();
				if ((!topRegionsIds.contains(regionId) || WorldRegion.RUSSIA_REGION_ID.equals(regionId))
						&& (!WorldRegion.WORLD.equals(regionId))) {
					builder.append(region.getLocaleName()).append(divider);
				}
			}
			builder.append(regionName);
		}
		return builder.toString();
	}

	public WorldRegion getWorldRegion() {
		return worldRegion;
	}

	public boolean isInitialized() {
		return reader != null;
	}

	public static boolean contain(BinaryMapDataObject bo, int tx, int ty) {
		int t = 0;
		for (int i = 1; i < bo.getPointsLength(); i++) {
			int fx = MapAlgorithms.ray_intersect_x(bo.getPoint31XTile(i - 1),
					bo.getPoint31YTile(i - 1),
					bo.getPoint31XTile(i),
					bo.getPoint31YTile(i), ty);
			if (Integer.MIN_VALUE != fx && tx >= fx) {
				t++;
			}
		}
		return t % 2 == 1;
	}

	public static boolean intersect(BinaryMapDataObject bo, int lx, int ty, int rx, int by) {
		// 1. polygon in object 
		if (contain(bo, lx, ty)) {
			return true;
		}
		// 2. object in polygon
		if (bo.getPointsLength() == 0) {
			return false;
		}
		if (bo.getPoint31XTile(0) >= lx && bo.getPoint31XTile(0) <= rx &&
				bo.getPoint31YTile(0) >= ty && bo.getPoint31YTile(0) <= by) {
			return true;
		}

		// 3. find intersection
		for (int i = 1; i < bo.getPointsLength(); i++) {
			int px = bo.getPoint31XTile(i - 1);
			int x = bo.getPoint31XTile(i);
			int py = bo.getPoint31YTile(i - 1);
			int y = bo.getPoint31YTile(i);
			if (x < lx && px < lx) {
				continue;
			} else if (x > rx && px > rx) {
				continue;
			} else if (y > by && py > by) {
				continue;
			} else if (y < ty && py < ty) {
				continue;
			}
			long in = MapAlgorithms.calculateIntersection(px, py, x, y, lx, rx, by, ty);
			if (in != -1) {
				return true;
			}
		}

		return false;
	}

	public static double getArea(BinaryMapDataObject bo) {
		double area = 0.0;
		if (bo.getPointsLength() > 0) {
			for (int i = 1; i < bo.getPointsLength(); i++) {
				double ax = bo.getPoint31XTile(i - 1);
				double bx = bo.getPoint31XTile(i);
				double ay = bo.getPoint31YTile(i - 1);
				double by = bo.getPoint31YTile(i);
				area += (bx + ax) * (by - ay) / 1.631E10;
			}
		}
		return Math.abs(area);
	}

	private List<BinaryMapDataObject> getCountries(int lx, int rx, int ty, int by, final boolean checkCenter) throws IOException {
		HashSet<String> set = new HashSet<String>(quadTree.queryInBox(new QuadRect(lx, ty, rx, by),
				new ArrayList<String>()));
		List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
		Iterator<String> it = set.iterator();
		int mx = lx / 2 + rx / 2;
		int my = ty / 2 + by / 2;
		while (it.hasNext()) {
			String cname = it.next();
			BinaryMapDataObject container = null;
			int count = 0;
			for (BinaryMapDataObject bo : countriesByDownloadName.get(cname)) {
				if (!checkCenter || contain(bo, mx, my)) {
					count++;
					container = bo;
					break;
				}
			}
			if (count % 2 == 1) {
				result.add(container);
			}
		}
		return result;
	}
	
	public String getCountryName(LatLon ll) {
		double lat = ll.getLatitude();
		double lon = ll.getLongitude();
		int y = MapUtils.get31TileNumberY(lat);
		int x = MapUtils.get31TileNumberX(lon);
		try {
			List<BinaryMapDataObject> list = query(x, y);
			for (BinaryMapDataObject o : list) {
				if (contain(o, x, y)) {
					String name = mapIndexFields.get(mapIndexFields.nameType, o);
					if (name != null) {
						return name;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<BinaryMapDataObject> query(int lx, int rx, int ty, int by) throws IOException {
		return query(lx, rx, ty, by, true);
	}

	public List<BinaryMapDataObject> query(int lx, int rx, int ty, int by, boolean checkCenter) throws IOException {
		if (quadTree != null) {
			return getCountries(lx, rx, ty, by, checkCenter);
		}
		return queryBboxNoInit(lx, rx, ty, by, checkCenter);
	}

	public List<BinaryMapDataObject> query(final int tile31x, final int tile31y) throws IOException {
		if (quadTree != null) {
			return getCountries(tile31x, tile31x, tile31y, tile31y, true);
		}
		return queryBboxNoInit(tile31x, tile31x, tile31y, tile31y, true);
	}

	private synchronized List<BinaryMapDataObject> queryBboxNoInit(int lx, int rx, int ty, int by, final boolean checkCenter) throws IOException {
		final List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
		final int mx = lx / 2 + rx / 2;
		final int my = ty / 2 + by / 2;
		BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(lx, rx, ty, by,
				5, new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				}, new ResultMatcher<BinaryMapDataObject>() {

					@Override
					public boolean publish(BinaryMapDataObject object) {
						if (object.getPointsLength() < 1) {
							return false;
						}
						initTypes(object);
						if (!checkCenter || contain(object, mx, my)) {
							result.add(object);
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}
		);
		sr.log = false;
		if (reader != null) {
			reader.searchMapIndex(sr);
		} else {
			throw new IOException("Reader == null");
		}
		return result;
	}

	public void setLocale(String locale) {
		setLocale(locale, null);
	}

	public void setLocale(String locale, String country) {
		this.locale = locale;
		// Check locale and give 2 locale names 
		if ("zh".equals(locale)) {
			if ("TW".equalsIgnoreCase(country)) {
				this.locale2 = "zh-hant";
			} else if ("CN".equalsIgnoreCase(country)) {
				this.locale2 = "zh-hans";
			}
		}
	}


	public WorldRegion getRegionData(String fullName) {
		if (WorldRegion.WORLD.equals(fullName)) {
			return worldRegion;
		}
		return fullNamesToRegionData.get(fullName);
	}

	public WorldRegion getRegionDataByDownloadName(String downloadName) {
		if (downloadName == null) {
			return null;
		} else {
			return getRegionData(downloadNamesToFullNames.get(downloadName.toLowerCase()));
		}
	}

	public String getDownloadName(BinaryMapDataObject o) {
		return mapIndexFields.get(mapIndexFields.downloadNameType, o);
	}

	public String getFullName(BinaryMapDataObject o) {
		return mapIndexFields.get(mapIndexFields.fullNameType, o);
	}

	public List<String> getFlattenedWorldRegionIds() {
		List<String> regionIds = new ArrayList<>();
		for (WorldRegion region : getFlattenedWorldRegions()) {
			regionIds.add(region.getRegionId());
		}
		return regionIds;
	}

	public List<WorldRegion> getFlattenedWorldRegions() {
		List<WorldRegion> result = new ArrayList<>();
		result.add(getWorldRegion());
		result.addAll(getAllRegionData());
		return result;
	}

	public List<WorldRegion> getAllRegionData() {
		return new ArrayList<>(fullNamesToRegionData.values());
	}


	private WorldRegion initRegionData(final Map<String, String> parentRelations, BinaryMapDataObject object) {
		String regionDownloadName = mapIndexFields.get(mapIndexFields.downloadNameType, object);
		String regionFullName = mapIndexFields.get(mapIndexFields.fullNameType, object);
		if (Algorithms.isEmpty(regionFullName)) {
			return null;
		}
		WorldRegion rd = new WorldRegion(regionFullName, regionDownloadName);
		double cx = 0;
		double cy = 0;
		for (int i = 0; i < object.getPointsLength(); i++) {
			cx += object.getPoint31XTile(i);
			cy += object.getPoint31YTile(i);
		}
		if (object.getPointsLength() > 0) {
			cx /= object.getPointsLength();
			cy /= object.getPointsLength();
			rd.regionCenter = new LatLon(MapUtils.get31LatitudeY((int) cy), MapUtils.get31LongitudeX((int) cx));
			findBoundaries(rd, object);
		}

		rd.regionParentFullName = mapIndexFields.get(mapIndexFields.parentFullName, object);
		if (!Algorithms.isEmpty(rd.regionParentFullName)) {
			parentRelations.put(rd.regionFullName, rd.regionParentFullName);
		}
		rd.regionName = mapIndexFields.get(mapIndexFields.nameType, object);
		if (mapIndexFields.nameLocale2Type != null) {
			rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocale2Type, object);
		}
		if (rd.regionNameLocale == null) {
			rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocaleType, object);
		}
		rd.regionNameEn = mapIndexFields.get(mapIndexFields.nameEnType, object);
		rd.params.regionLang = mapIndexFields.get(mapIndexFields.langType, object);
		rd.params.regionLeftHandDriving = mapIndexFields.get(mapIndexFields.leftHandDrivingType, object);
		rd.params.regionMetric = mapIndexFields.get(mapIndexFields.metricType, object);
		rd.params.regionRoadSigns = mapIndexFields.get(mapIndexFields.roadSignsType, object);
		rd.params.wikiLink = mapIndexFields.get(mapIndexFields.wikiLinkType, object);
		rd.params.population = mapIndexFields.get(mapIndexFields.populationType, object);
		rd.regionSearchText = getSearchIndex(object);
		rd.regionMapDownload = isDownloadOfType(object, MAP_TYPE);
		rd.regionRoadsDownload = isDownloadOfType(object, ROADS_TYPE);
		return rd;
	}

	private void findBoundaries(WorldRegion rd, BinaryMapDataObject object) {
		if (object.getPointsLength() == 0) {
			return;
		}

		List<LatLon> polygon = new ArrayList<>();
		double x = MapUtils.get31LongitudeX(object.getPoint31XTile(0));
		double y = MapUtils.get31LatitudeY(object.getPoint31YTile(0));
		polygon.add(new LatLon(y, x));
		double minX = x;
		double maxX = x;
		double minY = y;
		double maxY = y;

		if (object.getPointsLength() > 1) {
			for (int i = 1; i < object.getPointsLength(); i++) {
				x = MapUtils.get31LongitudeX(object.getPoint31XTile(i));
				y = MapUtils.get31LatitudeY(object.getPoint31YTile(i));
				if (x > maxX) {
					maxX = x;
				} else if (x < minX) {
					minX = x;
				}
				if (y < maxY) {
					maxY = y;
				} else if (y > minY) {
					minY = y;
				}
				polygon.add(new LatLon(y, x));
			}
		}

		rd.boundingBox = new QuadRect(minX, minY, maxX, maxY);
		rd.polygon = polygon;
	}

	private String getSearchIndex(BinaryMapDataObject object) {
		MapIndex mi = object.getMapIndex();
		TIntObjectIterator<String> it = object.getObjectNames().iterator();
		StringBuilder ind = new StringBuilder();
		while (it.hasNext()) {
			it.advance();
			TagValuePair tp = mi.decodeType(it.key());
			if (tp.tag.startsWith("name") || tp.tag.equals("key_name")) {
				final String vl = it.value().toLowerCase();
//				if (!CollatorStringMatcher.ccontains(clt, ind.toString(), vl)) {
				if (ind.indexOf(vl) == -1) {
					ind.append(" ").append(vl);
				}
			}
		}
		return ind.toString();
	}


	public boolean isDownloadOfType(BinaryMapDataObject object, String type) {
		int[] addtypes = object.getAdditionalTypes();
		for (int i = 0; i < addtypes.length; i++) {
			TagValuePair tp = object.getMapIndex().decodeType(addtypes[i]);
			if (type.equals(tp.tag) && "yes".equals(tp.value)) {
				return true;
			}
		}
		return false;
	}


	public Map<String, LinkedList<BinaryMapDataObject>> cacheAllCountries() throws IOException {
		return cacheAllCountries(true);
	}
	
	public Map<String, LinkedList<BinaryMapDataObject>> cacheAllCountries(final boolean useDownloadName) throws IOException {
		quadTree = new QuadTree<String>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
				8, 0.55f);
		final ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {
//			int c = 0;
			@Override
			public boolean publish(BinaryMapDataObject object) {
				if (object.getPointsLength() < 1) {
					return false;
				}
				initTypes(object);
				String nm = mapIndexFields.get(useDownloadName ? mapIndexFields.downloadNameType : mapIndexFields.fullNameType, object);
				if (!countriesByDownloadName.containsKey(nm)) {
					LinkedList<BinaryMapDataObject> ls = new LinkedList<BinaryMapDataObject>();
					countriesByDownloadName.put(nm, ls);
					ls.add(object);
				} else {
					countriesByDownloadName.get(nm).add(object);
				}
				int maxx = object.getPoint31XTile(0);
				int maxy = object.getPoint31YTile(0);
				int minx = maxx;
				int miny = maxy;
				for (int i = 1; i < object.getPointsLength(); i++) {
					int x = object.getPoint31XTile(i);
					int y = object.getPoint31YTile(i);
					if (y < miny) {
						miny = y;
					} else if (y > maxy) {
						maxy = y;
					}
					if (x < minx) {
						minx = x;
					} else if (x > maxx) {
						maxx = x;
					}
				}
				quadTree.insert(nm, new QuadRect(minx, miny, maxx, maxy));
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		iterateOverAllObjects(resultMatcher);
		return countriesByDownloadName;
	}

	private synchronized void iterateOverAllObjects(final ResultMatcher<BinaryMapDataObject> resultMatcher) throws IOException {
		BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
				5, new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				}, resultMatcher);
		if (reader != null) {
			reader.searchMapIndex(sr);
		}
	}


	private void initTypes(BinaryMapDataObject object) {
		if (mapIndexFields == null) {
			mapIndexFields = new MapIndexFields();
			// mapIndexFields.mapIndex = object.getMapIndex();
			mapIndexFields.downloadNameType = object.getMapIndex().getRule(FIELD_DOWNLOAD_NAME, null);
			mapIndexFields.nameType = object.getMapIndex().getRule(FIELD_NAME, null);
			mapIndexFields.nameEnType = object.getMapIndex().getRule(FIELD_NAME_EN, null);
			mapIndexFields.nameLocaleType = object.getMapIndex().getRule(FIELD_NAME + ":" + locale, null);
			if (locale2 != null) {
				mapIndexFields.nameLocale2Type = object.getMapIndex().getRule(FIELD_NAME + ":" + locale2, null);
			}
			mapIndexFields.parentFullName = object.getMapIndex().getRule(FIELD_REGION_PARENT_NAME, null);
			mapIndexFields.fullNameType = object.getMapIndex().getRule(FIELD_REGION_FULL_NAME, null);
			mapIndexFields.langType = object.getMapIndex().getRule(FIELD_LANG, null);
			mapIndexFields.metricType = object.getMapIndex().getRule(FIELD_METRIC, null);
			mapIndexFields.leftHandDrivingType = object.getMapIndex().getRule(FIELD_LEFT_HAND_DRIVING, null);
			mapIndexFields.roadSignsType = object.getMapIndex().getRule(FIELD_ROAD_SIGNS, null);
			mapIndexFields.wikiLinkType = object.getMapIndex().getRule(FIELD_WIKI_LINK, null);
			mapIndexFields.populationType = object.getMapIndex().getRule(FIELD_POPULATION, null);
		}
	}


	private static void testCountry(OsmandRegions or, double lat, double lon, String... test) throws IOException {
		long t = System.currentTimeMillis();
		List<BinaryMapDataObject> cs = or.query(MapUtils.get31TileNumberX(lon), MapUtils.get31TileNumberY(lat));
		Set<String> expected = new TreeSet<String>(Arrays.asList(test));
		Set<String> found = new TreeSet<String>();

		for (BinaryMapDataObject b : cs) {
			String nm = b.getNameByType(or.mapIndexFields.nameEnType);
			if (nm == null) {
				nm = b.getName();
				System.out.println(or.getLocaleName(or.getDownloadName(b), false));
			}
			if (or.isDownloadOfType(b, MAP_TYPE)) {
				found.add(nm.toLowerCase());
				String localName = b.getNameByType(or.mapIndexFields.nameLocaleType);
				if (or.mapIndexFields.nameLocale2Type != null) {
					localName = b.getNameByType(or.mapIndexFields.nameLocale2Type);
				}
				System.out.printf("Region %s %s%n", b.getName(), localName);
			}
		}

		if (!found.equals(expected)) {
			throw new IllegalStateException(" Expected " + expected + " but was " + found);
		}
		System.out.println("Found " + expected + " in " + (System.currentTimeMillis() - t) + " ms");
	}


	public static void main(String[] args) throws IOException {
		OsmandRegions or = new OsmandRegions();
		Locale tw = Locale.CHINA;
		or.setLocale(tw.getLanguage(), null);
//		or.setLocale(tw.getLanguage(), tw.getCountry());
//		or.prepareFile("/repos/resources/countries-info/regions.ocbf");
		LinkedList<WorldRegion> lst = new LinkedList<WorldRegion>();
		lst.add(or.getWorldRegion());
//		int i =0;
		while (!lst.isEmpty()) {
			WorldRegion wd = lst.pollFirst();
			System.out.println((wd.superregion == null ? "" : wd.superregion.getLocaleName()) + " "
					+ wd.getLocaleName() + " " + wd.getRegionDownloadName());
//			if(i++ <=5)
//				lst.addAll(wd.getSubregions());
		}

		
		or.cacheAllCountries();
//		long t = System.currentTimeMillis();
//		or.cacheAllCountries();
//		System.out.println("Init " + (System.currentTimeMillis() - t));

		testCountry(or, 53.8820, 27.5726, "belarus", "minsk");
//		testCountry(or, 52.10, 4.92, "the netherlands", "utrecht");
//		testCountry(or, 52.15, 7.50, "north rhine-westphalia");
//		testCountry(or, 28.8056, 29.9858, "egypt");
//		testCountry(or, 40.0760, 9.2807, "italy", "sardinia");
//		testCountry(or, 35.7521, 139.7887, "japan");
//		testCountry(or, 46.5145, 102.2580, "mongolia");
//		testCountry(or, 62.54, 43.36, "arkhangelsk oblast", "northwestern federal district");
	}


	public interface RegionTranslation {

		public String getTranslation(String id);
	}

	private void initWorldRegion(WorldRegion world, String id) {
		WorldRegion rg = new WorldRegion(id);
		rg.regionParentFullName = world.regionFullName;
		if (translator != null) {
			rg.regionName = translator.getTranslation(id);
		}
		world.addSubregion(rg);

	}

	private List<String> getTopRegionsIds() {
		List<String> regionIds = new ArrayList<>();
		regionIds.add(WorldRegion.ANTARCTICA_REGION_ID);
		regionIds.add(WorldRegion.AFRICA_REGION_ID);
		regionIds.add(WorldRegion.ASIA_REGION_ID);
		regionIds.add(WorldRegion.CENTRAL_AMERICA_REGION_ID);
		regionIds.add(WorldRegion.EUROPE_REGION_ID);
		regionIds.add(WorldRegion.NORTH_AMERICA_REGION_ID);
		regionIds.add(WorldRegion.RUSSIA_REGION_ID);
		regionIds.add(WorldRegion.SOUTH_AMERICA_REGION_ID);
		regionIds.add(WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID);
		return regionIds;
	}

	public void structureWorldRegions(List<WorldRegion> loadedItems) {
		if (loadedItems.size() == 0) {
			return;
		}
		WorldRegion world = new WorldRegion(WorldRegion.WORLD);
		for (String regionId : getTopRegionsIds()) {
			initWorldRegion(world, regionId);
		}
		Iterator<WorldRegion> it = loadedItems.iterator();
		while (it.hasNext()) {
			WorldRegion region = it.next();
			if (region.superregion == null) {
				boolean found = false;
				for (WorldRegion worldSubregion : world.subregions) {
					if (worldSubregion.getRegionId().equalsIgnoreCase(region.regionFullName)) {
						for (WorldRegion rg : region.subregions) {
							worldSubregion.addSubregion(rg);
						}
						found = true;
						break;
					}
				}
				if (found) {
					it.remove();
				} else if (region.getRegionId().contains("basemap")) {
					it.remove();
				}
			} else {
				it.remove();
			}
		}
		Comparator<WorldRegion> nameComparator = new Comparator<WorldRegion>() {
			final net.osmand.Collator collator = OsmAndCollator.primaryCollator();

			@Override
			public int compare(WorldRegion w1, WorldRegion w2) {
				return collator.compare(w1.getLocaleName(), w2.getLocaleName());
			}
		};
		sortSubregions(world, nameComparator);

		this.worldRegion = world;
		if (loadedItems.size() > 0) {
			LOG.warn("Found orphaned regions: " + loadedItems.size());
			for (WorldRegion regionId : loadedItems) {
				LOG.warn("FullName = " + regionId.regionFullName +
						" parent=" + regionId.regionParentFullName);
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

	public List<WorldRegion> getWorldRegionsAt(LatLon latLon) throws IOException {
		return getWorldRegionsAt(latLon, false);
	}

	public List<WorldRegion> getWorldRegionsAt(LatLon latLon, boolean includeRoadRegions) throws IOException {
		Map<WorldRegion, BinaryMapDataObject> mapDataObjects = getBinaryMapDataObjectsWithRegionsAt(latLon, includeRoadRegions);
		return new ArrayList<>(mapDataObjects.keySet());
	}

	public Map.Entry<WorldRegion, BinaryMapDataObject> getSmallestBinaryMapDataObjectAt(LatLon latLon) throws IOException {
		Map<WorldRegion, BinaryMapDataObject> mapDataObjectsWithRegions = getBinaryMapDataObjectsWithRegionsAt(latLon);
		return getSmallestBinaryMapDataObjectAt(mapDataObjectsWithRegions);
	}

	public Map.Entry<WorldRegion, BinaryMapDataObject> getSmallestBinaryMapDataObjectAt(Map<WorldRegion, BinaryMapDataObject> mapDataObjectsWithRegions) {
		Map.Entry<WorldRegion, BinaryMapDataObject> res = null;
		double smallestArea = -1;
		for (Map.Entry<WorldRegion, BinaryMapDataObject> o : mapDataObjectsWithRegions.entrySet()) {
			double area = OsmandRegions.getArea(o.getValue());
			if (smallestArea == -1) {
				smallestArea = area;
				res = o;
			} else if (area < smallestArea) {
				smallestArea = area;
				res = o;
			}
		}
		return res;
	}

	private Map<WorldRegion, BinaryMapDataObject> getBinaryMapDataObjectsWithRegionsAt(LatLon latLon) throws IOException {
		return getBinaryMapDataObjectsWithRegionsAt(latLon, false);
	}

	private Map<WorldRegion, BinaryMapDataObject> getBinaryMapDataObjectsWithRegionsAt(LatLon latLon, boolean includeRoadRegions) throws IOException {
		int point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		int point31y = MapUtils.get31TileNumberY(latLon.getLatitude());
		Map<WorldRegion, BinaryMapDataObject> foundObjects = new LinkedHashMap<>();
		List<BinaryMapDataObject> mapDataObjects;
		try {
			mapDataObjects = queryBboxNoInit(point31x, point31x, point31y, point31y, true);
		} catch (IOException e) {
			throw new IOException("Error while calling queryBbox");
		}
		Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
		while (it.hasNext()) {
			BinaryMapDataObject o = it.next();
			if (o.getTypes() != null) {
				WorldRegion downloadRegion = getRegionData(getFullName(o));
				if (downloadRegion == null
						|| (includeRoadRegions ? !downloadRegion.isRegionRoadsDownload() && !downloadRegion.isRegionMapDownload() : !downloadRegion.isRegionMapDownload())
						|| !contain(o, point31x, point31y)) {
					it.remove();
				} else {
					foundObjects.put(downloadRegion, o);
				}
			}
		}
		return foundObjects;
	}

	public List<BinaryMapDataObject> getRegionsToDownload(double lat, double lon) throws IOException {
		List<BinaryMapDataObject> l = new ArrayList<BinaryMapDataObject>();
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		List<BinaryMapDataObject> cs = query(x31, y31);
		for (BinaryMapDataObject b : cs) {
			if (contain(b, x31, y31) && !Algorithms.isEmpty(getDownloadName(b))) {
				l.add(b);
			}
		}
		return l;
	}
	
	public List<String> getRegionsToDownload(double lat, double lon, List<String> keyNames) throws IOException {
		keyNames.clear();
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		List<BinaryMapDataObject> cs = query(x31, y31);
		for (BinaryMapDataObject b : cs) {
			if (contain(b, x31, y31)) {
				String downloadName = getDownloadName(b);
				if (!Algorithms.isEmpty(downloadName)) {
					keyNames.add(downloadName);
				}
			}
		}
		return keyNames;
	}

	private void addPolygonToRegionIfValid(BinaryMapDataObject mapObject, WorldRegion worldRegion) {
		if (mapObject.getPointsLength() < 3) {
			return;
		}

		List<LatLon> polygon = new ArrayList<>();
		for (int i = 0; i < mapObject.getPointsLength(); i++) {
			int x = mapObject.getPoint31XTile(i);
			int y = mapObject.getPoint31YTile(i);
			double lat = MapUtils.get31LatitudeY(y);
			double lon = MapUtils.get31LongitudeX(x);
			polygon.add(new LatLon(lat, lon));
		}

		boolean outside = true;
		for (LatLon point : polygon) {
			if (worldRegion.containsPoint(point)) {
				outside = false;
				break;
			}
		}

		if (outside) {
			worldRegion.additionalPolygons.add(polygon);
		}
	}
}

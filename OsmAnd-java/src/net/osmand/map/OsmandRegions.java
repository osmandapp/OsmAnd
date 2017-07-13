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
import java.io.IOException;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;

public class OsmandRegions {

	public static final String MAP_TYPE = "region_map";

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

	private BinaryMapIndexReader reader;
	private String locale = "en";
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandRegions.class);

	WorldRegion worldRegion = new WorldRegion(WorldRegion.WORLD);
	Map<String, WorldRegion> fullNamesToRegionData = new HashMap<String, WorldRegion>();
	Map<String, String> downloadNamesToFullNames = new HashMap<String, String>();
	Map<String, LinkedList<BinaryMapDataObject>> countriesByDownloadName = new HashMap<String, LinkedList<BinaryMapDataObject>>();


	QuadTree<String> quadTree;
	MapIndexFields mapIndexFields;
	RegionTranslation translator;

	private class MapIndexFields {
		Integer parentFullName = null;
		Integer fullNameType = null;
		Integer downloadNameType = null;
		Integer nameEnType = null;
		Integer nameType = null;
		Integer nameLocaleType = null;
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


	public void prepareFile(String fileName) throws IOException {
		reader = new BinaryMapIndexReader(new RandomAccessFile(fileName, "r"), new File(fileName));
//		final Collator clt = OsmAndCollator.primaryCollator();
		final Map<String, String> parentRelations = new LinkedHashMap<String, String>();
		final ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {

			@Override
			public boolean publish(BinaryMapDataObject object) {
				initTypes(object);
				int[] types = object.getTypes();
				for (int i = 0; i < types.length; i++) {
					TagValuePair tp = object.getMapIndex().decodeType(types[i]);
					if ("boundary".equals(tp.value)) {
						return false;
					}
				}
				WorldRegion rd = initRegionData(parentRelations, object);
				if (rd == null) {
					return false;
				}
				if (rd.regionDownloadName != null) {
					downloadNamesToFullNames.put(rd.regionDownloadName, rd.regionFullName);
				}
				fullNamesToRegionData.put(rd.regionFullName, rd);
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
		structureWorldRegions(new ArrayList<WorldRegion>(fullNamesToRegionData.values()));
	}

	public boolean containsCountry(String name) {
		return countriesByDownloadName.containsKey(name);
	}


	public String getLocaleName(String downloadName, boolean includingParent) {
		final String lc = downloadName.toLowerCase();
		if (downloadNamesToFullNames.containsKey(lc)) {
			String fullName = downloadNamesToFullNames.get(lc);
			return getLocaleNameByFullName(fullName, includingParent);
		}
		return downloadName.replace('_', ' ');
	}

	public String getLocaleNameByFullName(String fullName, boolean includingParent) {
		WorldRegion rd = fullNamesToRegionData.get(fullName);
		if (rd == null) {
			return fullName.replace('_', ' ');
		}
		if (includingParent && rd.getSuperregion() != null && rd.getSuperregion().getSuperregion() != null) {
			WorldRegion parentParent = rd.getSuperregion().getSuperregion();
			WorldRegion parent = rd.getSuperregion();
			if (parentParent.getRegionId().equals(WorldRegion.WORLD) &&
					!parent.getRegionId().equals(WorldRegion.RUSSIA_REGION_ID)) {
				return rd.getLocaleName();
			}
			if (parentParent.getRegionId().equals(WorldRegion.RUSSIA_REGION_ID)) {
				return parentParent.getLocaleName() + " " + rd.getLocaleName();
			}
			if (parentParent.getRegionId().equals(WorldRegion.JAPAN_REGION_ID)) {
				return parentParent.getLocaleName() + " " + rd.getLocaleName();
			}
			return parent.getLocaleName() + " " + rd.getLocaleName();
		} else {
			return rd.getLocaleName();
		}
	}

	public WorldRegion getWorldRegion() {
		return worldRegion;
	}


	public boolean isInitialized() {
		return reader != null;
	}

	public boolean contain(BinaryMapDataObject bo, int tx, int ty) {
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

	public boolean intersect(BinaryMapDataObject bo, int lx, int ty, int rx, int by) {
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

	private List<BinaryMapDataObject> getCountries(int tile31x, int tile31y) {
		HashSet<String> set = new HashSet<String>(quadTree.queryInBox(new QuadRect(tile31x, tile31y, tile31x, tile31y),
				new ArrayList<String>()));
		List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {
			String cname = it.next();
			BinaryMapDataObject container = null;
			int count = 0;
			for (BinaryMapDataObject bo : countriesByDownloadName.get(cname)) {
				if (contain(bo, tile31x, tile31y)) {
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
			for(BinaryMapDataObject o : list) {
				if(contain(o, x, y)) {
					String name = mapIndexFields.get(mapIndexFields.nameType, o);
					if(name != null) {
						return name;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public List<BinaryMapDataObject> query(final int tile31x, final int tile31y) throws IOException {
		if (quadTree != null) {
			return getCountries(tile31x, tile31y);
		}
		return queryNoInit(tile31x, tile31y);
	}

	private synchronized List<BinaryMapDataObject> queryNoInit(final int tile31x, final int tile31y) throws IOException {
		final List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
		BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(tile31x, tile31x, tile31y, tile31y,
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
						if (contain(object, tile31x, tile31y)) {
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
		if (reader != null) {
			reader.searchMapIndex(sr);
		}
		return result;
	}


	public synchronized List<BinaryMapDataObject> queryBbox(int lx, int rx, int ty, int by) throws IOException {
		final List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
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
						result.add(object);
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
		this.locale = locale;
	}


	public WorldRegion getRegionData(String fullname) {
		return fullNamesToRegionData.get(fullname);
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

	public List<WorldRegion> getAllRegionData() {
		return new ArrayList<WorldRegion>(fullNamesToRegionData.values());
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
		}

		rd.regionParentFullName = mapIndexFields.get(mapIndexFields.parentFullName, object);
		if (!Algorithms.isEmpty(rd.regionParentFullName)) {
			parentRelations.put(rd.regionFullName, rd.regionParentFullName);
		}
		rd.regionName = mapIndexFields.get(mapIndexFields.nameType, object);
		rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocaleType, object);
		rd.regionNameEn = mapIndexFields.get(mapIndexFields.nameEnType, object);
		rd.params.regionLang = mapIndexFields.get(mapIndexFields.langType, object);
		rd.params.regionLeftHandDriving = mapIndexFields.get(mapIndexFields.leftHandDrivingType, object);
		rd.params.regionMetric = mapIndexFields.get(mapIndexFields.metricType, object);
		rd.params.regionRoadSigns = mapIndexFields.get(mapIndexFields.roadSignsType, object);
		rd.params.wikiLink = mapIndexFields.get(mapIndexFields.wikiLinkType, object);
		rd.params.population = mapIndexFields.get(mapIndexFields.populationType, object);
		rd.regionSearchText = getSearchIndex(object);
		rd.regionMapDownload = isDownloadOfType(object, MAP_TYPE);
		return rd;
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
				String nm = mapIndexFields.get(mapIndexFields.downloadNameType, object);
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
			}
			if (or.isDownloadOfType(b, MAP_TYPE)) {
				found.add(nm.toLowerCase());
			}
		}

		if (!found.equals(expected)) {
			throw new IllegalStateException(" Expected " + expected + " but was " + found);
		}
		System.out.println("Found " + expected + " in " + (System.currentTimeMillis() - t) + " ms");
	}


	public static void main(String[] args) throws IOException {
		OsmandRegions or = new OsmandRegions();
		or.prepareFile("/Users/victorshcherb/osmand/repos/resources/countries-info/regions.ocbf");
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

		//testCountry(or, 15.8, 23.09, "chad");
		testCountry(or, 52.10, 4.92, "the netherlands", "utrecht");
		testCountry(or, 52.15, 7.50, "north rhine-westphalia");
		testCountry(or, 28.8056, 29.9858, "egypt");
//		testCountry(or, 40.0760, 9.2807, "italy", "sardinia");
		testCountry(or, 35.7521, 139.7887, "japan");
		testCountry(or, 46.5145, 102.2580, "mongolia");
		testCountry(or, 62.54, 43.36, "arkhangelsk oblast", "northwestern federal district");
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

	public void structureWorldRegions(List<WorldRegion> loadedItems) {
		if (loadedItems.size() == 0) {
			return;
		}
		WorldRegion world = new WorldRegion(WorldRegion.WORLD);
		initWorldRegion(world, WorldRegion.AFRICA_REGION_ID);
		initWorldRegion(world, WorldRegion.ASIA_REGION_ID);
		initWorldRegion(world, WorldRegion.CENTRAL_AMERICA_REGION_ID);
		initWorldRegion(world, WorldRegion.EUROPE_REGION_ID);
		initWorldRegion(world, WorldRegion.NORTH_AMERICA_REGION_ID);
		initWorldRegion(world, WorldRegion.RUSSIA_REGION_ID);
		initWorldRegion(world, WorldRegion.SOUTH_AMERICA_REGION_ID);
		initWorldRegion(world, WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID);
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

	public BinaryMapDataObject findBinaryMapDataObject(LatLon latLon) throws IOException {
		int point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		int point31y = MapUtils.get31TileNumberY(latLon.getLatitude());

		BinaryMapDataObject res = null;
		List<BinaryMapDataObject> mapDataObjects;
		try {
			mapDataObjects = queryBbox(point31x, point31x, point31y, point31y);
		} catch (IOException e) {
			throw new IOException("Error while calling queryBbox");
		}

		if (mapDataObjects != null) {
			Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (o.getTypes() != null) {
					boolean isRegion = true;
					for (int i = 0; i < o.getTypes().length; i++) {
						TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
						if ("boundary".equals(tp.value)) {
							isRegion = false;
							break;
						}
					}
					WorldRegion downloadRegion = getRegionData(getFullName(o));
					if (!isRegion
							|| downloadRegion == null
							|| !downloadRegion.isRegionMapDownload()
							|| !contain(o, point31x, point31y)) {
						it.remove();
					}
				}
			}
			double smallestArea = -1;
			for (BinaryMapDataObject o : mapDataObjects) {
				double area = OsmandRegions.getArea(o);
				if (smallestArea == -1) {
					smallestArea = area;
					res = o;
				} else if (area < smallestArea) {
					smallestArea = area;
					res = o;
				}
			}
		}
		return res;
	}

}

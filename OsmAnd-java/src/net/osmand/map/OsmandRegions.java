package net.osmand.map;


import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

public class OsmandRegions {

	private static final String MAP_TYPE = "region_map";
	
	public static final String FIELD_LEFT_HAND_DRIVING = "left_hand_driving";
	public static final String FIELD_DOWNLOAD_NAME = "download_name";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_NAME_EN = "name:en";
	public static final String FIELD_REGION_PARENT_NAME = "region_parent_name";
	public static final String FIELD_REGION_FULL_NAME = "region_full_name";
	public static final String FIELD_LANG = "lang";
	public static final String FIELD_METRIC = "metric";
	public static final String FIELD_ROAD_SIGNS = "road_signs";
	
	private BinaryMapIndexReader reader;
	
	Map<String, RegionData> fullNamesToRegionData = new HashMap<String, RegionData>();
	Map<String, LinkedList<BinaryMapDataObject>> countriesByDownloadName = new HashMap<String, LinkedList<BinaryMapDataObject>>();
	Map<String, String> downloadNamesToFullNames = new HashMap<String, String>();
//	Map<String, String> fullNamesToLocaleNames = new HashMap<String, String>();
//	Map<String, String> fullNamesNoParentToLocaleNames = new HashMap<String, String>();
//	Map<String, String> fullMapNamesToDownloadNames = new HashMap<String, String>();
//	Map<String, String> fullNamesToLowercaseIndex = new HashMap<String, String>();
//	Map<String, String> fullNamesToParentFullNames = new HashMap<String, String>();
//	Map<String, String> fullNamesToDownloadNames = new HashMap<String, String>();
//	Map<String, String> fullNamesToLangs = new HashMap<String, String>();
//	Map<String, String> fullNamesToMetrics = new HashMap<String, String>();
//	Map<String, String> fullNamesToLeftHandDrivings = new HashMap<String, String>();
//	Map<String, String> fullNamesToRoadSigns = new HashMap<String, String>();

	QuadTree<String> quadTree = null ;
	String locale = "en";
	MapIndexFields mapIndexFields = new MapIndexFields();

	private class MapIndexFields {
		MapIndex mapIndex;
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
		
		public String get(Integer tp, BinaryMapDataObject object) {
			if(tp == null) {
				return null;
			}
			return object.getNameByType(tp);
		}
	}



	public void prepareFile(String fileName) throws IOException {
		reader = new BinaryMapIndexReader(new RandomAccessFile(fileName, "r"), new File(fileName));
		initLocaleNames();
	}

	public boolean containsCountry(String name){
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
		RegionData rd = fullNamesToRegionData.get(fullName);
		if(rd == null) {
			return fullName.replace('_', ' ');
		}
		if (includingParent && rd.parent != null) {
			return rd.parent.getLocaleName() + " " + rd.getLocaleName();
		} else {
			return rd.getLocaleName();
		}
	}

	public String getDownloadNameIndexLowercase(String downloadName) {
		if(downloadName == null) {
			return null;
		}
		final String lc = downloadName.toLowerCase();
		if (downloadNamesToFullNames.containsKey(lc)) {
			String fullName = downloadNamesToFullNames.get(lc);
			if (fullNamesToLowercaseIndex.containsKey(fullName)) {
				return fullNamesToLowercaseIndex.get(fullName);
			}
		}
		return null;
	}
	
	

	public boolean isInitialized(){
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
		int t = 0;
		// 1. polygon in object 
		if(contain(bo, lx, ty)) {
			return true;
		}
		// 2. object in polygon
		if(bo.getPointsLength() == 0) {
			return false;
		}
		if(bo.getPoint31XTile(0) >= lx && bo.getPoint31XTile(0) <= rx && 
				bo.getPoint31YTile(0) >= ty && bo.getPoint31YTile(0) <= by ){
			return true;
		}
			
		// 3. find intersection
		for (int i = 1; i < bo.getPointsLength(); i++) {
			int px = bo.getPoint31XTile(i - 1);
			int x = bo.getPoint31XTile(i);
			int py = bo.getPoint31YTile(i - 1);
			int y = bo.getPoint31YTile(i);
			if(x < lx && px < lx) {
				continue;
			} else if(x > rx && px > rx) {
				continue;
			} else if(y > by && py > by) {
				continue;
			} else if(y < ty && py < ty) {
				continue;
			}
			long in = MapAlgorithms.calculateIntersection(px, py, x, y, lx, rx, by, ty);
			if(in != -1) {
				return true;
			}
		}
		
		return false;
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


	public List<BinaryMapDataObject> query(final int tile31x, final int tile31y) throws IOException {
		if(quadTree != null) {
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
		if(reader != null) {
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
		if(reader != null) {
			reader.searchMapIndex(sr);
		}
		return result;
	}
	
	public void setLocale(String locale) {
		this.locale = locale;
	}
	
	public String getMapDownloadType(String fullname) {
		return fullMapNamesToDownloadNames.get(fullname);
	}

	public String getDownloadName(String fullname) {
		return fullNamesToDownloadNames.get(fullname);
	}

	public String getParentFullName(String fullname) {
		return fullNamesToParentFullNames.get(fullname);
	}

	public void initLocaleNames() throws IOException {
//		final Collator clt = OsmAndCollator.primaryCollator();
		final Map<String, String> parentRelations = new LinkedHashMap<String, String>();
		final ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {
			
			@Override
			public boolean publish(BinaryMapDataObject object) {
				initTypes(object);
				int[] types = object.getTypes();
				for(int i = 0; i < types.length; i++ ) {
					TagValuePair tp = object.getMapIndex().decodeType(types[i]);
					if("boundary".equals(tp.value)) {
						return false;
					}
				}
				RegionData rd = new RegionData();
				rd.downloadsId = mapIndexFields.get(mapIndexFields.downloadNameType, object);
				rd.regionFullName = mapIndexFields.get(mapIndexFields.fullNameType, object);
				rd.regionParentFullName = mapIndexFields.get(mapIndexFields.parentFullName, object);
				if(!Algorithms.isEmpty(rd.regionParentFullName)) {
					parentRelations.put(rd.regionFullName, rd.regionParentFullName);
				}				
				rd.regionName = mapIndexFields.get(mapIndexFields.nameType, object);
				rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocaleType, object);
				rd.regionNameEn = mapIndexFields.get(mapIndexFields.nameEnType, object);
				rd.regionLang = mapIndexFields.get(mapIndexFields.langType, object);
				rd.regionLeftHandDriving = mapIndexFields.get(mapIndexFields.leftHandDrivingType, object);
				rd.regionMetric = mapIndexFields.get(mapIndexFields.metricType, object);
				rd.regionRoadSigns = mapIndexFields.get(mapIndexFields.roadSignsType, object);

				String roadSigns = getRoadSigns(object);
				if(!Algorithms.isEmpty(roadSigns)){
					fullNamesToRoadSigns.put(fullName, roadSigns);
				}

				rd.searchText = getSearchIndex(object);
				fullNamesToLowercaseIndex.put(fullName, ind.toString());
				String downloadName = getDownloadName(object);
				if(downloadName != null) {
					fullNamesToDownloadNames.put(fullName, downloadName);
					downloadNamesToFullNames.put(downloadName, fullName);
					if(isDownloadOfType(object, MAP_TYPE)) {
						fullMapNamesToDownloadNames.put(fullName, downloadName);
					}
				}
				return false;
			}


			private String getSearchIndex(BinaryMapDataObject object) {
				MapIndex mi = object.getMapIndex();
				TIntObjectIterator<String> it = object.getObjectNames().iterator();
				StringBuilder ind = new StringBuilder();
				while(it.hasNext()) {
					it.advance();
					TagValuePair tp = mi.decodeType(it.key());
					if(tp.tag.startsWith("name") || tp.tag.equals("key_name")) {
						final String vl = it.value().toLowerCase();
//						if (!CollatorStringMatcher.ccontains(clt, ind.toString(), vl)) {
						if(ind.indexOf(vl) == -1) {
							ind.append(" ").append(vl);
						}
					}	
				}
				return ind.toString();
			}


			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		iterateOverAllObjects(resultMatcher);
		// post process download names
		for(Map.Entry<String, String> e : parentRelations.entrySet()) {
			String fullName = e.getKey();
			String parentFullName = e.getValue();
			String parentParentFulName = parentRelations.get(parentFullName);
			if(!Algorithms.isEmpty(parentFullName) && 
					!Algorithms.isEmpty(parentParentFulName)) {
				if(parentParentFulName.contains("russia") || parentParentFulName.contains("japan")) {
					parentFullName = parentParentFulName;
				}
				String locPrefix = fullNamesToLocaleNames.get(parentFullName);
				String locName = fullNamesToLocaleNames.get(fullName);
				if(locPrefix == null || locName == null) {
					throw new IllegalStateException("There is no prefix registered for " + fullName + " (" + parentFullName + ") ");
				}
				fullNamesToLocaleNames.put(fullName, locPrefix + " " + locName);
				fullNamesNoParentToLocaleNames.put(fullName, locName);
				// don't add parent to index
//				String index = fullNamesToLowercaseIndex.get(fullName);
//				String prindex = fullNamesToLowercaseIndex.get(parentFullName);
//				fullNamesToLowercaseIndex.put(fullName, index + " " + prindex);	
			}
		}
		
	}
	

	private boolean isDownloadOfType(BinaryMapDataObject object, String type) {
		int[] addtypes = object.getAdditionalTypes();
		for(int i = 0; i < addtypes.length; i++) {
			TagValuePair tp = object.getMapIndex().decodeType(addtypes[i]);
			if(type.equals(tp.tag) && "yes".equals(tp.value)) {
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
				String nm = object.getNameByType(downloadNameType);
//				if(nm != null) {
//					System.out.println((c++) +" " + nm);
//				}
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
		if(reader != null) {
			reader.searchMapIndex(sr);
		}
	}

	
	private void initTypes(BinaryMapDataObject object) {
		if (mapIndexFields == null) {
			mapIndexFields = new MapIndexFields();
			mapIndexFields.mapIndex = object.getMapIndex();
			mapIndexFields.downloadNameType = object.getMapIndex().getRule(FIELD_DOWNLOAD_NAME, null);
			mapIndexFields.nameType = object.getMapIndex().getRule(FIELD_NAME, null);
			mapIndexFields.nameEnType = object.getMapIndex().getRule(FIELD_NAME_EN, null);
			mapIndexFields.nameLocaleType = object.getMapIndex().getRule(FIELD_NAME+":"+locale, null);
			mapIndexFields.parentFullName = object.getMapIndex().getRule(FIELD_REGION_PARENT_NAME, null);
			mapIndexFields.fullNameType = object.getMapIndex().getRule(FIELD_REGION_FULL_NAME, null);
			mapIndexFields.langType = object.getMapIndex().getRule(FIELD_LANG, null);
			mapIndexFields.metricType = object.getMapIndex().getRule(FIELD_METRIC, null);
			mapIndexFields.leftHandDrivingType = object.getMapIndex().getRule(FIELD_LEFT_HAND_DRIVING, null);
			mapIndexFields.roadSignsType = object.getMapIndex().getRule(FIELD_ROAD_SIGNS, null);
			mapIndexFields.nameType = object.getMapIndex().getRule(FIELD_NAME, null);
		}
	}


	private static void testCountry(OsmandRegions or, double lat, double lon, String... test) throws IOException {
		long t = System.currentTimeMillis();
		List<BinaryMapDataObject> cs = or.query(MapUtils.get31TileNumberX(lon), MapUtils.get31TileNumberY(lat));
		Set<String> expected = new TreeSet<String>(Arrays.asList(test));
		Set<String> found = new TreeSet<String>();

		for (BinaryMapDataObject b : cs) {
			String nm = b.getNameByType(or.nameEnType);
			if(nm == null) {
				nm = b.getName();
			}
			if(or.isDownloadOfType(b, MAP_TYPE)) {
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
		or.cacheAllCountries();
//		long t = System.currentTimeMillis();
//		or.cacheAllCountries();
//		System.out.println("Init " + (System.currentTimeMillis() - t));

		//testCountry(or, 15.8, 23.09, "chad");
		testCountry(or, 52.10, 4.92, "the netherlands", "utrecht");
		testCountry(or, 52.15, 7.50, "north rhine-westphalia");
		testCountry(or, 28.8056, 29.9858, "egypt" );
//		testCountry(or, 40.0760, 9.2807, "italy", "sardinia");
		testCountry(or, 35.7521, 139.7887, "japan");
		testCountry(or, 46.5145, 102.2580, "mongolia");
		testCountry(or, 62.54, 43.36, "arkhangelsk oblast", "northwestern federal district");


	}


	public static class RegionData {
		// filled by osmand regions
		protected RegionData parent = null;
		protected String regionLeftHandDriving;
		protected String regionLang;
		protected String regionMetric;
		protected String regionRoadSigns;
		protected String regionFullName;
		protected String regionParentFullName;
		protected String regionName;
		protected String regionNameEn;
		protected String regionNameLocale;
		
		///
		String regionId;
		private String downloadsId;
		private String name;
		private String searchText;
		private LatLon center;
		
		public String getLocaleName() {
			if(!Algorithms.isEmpty(regionNameLocale)) {
				return regionNameLocale;
			}
			if(!Algorithms.isEmpty(regionNameEn)) {
				return regionNameEn;
			}
			return regionName;
		}
		
		public LatLon getCenter() {
			return center;
		}
		
		public String getSearchText() {
			return searchText;
		}
		
		public String getName() {
			return name;
		}
		
		public String getRegionId() {
			return regionId;
		}
		
		public String getDownloadsId() {
			return downloadsId;
		}
	}
}

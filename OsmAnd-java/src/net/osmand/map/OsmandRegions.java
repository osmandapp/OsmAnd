package net.osmand.map;


import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;

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
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

public class OsmandRegions {

	private static final String MAP_TYPE = "region_map";
//	regionSrtm = object.getMapIndex().getRule("region_srtm", null);
//	regionWiki = object.getMapIndex().getRule("region_wiki", null);
//	regionRoads = object.getMapIndex().getRule("region_roads", null);
//	regionHillshade = object.getMapIndex().getRule("region_hillshade", null);
	
	private BinaryMapIndexReader reader;
	Map<String, LinkedList<BinaryMapDataObject>> countriesByDownloadName = new HashMap<String, LinkedList<BinaryMapDataObject>>();
	Map<String, String> fullNamesToLocaleNames = new HashMap<String, String>();
	Map<String, String> fullMapNamesToDownloadNames = new HashMap<String, String>();
	Map<String, String> downloadNamesToFullNames = new HashMap<String, String>();
	Map<String, String> fullNamesToLowercaseIndex = new HashMap<String, String>();
	QuadTree<String> quadTree = null ;


	Integer parentFullName = null;
	Integer fullNameType = null;
	Integer downloadNameType = null;
	Integer nameEnType = null;
	Integer nameType = null;
	Integer nameLocaleType = null;
	String locale = "en";


	public void prepareFile(String fileName) throws IOException {
		reader = new BinaryMapIndexReader(new RandomAccessFile(fileName, "r"));
		initLocaleNames();
	}

	public boolean containsCountry(String name){
		return countriesByDownloadName.containsKey(name);
	}

	public String getDownloadName(BinaryMapDataObject o) {
		if(downloadNameType == null) {
			return null;
		}
		return o.getNameByType(downloadNameType);
	}
	
	public Integer getNameEnType() {
		return nameEnType;
	}
	
	public String getLocaleName(String downloadName) {
		final String lc = downloadName.toLowerCase();
		if (downloadNamesToFullNames.containsKey(lc)) {
			String fullName = downloadNamesToFullNames.get(lc);
			if (fullNamesToLocaleNames.containsKey(fullName)) {
				return fullNamesToLocaleNames.get(fullName);
			}
		}
		return downloadName.replace('_', ' ');
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
	
	public String getLocaleName(BinaryMapDataObject object) {
		String locName = "";
		if(locName == null || locName.length() == 0){
			if(nameLocaleType != null) {
				locName = object.getNameByType(nameLocaleType);
			}
		}
		if(locName == null || locName.length() == 0){
			if(nameEnType != null) {
				locName = object.getNameByType(nameEnType);
			}
		}
		if(locName == null || locName.length() == 0){
			if(nameType != null) {
				locName = object.getNameByType(nameType);
			}
		}
		return locName;
	}

	public String getParentFullName(BinaryMapDataObject o) {
		if(parentFullName == null) {
			return null;
		}
		return o.getNameByType(parentFullName);
	}

	
	
	public String getFullName(BinaryMapDataObject o) {
		if(fullNameType == null) {
			return null;
		}
		return o.getNameByType(fullNameType);
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
				String parentFullName = getParentFullName(object);
				String fullName = getFullName(object);
				if(!Algorithms.isEmpty(parentFullName)) {
					parentRelations.put(fullName, parentFullName);
				}				
				String locName = getLocaleName(object);
				if(!Algorithms.isEmpty(locName)){
					fullNamesToLocaleNames.put(fullName, locName);
				}
				MapIndex mi = object.getMapIndex();
				TIntObjectIterator<String> it = object.getObjectNames().iterator();
				StringBuilder ind = new StringBuilder();
				while(it.hasNext()) {
					it.advance();
					TagValuePair tp = mi.decodeType(it.key());
					if (tp.tag.equals("key_name") && Algorithms.isEmpty(locName)) {
						fullNamesToLocaleNames.put(fullName,
								Algorithms.capitalizeFirstLetterAndLowercase(it.value().replace('_', '-')));
					}
					if(tp.tag.startsWith("name") || tp.tag.equals("key_name")) {
						final String vl = it.value().toLowerCase();
//						if (!CollatorStringMatcher.ccontains(clt, ind.toString(), vl)) {
						if(ind.indexOf(vl) == -1) {
							ind.append(" ").append(vl);
						}
					}	
				}
				fullNamesToLowercaseIndex.put(fullName, ind.toString());
				String downloadName = getDownloadName(object);
				if(downloadName != null) {
					downloadNamesToFullNames.put(downloadName, fullName);
					if(isDownloadOfType(object, MAP_TYPE)) {
						fullMapNamesToDownloadNames.put(fullName, downloadName);
					}
				}
				return false;
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
				String index = fullNamesToLowercaseIndex.get(fullName);
				String prindex = fullNamesToLowercaseIndex.get(parentFullName);
				fullNamesToLowercaseIndex.put(fullName, index + " " + prindex);	
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


	public void cacheAllCountries() throws IOException {
		quadTree = new QuadTree<String>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
				8, 0.55f);
		final ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {
			@Override
			public boolean publish(BinaryMapDataObject object) {
				if (object.getPointsLength() < 1) {
					return false;
				}
				initTypes(object);
				String nm = object.getNameByType(downloadNameType);
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
		if (downloadNameType == null) {
			downloadNameType = object.getMapIndex().getRule("download_name", null);
			nameType = object.getMapIndex().getRule("name", null);
			nameEnType = object.getMapIndex().getRule("name:en", null);
			nameLocaleType = object.getMapIndex().getRule("name:" + locale, null);
			parentFullName = object.getMapIndex().getRule("region_parent_name", null);
			fullNameType = object.getMapIndex().getRule("region_full_name", null);
			
			if (downloadNameType == null || nameType == null) {
				throw new IllegalStateException();
			}
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
			found.add(nm.toLowerCase());
		}

		if (!found.equals(expected)) {
			throw new IllegalStateException(" Expected " + expected + " but was " + found);
		}
		System.out.println("Found " + expected + " in " + (System.currentTimeMillis() - t) + " ms");
	}


	public static void main(String[] args) throws IOException {
		OsmandRegions or = new OsmandRegions();
		or.prepareFile("/home/victor/projects/osmand/repo/resources/countries-info/regions.ocbf");
		or.cacheAllCountries();
//		long t = System.currentTimeMillis();
//		or.cacheAllCountries();
//		System.out.println("Init " + (System.currentTimeMillis() - t));

		//testCountry(or, 15.8, 23.09, "chad");
		testCountry(or, 52.10, 4.92, "the netherlands");
		testCountry(or, 52.15, 7.50, "north rhine-westphalia");
		testCountry(or, 40.0760, 9.2807, "italy", "sardinia");
		testCountry(or, 28.8056, 29.9858, "africa", "egypt" );
		testCountry(or, 35.7521, 139.7887, "japan");
		testCountry(or, 46.5145, 102.2580, "mongolia");
		testCountry(or, 62.54, 43.36, "arkhangelsk oblast", "northwestern federal district");


	}
}

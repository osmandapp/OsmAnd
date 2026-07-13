package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderBytes;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.NameIndexReader.ValueFreq;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchContext {

	private static int SHIFT_FILE_IND = 14; // maxism files 16K
	private static int SHIFT_POI_IND = 10; // maximum poi 1024

	final List<BinaryMapIndexReader> files;
	final List<SpatialSearchFileCache> internalFile = new ArrayList<>();
	final LatLon location; // could be null
	final int[][] limitLocationBboxes;
	
	final SpatialPoiSearch poiSearch;
	final SpatialTextSearchSettings settings;
	final SpatialSearchStats stats = new SpatialSearchStats();

	List<SpatialSearchToken> tokens = null; // non initiatilized
	Set<String> commonlyUsedWords = new HashSet<String>();
	
	public static class SpatialSearchStats {
		public Timer requestTime = new Timer();
		public Timer step1Atoms = new Timer();
		public Timer sub1FileAtomsTime = new Timer();
		public Timer sub1MatchTime = new Timer();
		public Timer sub1PoiNameBoundaryTime = new Timer();
		public int tokenObjs;
		
		public Timer step2Compute = new Timer();
		public Timer sub2LoadObjectsBldTime = new Timer();
		public Timer sub2ReadObjTime = new Timer();
		public int maxCombinations = 0;
		
		public Timer step3Sort = new Timer();
		
		public long readTableBytes = 0;
		public long readAtomsBytes = 0;
		public long readObjsBytes = 0;
		public long skipTableBytes = 0;
		public long skipAtomsBytes = 0;
		
		// separate api
		public Timer poiByTypeTime = new Timer();
		public long poiByTypeBytes = 0;
		public long poiByTypeBboxes = 0;
		
		public boolean doTiming = true;
		public boolean printLogs = true;
	
		public class Timer {
			public long time = 0;
			public long endTime = System.nanoTime();
			
			public void start() {
				if (doTiming) {
					time -= System.nanoTime();
				}
			}

			public void finish() {
				if (doTiming) {
					time += System.nanoTime();
				}
			}
			
			public double ms() {
				return time / 1e6;
			}
		}
		
		@Override
		public String toString() {
			return String.format(
					"Search Stats %.1f ms (read %,d KB) - %.1f ms %,d atoms (read %.1f, match %.1f, poi %.1f), "
					+ "%.1f ms compute %,d (loadBld %.1f, read %.1f)",
					requestTime.ms(), (readTableBytes + readAtomsBytes + readObjsBytes) / 1024,
					step1Atoms.ms(), tokenObjs,  sub1FileAtomsTime.ms(), sub1MatchTime.ms(), sub1PoiNameBoundaryTime.ms(),
					step2Compute.ms(), maxCombinations, sub2LoadObjectsBldTime.ms(), sub2ReadObjTime.ms());
		}

	}

	public SpatialSearchContext(SpatialTextSearchSettings settings, List<BinaryMapIndexReader> files,
			SpatialPoiSearch poiSearch, LatLon location) {
		this.files = files;
		// SpatialPoiSearch will be passed as parameter
		this.poiSearch = poiSearch;
		this.location = location;
		this.settings = settings;
		limitLocationBboxes = new int[settings.OPTIM_LIMIT_RADIUS.length][];
		LatLon loc = getLimitLocationFromFiles(files, location);
		for (int k = 0; k < limitLocationBboxes.length; k++) {
			limitLocationBboxes[k] = calculateBbox(settings.OPTIM_LIMIT_RADIUS[k], loc);
		}
	}
	
	private LatLon getLimitLocationFromFiles(List<BinaryMapIndexReader> files, LatLon limitLocation) {
		if (limitLocation == null) {
			for (BinaryMapIndexReader f : files) {
				limitLocation = f.getRegionCenter();
				if (limitLocation != null) {
					break;
				}
			}
			if (limitLocation == null) {
				limitLocation = new LatLon(0, 0);
			}
		}
		return limitLocation;
	}
	
	public static int[] calculateBbox(int radiusMeters, LatLon l) {
		QuadRect qr = MapUtils.calculateBbox(radiusMeters, l);
		int[] bbox31 = new int[4];
//		System.out.printf("Bbox limit: %.4f %.4f - %.4f %.4f\n", northWest.getLatitude(), northWest.getLongitude(),
//				southEast.getLatitude(), southEast.getLongitude());
//		int xleft = bbox31[0], xright = bbox31[2];
//		int ytop = bbox31[1], ybottom = bbox31[3];
		bbox31[1] = (int) qr.top;
		bbox31[0] = (int) qr.left;
		bbox31[3] = (int) qr.bottom;
		bbox31[2] = (int) qr.right;
		return bbox31;
	}
	
	public SpatialSearchStats getStats() {
		return stats;
	}

	public void initFiles(SpatialSearchGlobalCache cache) throws IOException {
		int indexInd = 0;
		int fileInd = 0;
		for (BinaryMapIndexReader bir : files) {
			SpatialSearchFileCache fc = cache.filesCache.get(bir.getFile().getName());
			if (fc == null || !fc.test(bir)) {
				fc = new SpatialSearchFileCache(bir);
			}
			cache.filesCache.put(fc.file, fc);
			fc.indexInd = indexInd;
			fc.fileInd = fileInd;
			this.internalFile.add(fc);
			indexInd += fc.indexReaders.size();
			fileInd++;
			boolean initPoi = false;
			if (fc.poiFrequencies != null || fc.poiSearch != poiSearch) {
				fc.poiFrequencies = new HashMap<String, Integer>();
				fc.poiSearch = poiSearch;
				initPoi = true;
			}
			for (NameIndexReader r : fc.indexReaders) {
				if (r.poiRegion != null) {
					long bRead = bir.getBytesRead();
					bir.initCategories(r.poiRegion);
					if (initPoi) {
						poiSearch.init(cache, fc, bir, r.poiRegion);
					}
					stats.readAtomsBytes += (bir.getBytesRead() - bRead);
				}
				r.gcPrefixes(settings.AUTO_CLEAR_PREFIX_CACHE_LIMIT);
			}
		}
	}
	
	
	public void setTokens(List<SpatialSearchToken> tokens) {
		this.tokens = tokens;
	}

	
	void processPoiCategories() throws IOException {
		if (settings.SEARCH_POI_CATEGORIES) {
			poiSearch.processPoiCategories(this, tokens);
		}
	}

	void readAtoms() throws IOException {
		int indxInd = 0;
		
		for (int fileInd = 0; fileInd < files.size(); fileInd++) {
			SpatialSearchFileCache iCache = internalFile.get(fileInd);
			BinaryMapIndexReader b = files.get(fileInd);
			for (NameIndexReader indx : iCache.indexReaders) {
				indx.resetBytesStat();
				readAtoms(tokens, b, indx, indxInd);
				indxInd++;
				NameIndexReaderBytes bytesStat = indx.getBytesStat();
				stats.readAtomsBytes += bytesStat.readAtomBytes;
				stats.skipAtomsBytes += bytesStat.skipAtomBytes;
				stats.readTableBytes += (bytesStat.readTableBytes - bytesStat.skipTableBytes);
				stats.skipTableBytes += bytesStat.skipTableBytes;
			}
		}
		// add partial once we read all files
		if (settings.OPTIM_READ_COMMON_WORDS_ATOMS) {
			for (SpatialSearchToken t : tokens) {
				List<NameIndexAtom> partialCommonAtoms = t.getPartialCommonAtoms();
				// 'haupstrasse' vs 'haupstrasse <specifier>'
				boolean partialAreSameFreq = partialCommonAtoms.size() < t.atoms.size() / 2;
				int nearbyLimit = Integer.MAX_VALUE;
				if (!partialAreSameFreq) {
					int[] cnts = new int[settings.OPTIM_LIMIT_RADIUS.length + 1];
					for (NameIndexAtom a : partialCommonAtoms) {
						cnts[a.nearbyRadius]++;
					}
					nearbyLimit = 0;
					int cnt = t.atoms.size();
					while (nearbyLimit < cnts.length
							&& cnts[nearbyLimit] + cnt < settings.OPTIM_READ_COMMON_WORDS_LIMIT) {
						cnt += cnts[nearbyLimit];
						nearbyLimit++;
					}
				}
				for (int ind = 0; ind < partialCommonAtoms.size(); ind++) {
					NameIndexAtom atom = partialCommonAtoms.get(ind);
					if (atom.nearbyRadius >= nearbyLimit) {
						continue;
					}
					List<SpatialSearchToken> otherTokens = t.getPartialOtherTokens(ind);
					t.addAtom(atom);
					if (otherTokens != null) {
						for (SpatialSearchToken otherToken : otherTokens) {
							otherToken.addAtom(new NameIndexAtom(atom));
						}
					}
					addBuildingAtoms(t, tokens, otherTokens, t.getPartialNumericNonMatch(ind), atom);
				}
				t.clearPartialAtoms();
			}
		}
		if (stats.printLogs) {
			System.out.println(tokenStats(tokens).toString());
 		}
		if (settings.OPTIM_DELETE_EMBEDDED_BOUNDARIES) {
			stats.sub1PoiNameBoundaryTime.start();
			Map<TIntArrayList, List<AtomByTokens>> boundaries = filterEmbeddedBoundaries(tokens);
			if (settings.OPTIM_FLAG_POI_SAME_AS_CITY_STREET || settings.OPTIM_DELETE_POI_SAME_AS_CITY_STREET) {
				assignPoiFlagGeo(boundaries, tokens);
			}
			stats.sub1PoiNameBoundaryTime.finish();
		}
		
	}
	

	private void assignPoiFlagGeo(Map<TIntArrayList, List<AtomByTokens>> cities, List<SpatialSearchToken> tokens) {
		Map<TIntArrayList, List<AtomByTokens>> streets = groupAtomsByTokens(tokens, t -> t.isStreet()); // check performance
		Map<TIntArrayList, List<AtomByTokens>> pois = groupAtomsByTokens(tokens, t -> t.isPOI());
		Iterator<Entry<TIntArrayList, List<AtomByTokens>>> it = pois.entrySet().iterator();
		while (it.hasNext()) {
			Entry<TIntArrayList, List<AtomByTokens>> e = it.next();
			TIntArrayList lst = e.getKey();
			List<AtomByTokens> cityNames = cities.get(lst);
			if (cityNames != null) {
				for (AtomByTokens poi : e.getValue()) {
					markPOIAsArea(poi, cityNames, lst, tokens);
				}
			}
			List<AtomByTokens> streetNames = streets.get(lst);
			if (streetNames != null) {
				for (AtomByTokens poi : e.getValue()) {
					markPOIAsArea(poi, streetNames, lst, tokens);
				}
			}
		}
	}

	private void markPOIAsArea(AtomByTokens poi, List<AtomByTokens> cityNames, TIntArrayList indxs,
			List<SpatialSearchToken> tokens) {
		for (AtomByTokens largeArea : cityNames) {
			if (largeArea.obj.coords.contains(poi.obj.coords)) {
				TIntIterator it = indxs.iterator();
				while (it.hasNext()) {
					int indx = it.next();
					if (settings.OPTIM_DELETE_POI_SAME_AS_CITY_STREET) {
						// delete completely no clear use case for improvement yet found
						tokens.get(indx).removeAtom(poi.obj);
					} else {
						// mark to not intersect
						NameIndexAtom atomSet = tokens.get(indx).getAtomToken(poi.obj);
						atomSet.sameNameAreaObj = largeArea.obj;
					}
				}
				return;
			}
		}

	}

	record AtomByTokens(NameIndexAtom obj, TIntArrayList lstTokens) {
	}
	
	
	
	private Map<TIntArrayList, List<AtomByTokens>> filterEmbeddedBoundaries(List<SpatialSearchToken> tokens) {
		Map<TIntArrayList, List<AtomByTokens>> group = groupAtomsByTokens(tokens, t -> t.isCityVillage() || t.isBoundary());
		// 3. find the largest boundary and delete embedded
		Iterator<Entry<TIntArrayList, List<AtomByTokens>>> it = group.entrySet().iterator();
		while (it.hasNext()) {
			Entry<TIntArrayList, List<AtomByTokens>> e = it.next();
			TIntArrayList lst = e.getKey();
//			if (lst.size() == tokens.size()) {
			if (lst.size() >= tokens.size() - 1) {
				// do not delete full match tokens
				continue;
			}
			List<AtomByTokens> collection = e.getValue();
			for (int k = 0; k < collection.size();) {
				AtomByTokens aBoundary = collection.get(k);
				AtomByTokens toDelete = null;
//				BoundaryTokens reason = null;
				for (int l = 0; l < collection.size(); l++) {
					if (k == l) {
						continue;
					}
					AtomByTokens bBoundary = collection.get(l);
					if (bBoundary.obj.coords.contains(aBoundary.obj.coords)
							&& bBoundary.obj.otherWordsCnt <= aBoundary.obj.otherWordsCnt) {
						toDelete = aBoundary;
//						reason = bBoundary;
						break;
					}
				}
				if (toDelete != null) {
//					System.out.println("DELETE " + aBoundary + " of " + reason);
					collection.remove(k);
					for (int token : lst.toArray()) {
						tokens.get(token).removeAtom(toDelete.obj);
					}
				} else {
					k++;
				}
			}
//			System.out.printf("Boundaries clean up '%s' %d -> %d: %s \n", words, sz, collection.size(), collection);
		}
		return group;
	}

	private Map<TIntArrayList, List<AtomByTokens>> groupAtomsByTokens(List<SpatialSearchToken> tokens, Predicate<NameIndexAtom> filter) {
		TLongObjectHashMap<AtomByTokens> boundaries = new TLongObjectHashMap<>();
		// 1. index boundaries by tokens 
		for (int tokenOrder = 0; tokenOrder < tokens.size(); tokenOrder++) {
			SpatialSearchToken token = tokens.get(tokenOrder);
			for (NameIndexAtom a : token.atoms) {
				if (filter.test(a)) {
					if (!boundaries.containsKey(a.id)) {
						boundaries.put(a.id, new AtomByTokens(a, new TIntArrayList(5)));
					}
					boundaries.get(a.id).lstTokens.add(tokenOrder);
				}
			}
		}
//		System.out.println("Boundaries " + boundaries.size());
		// 2. combine boundaries by same tokens to find the largest boundary
		Map<TIntArrayList, List<AtomByTokens>> regroup = new HashMap<>();
		for(AtomByTokens b : boundaries.valueCollection()) {
			List<AtomByTokens> list = regroup.get(b.lstTokens);
			if (list == null) {
				list = new ArrayList<>();
				regroup.put(b.lstTokens, list);
			}
			list.add(b);
		}
		return regroup;
	}

	private StringBuilder tokenStats(List<SpatialSearchToken> tokens) {
		StringBuilder s = new StringBuilder("Token stats: ");
		TLongHashSet ids = new TLongHashSet();
		for (SpatialSearchToken t : tokens) {
			int level0 = 0;
			for (NameIndexAtom at : t.atoms) {
				ids.add(at.id);
				if (at.nearbyRadius <= 1) {
					level0++;
				}
			}
			s.append(String.format("'%s' (%,d, 5km-%,d), ", t.word, t.atoms.size(), level0));
		}
		return s;
	}
	
	private void readAtoms(List<SpatialSearchToken> tokens, BinaryMapIndexReader b, NameIndexReader indx, int indxInd)
			throws IOException {
		// sort to assign tokens to '2nd street 2' first instead '2 2nd street'
		tokens.sort(new Comparator<SpatialSearchToken>() {
			@Override
			public int compare(SpatialSearchToken o1, SpatialSearchToken o2) {
				int cm = Boolean.compare(SearchAlgorithms.isNumber2Letters(o1.word), SearchAlgorithms.isNumber2Letters(o2.word));
				if (cm != 0) {
					return cm;
				}
				return Integer.compare(o1.originalOrder, o2.originalOrder);
			}
		});
		for (SpatialSearchToken t : tokens) {
			List<PrefixNameValue> matchedPrefixes = indx.getMatchedPrefixes(t.word);
			if (matchedPrefixes == null) {
				stats.sub1FileAtomsTime.start();
				matchedPrefixes = b.readFullNameIndex(indx.setQuery(t.word, t.getPrefixMatcher(stats)));
				if (matchedPrefixes == null) {
					continue;
				}
				stats.sub1FileAtomsTime.finish();
			}
			for (PrefixNameValue prefix : matchedPrefixes) {
				parseAtomSuffixes(t, indxInd, indx, prefix, tokens);
			}
		}
	}
	
	static boolean checkPoiTypeId(int poiTypeId) {
		// first 2^8 - 256 bytes not possible to be used by file
		if (poiTypeId > (1 << (SHIFT_FILE_IND + 8))) {
			throw new IllegalStateException("Possible overlap with addr / poi id");
		}
		return true;
	}

	private long makeAddrId(int fileInd, long shiftToIndex) {
		if (fileInd > 1 << SHIFT_FILE_IND) {
			throw new IllegalStateException();
		}
		long id = (shiftToIndex << SHIFT_FILE_IND) + fileInd;
		return id;
	}
	
	private long makePoiId(int fileInd, long shiftToIndex, int poiInd) {
		if (fileInd > 1 << SHIFT_FILE_IND) {
			throw new IllegalStateException();
		}
		if (poiInd > 1 << SHIFT_POI_IND) {
			throw new IllegalStateException();
		}
		long id = (((shiftToIndex << SHIFT_POI_IND) + poiInd) << SHIFT_FILE_IND) + fileInd;
		return id;
	}

	private void parseAtomSuffixes(SpatialSearchToken t, int indInd, NameIndexReader indx, PrefixNameValue prefix,
			List<SpatialSearchToken> allTokens) throws IOException {
		String curSuffix = null;
		List<String> suffixes = new ArrayList<>();
		List<String> commonSuffixes = new ArrayList<>();
		boolean addr = prefix.addr != null;
		for (String s : addr ? prefix.addr.getSuffixesDictionaryList() : prefix.poi.getSuffixesDictionaryList()) {
			curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
			suffixes.add(prefix.key + curSuffix);
		}
		for (Integer i : addr ? prefix.addr.getSuffixesCommonDictionaryList()
				: prefix.poi.getSuffixesCommonDictionaryList()) {
			commonSuffixes.add(indx.getCommonIndexed(i));
		}
		if (addr && settings.SEARCH_ADDR) {
			for (AddressNameIndexDataAtom a : prefix.addr.getAtomList()) {
				long lid = makeAddrId(indInd, prefix.shift - a.getShiftToIndex(0));
				long pid = 0;
				if (a.getType() == CityBlocks.STREET_TYPE.index) {
					pid = makeAddrId(indInd, prefix.shift - a.getShiftToCityIndex(0));
				} else if (a.getType() != CityBlocks.BOUNDARY_TYPE.index && a.getType() != CityBlocks.CITY_TOWN_TYPE.index
						&& a.getType() != CityBlocks.VILLAGES_TYPE.index && a.getType() != CityBlocks.POSTCODES_TYPE.index) {
					continue;
				}
				MapObject obj = null;
				if (settings.DEV_READ_ADDR_OBJECTS) {
					obj = readAddrObject(lid, pid, null);
				}
				parseSuffixes(t, indx, suffixes, commonSuffixes, a, null, lid, pid, obj, allTokens);
			}
		} else if (!addr && settings.SEARCH_POI) {
			for (OsmAndPoiNameIndexDataAtom a : prefix.poi.getAtomsList()) {
				if (a.getPoiIndInBlockCount() == 0) {
					// intermediate version ignore
					continue;
				}
				long lid = makePoiId(indInd, BinaryMapIndexReader.convertFixed32ToRef(a.getShiftTo()),
						a.getPoiIndInBlock(0));
				MapObject amenity = null;
				if (settings.DEV_READ_POI_OBJECTS) {
					amenity = readPoiObject(lid, null);
				}
				parseSuffixes(t, indx, suffixes, commonSuffixes, null, a, lid, 0, amenity, allTokens);
			}
		}
	}
	
	
	public void readPOIBboxes(int indInd, TLongHashSet tiles) throws IOException {
		NameIndexReader nameIndex = null;
		SpatialSearchFileCache c = null;
		for (int k = 0; k < internalFile.size(); k++) {
			c = internalFile.get(k);
			if (indInd < c.indexInd + c.indexReaders.size()) {
				nameIndex = c.indexReaders.get(indInd - c.indexInd);
				break;
			}
		}
		stats.sub2ReadObjTime.start();
		BinaryMapIndexReader bmir = files.get(c.fileInd);
		long bytesRead = bmir.getBytesRead();
		bmir.readAmenityBboxes(nameIndex.poiRegion, tiles);
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead);
		stats.sub2ReadObjTime.finish();
	}
	
	public int getFileInd(long id) {
		int indInd = (int) (id & ((1l << SHIFT_FILE_IND) - 1));
		return indInd;
	}

	public MapObject readPoiObject(long id, TLongObjectHashMap<MapObject> cache) throws IOException {
		if (cache != null) {
			MapObject mapObject = cache.get(id);
			if (mapObject != null) {
				return mapObject;
			}
		}
		long oid = id;
		int indInd = (int) (id & ((1l << SHIFT_FILE_IND) - 1));
		id >>= SHIFT_FILE_IND;
		int poiInd = (int) (id & ((1l << SHIFT_POI_IND) - 1));
		id >>= SHIFT_POI_IND;
		long shift = id;

		NameIndexReader nameIndex = null;
		SpatialSearchFileCache c = null;
		for (int k = 0; k < internalFile.size(); k++) {
			c = internalFile.get(k);
			if (indInd < c.indexInd + c.indexReaders.size()) {
				nameIndex = c.indexReaders.get(indInd - c.indexInd);
				break;
			}
		}

		stats.sub2ReadObjTime.start();
		BinaryMapIndexReader bmir = files.get(c.fileInd);
		long bytesRead = bmir.getBytesRead();
		List<Amenity> lst = bmir.readAmenityBlock(nameIndex.poiRegion, shift, poiInd);
		if (cache != null) {
			long ofirstid = oid - (poiInd << SHIFT_FILE_IND);
			for (int i = 0; i < lst.size(); i++) {
				cache.put(ofirstid + (i << SHIFT_FILE_IND), lst.get(i));
			}
		}
		MapObject amenity = lst.get(poiInd);
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead);
		stats.sub2ReadObjTime.finish();
		return amenity;
	}

	public MapObject readAddrObject(long id, long pid, TLongObjectHashMap<MapObject> cache) throws IOException {
		if (cache != null) {
			MapObject obj = cache.get(id);
			if (obj != null) {
				return obj;
			}
		}
		long opid = pid;
		int indInd = (int) (id & ((1l << SHIFT_FILE_IND) - 1));
		id >>= SHIFT_FILE_IND;
		long shift = id;
		
		NameIndexReader nameIndex = null;
		SpatialSearchFileCache c = null;
		for (int k = 0; k < internalFile.size(); k++) {
			c = internalFile.get(k);
			if (indInd < c.indexInd + c.indexReaders.size()) {
				nameIndex = c.indexReaders.get(indInd - c.indexInd);
				break;
			}
		}		
		BinaryMapIndexReader bmir = files.get(c.fileInd);
		long bytesRead = bmir.getBytesRead();
		stats.sub2ReadObjTime.start();
		MapObject obj;
		if (pid != 0) {
			int pIndInd = (int) (pid & ((1l << SHIFT_FILE_IND) - 1));
			pid >>= SHIFT_FILE_IND;
			long pshift = pid;
			if (pIndInd != indInd) {
				throw new UnsupportedOperationException();
			}
			City city = null;
			if (cache != null) {
				city = (City) cache.get(opid);
			}
			if (city == null) {
				city = bmir.readCityObject(nameIndex.addressRegion, pshift);
			}
			obj = bmir.readStreetObject(nameIndex.addressRegion, city, shift);
		} else  {
			obj = bmir.readCityObject(nameIndex.addressRegion, shift);
		}
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead);
		stats.sub2ReadObjTime.finish();
		return obj;
	}

	private void parseSuffixes(SpatialSearchToken t, NameIndexReader indx, List<String> suffixes,
			List<String> commonSuffixes, AddressNameIndexDataAtom a, OsmAndPoiNameIndexDataAtom b, long cid, long pid,
			MapObject obj, List<SpatialSearchToken> allTokens) {
		int cnt = a != null ? a.getSuffixesBitsetIndexCount() : b.getSuffixesBitsetIndexCount();
		String name = "";
		int wordInd = 0;
		int type = a != null ? a.getType() : SpatialSearchToken.POI_TYPE;
		for (int i = 0; i < cnt; i++) {
			int suffBit = a != null ? a.getSuffixesBitsetIndex(i) : b.getSuffixesBitsetIndex(i);
			if (suffBit % 2 == 0) {
				int ind = suffBit / 2 - 1;
				if (ind == -1) {
					if (a != null && wordInd < a.getExtraSuffixCount()) {
						name += a.getExtraSuffix(wordInd);
					} else if(b != null && wordInd < b.getExtraSuffixCount()) {
						name += b.getExtraSuffix(wordInd);
					}
					if (matchName(t, name) || (name = matchPartName(t, name, allTokens)) != null) {
						int other;
						if (a != null) {
							other = wordInd < a.getOtherWordsCountCount() ? a.getOtherWordsCount(wordInd) : 0;
						} else {
							other = wordInd < b.getOtherWordsCountCount() ? b.getOtherWordsCount(wordInd) : 0;
						}
						addObject(t, indx, name, type, cid, pid, obj, other, new NameIndexAtomXY(a, b, settings), allTokens);
					}
					wordInd++;
					name = "";
				} else if (ind < suffixes.size()) {
					name += suffixes.get(ind);
				} else {
					// common suffix
					name += " " + commonSuffixes.get(ind - suffixes.size());
				}
			} else {
				if (suffBit % 4 == 1) {
					// separated number
					name += " " + (suffBit >> 2);
				} else {
					// partial
					name += (suffBit >> 2);
				}
			}
		}
		if (a != null && wordInd < a.getExtraSuffixCount()) {
			name += a.getExtraSuffix(wordInd);
		} else if (b != null && wordInd < b.getExtraSuffixCount()) {
			name += b.getExtraSuffix(wordInd);
		}
		
		if (name.length() != 0 && (matchName(t, name) || (name = matchPartName(t, name, allTokens)) != null)) {
			int other;
			if (a != null) {
				other = wordInd < a.getOtherWordsCountCount() ? a.getOtherWordsCount(wordInd) : 0;
			} else {
				other = wordInd < b.getOtherWordsCountCount() ? b.getOtherWordsCount(wordInd) : 0;
			}
			// object will be added once it's read rare word
			// disabled for now as it could only have effect for frequent words in index
			addObject(t, indx, name, type, cid, pid, obj, other, new NameIndexAtomXY(a, b, settings), allTokens);
		}
	}

	private boolean matchName(SpatialSearchToken t, String name) {
		stats.sub1MatchTime.start();
		boolean acceptName = t.matchName(name);
		stats.sub1MatchTime.finish();
		return acceptName;
	}
	
	private String matchPartName(SpatialSearchToken t, String name, List<SpatialSearchToken> allTokens) {
		stats.sub1MatchTime.start();
		String[] res = t.matchSplitName(name);
		String resName = null;
		if (res != null) {
			for (SpatialSearchToken st : allTokens) {
				if (st != t && st.matchName(res[1])) {
//					System.out.printf("%s -> '%s %s'\n", name, res[0], res[1]);
					resName = res[0] + " " + res[1];
					break;
				}
			}
		}
		stats.sub1MatchTime.finish();
		return resName;
	}

	private void addObject(SpatialSearchToken t, NameIndexReader indx, String name, int type, long lid, long pid,
			MapObject obj, int other, NameIndexAtomXY coords, List<SpatialSearchToken> allTokens) {
		List<SpatialSearchToken> otherTokens = null;
		boolean streetCity = false;
		boolean numericNotMatch = false;
		List<String> split = null;
		if (name.indexOf(' ') != -1) {
			split = SearchAlgorithms.splitAndNormalize(name, false);
		}
		// split '-' to allow search 'M-42' as 'M 42'
		if (name.indexOf('-') != -1 && !t.word.contains("-")) {
			split = SearchAlgorithms.splitAndNormalize(name.replace('-', ' '), false);
		}
		// '2.Sokak'
		if (name.indexOf('.') != -1) {
			split = SearchAlgorithms.splitAndNormalize(name.replace('.', ' '), false);
		}
		if (split != null) {
			for (int k = 1; k < split.size(); k++) {
				String otherName = split.get(k);
				boolean numeric = SearchAlgorithms.isNumber2Letters(otherName);
				if (otherName.equalsIgnoreCase(NameIndexReader.CITY_AS_STREET_COMMON)) {
					streetCity = true;
					continue;
				}
				boolean matched = false;
				for (SpatialSearchToken token : allTokens) {
					if (t != token && matchName(token, otherName)
							&& (otherTokens == null || !otherTokens.contains(token))) {
						if (otherTokens == null) {
							otherTokens = new ArrayList<>(3);
						}
						otherTokens.add(token);
						matched = true;
						break;
					}
				}
				if (!matched) {
					if (numeric) {
						numericNotMatch = !t.word.contains(otherName); // "us 15" data, "us-15" token
					}
					if (!Abbreviations.isCommonSkipOtherCnt(otherName)) {
						other++;
					}
				}
			}
		}
		int otherFound = otherTokens == null ? 0 : otherTokens.size();
		int nearByType = 0;
		for (; nearByType < limitLocationBboxes.length; nearByType++) {
			if (coords.intersects(limitLocationBboxes[nearByType])) {
				break;
			}
		}
		NameIndexAtom atom = new NameIndexAtom(name, type, lid, pid, obj, streetCity, other, otherFound, coords, nearByType);
		// for all common always false, for some frequent could be optimization
		if (settings.OPTIM_READ_COMMON_WORDS_ATOMS && other > 0) {
			boolean matchMainWord = true;
			// token could match multiple different words (common & non-common)
			String mainWord = name;
			if (split != null) {
				mainWord = split.get(0); // name 'ru de rue' could match 'rue' it's because of prefix & suffixes
				matchMainWord = matchName(t, mainWord);
			}
			if (!matchMainWord || isWordCommonlyUsed(indx, mainWord)) {
				// other tokens didn't match for common word but present in object
				t.addPartialCommonAtom(atom, otherTokens, numericNotMatch);
				return;
			}
		}
		t.addAtom(atom);
		if (otherTokens != null) {
			for (SpatialSearchToken otherToken : otherTokens) {
				otherToken.addAtom(new NameIndexAtom(atom));
			}
		}
		addBuildingAtoms(t, allTokens, otherTokens, numericNotMatch, atom);

	}

	private boolean isWordCommonlyUsed(NameIndexReader indx, String mainWord) {
		// store commonlyUsedWords across all files
		if (commonlyUsedWords.contains(mainWord)) {
			return true;
		}
		// always search numbers as they could be very specific - "2" token could match "2-nd" atom
		// However 2 shouldn't be in common words by design!
		if(SearchAlgorithms.isNumber2Letters(mainWord)) {
			return false;
		}
		ValueFreq isCommonWord = indx.getCommonWordsStats().get(SearchAlgorithms.alignChars(mainWord));
		if (isCommonWord == null) {
			return false;
		}
		// we can't yet rely on indexed stats as ('chateau vieux ...') 
		// still shouldn't show up on single 'chateau' as it contains extra words but will be indexed
		// this could be an issue for rare cases "haupstrasse <villagename>"
//		if (isCommonWord.freq > 2 * isCommonWord.extra) {
		commonlyUsedWords.add(mainWord);
		return true;
	}

	private void addBuildingAtoms(SpatialSearchToken t, List<SpatialSearchToken> allTokens,
			List<SpatialSearchToken> otherTokens, boolean numericNotMatch, NameIndexAtom atom) {
		boolean street = atom.type == SpatialSearchToken.STREET_TYPE;
		// numericNotMatch - require full street match to assign buildings 
		if (!numericNotMatch && street && settings.SEARCH_BUILDINGS) {
			for (SpatialSearchToken token : allTokens) {
				// assign building to word token isNumber2Letters (number + 1 char) + possible buildings
				if (t != token && token.likelyPartOfBuilding()
						&& (otherTokens == null || !otherTokens.contains(token))) {
					NameIndexAtom atomB = new NameIndexAtom(atom.name, SpatialSearchToken.BUILDING_TYPE, atom.id, atom.parentid, atom.object,
							atom.cityAsStreet, atom.otherWordsCnt, atom.otherFoundCnt, atom.coords, atom.nearbyRadius, t.originalOrder);
					token.addAtom(atomB);
				}
			}
		}
	}



}
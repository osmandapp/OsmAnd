package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.spatial.SpatialPoiSearch.SpatialPoiType;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;


public class SpatialSearchResultsList implements Comparable<SpatialSearchResultsList> {
	final SpatialSearchToken[] tokens; // non modifiable!
	final int tCount;
	
	int MIN_ELO_RATING = Amenity.DEFAULT_ELO;
	
	public static long DEFAULT_BLD_ID = -0; // special flag for building partial match
	public static long PARTIAL_ID_MATCH = -4; // special flag for building partial match
	public static long SURPLUS_ID_MATCH = -16; // special flag for building partial match (11-NUON)

	// NameIndexAtom[][] -- should be double array to store list of combinations
	List<NameIndexAtom> linearResults = new ArrayList<>();
	TIntArrayList typeIntersections = new TIntArrayList();
	TLongArrayList tileIds = new TLongArrayList();
	TIntArrayList tileZooms = new TIntArrayList();
	HashQuadTree<Integer> quadTree = new HashQuadTree<>(16);
	
	int limitIntersection = -1;

	TIntObjectHashMap<Boolean> skipResults = new TIntObjectHashMap<>();
	Map<Integer, LatLon> preciseLocations = new HashMap<Integer, LatLon>();
	List<SpatialSearchResult> finalResult = null;
	
	List<String> tempBuildNames1 = new ArrayList<String>();
	List<String> tempBuildNames2 = new ArrayList<String>();
	Map<String, SpatialSearchResult> extraIdsResults = new HashMap<>();
	TLongObjectHashMap<SpatialSearchResult> uniqueIdsResults = new TLongObjectHashMap<>();
	
	public SpatialSearchResultsList() {
		this(null, null, null);
	}

	public SpatialSearchResultsList(SpatialSearchContext ctx, SpatialSearchToken token, SpatialSearchResultsList parent) {
		if (token == null) {
			tokens = new SpatialSearchToken[0];
		} else {
			tokens = new SpatialSearchToken[parent.tokens.length + 1];
			tokens[0] = token;
			for (int k = 0; k < parent.tokens.length; k++) {
				tokens[k + 1] = parent.tokens[k];
			}
		}
		tCount = tokens.length;
		if (parent != null) {
			limitIntersection = parent.limitIntersection == -1 ? (ctx.limitLocationBboxes.length) : parent.limitIntersection;
			MIN_ELO_RATING = ctx.settings.MIN_ELO_RATING;
			calculateMainIntersection(ctx, token, parent);
		}
	}
	
	private void loadObjects(SpatialSearchContext ctx, int type, TLongObjectHashMap<MapObject> cache) throws IOException {
		TLongObjectHashMap<Long> lstMap = new TLongObjectHashMap<>();
		
		Map<Integer, TLongHashSet> poiBboxes = new LinkedHashMap<Integer, TLongHashSet>();
		for (NameIndexAtom a : linearResults) {
			if (a.object != null) {
				cache.put(a.id, a.object);
				continue;
			}
			if (a.type == SpatialSearchToken.POI_TYPE) {
				int indInd = ctx.getFileInd(a.id);
				if(!poiBboxes.containsKey(indInd)) {
					poiBboxes.put(indInd, new TLongHashSet());
				}
				poiBboxes.get(indInd).add(HashQuadTree.encodeTileId31(BinaryMapPoiReaderAdapter.EVAL_TAG_GROUP_ZOOM,
						a.coords.x16 << 15, a.coords.y16 << 15));
			}
			if (a.type == type || (type == SpatialSearchToken.ALL_CITY_TYPE && a.type != SpatialSearchToken.POI_TYPE
					&& a.type != SpatialSearchToken.STREET_TYPE && a.type != SpatialSearchToken.POI_CATEGORY_TYPE)) {
				lstMap.put(a.id, a.parentid);
			} else if(type == SpatialSearchToken.ALL_CITY_TYPE && a.type == SpatialSearchToken.STREET_TYPE) {
				lstMap.put(a.parentid, (long) 0);
			}
		}
		for (Map.Entry<Integer, TLongHashSet> poiBoxEntry : poiBboxes.entrySet()) {
			ctx.readPOIBboxes(poiBoxEntry.getKey(), poiBoxEntry.getValue());
		}
		TLongArrayList lst = new TLongArrayList(lstMap.keySet());
		lst.sort(); // sort is not correct for file ind last bits >>> 12 
		for(int i = 0; i < lst.size(); i++) {
			long id = lst.get(i);
			if (type == SpatialSearchToken.POI_TYPE) {
				cache.put(id, ctx.readPoiObject(id, cache));
			} else {
				cache.put(id, ctx.readAddrObject(id, lstMap.get(id), cache));
			}
		}
		for (NameIndexAtom a : linearResults) {
			MapObject mo = cache.get(a.id);
			if (mo != null) {
				a.object = mo;
			}
		}
	}
	
	public void loadObjects(SpatialSearchContext ctx) throws IOException {
		TLongObjectHashMap<MapObject> cache = new TLongObjectHashMap<MapObject>();
		loadObjects(ctx, SpatialSearchToken.POI_TYPE, cache);
		cache.clear();
		loadObjects(ctx, SpatialSearchToken.ALL_CITY_TYPE, cache);
		loadObjects(ctx, SpatialSearchToken.STREET_TYPE, cache);
	}
	
	public List<SpatialSearchToken> getMissingTokens(SpatialSearchContext ctx) {
		List<SpatialSearchToken> lst = new ArrayList<>(ctx.tokens);
		for (SpatialSearchToken s : tokens) {
			lst.remove(s);
		}
		return lst;
	}
	
	public void loadObjectsAndCalcBuildings(SpatialSearchContext ctx) throws IOException {
		ctx.stats.sub2LoadObjectsBldTime.start();
		loadObjects(ctx);
		// calculate amenity type
		for (int indx = 0; indx < getCombinations(); indx++) {
			if (skipResults.contains(indx)) {
				continue;
			}
			boolean skip = checkAmenityType(ctx, indx);
			if (!skip) {
				skipResults.put(indx, true);
			}
		}
		
		if (ctx.settings.SEARCH_BUILDINGS) {
			Map<String, Building> bldCheckCache = new HashMap<>();
			for (int indx = 0; indx < getCombinations(); indx++) {
				if (!skipResults.contains(indx)) {
					calcBuilding(ctx, indx, bldCheckCache);
				}
//				if (!skipResults.contains(indx)) {
//					System.out.println(indx + " " + getRawAtoms(indx) + " " + skipResults.contains(indx));
//				}
			}
		}
		if (ctx.settings.SEARCH_STREET_INTERSECTIONS) {
			for (int indx = 0; indx < getCombinations(); indx++) {
				if (!skipResults.contains(indx)) {
					calcStreetIntersections(ctx, indx);
				}
			}
		}
		
		ctx.stats.sub2LoadObjectsBldTime.finish();
	}

	private boolean checkAmenityType(SpatialSearchContext ctx, int indx) {
		SpatialPoiType type = null;
		for (int i = 0; i < tCount; i++) {
			NameIndexAtom atom = linearResults.get(indx * tCount + i);
			if (atom.isPoiCategory()) {
				type = ctx.poiSearch.getById((int) atom.id);
				break;
			}
		}
		if (type == null) {
			return true;
		}
		for (int i = 0; i < tCount; i++) {
			NameIndexAtom atom = linearResults.get(indx * tCount + i);
			if (atom.object instanceof Amenity a) {
				boolean matched = type.accept(a);
				if (!matched) {
					return false;
				}
				// old logic for missing tokens
//				for (SpatialSearchToken st : missingTokens) {
//					if (st.hasPoiType(a.getType().getKeyName(), ctx.poiSearch) != null
//							|| st.hasPoiType(a.getSubType(), ctx.poiSearch) != null) {
//						matched = true;
//						break;
//					}
//					if (a.getSubType().indexOf(';') != -1) {
//						for (String subtStr : a.getSubType().split(";")) {
//							if (st.hasPoiType(subtStr, ctx.poiSearch) != null) {
//								matched = true;
//								break;
//							}
//						}
//					}
//				}
//				if (matched) {
//					atom.otherWordsCnt = -1; // could be added to surplusWords later 
//					break;
//				}
			}
		}
		return true;
	}
	
	private void calcStreetIntersections(SpatialSearchContext ctx, int indx) {
		NameIndexAtom first = null;
		NameIndexAtom second = null;
		for (int i = 0; i < tCount; i++) {
			NameIndexAtom atom = linearResults.get(indx * tCount + i);
			if (atom.object instanceof Street && !atom.isCityStreetName()) {
				if (first == null || first.object.getId().equals(atom.object.getId())) {
					first = atom;
				} else {
					second = atom;
				}
			}
		}
		if (first != null && first.object instanceof Street s1 && second != null
				&& second.object instanceof Street s2) {
			LatLon insLoc = null; 
			if (insLoc == null) {
				for (Street ins : s1.getIntersectedStreets()) {
					if (ins.getName().equals(s2.getName())) {
						insLoc = ins.getLocation();
//						System.out.printf("INTERSECTION x1 %.4f, %.4f %s (%s) x %s\n", insLoc.getLatitude(),
//								insLoc.getLongitude(), second.toString(), ins.getName(), first.toString());
						break;
					}
				}
			}
			if (insLoc == null) {
				for (Street ins : s2.getIntersectedStreets()) {
					if (ins.getName().equals(s1.getName())) {
						insLoc = ins.getLocation();
//						System.out.printf("INTERSECTION x2 %.4f, %.4f %s (%s) x %s\n", ins.getLocation().getLatitude(),
//								ins.getLocation().getLongitude(), first.toString(), ins.getName(), second.toString());
						break;
					}
				}
			}
			if (insLoc == null && ctx.settings.ALLOW_VIRTUAL_STREET_INTERSECTIONS && 
					s2.getIntersectedStreets().size() > 0 && s1.getIntersectedStreets().size() > 0) {
				// some virtual streets from addr:place end up without names and with 0 intersected streets Venezia
				LatLon l1 = s1.getLocation();
				LatLon l2 = s2.getLocation();
				insLoc = new LatLon(l1.getLatitude() / 2 + l2.getLatitude() / 2, l1.getLongitude() / 2 + l2.getLongitude() / 2);
// 				System.out.printf("INTERSECTION NO %.4f %.4f %s x %s\n", insLoc.getLatitude(),
//						insLoc.getLongitude(), first.toString(), second.toString());
			}
			if (insLoc != null && !first.isCityStreetName() && !second.isCityStreetName()) {
				if (second.object.getName().equals("Cannaregio")) {
					System.out.println(first + " " + second + " " + insLoc);
				}
				preciseLocations.put(indx, insLoc);
			} else {
				skipResults.put(indx, true);
			}
		}
	}
	
	private void calcBuilding(SpatialSearchContext ctx, int indx, Map<String, Building> bldCheckCache) {
		Map<NameIndexAtom, String> bldCheckMap = null; // mostly single key-value map, value accumulated
		boolean noBuildings = true;
		NameIndexAtom noBldStreet = null; 
		for (int i = 0; i < tCount; i++) {
			NameIndexAtom bld = linearResults.get(indx * tCount + i);
			if (bld.buildingInd >= 0) {
				noBuildings = false;
				int strTokenInd = getTokenByOriginalOrder(bld.buildingInd);
				if (strTokenInd < 0) {
					skipResults.put(indx, true);
					break;
				}
				NameIndexAtom str = linearResults.get(indx * tCount + strTokenInd);
				if (str.id != bld.id) {
					continue;
				}
				if (bldCheckMap == null) {
					bldCheckMap = new HashMap<>();
				}
				String searchKey = bldCheckMap.get(str);
				if (searchKey == null) {
					searchKey = tokens[i].word;
				} else {
					searchKey += " " + tokens[i].word;
				}
				bldCheckMap.put(str, searchKey);
			} else if (bld.isStreet()) {
				if (noBldStreet == null || bld.isCityStreetName()) {
					noBldStreet = bld;
				}
			}
		}
		int surplus = 0;
		if (noBuildings && noBldStreet != null) {
			if (ctx.tokens.size() > tCount) {
				if (bldCheckMap == null) {
					bldCheckMap = new HashMap<>();
				}
				String searchKey = "";
				List<SpatialSearchToken> current = Arrays.asList(tokens);
				for (SpatialSearchToken t : ctx.tokens) {
					if (!current.contains(t)) {
						searchKey += t.word + " ";
						surplus++;
					}
				}
				bldCheckMap.put(noBldStreet, searchKey);
			} else if (noBldStreet.cityAsStreet) {
				skipResults.put(indx, true);
			}
		}
		// check many buildings on same street possibly unit or ref
		if (bldCheckMap != null) {
			Iterator<Entry<NameIndexAtom, String>> it = bldCheckMap.entrySet().iterator();
			// usually map is just single street
			while (it.hasNext()) {
				Entry<NameIndexAtom, String> e = it.next();
				NameIndexAtom str = e.getKey();
				String bldName = e.getValue();
				String cacheKey = str.id + " " + bldName;
				Building bldObj = null;
				if (bldCheckCache.containsKey(cacheKey)) {
					bldObj = bldCheckCache.get(cacheKey);
				} else {
					bldObj = checkBuilding(ctx, (Street) str.object, bldName);
					if (bldObj == null) {
//						System.out.printf("No building '%s': %s\n", bldName, str.object + " " + ((Street) str.object).getBuildings());
					} else {
//						System.out.printf("Building found '%s' -'%s': %s\n", bldObj, bldName, str.object);
						if (surplus > 0) {
							bldObj.setId(SURPLUS_ID_MATCH);
						}
					}
					bldCheckCache.put(cacheKey, bldObj);
				}
				if (bldObj == null) {
					if (!noBuildings || (noBldStreet != null && noBldStreet.cityAsStreet)) {
						skipResults.put(indx, true);
						break;
					}
				} else {
					// assign buildings
					for (int i = 0; i < tCount; i++) {
						NameIndexAtom bld = linearResults.get(indx * tCount + i);
						if (bld.buildingInd >= 0 && str.id == bld.id) {
							bld.bldObject = bldObj;
							// bld.name = bldObj.getName();
						} else if(noBuildings  && str.id == bld.id) {
							bld.bldObject = bldObj;
						}
					}
					if (bldObj.isInterpolation()) {
						preciseLocations.put(indx, bldObj.getLocation(bldObj.interpolation(bldName)));
					}
				}
			}
		}
		
	}
	
	
	private Building checkBuilding(SpatialSearchContext ctx, Street street, String bld) {
		Building interpolation = null;
		Building partial2 = null;
		double distPartial1 = 0;
		Building partial1 = null;
		double distExact = 0;
		Building exact = null;
		Set<String> query = SearchAlgorithms.getBuildingCompareSet(bld, tempBuildNames1);
		for (Building b : street.getBuildings()) {
			if (b.isInterpolation()) {
				// interpolation only over 1 set
				if (query.size() == 1 && b.belongsToInterpolation(query.iterator().next())) {
					interpolation = b;
				} else if (query.size() > 1 && b.belongsToInterpolation(bld)) {
					interpolation = b;
				}
			} else {
				Set<String> bldSet = SearchAlgorithms.getBuildingCompareSet(b.getName(), tempBuildNames2);
//				System.out.println(street + " " + original + " ?= " + cmp);
				if (bldSet.equals(query)) {
					// exact match but could be duplicates 8-8
					// duplicate could be in query or in house number
					if ((tempBuildNames2.size() > bldSet.size() || tempBuildNames1.size() > query.size()) 
							&& !tempBuildNames2.equals(tempBuildNames1)) {
						double d = ctx.location == null ? 0 : MapUtils.getDistance(ctx.location, b.getLocation());
						// just for tests makes sense
						if (distPartial1 == 0 || d < distPartial1) {
							partial1 = b;
							distPartial1 = d;
						}
					} else {
						b.setId(DEFAULT_BLD_ID);
						// just for tests makes sense
						double d = ctx.location == null ? 0 : MapUtils.getDistance(ctx.location, b.getLocation());
						if (distExact == 0 || d < distExact) {
							exact = b;
							distExact = d;
						}
//						return b;
					}
				}
				if (bldSet.size() == query.size() + 1 || bldSet.size() == query.size() + 2) {
					// case data only 18-B present, 18 searched (151 + unit Upper level)
					if (bldSet.containsAll(query)) {
						double d = ctx.location == null ? 0 : MapUtils.getDistance(ctx.location, b.getLocation());
						// just for tests makes sense
						if (distPartial1 == 0 || d < distPartial1) {
							partial1 = b;
							distPartial1 = d;
						}
					}
				} else if (bldSet.size() + 1 == query.size()) {
					// case data only 18 present, 18-B searched 
					if (query.containsAll(bldSet)) {
						partial2 = b;
					}
				}
			}
		}
		if (exact != null) {
			return exact;
		}
		if (partial1 != null) {
			partial1.setId(DEFAULT_BLD_ID);
			if (tempBuildNames1.size() > query.size()) {
				partial1.setId(PARTIAL_ID_MATCH);
			}
			return partial1;
		}
		if (interpolation != null) {
			interpolation.setId(DEFAULT_BLD_ID);
			return interpolation;
		}
		if (partial2 != null) {
			partial2.setId(PARTIAL_ID_MATCH);
			return partial2;
		}
		return null;
	}

	private int getTokenByOriginalOrder(int originalOrder) {
		for(int ind = 0; ind < tokens.length; ind++) {
			if (tokens[ind].originalOrder == originalOrder) {
				return ind;
			}
		}
		return -1;
	}

	public SpatialSearchToken getFirstToken() {
		return tokens.length == 0 ? null : tokens[0];
	}
	
	public int nearbyResult(int ind) {
		int min = 0;
		for (int i = 0; i < tCount; i++) {
			NameIndexAtom ni = linearResults.get(ind * tCount + i);
			min = Math.max(ni.nearbyRadius, min);
		}
		return min;
	}
	
	public List<NameIndexAtom> getRawAtoms(int ind) {
		return linearResults.subList(ind * tCount, (ind + 1) * tCount);
	}
	
	public long getRawAtomsSumId(int ind) {
		long l = 0;
		for(NameIndexAtom a : linearResults.subList(ind * tCount, (ind + 1) * tCount)) {
			l += a.id; 
		}
		return l;
	}

	public int getCombinations() {
		return tileIds.size();
	}

	public int getTokenCount() {
		return tCount;
	}
	
	public List<SpatialSearchResult> getFinalResult() {
		return finalResult;
	}

	public List<SpatialSearchResult> sortResults(SpatialSearchContext ctx, boolean deduplicate) {
		finalResult = new ArrayList<>(tileIds.size());
		for (int i = 0; i < tileIds.size(); i++) {
			if (!skipResults.containsKey(i)) {
				finalResult.add(new SpatialSearchResult(this, i, preciseLocations.get(i)));
			}
		}		
		finalResult = sortResults(ctx, finalResult, deduplicate);
		return finalResult;
	}

	public List<SpatialSearchResult> sortResults(SpatialSearchContext ctx, List<SpatialSearchResult> finalResult, boolean deduplicate) {
		Collections.sort(finalResult, (o1, o2) -> SpatialSearchResult.compare(o1, o2, ctx.location));
		if (deduplicate) {
			uniqueIdsResults.clear();
			extraIdsResults.clear();
			List<SpatialSearchResult> result = new ArrayList<>();

			for (SpatialSearchResult s : finalResult) {
				boolean isUniq = true;
				long uniqueId = s.getIdDeduplication();
				if (uniqueIdsResults.containsKey(uniqueId)) {
					SpatialSearchResult unique = uniqueIdsResults.get(uniqueId);
					unique.addExtraResult(s, ctx.settings.LANG_DEDUPLICATE);
					isUniq = false;
				} else if (uniqueId != -1) {
					uniqueIdsResults.put(uniqueId, s);
				}
				List<String> extraDuplicateKeys = s.extraDeduplicateKeys();
				if (extraDuplicateKeys != null) {
					for (String key : extraDuplicateKeys) {
						if (extraIdsResults.containsKey(key)) {
							SpatialSearchResult unique = extraIdsResults.get(key);
							unique.addExtraResult(s, ctx.settings.LANG_DEDUPLICATE);
							isUniq = false;
						} else {
							extraIdsResults.put(key, s);
						}
					}
				}
				if (isUniq) {
					result.add(s);
				}
			}
			finalResult = result;
		}
		return finalResult;
	}
	
	
	@FunctionalInterface
	private interface IterateIntersection {

		void iterate(int parentIndx, NameIndexAtom atom, int atomIndex);

	}
	
	private void iterateIntersection(SpatialSearchResultsList parent, SpatialSearchToken token, IterateIntersection iterate) {
		// 1. iterate parent objects and find all objects from <parent>
		//    that are fully inside object <token> or have same the same tile
		for (int i = 0; i < parent.tileIds.size(); i++) {
			long tileId = parent.tileIds.get(i);
			int zoom = parent.tileZooms.get(i);
			final int indx = i;
			token.quadTree.forEachMatch(zoom, tileId, atoms -> {
				for (int ij = 0; ij < atoms.size(); ij++) {
					Integer indxAtom = atoms.get(ij);
					iterate.iterate(indx, token.atoms.get(indxAtom), indxAtom);
				}
			});
		}
		// 2. reverse search quad tree from <token> and find objects
		//    that are fully inside any object <parent> and not the same the same tile!
		for(int indxAtom = 0; indxAtom < token.atoms.size(); indxAtom++) {
			final int findxAtom = indxAtom;
			NameIndexAtom atom = token.atoms.get(indxAtom);
			parent.quadTree.forEachMatchHigherZoom(atom.coords.bboxTileZoom, atom.coords.bboxTileId, indxs -> {
				for (int indx : indxs) {
					iterate.iterate(indx, atom, findxAtom);
				}
			});
		}
	}
 	
	private void calculateMainIntersection(SpatialSearchContext ctx, SpatialSearchToken token, SpatialSearchResultsList parent) {
		if (parent.getTokenCount() == 0) {
			for(int indxAtom = 0; indxAtom < token.atoms.size(); indxAtom++) {
				NameIndexAtom atom = token.atoms.get(indxAtom);
				if (token.deletedAtoms.contains(indxAtom)) {
					continue;
				}
				addResult(null, 0, atom, 0);
			}
		} else if (parent.getCombinations() > 0) {
			long nt = System.nanoTime();
			TIntArrayList[] intersections = new TIntArrayList[limitIntersection + 1];
			TIntArrayList[] interPoiStreet = new TIntArrayList[limitIntersection + 1]; // type 1
			TIntArrayList[] interPoiPoiOrStreetStreet = new TIntArrayList[limitIntersection + 1]; // type 2
			for(int i = 0; i < intersections.length; i++) {
				intersections[i] = new TIntArrayList();
				interPoiPoiOrStreetStreet[i] = new TIntArrayList();
				interPoiStreet[i] = new TIntArrayList();
			}
			int originalLimit = limitIntersection;
			int[] typeIntersection = new int[] { 0 };
			iterateIntersection(parent, token, (parentIndx, atom,  indxAtom) -> {
				if (token.deletedAtoms.contains(indxAtom)) {
					return;
				}
				int level = Math.max(atom.nearbyRadius, parent.nearbyResult(parentIndx));
				if (level > limitIntersection) {
					return;
				}
				boolean acceptIntersection = acceptIntersection(ctx, parent, parentIndx, token, atom, typeIntersection);
//				long intId = atom.id + parent.getRawAtomsSumId(parentIndx);
//				System.out.println(intId + " " + atom + " " + parent.getRawAtoms(parentIndx) + " == " + acceptIntersection);
				if (acceptIntersection) {
					TIntArrayList c = intersections[level];
					if (typeIntersection[0] == 2) {
						c = interPoiPoiOrStreetStreet[level];
					} else if (typeIntersection[0] == 1) {
						c = interPoiStreet[level];
					}
					c.add(parentIndx);
					c.add(indxAtom);
					c.add(typeIntersection[0]);
				}
			});
			List<String> sizes = new ArrayList<String>();
			for (int k = 0; k < intersections.length; k++) {
				sizes.add(String.format("%,d/%,d/%,d", intersections[k].size() / 3,
						interPoiStreet[k].size() /3, interPoiPoiOrStreetStreet[k].size() /3));
			}
			int newLevel = 0;
			TIntArrayList res = new TIntArrayList();
			newLevel = addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, intersections, limitIntersection, res);
			addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, interPoiStreet, newLevel, res);
			addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, interPoiPoiOrStreetStreet, newLevel, res);
			
			if (ctx.stats.printLogs) {
				System.out.printf("Intersect (%.0f ms) /\\: %s (%d) -> %,d (%,d): '%s' (%,d) + %s (%,d)\n", 
					(System.nanoTime() - nt) / 1e6, sizes, originalLimit, res.size() / 3, newLevel,  
					token.originalWord, token.atoms.size(),
					parent.wordTokens(), parent.getCombinations());
			}
			limitIntersection = newLevel;
			TIntIterator it = res.iterator();
			while (it.hasNext()) {
				int parentIndx = it.next();
				int indxAtom = it.next();
				int type = it.next();
//				System.out.println(token.atoms.get(indxAtom) + " " + parent.getRawAtoms(parentIndx) + " ");
				addResult(parent, parentIndx, token.atoms.get(indxAtom), type);
			}
			
			
		}		
	}

	private int addResIntersections(int limit, TIntArrayList[] intersections, int maxLevel, TIntArrayList res) {
		int newLevel = 0;
		for (int level = 0; level <= maxLevel; level++) {
			TIntArrayList toAdd = intersections[level];
			if (res.size() == 0 || (level == 0 && maxLevel > 0) || res.size() + toAdd.size() < limit) {
				res.addAll(toAdd);
				newLevel = level;
			} else {
				break;
			}
		}
		return newLevel;
	}


	public int sumTokenAtomSize() {
		int sum = 0;
		for (SpatialSearchToken s : tokens) {
			sum += s.atoms.size();
		}
		return sum;
	}

	boolean addResult(SpatialSearchResultsList parent, int pindx, NameIndexAtom a, int typeIntersection) {
		finalResult = null;
		int pzoom = parent == null ? 0 : parent.tileZooms.get(pindx);
		int zoom = Math.max(pzoom, a.coords.bboxTileZoom);
		long tileId = pzoom > a.coords.bboxTileZoom ? parent.tileIds.get(pindx) : a.coords.bboxTileId;
		int insIndx = this.tileIds.size();
		this.linearResults.add(a);
		for (int i = 0; parent != null && i < parent.tCount; i++) {
			this.linearResults.add(parent.linearResults.get(pindx * parent.tCount + i));
		}
		this.typeIntersections.add(typeIntersection);
		this.tileIds.add(tileId);
		this.tileZooms.add(zoom);
		quadTree.put(zoom, tileId, insIndx);
		return true;
	}

	private boolean acceptIntersection(SpatialSearchContext ctx,  SpatialSearchResultsList parent, int pindx, SpatialSearchToken token, NameIndexAtom a,
			int[] typeIntersection) {
		SpatialTextSearchSettings settings = ctx.settings;
		typeIntersection[0] = -1;
		// 1. speed up for same id
		// Don't allow intersect potential building with other object
		for (int i = 0; parent != null && i < parent.tCount; i++) {
			NameIndexAtom pa = parent.linearResults.get(pindx * parent.tCount + i);
			if (pa.id == a.id) {
				typeIntersection[0] = parent.typeIntersections.get(pindx);
				continue;
			}
			// pa and a using same tokens for street & house but different streets / poi same as below
			if (parent.tokens[i].originalOrder == a.buildingInd) {
				return false;
			} else if (pa.buildingInd == token.originalOrder) {
				return false;
			}
			// don't intersect building with other street except if street-building is city
			if ((pa.buildingInd >= 0) && a.isStreetBuilding() && !pa.isCityStreetName()) {
				return false;
			} else if ((a.buildingInd >= 0) && pa.isStreetBuilding() && !a.isCityStreetName()) {
				return false; 
			}
			// if poi doesn't have bbox don't intersect or add bbox! (transport stops take street names)
			if (!ctx.settings.ALLOW_HOUSE_POI_TYPE_INTERSECTION) {
				if ((pa.buildingInd >= 0) && a.isPOI() && a.coords.bbox31 == null) {
					return false;
				} else if ((a.buildingInd >= 0) && pa.isPOI() && pa.coords.bbox31 == null) {
					return false;
				}
			}
//			
		}
		// 2. Duplicate words in query
		for (int i = 0; parent != null && i < parent.tCount; i++) {
			NameIndexAtom pa = parent.linearResults.get(pindx * parent.tCount + i);
			if (pa.id == a.id && tokens[0].word.equals(tokens[i + 1].word) && 
					!pa.isBuilding() && !a.isBuilding()) {
				// NameIndexAtom supports "<word> <...> <word>" but it's not present in DATA now
				int indexOf = a.name.indexOf(tokens[0].word, pindx);
				if (indexOf != -1 && a.name.indexOf(tokens[0].word, indexOf + 1) >= 0) {
					// duplicate name in object
//					System.out.println("Duplicate word  " + a.name);
				} else {
					return false;
				}
			}
		}
		// speed up for same id checks
		if (typeIntersection[0] >= 0) {
			return true;
		}
		
		typeIntersection[0] = 0;
		// 3. Precise intersection
		// no cache for parent now needed
		boolean intersect = true;
		for (int i = 0; parent != null && i < parent.tCount; i++) {
			NameIndexAtom pa = parent.linearResults.get(pindx * parent.tCount + i);
			if (!pa.coords.intersects(a.coords)) {
				intersect = false;
				break;
			}
		}
		if (!intersect) {
			return false;
		}
		// 4. New intersection check the limits
		HashMap<Long, NameIndexAtom> atomObjs = new HashMap<>(4);
		if (a.atomicObject()) {
			atomObjs.put(a.id, a);
		}
		NameIndexAtom poiType = a.isPoiCategory() ? a : null;
		SpatialSearchToken poiTypeToken = tokens[0];
		for (int i = 0; parent != null && i < parent.tCount; i++) {
			NameIndexAtom pa = parent.linearResults.get(pindx * parent.tCount + i);
			if (pa.id == a.id) {
				continue;
			}
			if (pa.isPoiCategory()) {
				if (poiType == null || pa.id == a.id) {
					poiType = pa;
					poiTypeToken = parent.tokens[i];
				} else {
					// no 2 poi categories
					return false;
				}
			}
			// check that token is reused in parent
			// ignore every object that has this name already (except duplicate words && numbers assigned to building)
			if (!tokens[0].word.equals(parent.tokens[i].word)) {
				NameIndexAtom existing = parent.tokens[i].index.get(a.id);
				if (existing != null && !existing.isBuilding()) {
					return false;
				}
				existing = tokens[0].index.get(pa.id);
				if (existing != null && !existing.isBuilding()) {
					return false;
				}
			}
			if (pa.atomicObject()) {
				atomObjs.put(pa.id, pa);
			}
			if (atomObjs.size() > settings.LIMIT_ATOMIC_OBJECTS) {
				return false;
			}
			// allow intersection for Buildings associated with place
			// Don't intersect <City Street> ('<Salt Lake City>') with Street ('Pennsylvania street')
//			if ((a.isCityStreetName() && pa.id != a.id) || (pa.isCityStreetName() && a.id != pa.id)) {
//				return false;
//			}
		}
		// investigate to find types
		if (poiType != null && atomObjs.size() > 0) {
			NameIndexAtom p = atomObjs.values().iterator().next();
			if (poiTypeToken.incomplete) {
				return false;
			}
			if (!p.isPOI()) {
				return false;
			}
		}
		if (atomObjs.size() > 1) {
			Iterator<NameIndexAtom> it = atomObjs.values().iterator();
			NameIndexAtom a1 = it.next();
			NameIndexAtom a2 = it.next();
			// if one object is same as city name don't intersect with street poi...
			if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) {
				return false;
			}
			if ((a1.isStreetBuilding() != a2.isStreetBuilding()) && !it.hasNext()) {
				typeIntersection[0] = 1;
			} else {
				typeIntersection[0] = 2;
				boolean twoStreets = a1.isStreetBuilding() && a2.isStreetBuilding();
				boolean twoPOIs = !a1.isStreetBuilding() && !a2.isStreetBuilding();
				if (!settings.SEARCH_STREET_INTERSECTIONS && twoStreets) {
					return false;
				} else if (!settings.SEARCH_POI_INTERSECTIONS && twoPOIs) {
					return false;
				}
			}
		}
		
		
		return true;
	}

	@Override
	public int compareTo(SpatialSearchResultsList o) {
		int s1 = tCount;
		int s2 = o.tCount;
		if (s1 != s2) {
			return -Integer.compare(s1, s2);
		}
		s1 = sumTokenAtomSize();
		s2 = o.sumTokenAtomSize();
		return Integer.compare(s1, s2);
	}

	public String toString(boolean extended) {
		List<String> words = wordTokens();
		return String.format("Results %d tokens %,d%s - %s %s", tCount, getCombinations(),
				finalResult == null ? "" : String.format(" (%,d unique)", finalResult.size()), 
				words, !extended ? "" : (" results: " + linearResults));
	}

	public List<String> wordTokens() {
		List<String> words = new ArrayList<>();
		for (SpatialSearchToken t : tokens) {
			words.add(t.originalWord);
		}
		return words;
	}
	
	

	@Override
	public String toString() {
		return toString(false);
	}

	

	
}
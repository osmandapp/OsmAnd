package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.protobuf.ByteString;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.ObfConstants;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.Building;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchToken {
	public static final int ALL_CITY_TYPE = -10;
	
	public static final int POI_CATEGORY_TYPE = -5;
	public static final int POI_REF_TYPE = -3;
	public static final int BUILDING_TYPE = -2;
	public static final int POI_TYPE = -1;
	public static final int STREET_TYPE = CityBlocks.STREET_TYPE.index;
	public static final String DOT_INCOMPLETE_STRING = CollatorStringMatcher.INCOMPLETE_DOT + "";

	int MIN_CHAR_INCOMPLETE;
	
	int originalOrder = 0;
	int sortedOrder = 0;
	
	boolean incomplete;
	String originalWord;
	String word;
	String wordAligned;
	String wordNoDot;
	Set<String> bldWordSplit;
	
	Set<String> poiCategoryKeysToAutocomplete = new HashSet<>();
	Set<Integer> poiCategoryIds = new HashSet<>();
	List<NameIndexAtom> atoms = new ArrayList<>();
	TLongObjectHashMap<NameIndexAtom> index = new TLongObjectHashMap<>();
	HashQuadTree<Integer> quadTree = new HashQuadTree<>(16);
	TLongObjectHashMap<NameIndexAtom> indexByOsmIds = new TLongObjectHashMap<>();
	Set<Integer> deletedAtoms = new HashSet<Integer>();
	
	// partial place holder
	List<PartialMatch> partialExactMatch = new ArrayList<>();
	List<PartialMatch> partialMatch = new ArrayList<>();

	CollatorStringMatcher collatorMain;
	CollatorStringMatcher noDotCollatorMain;
	CollatorStringMatcher noHyphenCollatorMain;
	// cache for popular split
	String wordSpacePrefixCache;
	CollatorStringMatcher wordSpaceCollatorSuffix;
	
	int mainNumber = -1;
	CollatorStringMatcher[] otherMatch;
	
	public record PartialMatch(NameIndexAtom atom, List<SpatialSearchToken> other, boolean nonNumericMatch) {
		
	}


	public SpatialSearchToken(int MIN_CHAR_INCOMPLETE, String ow, String original, int order) {
		this.MIN_CHAR_INCOMPLETE = MIN_CHAR_INCOMPLETE;
		originalWord = original;
		word = ow;
		wordAligned = SearchAlgorithms.alignChars(word);
		bldWordSplit = SearchAlgorithms.getBuildingCompareSet(word, null);
		originalOrder = order;
		String noDot = wordAligned;
		if (wordAligned.endsWith(DOT_INCOMPLETE_STRING)) {
			incomplete = true;
			noDot = wordAligned.substring(0, wordAligned.length() - 1);
		}
		this.wordNoDot = noDot;
		// . already in collator w.endsWith(DOT_INCOMPLETE_STRING)
		collatorMain = new CollatorStringMatcher(wordAligned, StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
		if (incomplete && word.length() <= MIN_CHAR_INCOMPLETE + 1) {
			noDotCollatorMain = new CollatorStringMatcher(noDot, StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
		} else {
			if (SearchAlgorithms.letters(noDot) == 0) {
				// pos case '4', '#4' query should match 4th, wrong case token '4' should not match '48th'
				// we use number to compare if we use is isNumber2Letters to many weird results on '2B'
				mainNumber = Algorithms.extractFirstIntegerNumber(noDot);
			}
		}
		if (wordAligned.indexOf('-') != -1) {
			// PA-21
			noHyphenCollatorMain = new CollatorStringMatcher(wordAligned.replace("-", ""), StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
		}
		String abbr = Abbreviations.getSearchabbreviations().get(noDot);
		if (abbr != null) {
			List<String> other = SearchAlgorithms.splitAndNormalize(abbr, true);
			otherMatch = new CollatorStringMatcher[other.size()];
			for(int i = 0; i < other.size(); i++) {
				otherMatch[i] = new CollatorStringMatcher(other.get(i), StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
			}
		}
	}
	
	public int getMainNumber() {
		return mainNumber;
	}
	
	public boolean likelyPartOfBuilding() {
		return Abbreviations.likelyPartOfBuilding(word, bldWordSplit);
	}
	
	public boolean likelyRef() {
		return Abbreviations.likelyPartOfRef(word, bldWordSplit);
	}

	public CollatorStringMatcher getMainCollator() {
		return collatorMain;
	}
	
	public boolean isOnlyFullMatch() {
		return incomplete && word.length() <= MIN_CHAR_INCOMPLETE + 1;
	}

	@Override
	public String toString() {
		return String.format("%d. %s - %d atoms", sortedOrder, originalWord, atoms.size());
	}
	
	
	NameIndexReaderMatcher getPrefixMatcher(SpatialSearchStats stats) {
		return new NameIndexReaderMatcher(word) {
			
			@Override
			public boolean matchKey(String key) {
				stats.sub1MatchTime.start();
				if (key.startsWith(NameIndexReader.POI_CATEGORY_PREFIX) && poiCategoryKeysToAutocomplete.size() > 0) {
					for (String poiCatKey : poiCategoryKeysToAutocomplete) {
						if (poiCatKey.startsWith(key.substring(NameIndexReader.POI_CATEGORY_PREFIX.length()))) {
							stats.sub1MatchTime.finish();
							return true;
						}
					}
				}
				
				String alignedKey = SearchAlgorithms.alignChars(key);
				// could be empty after align so match = true! ("''" -> "")
				boolean matched = matchAlignedKey(alignedKey);
				if (!matched && mainNumber > 0) {
					// 4th - key, "4" token
					matched = Algorithms.extractFirstIntegerNumber(key) == mainNumber;
				}
				if (!matched && otherMatch != null) {
					for (CollatorStringMatcher o : otherMatch) {
						matched |= CollatorStringMatcher.cmatches(collator, o.getPart(), alignedKey,
								StringMatcherMode.CHECK_ONLY_STARTS_WITH);
						// o.matches(alignedKey) could be needed for matching data with non-processed abbrevations
//						System.out.println(alignedKey + " ??? " + matched + " " + o.getPart());
					}
				}
				if (!matched && key.startsWith(wordNoDot)
						&& SearchAlgorithms.letters(key) == SearchAlgorithms.letters(wordNoDot)) {
					// query 'pa 21' match 'pa21' key
					matched = true;
				}
				
				stats.sub1MatchTime.finish();
				return matched;
			}
		};
	}

	
	NameIndexAtom getAtomToken(NameIndexAtom atom) {
		return index.get(atom.id);
	}
	
	void removeAtom(NameIndexAtom atom) {
		NameIndexAtom na = index.get(atom.id);
		deletedAtoms.add(na.indexInToken);
	}

	void enlargeBbox(NameIndexAtom atom, double mult) {
		NameIndexAtom na = index.get(atom.id);
		quadTree.delete(atom.coords.bboxTileZoom, atom.coords.bboxTileId, na.indexInToken);
		atom.coords.enlargeBbox31(mult);
		quadTree.put(atom.coords.bboxTileZoom, atom.coords.bboxTileId, na.indexInToken);
	}
	
	void addPoiCategoryMatch(int id) {
		poiCategoryIds.add(id);
	}
	
	boolean addAtom(NameIndexAtom atom) {
		if (atom.isPoiCategory()) {
			poiCategoryKeysToAutocomplete.add(atom.name);
			poiCategoryIds.add((int) atom.id);
		}
		if (atom.object != null && !(atom.object instanceof Street) && 
				atom.object.getId() != null &&  atom.object.getId() > 0) {
			// mostly not used as disabled in settings for speed up
			long osmId = ObfConstants.getOsmIdFromMapObjectId(atom.object.getId());
			NameIndexAtom ex = indexByOsmIds.get(osmId);
			if (ex != null) {
				return false;
			}
			indexByOsmIds.put(osmId, atom);
		}

		NameIndexAtom existing = index.get(atom.id);
		if (existing != null) {
			if (existing != atom) {
				// compare convention like method important!
				// select shortest available version
				int res = Integer.compare(atom.otherWordsCnt + atom.otherFoundCnt,
						existing.otherWordsCnt + existing.otherFoundCnt);
				// '2 south 2nd street' vs '25 садова вулиця' (25-та) -
				// don't use it for now as it replaces building link 
				// (if it stops working -then analyse should be done in checkBuilding and find duplicate assigned word) 
//				res = Boolean.compare(atom.isBuilding(), existing.isBuilding());
				boolean replace = res < 0;
				if (replace) {
					atom.indexInToken = existing.indexInToken;
					index.put(atom.id, atom);
					atoms.set(atom.indexInToken, atom);
				}
				return true;
			}
			return false;
		}
		index.put(atom.id, atom);
		atoms.add(atom);
		int indx = atoms.size() - 1;
		atom.indexInToken = indx;
		quadTree.put(atom.coords.bboxTileZoom, atom.coords.bboxTileId, indx);
		return true;
	}

	boolean matchName(String name, TIntArrayList poiTypes) {
//		System.out.printf("query '%s' matches '%s' %s\n", word, name, collatorMain.matches(name) || 
//				collatorMain.matches(name.replace(' ', '-')));
		if (name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			return poiTypes != null && matchPoiCategoryKeys(poiTypes);
		}
		if (mainNumber > 0) {
			if (mainNumber == Algorithms.extractFirstIntegerNumber(name)) {
				return true;
			}
		}
		if (otherMatch != null) {
			for (CollatorStringMatcher o : otherMatch) {
				if (o.matches(name)) {
					return true;
				}
			}
		}
		if ((noDotCollatorMain == null ? collatorMain : noDotCollatorMain).matches(name)) {
			return true;
		}
		if (noHyphenCollatorMain != null && noHyphenCollatorMain.matches(name)) {
			return true;
		}
		return false;
	}
	
	public List<PartialMatch> getPartialExactMatch() {
		return partialExactMatch;
	}
	
	public List<PartialMatch> getPartialMatch() {
		return partialMatch;
	}
	
	public void clearPartialAtoms() {
		partialExactMatch.clear();
		partialMatch.clear();
	}
	
	public boolean hasPoiCategoryKeys() {
		return !poiCategoryKeysToAutocomplete.isEmpty();
	}
	
	public boolean matchPoiCategoryKeys(TIntArrayList poiTypes) {
		for (int k = 0; k < poiTypes.size(); k++) {
			if (poiCategoryIds.contains(poiTypes.getQuick(k))) {
				return true;
			}
		}
		return false;
	}
	
	public void addPartialCommonAtom(NameIndexAtom atom, List<SpatialSearchToken> otherTokens, boolean numericNotMatch) {
		partialExactMatch.add(new PartialMatch(atom, otherTokens, numericNotMatch));
	}
	
	public void addPartialOtherAtom(NameIndexAtom atom, List<SpatialSearchToken> otherTokens, boolean numericNotMatch) {
		partialMatch.add(new PartialMatch(atom, otherTokens, numericNotMatch));
	}
	
	String[] matchSplitName(String name) {
		name = SearchAlgorithms.alignChars(name);
		String[] res = null;
		if (wordAligned.length() < name.length()
				&& collatorMain.getCollator().equals(name.substring(0, wordAligned.length()), wordAligned)) {
			res = new String[2];
			res[0] = name.substring(0, wordAligned.length());
			res[1] = name.substring(wordAligned.length());
			while (res[1].length() > 0 && !Character.isLetter(res[1].charAt(0))
					&& !Character.isDigit(res[1].charAt(0))) {
				res[1] = res[1].substring(1);
			}
		}
		return res;
	}

	public static class NameIndexAtomXY {
		// SHOULD BE NOT MODIFIABLE AS WE INTERSECT OBJECTS atom x atom
		int[] bbox31; // if exists [xleft, yleft, xright, yright]
		long bboxTileId; // encodes zoom, tileX, tileY
		int bboxTileZoom;
		int x16, y16;
		
		public NameIndexAtomXY(AddressNameIndexDataAtom a, OsmAndPoiNameIndexDataAtom b, 
				SpatialTextSearchSettings settings) {
			if (a != null) {
				init(a, settings);
			} else if(b != null){
				init(b);
			} else {
				// full world
				bboxTileZoom = 0;
				bboxTileId = 0;
			}
		}

		public boolean intersects(NameIndexAtomXY a) {
			if (bbox31 == null && a.bbox31 == null) {
				int z1 = bboxTileZoom, z2 = a.bboxTileZoom;
				long tid1 = bboxTileId, tid2 = a.bboxTileId;
				while (z1 > z2) {
					tid1 >>= 2;
					z1--;
				}
				while (z2 > z1) {
					tid2 >>= 2;
					z2--;
				}
				return tid1 == tid2;
			} else if (a.bbox31 == null) {
				return a.intersects(this);
			} else {
				return intersects(a.bbox31);
			}
		}
		
		public boolean intersects(int[] abbox31) {
			// if exists [xleft, ytop, xright, ybottom]
			if (this.bbox31 == null) {
				int xleft = abbox31[0] >> (31 - this.bboxTileZoom);
				int xright = abbox31[2] >> (31 - this.bboxTileZoom);
				int ytop = abbox31[1] >> (31 - this.bboxTileZoom);
				int ybottom = abbox31[3] >> (31 - this.bboxTileZoom);
				long x = MapUtils.deinterleaveX(this.bboxTileId);
				long y = MapUtils.deinterleaveY(this.bboxTileId);
				return xleft <= x && x <= xright && ytop <= y && y <= ybottom;
			} else {
				// if exists [xleft, ytop, xright, ybottom]
				return this.bbox31[0] <= abbox31[2] && this.bbox31[2] >= abbox31[0] && this.bbox31[1] <= abbox31[3]
						&& this.bbox31[3] >= abbox31[1];
			}
		}
		
		public boolean contains(NameIndexAtomXY a) {
			if (bbox31 == null || a.bbox31 == null) {
				int z1 = bboxTileZoom, z2 = a.bboxTileZoom;
				long tid1 = bboxTileId, tid2 = a.bboxTileId;
				while (z2 > z1) {
					tid2 >>= 2;
					z2--;
				}
				return tid1 == tid2 && z2 == z1;
			}
			// if exists [xleft, ytop, xright, ybottom]
			return this.bbox31[0] <= a.bbox31[0] && this.bbox31[2] >= a.bbox31[2] && this.bbox31[1] <= a.bbox31[1]
					&& this.bbox31[3] >= a.bbox31[3];
		}
		
		public String tileIdString() {
			return this.bboxTileZoom + " "
					+ MapUtils.deinterleaveX(bboxTileId) + " "
					+ MapUtils.deinterleaveY(bboxTileId);
		}

		private void init(AddressNameIndexDataAtom addr, SpatialTextSearchSettings settings) {
			if (addr.getXy16Count() >= 1) {
				int xy16 = addr.getXy16(0);
				this.x16 = (xy16 >>> 16);
				this.y16 = (xy16 & ((1 << 16) - 1));
				decodeBBox(addr.hasBbox() ? addr.getBbox() : null);
				if (bbox31 == null) {
					// not needed as we calculate on server for all cities
//					if (addr.getType() != CityBlocks.STREET_TYPE.index) {
//						// possibly needs to be calculated on server
//						int shift = (1 << (16 - 12)); // extend 12th tile
//						bbox31 = new int[4];
//						bbox31[0] = (x16 - shift) << 15;
//						bbox31[2] = (x16 + shift) << 15;
//						bbox31[1] = (y16 - shift) << 15;
//						bbox31[3] = (y16 + shift) << 15;
//						calcTileFromBbox();
//					} else {
						bboxTileZoom = 15;
						bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16 / 2, y16 / 2);
//					}
				}
			}
		}
		

		private void decodeBBox(ByteString bbox) {
			if (bbox != null) {
				bbox31 = SearchAlgorithms.decodeBboxForNameAtomsBytes(bbox, x16, y16);
				calcTileFromBbox();
			}
		}

		private void calcTileFromBbox() {
			if (bbox31 != null) {
				int z = 31;
				// for 180 lat check max  
				int xleft = bbox31[0], xright = Math.max(bbox31[2], bbox31[0]);
				int ytop = bbox31[1], ybottom = Math.max(bbox31[3], bbox31[1]);
				while (xleft != xright || ytop != ybottom) {
					z--;
					xleft >>= 1;
					xright >>= 1;
					ytop >>= 1;
					ybottom >>= 1;
				}
				bboxTileZoom = z;
				bboxTileId = HashQuadTree.encodeTileId(z, xleft, ytop);
			}
		}

		private void init(OsmAndPoiNameIndexDataAtom poi) {
			this.x16 = poi.getX();
			this.y16 = poi.getY();
			bboxTileZoom = 16;
			bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16, y16);
			decodeBBox(poi.hasBbox() ? poi.getBbox() : null);
		}
		
		public void enlargeBbox31(double mult) {
			if (mult == 0) {
				return;
			}
			if (bbox31 != null) {
				int w = (int) ((bbox31[2] - bbox31[0]) * mult), h = (int) ((bbox31[3] - bbox31[1]) * mult);
				bbox31[0] = Math.max(Math.min(bbox31[0], bbox31[0] - w), 0); // xleft
				bbox31[2] = Math.max(bbox31[2] + w, bbox31[0]); // xright
				bbox31[1] = Math.max(Math.min(bbox31[1], bbox31[1] - h), 0); // ytop
				bbox31[3] = Math.max(bbox31[3] + h, bbox31[1]); // ybottom
			} else {
				int w = (int) Math.ceil(mult);
				bbox31 = new int[4];
				bbox31[0] = Math.max((x16 - w) << 15, 0); // xleft
				bbox31[2] = Math.max((x16 + w) << 15, bbox31[0]); // xright
				bbox31[1] = Math.max((y16 - w) << 15, 0); // ytop
				bbox31[3] = Math.max((y16 + w) << 15, bbox31[1]); // ybottom
			}
			calcTileFromBbox();
		}

		public double dimensionInM() {
			int xleft = x16 << 15, xright = (x16 + 1) << 15;
			int ytop = y16 << 15, ybottom = (y16 + 1) << 15;
			if (bbox31 != null) {
				xleft = bbox31[0];
				xright = bbox31[2];
				ytop = bbox31[1];
				ybottom = bbox31[3];
			}
			return MapUtils.getDistance(MapUtils.get31LatitudeY(ytop), MapUtils.get31LongitudeX(xleft),
					MapUtils.get31LatitudeY(Math.max(ytop, ybottom)),
					MapUtils.get31LongitudeX(Math.max(xleft, xright)));
		}

		

	}

	
	public static class NameIndexAtom {
		// SHOULD BE NOT MODIFIABLE AS WE INTERSECT OBJECTS atom x atom
		final String name;

		final int type; //
		final long id; // used to read object
		final long parentid; // used to read object
		
		MapObject object; // same for all
		MapObject bldObject; // same for all
		
		int otherWordsCnt; // added before intersection
		int otherFoundCnt;
		
		int indexInToken;
		final boolean cityAsStreet;
		final NameIndexAtomXY coords; 
		final int buildingOrRefInd; // added before intersection
		final int nearbyRadius;
		TIntArrayList poiTypes;
		int elo;
		NameIndexAtom sameNameAreaObj;

		NameIndexAtom(String name, long id, int total) {
			this(name, SpatialSearchToken.POI_CATEGORY_TYPE, id, 0, null, false, -total, total,
					new NameIndexAtomXY(null, null, null), 0, -1);
		}
		
		NameIndexAtom(NameIndexAtom cp) {
			this(cp.name, cp.type, cp.id, cp.parentid, cp.object, cp.cityAsStreet, cp.otherWordsCnt, cp.otherFoundCnt,
					cp.coords, cp.nearbyRadius, cp.buildingOrRefInd);
			this.poiTypes = cp.poiTypes;
		}

		NameIndexAtom(String name, int type, long id, long pid, MapObject obj, boolean cityAsStreet, int otherWordsCnt,
				int otherFoundCnt, NameIndexAtomXY coords, int nearbyRadius, int buildingInd) {
			this.name = name;
			this.id = id;
			this.parentid = pid;
			this.object = obj;
			this.type = type;
			this.cityAsStreet = cityAsStreet;
			this.otherWordsCnt = otherWordsCnt;
			this.otherFoundCnt = otherFoundCnt;
			this.coords = coords;
			this.nearbyRadius = nearbyRadius;
			this.buildingOrRefInd = buildingInd;
		}
		
		
		public boolean isCityStreetName() {
			return cityAsStreet;
		}
		
		public boolean isStreetBuilding() {
			return type == STREET_TYPE || type == BUILDING_TYPE;
		}
		
		public boolean isPoiCategory() {
			return type == POI_CATEGORY_TYPE;
		}
		
		public boolean isPostcode() {
			return type == CityBlocks.POSTCODES_TYPE.index;
		}
		
		public boolean isBoundary() {
			return type == CityBlocks.BOUNDARY_TYPE.index;
		}
		
		public boolean isCityVillage() {
			return type == CityBlocks.CITY_TOWN_TYPE.index || type == CityBlocks.VILLAGES_TYPE.index;
		}
		
		public boolean isCity() {
			return type == CityBlocks.CITY_TOWN_TYPE.index;
		}
		
		public boolean isStreet() {
			return type == STREET_TYPE ;
		}
		
		public boolean atomicObject() {
			return type == STREET_TYPE || type == POI_TYPE || type == BUILDING_TYPE || type == POI_REF_TYPE;
		}
		
		public boolean isBuilding() {
			return type == BUILDING_TYPE || (type == STREET_TYPE && bldObject != null);
		}
		
		public boolean isPOI() {
			return type == POI_TYPE || type == POI_REF_TYPE;
		}
		
		public boolean isPOIRef() {
			return type == POI_REF_TYPE;
		}

		public String typeStr() {
			String typeS = "";
			if (isPoiCategory()) {
				typeS = "POI_TYPE";
			} else if (isPOI()) {
				typeS = "POI";
			} else if (isBuilding()) {
				typeS = "Building";
			} else {
				typeS = CityBlocks.getByType(type).toString();
			}
			return typeS;
		}

		String simpleName(String name) {
			return String.format("%s %s %d (%.4f, %.4f)", typeStr(), name, (id % 0xffff),
					MapUtils.get31LatitudeY(coords.y16 << 15), MapUtils.get31LongitudeX(coords.x16 << 15));
		}
		
		public LatLon getResultLocation() {
			if (bldObject != null) {
				return bldObject.getLocation();
			}
			if (object != null) {
				return object.getLocation();
			}
			return new LatLon(MapUtils.get31LatitudeY(coords.y16 << 15), MapUtils.get31LongitudeX(coords.x16 << 15));
		}


		@Override
		public final String toString() {
			return object != null ? object.toString() : simpleName(name);
		}

		public String getName() {
			return name;
		}
		
		public MapObject getObject() {
			return object;
		}
		
		public Building getBuilding() {
			if (bldObject instanceof Building b) {
				return b;
			}
			return null;
		}

	}


	


}
package net.osmand.search.core.spatial;

import java.util.*;

import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.spatial.SpatialPoiSearch.SpatialPoiType;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchResult implements Comparable<SpatialSearchResult> {

	final int parentInd;
	final SpatialSearchResultsList parent;
	final List<SpatialSearchResultRef> objs = new ArrayList<>();
	final LatLon preciseLatlon;
	final String extraNameMatch; // refs and interpolation
	final int surplusWords; // negative some building numbers not found, positive some extra tokens matched
	int visibleLevel;
	public MapObject unitedObject;
	int biggestCityType = -1;

	private static final List<String> FILTER_DUPLICATE_POI_SUBTYPE = new ArrayList<String>(
			Arrays.asList("building", "internet_access_yes"));
	final int ZOOM_SIMILARITY_70_KM = 9 - 8; // 1 symbol - tile z=9 - 1 pixel of z=1
	final int ZOOM_SIMILARITY_10_KM = 12 - 8; // 2 symbols - tile z=12
	final int ZOOM_SIMILARITY_1_KM = 15 - 8; // 3 symbols
	
	SpatialSearchResult(SpatialSearchResultsList parentList, int parentInd, LatLon preciseLatlon, String extraName,
			Integer surplusWords) {
		this.parentInd = parentInd;
		this.parent = parentList;
		this.preciseLatlon = preciseLatlon;
		this.extraNameMatch = extraName;
		for (int i = 0; i < parent.tCount; i++) {
			NameIndexAtom atom = parent.linearResults.get(parentInd * parentList.tCount + i);
//			if (atom.matchExtraWord != 0) {
//				surplusWords += atom.matchExtraWord;
//			}
			SpatialSearchToken token = parent.tokens[i];
			SpatialSearchResultRef ref = null;
			// find same object or object & parent 
			for (SpatialSearchResultRef existing : objs) {
				if (atom.id == existing.atom.id) {
					ref = existing;
					// building-street
					if (existing.atom.type > atom.type) {
						// existing street - swap
						existing.atom = atom;
						break;
					}
				}
			}
			if (ref == null) {
				ref = new SpatialSearchResultRef(atom);
				objs.add(ref);
			}
			ref.tokens.add(token);
		}
		this.surplusWords = surplusWords != null ? surplusWords : 0;
		sortObjects();
	}
	
	void sortObjects() {
		for (SpatialSearchResultRef r : objs) {
			Collections.sort(r.tokens, (o1, o2) -> Integer.compare(o1.originalOrder, o2.originalOrder));
		}
		Collections.sort(objs, (o1, o2) -> {
			int r = Integer.compare(o1.typeOrder(SpatialSearchResultRef.MAX_TYPE_ORDER), o2.typeOrder(SpatialSearchResultRef.MAX_TYPE_ORDER));
			if (r != 0) {
				return r;
			}
			return Integer.compare(o1.tokens.get(0).originalOrder, o2.tokens.get(0).originalOrder);
		});
	}

	public SpatialSearchResultRef getFirstRef() {
		if (objs.size() > 0) {
			return objs.get(0);
		}
		return null;
	}
	
	public MapObject getFirstObject() {
		if (objs.size() > 0) {
			SpatialSearchResultRef o = objs.get(0);
			if (o.atom.bldObject != null) {
				return o.atom.bldObject;
			}
			return o.atom.object;
		}
		return null;
	}
	
	public List<MapObject> getAllObjects() {
		if (objs.isEmpty()) {
			return new ArrayList<>();
		}
		List<MapObject> result = new ArrayList<>();
		for (SpatialSearchResultRef ref : objs) {
			if (ref.atom.bldObject != null) {
				result.add(ref.atom.bldObject);
			} 
			if (ref.atom.object != null) {
				result.add(ref.atom.object);
			}
		}
		return result;
	}
	
	public List<MapObject> getObjects() {
		List<MapObject> o = new ArrayList<>();
		for (SpatialSearchResultRef r : objs) {
			if (r.atom.bldObject != null) {
				o.add(r.atom.bldObject);
			}
			if (r.atom.object != null && !o.contains(r.atom.object)) {
				o.add(r.atom.object);
			}
		}
		return o;
	}

	public boolean hasPoiTypes() {
		return objs.stream().anyMatch(r -> r.atom.isPoiCategory());
	}

	public List<SpatialPoiType> getPoiTypes(SpatialPoiSearch poiSearch) {
		List<SpatialPoiType> types = new ArrayList<>();
		for (SpatialSearchResultRef r : objs) {
			if (r.atom.isPoiCategory()) {
				SpatialPoiType type = poiSearch.getById((int)r.atom.id);
				if (type != null) {
					types.add(type);
				}
			}
		}
		return types;
	}
	
	public String getExtraNameMatch() {
		return extraNameMatch;
	}

	public LatLon getLatLon() {
		if (preciseLatlon != null) {
			return preciseLatlon;
		}
		for (SpatialSearchResultRef r : objs) {
			if (!r.atom.isPoiCategory()) {
				return r.atom.getResultLocation();
			}
		}
		return null;
	}

	public int visibleLevel() {
		return visibleLevel;
	}

	public List<String> extraDeduplicateKeys(SpatialSearchContext ctx) {
		List<String> result = null;
		result = addResult(result, getWikidata());
		result = addResult(result, getRouteId());
		MapObject mapObject = getFirstObject();		
		if (mapObject instanceof Amenity amenity) {
			if (amenity.getType().getKeyName().equals("natural")) {
				String name = SearchAlgorithms.normalizeToken(SearchAlgorithms.alignChars(amenity.getName()));
				String link = getShortLink(ZOOM_SIMILARITY_10_KM);
				if (name != null && link != null) {
					result = addResult(result, name + "_" + link);
				}
			}
		}
		if (getFirstRef().atom.isPoiCategory()) {
			SpatialPoiType type = ctx.poiSearch.getById((int) getFirstRef().atom.id);
			if (type != null && type.wikidataId != null) {
				result = addResult(result, type.wikidataId);
			}
		}
		return result;
	}
	
	private List<String> addResult(List<String> result, String value) {
		if (!Algorithms.isEmpty(value)) {
			if (result == null) {
				result = new ArrayList<String>();
			}
			result.add(value);
		}
		return result;
	}

	public void addExtraResult(SpatialSearchResult other, String lang) {
		MapObject object = getFirstObject();
		MapObject otherObj = other.getFirstObject();
		if (otherObj == null) {
			return; // nothing to merge
		}
		
		BaseDetailsObject baseDetails = new BaseDetailsObject(lang);
		baseDetails.addObject(unitedObject);
		baseDetails.addObject(object);
		baseDetails.addObject(otherObj);
		Amenity united = baseDetails.getSyntheticAmenity();
		if (united.getType() != null) {
			// Amenity united
			if (object != null) {
				united.copyNames(object);
			}
			united.copyNames(otherObj);
			unitedObject = united;
		} else {
			// MapObject united
			if (unitedObject == null && object != null) {
				unitedObject = object;
			}
			if (unitedObject != null) {
				unitedObject.copyNames(otherObj);
			} else {
				unitedObject = otherObj;
			}
		}
		if (unitedObject.getLocation() == null) {
			unitedObject.setLocation(otherObj.getLocation());
		}
	}
	
	public long getIdDeduplication() {
		if (!objs.isEmpty()) {
			SpatialSearchResultRef first = objs.get(0);
			// street intersection (!) or building interpolation
			if (preciseLatlon != null) {
				int y31 = MapUtils.get31TileNumberY(preciseLatlon.getLatitude());
				int x31 = MapUtils.get31TileNumberX(preciseLatlon.getLongitude());
				long id = HashQuadTree.encodeTileId31(19, x31, y31);
				return id;
			}
			if (first.atom.object != null) {
				return ObfConstants.getOsmObjectId(first.atom.object);
			}
			return first.atom.id;
		}
		return -1;
	}

	@Override
	public String toString() {
		String r = "";
		if (preciseLatlon != null) {
			r += String.format("%.4f, %.4f ", preciseLatlon.getLatitude(), preciseLatlon.getLongitude());
		}
		if (extraNameMatch != null) {
			r += extraNameMatch + " ";
		}
		return r + objs.toString();
	}

	public static class SpatialSearchResultRef {
		static final int MAX_TYPE_ORDER = 5;
		NameIndexAtom atom;
		List<SpatialSearchToken> tokens = new ArrayList<>();
		
		public SpatialSearchResultRef(NameIndexAtom atom) {
			this.atom = atom;
		}
		
		public boolean extraNameRelated() {
			return atom.buildingOrRefInd >= 0;
		}
		
		public int typeOrder(int min) {
			if (atom.isBuilding()) {
				return -1;
			} else if (atom.isPOI()) {
				return 0;
			} else if (atom.isStreet()) {
				return 1;
			} else if (atom.isPoiCategory()) {
				return 2;
			} else if(atom.isPostcode()) {
				return 3;
			} else if(atom.isBoundary()) {
				return min < 4 ? 4 : MAX_TYPE_ORDER;
			}
			// all cities, villages, hamlets
			return 4;
		}
		
		@Override
		public String toString() {
			StringBuilder words = new StringBuilder();
			for (SpatialSearchToken s : tokens) {
				words.append(" ") .append(s.word);
			}
			if (atom.object != null) {
				MapObject idObject = atom.object;
				String name = atom.object.getName();
				String type = atom.typeStr();
				if (atom.bldObject != null) {
					name = atom.bldObject.getName() + " " + name;
				} else if (atom.object instanceof Amenity a) {
					type += " " + a.getSubTypeStr();
					if (a.getTravelEloNumber() > Amenity.DEFAULT_ELO) {
						type += " " + a.getTravelEloNumber();// " " + a.getCityFromTagGroups("");
					}
				}
				LatLon resLoc = atom.getResultLocation();
				return String.format("\"%s\" [%s] '%s' %s (%.4f %.4f)", words.toString().trim(), type, name,
						"" + ObfConstants.getOsmObjectId(idObject) + " " + (atom.id % 0xffff), resLoc.getLatitude(), resLoc.getLongitude());
			} else if(atom.isPoiCategory()) {
				return String.format("\"%s\" [%s] '%s' id=%d, obj=%,d ", words.toString().trim(), atom.typeStr(), atom.name,
						atom.id, atom.otherWordsCnt );
			}
			return atom.simpleName(words.toString()); 
		}
		
		public NameIndexAtom getNameIndexAtom() {
			return atom;
		}
	}
	
	public int getObjectsSize() {
		return objs.size();
	}
	
	public int matchedTokens() {
		return parent.tCount;
	}
	
	public SpatialSearchResultsList getParent() {
		return parent;
	}

	public int sumOther() {
		int s1 = 0;
		for (SpatialSearchResultRef r : objs) {
			s1 += r.atom.otherWordsCnt; 
//			 Math.max(0, r.atom.otherWordsCnt + r.atom.otherFoundCnt - r.tokens.size());
		}
		return s1;
	}
	
	public int sumTypeOrder() {
		int s1 = 0;
		int min = SpatialSearchResultRef.MAX_TYPE_ORDER;
		for (SpatialSearchResultRef r : objs) {
			min = Math.min(r.typeOrder(min), min);
		}
		for (SpatialSearchResultRef r : objs) {
			s1 += r.typeOrder(min);
		}
		return s1;
	}
	
	public int getRating() {
		int rating = parent.MIN_ELO_RATING; // MIN Rating to make higher
		for (SpatialSearchResultRef r : objs) {
			if (r.atom.object instanceof Amenity a) {
				rating = Math.max(rating, a.getTravelEloNumber());
			}
		}
		return rating;
	}
	
	public long compareKey() {
		return compareKey(this);
	}
	
	private static long addCompareKey(long key, int bits, int value) {
		int max = (1 << bits) - 1;
		if(value < 0) {
			value = Math.max(0, max + value);
		} else {
			value = Math.min(max, value);
		}
		return (key << bits) + value;
	}
	
	public static String compareKeyString(SpatialSearchResult o) {
		int e = (o.getRating() - o.parent.MIN_ELO_RATING) / 64;
		String elo = e > 0 ? "-"+e+"elo" : "";
		return String.format("t%d+%d-w%d-oth%d%s-tp%d", o.parent.tCount, o.surplusWords, o.objs.size(), 
				Math.min(o.sumOther(), 3), elo, o.sumTypeOrder());
	}
	
	public static long compareKey(SpatialSearchResult o) {
		long key = 0;
		key = addCompareKey(key, 6, -o.parent.tCount); // 6 bit - 64
		key = addCompareKey(key, 3, -o.surplusWords); // 3 bit - 8
		key = addCompareKey(key, 6, o.objs.size()); // 6 bit - 64
		key = addCompareKey(key, 3, Math.min(o.sumOther(), 3)); // 3 bit - 3
		key = addCompareKey(key, 6, -(o.getRating() - o.parent.MIN_ELO_RATING) / 64); // 6 bit - 64 - group by 64 bucket
		key = addCompareKey(key, 6, -o.sumTypeOrder()); // 6 bit - 64
		// total 6+6+3+5+6+12 = 35
		return key;
	}
	
	public static int compare(SpatialSearchResult o1, SpatialSearchResult o2, LatLon center) {
		int res = -Integer.compare(o1.parent.tCount, o2.parent.tCount);
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.surplusWords, o2.surplusWords); // buildings 18 matches 18 B
		if (res != 0) {
			return res;
		}
		res = Integer.compare(o1.objs.size(), o2.objs.size());
		if (res != 0) {
			return res;
		}
		res = Integer.compare(o1.sumOther(), o2.sumOther());
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.getRating(), o2.getRating());
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.sumTypeOrder(), o2.sumTypeOrder());
		if (res != 0) {
			return res;
		}
		if (center != null) {
			double d1 = o1.getLatLon() == null ? 0 : MapUtils.getDistance(center, o1.getLatLon());
			double d2 = o2.getLatLon() == null ? 0 : MapUtils.getDistance(center, o2.getLatLon());
			if ((int) d1 != (int) d2) {
				res = Double.compare(d1, d2);
				if (res != 0) {
					return res;
				}
			}
		}
		res = Integer.compare(o1.getBiggestCityType(), o2.getBiggestCityType());
		if (res != 0) {
			return res;
		}
		
		if (o1.getFirstObject() instanceof Amenity a1 && o2.getFirstObject() instanceof Amenity a2) {
			int i1 = FILTER_DUPLICATE_POI_SUBTYPE.indexOf(a1.getSubType());
			int i2 = FILTER_DUPLICATE_POI_SUBTYPE.indexOf(a2.getSubType());
			res = Integer.compare(i1, i2);
			if (res != 0) {
				return res;
			}
		}
		if (res != 0) {
			return res;
		}
		return -Integer.compare(o1.parentInd, o2.parentInd);
	}

	private int getBiggestCityType() {
		if (biggestCityType == -1) {
			biggestCityType = City.CityType.values().length;
			for (SpatialSearchResultRef ref : objs) {
				if (ref.atom.object instanceof Street street) {
					biggestCityType = Math.min(biggestCityType, street.getCity().getType().ordinal());
				}
			}
		}
		return biggestCityType;
	}

	@Override
	public int compareTo(SpatialSearchResult o) {
		return compare(this, o, null);
	}

	private String getWikidata() {
		MapObject mapObject = getFirstObject();
		if (mapObject != null) {
			if (mapObject instanceof Amenity amenity) {
				return amenity.getWikidata();
			}
			return mapObject.getWikidata();
		}
		return null;
	}

	private String getRouteId() {
		MapObject obj = getFirstObject();
		if (obj instanceof Amenity amenity) {
			return amenity.getRouteId();
		}
		return null;
	}

	private String getShortLink(int zoom) {
		LatLon loc = getLatLon();
		if (loc == null) {
			return "";
		}
		return MapUtils.createShortLinkString(loc.getLatitude(), loc.getLongitude(), zoom);
	}
}
	

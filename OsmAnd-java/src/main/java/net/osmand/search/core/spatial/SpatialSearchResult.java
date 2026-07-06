package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.osmand.binary.ObfConstants;
import net.osmand.data.*;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.MapUtils;

public class SpatialSearchResult implements Comparable<SpatialSearchResult> {

	final int parentInd;
	final SpatialSearchResultsList parent;
	final List<SpatialSearchResultRef> objs = new ArrayList<>();
	final LatLon preciseLatlon; 
	final int surplusWords; // negative some building numbers not found, positive some extra tokens matched
	int visibleLevel;
	public BaseDetailsObject unitedObject;
	
	private static final List<String> FILTER_DUPLICATE_POI_SUBTYPE = new ArrayList<String>(
			Arrays.asList("building", "internet_access_yes"));
	
	SpatialSearchResult(SpatialSearchResultsList parentList, int parentInd, LatLon preciseLatlon) {
		this.parentInd = parentInd;
		this.parent = parentList;
		this.preciseLatlon = preciseLatlon;
		int surplusWords = 0;
		for (int i = 0; i < parent.tCount; i++) {
			NameIndexAtom atom = parent.linearResults.get(parentInd * parentList.tCount + i);
			if (atom.bldObject != null && atom.bldObject.getId() != null) {
				if(atom.bldObject.getId().longValue() == SpatialSearchResultsList.PARTIAL_ID_MATCH) {
					surplusWords--;
				} else if(atom.bldObject.getId().longValue() == SpatialSearchResultsList.SURPLUS_ID_MATCH) {
					surplusWords++;
				}
			}
			SpatialSearchToken token = parent.tokens[i];
			SpatialSearchResultRef ref = null;
			// find same object or object & parent 
			for (SpatialSearchResultRef existing : objs) {
				if (atom.id == existing.atom.id) {
					ref = existing;
					// building-street
					if (existing.atom.type > atom.type) {
						// existing street - swap
						existing.parent = existing.atom;
						existing.atom = atom;
						break;
					} else if (existing.atom.type < atom.type) {
						// existing building - swap
						existing.parent = atom;
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
		this.surplusWords = surplusWords;
		sortObjects();
	}
	
	void sortObjects() {
		for (SpatialSearchResultRef r : objs) {
			Collections.sort(r.tokens, (o1, o2) -> Integer.compare(o1.originalOrder, o2.originalOrder));
		}
		Collections.sort(objs, (o1, o2) -> {
			int r = Integer.compare(o1.typeOrder(), o2.typeOrder());
			if (r != 0) {
				return r;
			}
			return Integer.compare(o1.tokens.get(0).originalOrder, o2.tokens.get(0).originalOrder);
		});
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
	
	public List<MapObject> getObjects() {
		List<MapObject> o = new ArrayList<>();
		for (SpatialSearchResultRef r : objs) {
			if (r.atom.bldObject != null) {
				o.add(r.atom.bldObject);
			}
			o.add(r.atom.object);
			if (r.parent != null && r.parent.object != null) {
				o.add(r.parent.object);
			}
		}
		return o;
	}
	
	public LatLon getLatLon() {
		if (preciseLatlon != null) {
			return preciseLatlon;
		}
		if (objs.size() > 0) {
			return objs.get(0).atom.getResultLocation();
		}
		return null;
	}
	
	public int visibleLevel() {
		return visibleLevel;
	}
	
	public long getIdDeduplication() {
		if (objs.size() > 0) {
			SpatialSearchResultRef first = objs.get(0);
			if (first.parent != null && first.parent.object != null) {
				return ObfConstants.getOsmObjectId(first.parent.object);
			}
			// street intersection (!) or possibly building
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
		if (preciseLatlon != null) {
			return String.format("%.4f, %.4f %s", preciseLatlon.getLatitude(), preciseLatlon.getLongitude(),
					objs.toString());
		}
		return objs.toString();
	}
	
	
	public static class SpatialSearchResultRef {
		NameIndexAtom atom;
		NameIndexAtom parent; // street for building
		List<SpatialSearchToken> tokens = new ArrayList<>();
		
		public SpatialSearchResultRef(NameIndexAtom atom) {
			this.atom = atom;
		}
		
		public int typeOrder() {
			if (atom.isBuilding()) {
				return -1;
			} else if (atom.isPOI()) {
				return 0;
			} else if (atom.isStreet()) {
				return 1;
			} else if(atom.isPostcode()) {
				return 2;
			} else if(atom.isBoundary()) {
				return 5;
			}
			// all cities, villages, hamlets
			return 3;
		}
		
		@Override
		public String toString() {
			List<String> words = new ArrayList<String>();
			for (SpatialSearchToken s : tokens) {
				words.add(s.word);
			}
			if (atom.object != null) {
				MapObject idObject = atom.object;
				if (parent != null && parent.object != null) {
					idObject = parent.object;
				}
				String add = "";
				if (atom.bldObject != null) {
					add += " " + atom.bldObject.getName();
				} else if (atom.object instanceof Amenity a) {
					if (a.getTravelEloNumber() > Amenity.DEFAULT_ELO) {
						add += " elo " + a.getTravelEloNumber() + " " + a.getCityFromTagGroups("");
					}
					add += " " + a.getSubTypeStr();
				} else if (parent != null) {
					add += " " + parent.object.getName();
				}
				if (atom.object instanceof Street street && street.getCity() != null) {
					add += " " + street.getCity().getName();
				}
				LatLon resLoc = atom.getResultLocation();
				return String.format("%s %s (%s) %.4f %.4f ", words, atom.typeStr() + " " + atom.object.getName() + add,
						"" + ObfConstants.getOsmObjectId(idObject), 
						resLoc.getLatitude(), resLoc.getLongitude());
			}
			return atom.simpleName(words.toString()); 
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
			s1 += r.atom.otherWordsCnt + r.atom.otherFoundCnt;
		}
		return s1;
	}
	
	public int sumTypeOrder() {
		int s1 = 0;
		for (SpatialSearchResultRef r : objs) {
			s1 += r.typeOrder();
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
	
	public static long compareKey(SpatialSearchResult o) {
		long key = 0;
		key = addCompareKey(key, 6, -o.parent.tCount); // 6 bit - 64
		key = addCompareKey(key, 6, o.objs.size()); // 6 bit - 64
		key = addCompareKey(key, 3, -o.surplusWords); // 3 bit - 8
		key = addCompareKey(key, 3, Math.min(o.sumOther(), 3)); // 3 bit - 3
		key = addCompareKey(key, 6, -o.getRating() / 64); // 6 bit - 64 - group by 64 bucket
		key = addCompareKey(key, 6, -o.sumTypeOrder()); // 6 bit - 64
		// total 6+6+3+5+6+12 = 35
		return key;
	}
	
	public static int compare(SpatialSearchResult o1, SpatialSearchResult o2, LatLon center) {
		int res = -Integer.compare(o1.parent.tCount, o2.parent.tCount);
		if (res != 0) {
			return res;
		}
		res = Integer.compare(o1.objs.size(), o2.objs.size());
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.surplusWords, o2.surplusWords); // buildings 18 matches 18 B
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
			res = Double.compare(d1, d2);
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

	@Override
	public int compareTo(SpatialSearchResult o) {
		return compare(this, o, null);
	}
}
	

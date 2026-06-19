package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.MapUtils;

public class SpatialSearchResult implements Comparable<SpatialSearchResult> {

	final int parentInd;
	final SpatialSearchResultsList parent;
	final List<SpatialSearchResultRef> objs = new ArrayList<>(); 
	
	SpatialSearchResult(SpatialSearchResultsList parentList, int parentInd) {
		this.parentInd = parentInd;
		this.parent = parentList;
		for (int i = 0; i < parent.tCount; i++) {
			NameIndexAtom atom = parent.linearResults.get(parentInd * parentList.tCount + i);
			SpatialSearchToken token = parent.tokens[i];
			SpatialSearchResultRef ref = null;
			for (SpatialSearchResultRef ex : objs) {
				if (atom.id == ex.atom.id) {
					ref = ex;
					atom = null;
					break;
				}
			}
			if (ref == null) {
				ref = new SpatialSearchResultRef(atom);
				objs.add(ref);
			}
			ref.tokens.add(token);
		}
		sortObjects();
	}
	
	void sortObjects() {
		for (SpatialSearchResultRef r : objs) {
			Collections.sort(r.tokens, (o1, o2) -> Integer.compare(o1.originalOrder, o2.originalOrder));
		}
		Collections.sort(objs,
				(o1, o2) -> {
					int r = Integer.compare(o1.typeOrder(), o2.typeOrder());
					if (r != 0) {
						return r;
					}
					return Integer.compare(o1.tokens.get(0).originalOrder, o2.tokens.get(0).originalOrder);
				});
	}

	
	public List<MapObject> getObjects() {
		List<MapObject> o = new ArrayList<>();
		for (SpatialSearchResultRef r : objs) {
			o.add(r.atom.object);
		}
		return o;
	}
	
	public LatLon getLatLon() {
		if (objs.size() > 0 && objs.get(0).atom.object != null) {
			return objs.get(0).atom.object.getLocation();
		}
		return null;
	}
	
	public long getIdDeduplication() {
		if (objs.size() > 0) {
			NameIndexAtom atom = objs.get(0).atom;
			if (atom.object != null) {
				return ObfConstants.getOsmObjectId(atom.object);
			}
			return atom.id;
		}
		return -1;
	}

	@Override
	public String toString() {
		return objs.toString();
	}
	
	
	public static class SpatialSearchResultRef {
		public final NameIndexAtom atom;
		public final List<SpatialSearchToken> tokens = new ArrayList<>();
		
		public SpatialSearchResultRef(NameIndexAtom atom) {
			this.atom = atom;
		}
		
		public int typeOrder() {
			if (atom.type == SpatialSearchToken.POI_TYPE) {
				return 0;
			} else if (atom.type == SpatialSearchToken.STREET_TYPE) {
				return 1;
			} else if(atom.type == CityBlocks.POSTCODES_TYPE.index) {
				return 2;
			} else if(atom.type == CityBlocks.BOUNDARY_TYPE.index) {
				return 5;
			}
			return 3;
		}
		


		@Override
		public String toString() {
			List<String> words = new ArrayList<String>();
			for (SpatialSearchToken s : tokens) {
				words.add(s.word);
			}
			if (atom.object != null) {
				return String.format("%s %s (%s) %.4f %.4f ", words, 
						atom.typeStr() + " " + atom.object.getName(), 
						"" + ObfConstants.getOsmObjectId(atom.object) 
								//+ " " + atom.id,
						, atom.object.getLocation().getLatitude(), atom.object.getLocation().getLongitude());
			}
			return atom.simpleName(words.toString()); 
		}
	}
	
	public int matchedTokens() {
		return parent.tCount;
	}
	

	public int sumOther() {
		int s1 = 0;
		for (SpatialSearchResultRef r : objs) {
			s1 += r.atom.otherWordsCnt;
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
	
	public static int compare(SpatialSearchResult o1, SpatialSearchResult o2, LatLon center) {
		int res = -Integer.compare(o1.parent.tCount, o2.parent.tCount);
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
		res = -Integer.compare(o1.sumTypeOrder(), o2.sumTypeOrder());
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
	

package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.search.core.spatial.SpatialTextSearch.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchToken;
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
		useOriginalOrder();
	}
	
	private void useOriginalOrder() {
		for (SpatialSearchResultRef r : objs) {
			Collections.sort(r.tokens, (o1, o2) -> Integer.compare(o1.originalOrder, o2.originalOrder));
		}
		Collections.sort(objs,
				(o1, o2) -> Integer.compare(o1.tokens.get(0).originalOrder, o2.tokens.get(0).originalOrder));

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
		
		
		@Override
		public String toString() {
			List<String> words = new ArrayList<String>();
			for(SpatialSearchToken s : tokens) {
				words.add(s.word);
			}
			return String.format("%s %s %d (%.4f, %.4f) ",
					atom.getType(),
					words, (atom.id % 0xffff), 
					MapUtils.get31LatitudeY(atom.y16 << 15),
					MapUtils.get31LongitudeX(atom.x16 << 15));
		}
	}

	@Override
	public int compareTo(SpatialSearchResult o) {
		int res = Integer.compare(objs.size(), o.objs.size());
		if (res != 0) {
			return res;
		}
		return -Integer.compare(parentInd, o.parentInd);
	}
}

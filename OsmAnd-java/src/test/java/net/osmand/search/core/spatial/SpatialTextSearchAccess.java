package net.osmand.search.core.spatial;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.search.core.spatial.SpatialPoiSearch.SpatialPoiType;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

import static net.osmand.shared.compat.CommonWordsCompatTest.hex;
import static net.osmand.shared.compat.NameIndexCompatTest.fnv;
import static net.osmand.shared.compat.SpatialSearchTokenCompatTest.bits;

/**
 * The package-private part of the spatial search, as text, for the compat test of its copy in
 * OsmAnd-shared, {@code net.osmand.shared.compat.SpatialTextSearchCompatTest}: the words of a query
 * after a search and its results. {@code SpatialTextSearchTest} of the commonTest of OsmAnd-shared
 * writes the copy the same way.
 */
public final class SpatialTextSearchAccess {

	private SpatialTextSearchAccess() {
	}

	/** The words of a query as the search left them: their order, atoms and what each found. */
	public static String tokens(List<SpatialSearchToken> tokens) {
		StringBuilder sb = new StringBuilder();
		for (SpatialSearchToken t : tokens) {
			StringBuilder atoms = new StringBuilder();
			for (NameIndexAtom a : t.atoms) {
				atoms.append(a.id).append(',').append(a.type).append(',').append(a.nearbyRadius).append(';');
			}
			sb.append(hex(t.word)).append(':').append(t.originalOrder).append(':').append(t.sortedOrder).append(':')
					.append(t.atoms.size()).append(':').append(t.broad).append(':').append(t.incomplete).append(':')
					.append(t.getDeletedAtoms().size()).append(':').append(t.mainNumber).append(':')
					.append(t.numberNamedByOther).append(':').append(t.hasPoiCategoryKeys()).append(':')
					.append(Long.toHexString(fnv(atoms.toString()))).append(';');
		}
		return sb.toString();
	}

	/**
	 * The [words] words of a query by their order in it, where only the results of a search are at hand: those the results
	 * stand for as {@link #tokens} writes them, what the search left of them, and the others as "-:order:-:0:-".
	 */
	public static String tokensOf(List<SpatialSearchResult> results, int words) {
		TreeMap<Integer, SpatialSearchToken> byOrder = new TreeMap<>();
		for (SpatialSearchResult r : results) {
			for (SpatialSearchResultRef ref : r.objs) {
				for (SpatialSearchToken t : ref.tokens) {
					byOrder.putIfAbsent(t.originalOrder, t);
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.max(words, byOrder.isEmpty() ? 0 : byOrder.lastKey() + 1); i++) {
			SpatialSearchToken t = byOrder.get(i);
			sb.append(t == null ? "-:" + i + ":-:0:-;" : tokens(List.of(t)));
		}
		return sb.toString();
	}

	/** A context of the poi types of [api], all {@link #result} asks of one. */
	public static SpatialSearchContext context(SpatialTextSearchAPI api) throws ReflectiveOperationException {
		Field f = SpatialTextSearchAPI.class.getDeclaredField("poiSearch");
		f.setAccessible(true);
		return new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(), List.of(), (SpatialPoiSearch) f.get(api),
				null);
	}

	/** The name of its poi category a result was matched by. */
	public static String matchedName(SpatialSearchResult r, SpatialSearchContext ctx) {
		return r.getPoiCategoryMatchedName(ctx.poiSearch);
	}

	/** The poi types of the search by id: key, place, wikidata and names. */
	public static String poiTypes(SpatialPoiSearch poiSearch) {
		StringBuilder sb = new StringBuilder();
		for (int id = 0; id < poiSearch.ids.get(); id++) {
			SpatialPoiType t = poiSearch.getById(id);
			if (t == null) {
				sb.append("-;");
				continue;
			}
			sb.append(hex(t.key)).append(':').append(t.place).append(':')
					.append(t.wikidataId == null ? "-" : hex(t.wikidataId)).append(':').append(t.tokensInName).append(':');
			for (String n : t.names) {
				sb.append(hex(n)).append(',');
			}
			sb.append(';');
		}
		return sb.toString();
	}

	/** The names of all the poi types of the search. */
	public static Set<String> names(SpatialPoiSearch poiSearch) {
		Set<String> names = new HashSet<>();
		for (SpatialPoiType t : poiSearch.byKey.values()) {
			names.addAll(t.names);
		}
		return names;
	}

		/** The combinations a search made, as they say themselves. */
	public static String combinations(List<SpatialSearchResultsList> lists) {
		StringBuilder sb = new StringBuilder();
		for (SpatialSearchResultsList l : lists) {
			sb.append(hex(l.toString(false))).append(';');
		}
		return sb.toString();
	}

	/** The counts of the statistics that do not depend on the time or on how the file is buffered. */
	public static String stats(SpatialSearchStats s) {
		return s.tokenObjs + "," + s.maxCombinations + "," + s.sub1FileAtomsTime.intervals + ","
				+ s.sub1MatchTime.intervals + "," + s.sub2ReadObjTime.intervals + "," + s.poiByTypeBboxes;
	}

	/** One result: its level, its keys and score, where it is, the atoms it stands for and its objects. */
	public static String result(SpatialSearchResult r, SpatialSearchContext ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append(r.visibleLevel).append('\t').append(SpatialSearchResult.compareKey(r)).append('\t')
				.append(hex(SpatialSearchResult.compareKeyString(r))).append('\t').append(number(r.score)).append('\t')
				.append(r.getIdDeduplication()).append('\t').append(latLon(r.getLatLon())).append('\t')
				.append(r.surplusWords).append('\t')
				.append(r.getExtraNameMatch() == null ? "-" : hex(r.getExtraNameMatch())).append('\t')
				.append(r.matchedTokens()).append('\t');
		SpatialPoiType category = r.getPoiCategory(ctx.poiSearch);
		sb.append(category == null ? "-" : hex(category.getKey())).append('\t');
		String matched = r.getPoiCategoryMatchedName(ctx.poiSearch);
		sb.append(matched == null ? "-" : hex(matched)).append('\t');
		int[] bbox = r.getViewBBox31();
		sb.append(bbox == null ? "-" : bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3]).append('\t');
		for (SpatialSearchResultRef ref : r.objs) {
			NameIndexAtom a = ref.atom;
			sb.append(a.id).append(',').append(a.type).append(',').append(hex(a.name)).append(',');
			for (SpatialSearchToken t : ref.tokens) {
				sb.append(t.originalOrder).append('.');
			}
			sb.append(',').append(ref.otherWordsFound).append(',').append(ref.otherWordsNotFound).append(',')
					.append(a.object == null ? "-" : String.valueOf(ObfConstants.getOsmObjectId(a.object))).append(',')
					.append(a.bldObject == null ? "-" : hex(a.bldObject.getName())).append(',').append(ref.typeOrder(false))
					.append(';');
		}
		sb.append('\t');
		for (MapObject o : r.getObjects()) {
			sb.append(object(o)).append(';');
		}
		sb.append('\t');
		MapObject main = r.getMainObject();
		sb.append(main == null ? "-" : object(main) + "," + latLon(main.getLocation())).append('\t');
		sb.append(hex(r.toString(ctx)));
		return sb.toString();
	}

	public static String object(MapObject o) {
		return o.getClass().getSimpleName() + "," + ObfConstants.getOsmObjectId(o) + "," + hex(o.getName());
	}

	public static String latLon(LatLon l) {
		return l == null ? "-" : number(l.getLatitude()) + "," + number(l.getLongitude());
	}

	/** The bits of a double after '~': the replay on Kotlin/Native lets them be a few ulps off. */
	public static String number(double v) {
		return "~" + bits(v);
	}
}

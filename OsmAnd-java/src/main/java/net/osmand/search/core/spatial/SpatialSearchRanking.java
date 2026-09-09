package net.osmand.search.core.spatial;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.binary.NameIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

/**
 * Orders results by one bounded score instead of a lexicographic ladder, so a large gap on one
 * signal can outweigh a small gap on another. Every weight and every list below is fitted on
 * spatial_search/preferences.jsonl; the pref ids say which judgement each one answers to, and
 * preferences.md tells how they were collected.
 */
public class SpatialSearchRanking {

	// weights
	public double wName = 0.15;       // small: pref-0063 (a word for a kind means nothing)
	public double wType = 2.0;
	public double wRating = 0.5;      // an ordinary POI: pref-0116, pref-0126
	public double wRatingPlace = 2.0; // a settlement is looked for from anywhere: pref-0127
	public double wNear = 2.0;
	public double wExactName = 1.0;   // the whole name IS the query: pref-0063, pref-0104
	/** distance at which the proximity term is worth half of its maximum */
	public double halfWeightKm = 3.0;
	/** elo above MIN_ELO_RATING at which the rating term saturates */
	public double ratingSpan = 1200.0;

	// name
	private static final double NAME_EXACT = 1.00;
	private static final double NAME_PREFIX = 0.85;
	private static final double NAME_CONTAINS = 0.65;
	private static final double NAME_OTHER = 0.50;
	private static final double NAME_KIND_ONLY = 0.15;

	// kind
	private static final double TYPE_ADMIN = 1.00; // a country or a region IS the place it names
	private static final double TYPE_CITY = 0.95;
	private static final double TYPE_VILLAGE = 0.88;
	private static final double TYPE_LANDMARK = 0.80;
	private static final double TYPE_BUILDING = 0.75;
	private static final double TYPE_STREET = 0.55; // above a stop, below a village: pref-0106
	private static final double TYPE_BOUNDARY = 0.60;
	private static final double TYPE_POI = 0.50;
	private static final double TYPE_POSTCODE = 0.40;
	private static final double TYPE_STOP = 0.35;
	private static final double TYPE_INFRASTRUCTURE = 0.10;

	/** administrative areas stored as a POI */
	private static final Set<String> ADMIN_SUBTYPES = new HashSet<>(Arrays.asList(
			"country", "state", "region", "province", "county"));

	/** a settlement stored as a POI - the world basemap has New York only so: pref-0127 */
	private static final Set<String> PLACE_SUBTYPES = new HashSet<>(Arrays.asList(
			"city", "town", "village", "hamlet", "borough"));

	/** objects a person travels TO by name; a hospital is a service, not a landmark: pref-0068 */
	private static final Set<String> LANDMARK_SUBTYPES = new HashSet<>(Arrays.asList(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"townhall", "zoo", "peak", "mountain_pass", "wiki_place",
			"marketplace", "square", "park", "cathedral", "monastery"));

	/** a node describing something else; its name is not evidence that it IS it: pref-0023 */
	private static final Set<String> INFRASTRUCTURE_SUBTYPES = new HashSet<>(Arrays.asList(
			SpatialSearchTags.PUBLIC_TRANSPORT_PLATFORM, SpatialSearchTags.PUBLIC_TRANSPORT_STOP,
			SpatialSearchTags.SUBWAY_ENTRANCE, SpatialSearchTags.ELEVATOR,
			SpatialSearchTags.TICKET_VALIDATOR, SpatialSearchTags.ENTRANCE,
			SpatialSearchTags.LEVEL_CROSSING, SpatialSearchTags.MOTORWAY_JUNCTION,
			"boundary_stone", "street_lamp", "waste_basket", "bench", "vending_machine"));

	private static final Set<String> STOP_SUBTYPES = new HashSet<>(Arrays.asList(
			SpatialSearchTags.BUS_STOP, SpatialSearchTags.TRAM_STOP, SpatialSearchTags.RAILWAY_HALT,
			"taxi"));

	/** computed once per result: the value goes stale when deduplication changes getTotalRating() */
	private final Map<SpatialSearchResult, Double> scores = new IdentityHashMap<>();

	public void prepare(List<SpatialSearchResult> results, LatLon center) {
		scores.clear();
		for (SpatialSearchResult r : results) {
			scores.put(r, score(r, center));
		}
	}

	public double scoreOf(SpatialSearchResult r) {
		Double d = scores.get(r);
		return d == null ? 0 : d;
	}

	/**
	 * The order of the results. Above the score sit the tiers that are not a matter of degree:
	 * a category suggestion, how many query words matched, extra or missing words, and how many
	 * objects the answer is stitched from.
	 */
	public int compare(SpatialSearchResult o1, SpatialSearchResult o2) {
		int res = -Boolean.compare(o1.isPoiCategory(), o2.isPoiCategory());
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.parent.tCount, o2.parent.tCount);
		if (res != 0) {
			return res;
		}
		res = -Integer.compare(o1.surplusWords, o2.surplusWords);
		if (res != 0) {
			return res;
		}
		res = Integer.compare(answerParts(o1), answerParts(o2));
		if (res != 0) {
			return res;
		}
		res = -Double.compare(scoreOf(o1), scoreOf(o2));
		if (res != 0) {
			return res;
		}
		return -Long.compare(o1.getFirstRef().atom.id, o2.getFirstRef().atom.id);
	}

	/**
	 * How many things the answer is stitched from: one object beats the same words found in two,
	 * which keeps "Dr Lucas" off the 74th row. Two corrections, per RESULT so the comparator stays
	 * transitive: "<object> in <city>" counts as one when the city was named by its own name
	 * (pref-0117, pref-0121), and a node named after what it serves never counts as one (pref-0087).
	 */
	public int answerParts(SpatialSearchResult r) {
		int parts = r.objs.size();
		if (parts == 2) {
			SpatialSearchResultRef ref = r.objs.get(1);
			NameIndexAtom second = ref.atom;
			if ((second.isCity() || second.isCityVillage() || second.isBoundary())
					&& nameScore(ref) >= NAME_PREFIX) {
				parts = 1;
			}
		}
		if (parts < 2 && isSubordinateNode(r)) {
			parts = 2;
		}
		return parts;
	}

	/** higher is better; only meaningful within one bucket of the tiers above it */
	public double score(SpatialSearchResult r, LatLon center) {
		SpatialSearchResultRef head = r.getFirstRef();
		if (head == null) {
			return 0;
		}
		double near = nearScore(r, center);
		double name = nameScore(head);
		// undimmed by distance for what is looked for by name from anywhere: pref-0125, pref-0127
		double exact = name == NAME_EXACT && ownsItsName(r)
				? wExactName * (isPlace(head) || hasTravelRating(r) ? 1 : near) : 0;
		return wName * name * near
				+ wType * typeScore(head)
				+ (isPlace(head) ? wRatingPlace : wRating) * ratingScore(r)
				+ wNear * near
				+ exact;
	}

	/** did the query name this object, or only the word for its kind? */
	public double nameScore(SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.name == null) {
			return NAME_OTHER;
		}
		if (atom.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			return NAME_KIND_ONLY;
		}
		String queried = queriedWords(ref);
		if (queried.isEmpty()) {
			return NAME_OTHER;
		}
		if (atom.object == null) {
			return compareToName(atom.name, queried);
		}
		// any language the object carries, not only the default one: pref-0125
		double best = compareToName(atom.object.getName(), queried);
		Map<String, String> names = atom.object.getNamesMap(true);
		if (names != null) {
			for (String n : names.values()) {
				best = Math.max(best, compareToName(n, queried));
				if (best == NAME_EXACT) {
					return NAME_EXACT;
				}
			}
		}
		return best;
	}

	public double typeScore(SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.isPoiCategory()) {
			return TYPE_POI;
		}
		if (atom.isCity()) {
			return TYPE_CITY;
		}
		if (atom.isCityVillage()) {
			return TYPE_VILLAGE;
		}
		if (atom.isBuilding()) {
			return TYPE_BUILDING;
		}
		if (atom.isStreet()) {
			return TYPE_STREET;
		}
		if (atom.isPostcode()) {
			return TYPE_POSTCODE;
		}
		if (atom.isBoundary()) {
			return TYPE_BOUNDARY;
		}
		if (atom.object instanceof Amenity a) {
			String subType = a.getSubType();
			if (subType != null) {
				if (ADMIN_SUBTYPES.contains(subType)) {
					return TYPE_ADMIN;
				}
				if (PLACE_SUBTYPES.contains(subType)) {
					return "city".equals(subType) || "town".equals(subType) ? TYPE_CITY : TYPE_VILLAGE;
				}
				if (INFRASTRUCTURE_SUBTYPES.contains(subType)) {
					return TYPE_INFRASTRUCTURE;
				}
				if (STOP_SUBTYPES.contains(subType)) {
					return TYPE_STOP;
				}
				if (LANDMARK_SUBTYPES.contains(subType)) {
					return TYPE_LANDMARK;
				}
			}
		}
		return TYPE_POI;
	}

	/** bounded, so a famous place outranks an unknown one but not a much closer one */
	public double ratingScore(SpatialSearchResult r) {
		double over = r.getTotalRating() - r.parent.MIN_ELO_RATING;
		return Math.max(0, Math.min(1, over / ratingSpan));
	}

	public double nearScore(SpatialSearchResult r, LatLon center) {
		if (center == null || r.getLatLon() == null) {
			return 0;
		}
		double km = MapUtils.getDistance(center, r.getLatLon()) / 1000.0;
		return 1.0 / (1.0 + Math.max(0, km) / halfWeightKm);
	}

	/** a node that describes something else, so its name is not evidence that it IS that place */
	public boolean isSubordinateNode(SpatialSearchResult r) {
		SpatialSearchResultRef head = r.getFirstRef();
		if (head == null || !(head.atom.object instanceof Amenity a) || a.getSubType() == null) {
			return false;
		}
		return INFRASTRUCTURE_SUBTYPES.contains(a.getSubType())
				|| STOP_SUBTYPES.contains(a.getSubType());
	}

	/** a settlement or an administrative area, however the map happens to store it */
	private boolean isPlace(SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.isCity() || atom.isCityVillage()) {
			return true;
		}
		return atom.object instanceof Amenity a && a.getSubType() != null
				&& (ADMIN_SUBTYPES.contains(a.getSubType()) || PLACE_SUBTYPES.contains(a.getSubType()));
	}

	/** a travel rating above the floor: known well enough to be a destination, not a detail */
	private boolean hasTravelRating(SpatialSearchResult r) {
		return r.getTotalRating() > r.parent.MIN_ELO_RATING;
	}

	/** notable enough that carrying the name is not a coincidence: a rating or a wikipedia article */
	private boolean ownsItsName(SpatialSearchResult r) {
		if (hasTravelRating(r)) {
			return true;
		}
		MapObject o = r.getFirstRef() == null ? null : r.getFirstRef().atom.object;
		return o instanceof Amenity a && !Algorithms.isEmpty(a.getAdditionalInfo(Amenity.WIKIDATA));
	}

	private static double compareToName(String rawName, String queried) {
		String name = normalizeName(rawName);
		if (name.isEmpty()) {
			return NAME_OTHER;
		}
		if (name.equals(queried)) {
			return NAME_EXACT;
		}
		if (name.startsWith(queried)) {
			return NAME_PREFIX;
		}
		if (name.contains(queried)) {
			return NAME_CONTAINS;
		}
		return NAME_OTHER;
	}

	private static String queriedWords(SpatialSearchResultRef ref) {
		List<SpatialSearchToken> tokens = ref.tokens;
		if (tokens == null || tokens.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (SpatialSearchToken t : tokens) {
			if (!Algorithms.isEmpty(t.word)) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(t.word);
			}
		}
		return normalizeName(sb.toString());
	}

	/** drops the "(district)" suffix deduplication adds, so a street still matches its own name */
	private static String normalizeName(String s) {
		if (s == null) {
			return "";
		}
		int bracket = s.lastIndexOf(" (");
		if (bracket > 0 && s.endsWith(")")) {
			s = s.substring(0, bracket);
		}
		String n = SearchAlgorithms.normalizeToken(SearchAlgorithms.alignChars(s));
		return n == null ? "" : n.trim().toLowerCase();
	}
}

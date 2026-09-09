package net.osmand.search.core.spatial;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.binary.NameIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.SearchAlgorithms;

/**
 * Replaces tiers 4..11 of {@link SpatialSearchResult#compare} (objs.size, sumOther, mainRating,
 * totalRating, sumTypeOrder, distance, biggestCityType, osm id) with one bounded score, so that
 * a large gap on one signal can outweigh a small gap on another instead of deciding outright:
 * in the ladder one point of travel elo beats any distance, and every POI has typeOrder 0.
 */
public class SpatialSearchRanking {

	// Fitted on the 54 order preferences in spatial_search/preferences.jsonl (36 -> 46 satisfied).
	// What the two review rounds said, and what these numbers encode: between two ordinary POIs
	// the NEARER one wins - an exact name, a matching category and a higher elo all lose to
	// distance - while a node that merely describes a place loses to the place even from 5.5 km
	// closer.
	//
	// The rating weight is what the third round pinned down. A famous object is worth about one
	// point, which is roughly the gap between 3 km and 30 km: enough to put the better-known of
	// two castles first (Palazzo Pubblico, elo 2998 at 80 km, over Castello di Punta Ala, 2329 at
	// 32 km), not enough to keep a cathedral 80 km away above an ordinary church 800 m away.
	//
	// The name term is small AND multiplied by proximity, which is the fourth round's rule in the
	// reviewer's own words: "they are close to each other so full match is more important". A
	// per-result score cannot ask whether two candidates are close to EACH OTHER, but making the
	// name count only for what is close to the PERSON has the same effect whenever both are in
	// the same area, and lets the name fade for a far namesake - which is the other half of the
	// same judgements ("Camping-Freunde Berlin" at 32 km must not beat an unnamed camping office
	// at 24 km on the strength of the word in its name).
	//
	// Raising the weight further costs preferences at every step (0.3 -> 43 satisfied, 0.5 -> 42,
	// 0.8 -> 39, 1.0 -> 38): "supermarkt", "кафе" and "lekarna" are words for a KIND of object,
	// where carrying the word in the name says nothing about relevance, and the engine still
	// cannot tell such a word from a proper name.
	public double wName = 0.15;
	public double wType = 2.0;
	public double wRating = 0.5;
	public double wNear = 2.0;

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

	// type
	private static final double TYPE_CITY = 0.95;
	private static final double TYPE_VILLAGE = 0.88;
	private static final double TYPE_LANDMARK = 0.80;
	private static final double TYPE_BUILDING = 0.75;
	/**
	 * A street used to be worth 0.70, half way between a village and an ordinary POI. That put
	 * "Via del Mugello" 23 km away above the guest house called "Mugello" 13 km away, which the
	 * reviewer rejected twice; 0.55 leaves the street comfortably above a stop (0.35) and a
	 * platform (0.10), which is the comparison the value exists for.
	 */
	private static final double TYPE_STREET = 0.55;
	private static final double TYPE_BOUNDARY = 0.60;
	private static final double TYPE_POI = 0.50;
	private static final double TYPE_POSTCODE = 0.40;
	private static final double TYPE_STOP = 0.35;
	private static final double TYPE_INFRASTRUCTURE = 0.10;

	/** nodes that describe a place rather than being it - a station has a dozen of them */
	private static final Set<String> INFRASTRUCTURE_SUBTYPES = new HashSet<>(Arrays.asList(
			"public_transport_platform", "public_transport_stop_position", "subway_entrance",
			"elevator", "ticket_validator", "entrance", "level_crossing", "boundary_stone",
			"street_lamp", "waste_basket", "bench", "vending_machine", "motorway_junction"));

	private static final Set<String> STOP_SUBTYPES = new HashSet<>(Arrays.asList(
			"bus_stop", "tram_stop", "railway_halt", "taxi"));

	/**
	 * Objects a person travels TO by name. Services that merely have a name - a hospital, a
	 * university, a library - were in this list and are not any more: asked to choose between
	 * "Омега-Київ" (clinic, 1.8 km) and "DENIS" (hospital, 3.1 km) for a medical query, the
	 * reviewer took the nearer one, and the landmark bonus was the only thing preventing that.
	 */
	private static final Set<String> LANDMARK_SUBTYPES = new HashSet<>(Arrays.asList(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"townhall", "zoo", "peak", "mountain_pass",
			"marketplace", "square", "park", "cathedral", "monastery"));

	/**
	 * Nodes of ONE facility, spread over its whole footprint: the platforms, stop positions and
	 * entrances of a stop lie hundreds of metres apart and are still one stop. Deduplication may
	 * unite these across a wide radius - unlike a bench or a waste basket, which are also
	 * subordinate but are one object each: two benches called "Park Bench" 57 m apart are two
	 * benches, judged 2026-09-09.
	 */
	public static boolean isSpreadNode(SpatialSearchResult r) {
		SpatialSearchResultRef head = r == null ? null : r.getFirstRef();
		if (head == null || !(head.atom.object instanceof Amenity a)) {
			return false;
		}
		String subType = a.getSubType();
		return subType != null && (SPREAD_SUBTYPES.contains(subType) || STOP_SUBTYPES.contains(subType));
	}

	/** the parts a stop or a station is stored as */
	private static final Set<String> SPREAD_SUBTYPES = new HashSet<>(Arrays.asList(
			"public_transport_platform", "public_transport_stop_position", "subway_entrance",
			"elevator", "ticket_validator", "entrance", "level_crossing", "motorway_junction"));

	/**
	 * A node that exists to describe something else - a platform, a stop position, an entrance,
	 * a motorway junction. Named after the place it serves, so its name is never evidence that
	 * it IS that place.
	 */
	public static boolean isSubordinateNode(SpatialSearchResult r) {
		SpatialSearchResultRef head = r == null ? null : r.getFirstRef();
		if (head == null || !(head.atom.object instanceof Amenity a)) {
			return false;
		}
		String subType = a.getSubType();
		return subType != null
				&& (INFRASTRUCTURE_SUBTYPES.contains(subType) || STOP_SUBTYPES.contains(subType));
	}

	/** higher is better; only meaningful within one bucket of the structural tiers */
	public double score(SpatialSearchResult r, LatLon center) {
		SpatialSearchResultRef head = r.getFirstRef();
		if (head == null) {
			return 0;
		}
		double near = nearScore(r, center);
		return wName * nameScore(head) * near
				+ wType * typeScore(head)
				+ wRating * ratingScore(r)
				+ wNear * near;
	}

	/** did the query name this object, or only the word for its kind? */
	public double nameScore(SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.name == null) {
			return NAME_OTHER;
		}
		if (atom.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			// matched through the poi category key: "farm" -> Podere Colombaio
			return NAME_KIND_ONLY;
		}
		String queried = queriedWords(ref);
		if (queried.isEmpty()) {
			return NAME_OTHER;
		}
		String name = normalize(atom.name);
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
		if (center == null) {
			return 0;
		}
		double km = SpatialSearchResult.getDistance(r, center) / 1000.0;
		if (km < 0) {
			km = 0;
		}
		return 1.0 / (1.0 + km / halfWeightKm);
	}

	private static String queriedWords(SpatialSearchResultRef ref) {
		List<SpatialSearchToken> tokens = ref.tokens;
		if (tokens == null || tokens.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (SpatialSearchToken t : tokens) {
			if (t.word != null && !t.word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(t.word);
			}
		}
		return normalize(sb.toString());
	}

	private static String normalize(String s) {
		String n = SearchAlgorithms.normalizeToken(SearchAlgorithms.alignChars(s));
		return n == null ? "" : n.trim().toLowerCase();
	}
}

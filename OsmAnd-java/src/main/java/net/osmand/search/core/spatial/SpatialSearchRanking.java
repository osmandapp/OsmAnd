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
import net.osmand.util.Algorithms;
import net.osmand.util.SearchAlgorithms;

/**
 * One bounded score in place of tiers 4..11 of {@link SpatialSearchResult#compare}, so a large gap
 * on one signal can outweigh a small gap on another. Weights fitted on spatial_search/preferences.jsonl.
 */
public class SpatialSearchRanking {

	// Fitted on the recorded preferences: the nearer of two ordinary POIs wins, a name matters
	// only for what is close, and fame is worth about the gap between 3 km and 30 km.
	public double wName = 0.15;
	public double wType = 2.0;
	public double wRating = 0.5;
	public double wNear = 2.0;
	/** an object whose whole name IS the query, when it is notable enough to be that name */
	public double wExactName = 1.0;

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
	private static final double TYPE_ADMIN = 1.00; // a country or a region IS the place it names
	private static final double TYPE_CITY = 0.95;
	private static final double TYPE_VILLAGE = 0.88;
	private static final double TYPE_LANDMARK = 0.80;
	private static final double TYPE_BUILDING = 0.75;
	private static final double TYPE_STREET = 0.55; // above a stop 0.35, below a village
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

	/** objects a person travels TO by name; a hospital or a library is a service, not a landmark */
	/** administrative places stored as POI - the map has no address record for a country */
	private static final Set<String> ADMIN_SUBTYPES = new HashSet<>(Arrays.asList(
			"country", "state", "region", "province", "county"));

	private static final Set<String> LANDMARK_SUBTYPES = new HashSet<>(Arrays.asList(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"wiki_place",
			"townhall", "zoo", "peak", "mountain_pass",
			"marketplace", "square", "park", "cathedral", "monastery"));

	/** the parts ONE facility is stored as - spread over its footprint, unlike a bench */
	public boolean isSpreadNode(SpatialSearchResult r) {
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

	/** a node that describes something else, so its name is not evidence that it IS that place */
	public boolean isSubordinateNode(SpatialSearchResult r) {
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
		double name = nameScore(head);
		// "liechtenstein" returned the country 22nd, behind museums that merely carry the word.
		// An exact whole-name match is the strongest signal there is - but only for an object
		// notable enough to own the name, or "Supermarkt" would outrank the nearer supermarket.
		double exact = name == NAME_EXACT && isNotable(r) ? wExactName * near : 0;
		return wName * name * near
				+ wType * typeScore(head)
				+ wRating * ratingScore(r)
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
			// matched through the poi category key: "farm" -> Podere Colombaio
			return NAME_KIND_ONLY;
		}
		String queried = queriedWords(ref);
		if (queried.isEmpty()) {
			return NAME_OTHER;
		}
		// the OBJECT's name, not atom.name - the atom carries the matched TOKEN, so comparing
		// against it made every single-word match "exact" and the term compared the query with
		// itself: "Liechtensteinisches Landesmuseum Vaduz" scored the same as "Liechtenstein"
		String name = normalize(atom.object != null ? atom.object.getName() : atom.name);
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
				if (ADMIN_SUBTYPES.contains(subType)) {
					return TYPE_ADMIN;
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

	/** a travel rating above the floor: known well enough to be a destination, not a detail */
	public boolean isProminent(SpatialSearchResult r) {
		return r.getTotalRating() > r.parent.MIN_ELO_RATING;
	}

	/** carries a wikipedia article or a travel rating, so the name is its own, not a coincidence */
	public boolean isNotable(SpatialSearchResult r) {
		if (r.getTotalRating() > r.parent.MIN_ELO_RATING) {
			return true;
		}
		MapObject o = r.getFirstRef() == null ? null : r.getFirstRef().atom.object;
		return o instanceof Amenity a && !Algorithms.isEmpty(a.getAdditionalInfo(Amenity.WIKIDATA));
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
		// "4th Avenue (Manhattan)" is our own disambiguation, added when a street is united with
		// its district - it must not make the street a worse match for "4th avenue"
		int bracket = s == null ? -1 : s.lastIndexOf(" (");
		if (bracket > 0 && s.endsWith(")")) {
			s = s.substring(0, bracket);
		}
		String n = SearchAlgorithms.normalizeToken(SearchAlgorithms.alignChars(s));
		return n == null ? "" : n.trim().toLowerCase();
	}
}

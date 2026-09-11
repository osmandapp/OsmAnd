package net.osmand.search.core.spatial;

import java.util.Arrays;
import java.util.HashSet;
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
import net.osmand.util.SearchAlgorithms;

/**
 * One bounded score in place of tiers 4..11 of {@link SpatialSearchResult#compare}, so a large gap
 * on one signal can outweigh a small gap on another. Weights fitted on spatial_search/preferences.jsonl.
 */
public class SpatialSearchRanking {

	// Fitted on spatial_search/preferences.jsonl - see preferences.md for how, and the pref ids
	// below for the judgement each choice answers to.
	public double wName = 0.15;
	public double wType = 2.0;
	public double wRating = 0.5;      // an ordinary POI: pref-0116, pref-0126
	public double wRatingPlace = 2.0; // a settlement is looked for from anywhere: pref-0127
	public double wNear = 2.0;
	public double wExactName = 1.0; // the whole name IS the query: pref-0063, pref-0104

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
	private static final double TYPE_BOUNDARY = 0.60;
	private static final double TYPE_STREET = 0.55; // above a stop, below a village: pref-0106
	private static final double TYPE_POI = 0.50;
	private static final double TYPE_POSTCODE = 0.40;
	private static final double TYPE_NAME_ALIKE = 0.35;
	private static final double TYPE_NAME_ALIKE_PART = 0.10;

	/** distance from which a town or a village, named by a piece of its name, starts to lose its weight */
	private static final double PLACE_FAR_FROM_KM = 500;
	/** ... and at which, past that, it keeps half of it */
	private static final double PLACE_FAR_HALF_KM = 1000;

	/** elo above the floor at which fame alone makes an object a landmark: pref-0134 */
	private static final double LANDMARK_RATING = 1000;
	/** ... but only where a person can go to it - 1/(1+9/3): a famous church 80 km away is not
	 *  the answer to "christian church": pref-0086 */
	private static final double LANDMARK_NEAR = 0.25;

	/**
	 * Named alike the place they stand at - a stop, a bike dock, a car park called after the street, the square or the
	 * station. Searched by name, such an object is absorbed by the same-named place within 400 m and its weight keeps
	 * that place above it; searched by kind ("parking") each one is a row of its own.
	 */
	static final Set<String> NAME_ALIKE_SUBTYPES = new HashSet<>(Arrays.asList(
			"bus_stop", "tram_stop", "railway_halt", "taxi", "bicycle_rental", "parking", "parking_entrance",
			"bicycle_parking"));

	/** pieces of what they are named after: the parts a stop or a station is stored as (a metro platform with wi-fi is
	 *  stored as internet access too), the parts and the signs of a street, street furniture */
	static final Set<String> NAME_ALIKE_PART_SUBTYPES = new HashSet<>(Arrays.asList(
			"public_transport_platform", "public_transport_stop_position", "subway_entrance", "elevator",
			"ticket_validator", "entrance", "level_crossing", "motorway_junction", "internet_access_yes",
			"bridge", "tunnel", "viaduct", "ford", "highway_steps", "traffic_signals",
			"traffic_calming_bump", "traffic_calming_hump", "traffic_calming_cushion", "traffic_calming_chicane",
			"traffic_calming_rumble_strip", "traffic_calming_table", "traffic_calming_choker", "traffic_calming_island",
			"hazard_children", "hazard_school_zone", "hazard_animal_crossing", "hazard_pedestrians", "hazard_cyclists",
			"hazard_curve", "hazard_curves", "hazard_dangerous_junction", "hazard_slippery_road",
			"boundary_stone", "street_lamp", "waste_basket", "bench", "vending_machine"));


	static final Set<String> ADMIN_SUBTYPES = new HashSet<>(Arrays.asList(
			"country", "state", "region", "province", "county"));

	/** a settlement stored as a POI - the world basemap has New York only so: pref-0127 */
	static final Set<String> PLACE_SUBTYPES = new HashSet<>(Arrays.asList(
			"city", "town", "village", "hamlet", "borough"));

	/** objects a person travels TO by name; a hospital is a service, not a landmark: pref-0068 */
	static final Set<String> LANDMARK_SUBTYPES = new HashSet<>(Arrays.asList(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"townhall", "zoo", "peak", "mountain_pass", "wiki_place",
			"marketplace", "square", "park", "cathedral", "monastery"));

	/** a node describing something else; its name is not evidence that it IS it: pref-0023 */
	public static boolean isSubordinateNode(SpatialSearchResult r) {
		SpatialSearchResultRef head = r == null ? null : r.getFirstRef();
		if (head == null || !(head.atom.object instanceof Amenity a)) {
			return false;
		}
		String subType = a.getSubType();
		return subType != null && (NAME_ALIKE_SUBTYPES.contains(subType) || NAME_ALIKE_PART_SUBTYPES.contains(subType));
	}
	
	/** higher is better; only meaningful within one bucket of the structural tiers */
	public double score(SpatialSearchResult r, LatLon center) {
		SpatialSearchResultRef head = r.getFirstRef();
		if (head == null) {
			return 0;
		}
		double near = nearScore(r, center);
		double name = nameScore(head);
		// undimmed by distance for what is looked for by name from anywhere: pref-0125, pref-0127
		double exact = name == NAME_EXACT && isNotable(r)
				? wExactName * (isPlace(head) || isProminent(r) ? 1 : near) : 0;
		double type = Math.max(typeScore(head), landmarkByRating(r, near));
		double rating = (isPlace(head) ? wRatingPlace : wRating) * ratingScore(r);
		double far = farPlaceFactor(r, head, name, center);
		return wName * name * near
				+ wType * type * far
				+ rating * far
				+ wNear * near
				+ exact;
	}

	/** a city is looked for by name from anywhere; a town, a village, a hamlet or an area named by a piece
	 *  of its name is not: "farm" in Amsterdam is not 八五九农场 7900 km away. Within PLACE_FAR_FROM_KM
	 *  nothing changes - "rifugio" still finds the village 128 km off: pref-0045, pref-0138 */
	private double farPlaceFactor(SpatialSearchResult r, SpatialSearchResultRef head, double name, LatLon center) {
		if (center == null || name >= NAME_EXACT || !isPlace(head) || isCity(head)) {
			return 1;
		}
		double km = SpatialSearchResult.getDistance(r, center) / 1000.0;
		return km <= PLACE_FAR_FROM_KM ? 1 : 1.0 / (1.0 + (km - PLACE_FAR_FROM_KM) / PLACE_FAR_HALF_KM);
	}

	/** a city by its own place type, however the map stores it - the address index writes towns as cities too */
	private boolean isCity(SpatialSearchResultRef ref) {
		if (ref.atom.object instanceof Amenity a) {
			return "city".equals(a.getSubType());
		}
		return ref.atom.object instanceof City c && c.getType() == City.CityType.CITY;
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
		// any language the object carries, not only the default one: pref-0125
		double best = NAME_OTHER;
		if (atom.object != null) {
			best = Math.max(best, compareToName(atom.object.getName(), queried));
			// alternative names cost a map per result: worth it only for a rated object
			Map<String, String> names = best == NAME_EXACT || atom.elo <= 0 ? null
					: atom.object.getNamesMap(true);
			if (names != null) {
				for (String n : names.values()) {
					best = Math.max(best, compareToName(n, queried));
					if (best == NAME_EXACT) {
						return NAME_EXACT;
					}
				}
			}
		} else {
			best = Math.max(best, compareToName(atom.name, queried));
		}
		return best;
	}

	private double compareToName(String rawName, String queried) {
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
				if (NAME_ALIKE_PART_SUBTYPES.contains(subType)) {
					return TYPE_NAME_ALIKE_PART;
				}
				if (NAME_ALIKE_SUBTYPES.contains(subType)) {
					return TYPE_NAME_ALIKE;
				}
				if (LANDMARK_SUBTYPES.contains(subType)) {
					return TYPE_LANDMARK;
				}
			}
		}
		return TYPE_POI;
	}
	
	/**
	 * How many things the answer is stitched from: one object beats the same words found in two,
	 * which keeps "Dr Lucas" off the 74th row. Two corrections, per RESULT so the comparator stays
	 * transitive: "<object> in <city>" counts as one when the city was named by its own name
	 * (pref-0117), and a node named after what it serves never counts as one (pref-0087).
	 */
	public int answerParts(SpatialSearchResult r) {
		int parts = r.objs.size();
		if (parts == 2) {
			SpatialSearchResultRef ref = r.objs.get(1);
			NameIndexAtom second = ref.atom;
			// named, not reached through an alias ("apple" finds New York): pref-0121
			if ((second.isCity() || second.isCityVillage() || second.isBoundary())
					&& matchesWholeName(ref)) {
				parts = 1;
			}
		}
		if (parts < 2 && isSubordinateNode(r)) {
			parts = 2;
		}
		return parts;
	}

	/** the query said how many and what kind, but never which street: "4 av" is 4th Avenue, not
	 *  house 4 on any avenue. Whether a word only says what kind ("avenue", "sokak", "вулиця") comes
	 *  from the common words of the map that holds the street, not from a list kept here. */
	public boolean kindOnlyAddress(SpatialSearchResult r) {
		SpatialSearchResultRef head = r == null ? null : r.getFirstRef();
		return head != null && head.atom != null && head.atom.isBuilding() && head.atom.distinctFoundCnt == 0;
	}

	/** the query named this object, rather than reaching it through an alias or a category */
	public boolean matchesOwnName(SpatialSearchResultRef ref) {
		return nameScore(ref) >= NAME_PREFIX;
	}

	/** the query spelled the whole name, not the start of a longer one: "rue de la" is not the
	 *  boundary "Rue de la République", and an object behind it is not what the query asked for */
	public boolean matchesWholeName(SpatialSearchResultRef ref) {
		return nameScore(ref) >= NAME_EXACT;
	}

	/** a settlement or an administrative area, however the map happens to store it */
	private boolean isPlace(SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.isCity() || atom.isCityVillage()) {
			return true;
		}
		if (atom.object instanceof Amenity a && a.getSubType() != null) {
			return ADMIN_SUBTYPES.contains(a.getSubType()) || PLACE_SUBTYPES.contains(a.getSubType());
		}
		return false;
	}

	/** a travel rating above the floor: known well enough to be a destination, not a detail */
	public static boolean isProminent(SpatialSearchResult r) {
		return r.getTotalRating() > r.parent.MIN_ELO_RATING;
	}

	/** carries a wikipedia article or a travel rating, so the name is its own, not a coincidence */
	private boolean isNotable(SpatialSearchResult r) {
		if (isProminent(r)) {
			return true;
		}
		MapObject o = r.getFirstRef() == null ? null : r.getFirstRef().atom.object;
		return o instanceof Amenity a && !Algorithms.isEmpty(a.getAdditionalInfo(Amenity.WIKIDATA));
	}

	/** an object famous enough is a landmark whatever its subtype says: The Plaza is not "a hotel" */
	private double landmarkByRating(SpatialSearchResult r, double near) {
		return near >= LANDMARK_NEAR && r.getTotalRating() >= r.parent.MIN_ELO_RATING + LANDMARK_RATING
				? TYPE_LANDMARK : 0;
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

	private String queriedWords(SpatialSearchResultRef ref) {
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
		return normalizeName(sb.toString());
	}

	/** drops the "(district)" suffix deduplication adds, so a street still matches its own name */
	private String normalizeName(String s) {
		int bracket = s == null ? -1 : s.lastIndexOf(" (");
		if (bracket > 0 && s.endsWith(")")) {
			s = s.substring(0, bracket);
		}
		String n = SearchAlgorithms.normalizeToken(SearchAlgorithms.alignChars(s));
		return n == null ? "" : n.trim().toLowerCase();
	}
}

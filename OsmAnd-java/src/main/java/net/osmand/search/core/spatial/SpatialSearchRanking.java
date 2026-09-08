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

	// Ablation over the 60 preferences in spatial_search/preferences.jsonl (zero one weight):
	// all 25 satisfied, no type 17, type alone 24, no rating 24, no name 26, no near 26.
	// name and near are kept despite that - the set has no category query, which is the case
	// they exist for ("farm" must return the nearest farm, not the famous one).
	public double wName = 2.0;
	public double wType = 1.5;
	public double wRating = 2.0;
	public double wNear = 1.5;

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
	private static final double TYPE_STREET = 0.70;
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

	private static final Set<String> LANDMARK_SUBTYPES = new HashSet<>(Arrays.asList(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"university", "hospital", "townhall", "library", "zoo", "peak", "mountain_pass",
			"marketplace", "square", "park", "place_of_worship", "cathedral", "monastery"));

	/** higher is better; only meaningful within one bucket of the structural tiers */
	public double score(SpatialSearchResult r, LatLon center) {
		SpatialSearchResultRef head = r.getFirstRef();
		if (head == null) {
			return 0;
		}
		return wName * nameScore(head)
				+ wType * typeScore(head)
				+ wRating * ratingScore(r)
				+ wNear * nearScore(r, center);
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

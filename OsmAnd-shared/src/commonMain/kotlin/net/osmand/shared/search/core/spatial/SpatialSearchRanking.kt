package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.math.max
import kotlin.math.min

/**
 * One bounded score in place of tiers 4..11 of [SpatialSearchResult.compare], so a large gap
 * on one signal can outweigh a small gap on another. Weights fitted on spatial_search/preferences.jsonl.
 *
 * A copy of `SpatialSearchRanking` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS.
 */
class SpatialSearchRanking {

	// Fitted on spatial_search/preferences.jsonl - see preferences.md for how, and the pref ids
	// below for the judgement each choice answers to.
	var wName = 0.15
	var wType = 2.0
	var wRating = 0.5 // an ordinary POI: pref-0116, pref-0126
	var wRatingPlace = 2.0 // a settlement is looked for from anywhere: pref-0127
	var wNear = 2.0
	var wExactName = 1.0 // the whole name IS the query: pref-0063, pref-0104
	/** "4 av" is 4th Avenue: a house whose street the query named by its kind only loses this much, so a
	 *  house 2 km off falls under the street 6 km off (pref-0136) while the house at the point stays first */
	var wKindOnly = 1.0
	/** tie-break: an object found by a spelling the OBF writer made by a rule ("Hauptstr") falls under the same
	 *  match by a name of OSM */
	var wRuleSpelling = 0.02

	/** distance at which the proximity term is worth half of its maximum */
	var halfWeightKm = 3.0
	/** elo above MIN_ELO_RATING at which the rating term saturates */
	var ratingSpan = 1200.0

	/** higher is better; only meaningful within one bucket of the structural tiers */
	fun score(r: SpatialSearchResult, center: KLatLon?): Double {
		val head = r.getFirstRef() ?: return 0.0
		val km: Double? = if (center == null) null else SpatialSearchResult.getDistance(r, center) / 1000.0
		return score(
			nameScore(head), typeScore(head), subType(head.atom), head.atom, r.getTotalRating(),
			r.parent.MIN_ELO_RATING, km, isNotable(r), queryIsKind(head), kindOnlyAddress(r)
		) - (if (head.atom.ruleSpelling) wRuleSpelling else 0.0)
	}

	/**
	 * The highest score a pre-result can reach once its objects are read, from its index atoms only: any of the
	 * atom POI types, the rating the index keeps, the name taken as exact and notable, a street as a house when
	 * the query has a number.
	 */
	fun prescore(
		res: SpatialPipelineObjectRes, center: KLatLon, poiSearch: SpatialPoiSearch, minElo: Int,
		queryIsKind: Boolean, number: Boolean
	): Double {
		val x = KMapUtils.get31TileNumberX(center.longitude)
		val y = KMapUtils.get31TileNumberY(center.latitude)
		var km = distKm(x, y, res.bbox[0], res.bbox[1], res.bbox[2], res.bbox[3])
		var best = 0.0
		for (a in res.atoms) {
			if (a == null) {
				continue
			}
			// the object lies anywhere in the z16 tile of the atom
			km = min(
				km, distKm(
					x, y, a.coords.x16 shl 15, a.coords.y16 shl 15, (a.coords.x16 + 1) shl 15,
					(a.coords.y16 + 1) shl 15
				)
			)
			// the index keeps (travel_elo - 1000) / 50
			val rating = max(a.elo, if (a.elo > 0) 1000 + (a.elo + 1) * 50 else 0)
			val name = if (a.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) NAME_KIND_ONLY else NAME_EXACT
			val poiTypes = a.poiTypes
			for (i in -1 until (poiTypes?.size() ?: 0)) {
				val pt = if (i < 0) null else poiSearch.getById(poiTypes!![i])
				val subType = pt?.getKey()
				val type = if (number && a.isStreet()) TYPE_BUILDING else typeScore(a, subType)
				best = max(best, score(name, type, subType, a, rating, minElo, km, true, queryIsKind, false))
			}
		}
		return best
	}

	private fun score(
		name: Double, type: Double, subType: String?, atom: NameIndexAtom, rating: Int, minElo: Int,
		km: Double?, notable: Boolean, queryIsKind: Boolean, kindOnly: Boolean
	): Double {
		val near = if (km == null) 0.0 else 1.0 / (1.0 + max(0.0, km) / halfWeightKm)
		val prominent = rating > minElo
		val place = isPlace(atom, subType)
		// undimmed by distance for what is looked for by name from anywhere: pref-0125, pref-0127
		val exact = if (name == NAME_EXACT && (notable || prominent)) wExactName * (if (place || prominent) 1.0 else near) else 0.0
		// an object famous enough is a landmark whatever its subtype says: The Plaza is not "a hotel"
		val landmark = if (near >= LANDMARK_NEAR && rating >= minElo + LANDMARK_RATING) TYPE_LANDMARK else 0.0
		// bounded, so a famous place outranks an unknown one but not a much closer one
		val ratingScore = (if (place) wRatingPlace else wRating) * max(0.0, min(1.0, (rating - minElo) / ratingSpan))
		// a city is looked for by name from anywhere; a town, a village, a hamlet or an area named by a piece
		// of its name is not: "farm" in Amsterdam is not 八五九农场 7900 km away, and neither is an unrated
		// airstrip or park named "... Farm" 230 km off, whatever its landmark type. Within PLACE_FAR_FROM_KM
		// nothing changes - "rifugio" still finds the village 128 km off: pref-0045, pref-0138.
		// An exact place name is no evidence when the query is the name of a kind: "farm" is not the town Farm
		var far = 1.0
		if (km != null && km > PLACE_FAR_FROM_KM && !isCity(atom, subType) && !(name >= NAME_EXACT && !queryIsKind)
			&& (place || type == TYPE_LANDMARK && !prominent)
		) {
			far = 1.0 / (1.0 + (km - PLACE_FAR_FROM_KM) / PLACE_FAR_HALF_KM)
		}
		return wName * name * near +
				wType * max(type, landmark) * far +
				ratingScore * far +
				wNear * near +
				exact -
				(if (kindOnly) wKindOnly else 0.0)
	}

	/** a city by its own place type, however the map stores it - the address index writes towns as cities too */
	private fun isCity(atom: NameIndexAtom, subType: String?): Boolean {
		val obj = atom.`object`
		if (subType != null || obj is Amenity) {
			return "city" == subType
		}
		return if (obj == null) atom.isCity() else obj is City && obj.getType() == CityType.CITY
	}

	/** did the query name this object, or only the word for its kind? */
	fun nameScore(ref: SpatialSearchResultRef): Double {
		val atom = ref.atom
		if (atom.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			// matched through the poi category key: "farm" -> Podere Colombaio
			return NAME_KIND_ONLY
		}
		val queried = queriedWords(ref, false)
		if (queried.isEmpty()) {
			return NAME_OTHER
		}
		// the dot that marks the last word incomplete while typing is not a part of the name: "main." is
		// "... Main"; a dot of the name itself ("о. Пасхи", "2. Sokak") is kept by the first form
		val noDot = queriedWords(ref, true)
		// any language the object carries, not only the default one: pref-0125
		var best = NAME_OTHER
		val obj = atom.`object`
		if (obj != null) {
			best = max(best, compareToName(obj.getName(), queried, noDot))
			// alternative names cost a map per result: worth it only for a rated object
			val names = if (best == NAME_EXACT || atom.elo <= 0) null else obj.getNamesMap(true)
			if (names != null) {
				for (n in names.values) {
					best = max(best, compareToName(n, queried, noDot))
					if (best == NAME_EXACT) {
						return NAME_EXACT
					}
				}
			}
		} else {
			best = max(best, compareToName(atom.name, queried, noDot))
		}
		return best
	}

	private fun compareToName(rawName: String, queried: String, noDot: String): Double {
		val res = compareToName(rawName, queried)
		return if (res == NAME_EXACT || noDot == queried) res else max(res, compareToName(rawName, noDot))
	}

	private fun compareToName(rawName: String, queried: String): Double {
		val name = normalizeName(rawName)
		if (name.isEmpty()) {
			return NAME_OTHER
		}
		if (name == queried) {
			return NAME_EXACT
		}
		if (name.startsWith(queried)) {
			return NAME_PREFIX
		}
		if (name.contains(queried)) {
			return NAME_CONTAINS
		}
		return NAME_OTHER
	}

	fun typeScore(ref: SpatialSearchResultRef): Double = typeScore(ref.atom, subType(ref.atom))

	private fun typeScore(atom: NameIndexAtom, subType: String?): Double {
		if (atom.isPoiCategory()) {
			return TYPE_POI
		}
		if (atom.isCity()) {
			return TYPE_CITY
		}
		if (atom.isCityVillage()) {
			return TYPE_VILLAGE
		}
		if (atom.isBuilding()) {
			return TYPE_BUILDING
		}
		if (atom.isStreet()) {
			return TYPE_STREET
		}
		if (atom.isPostcode()) {
			return TYPE_POSTCODE
		}
		if (atom.isBoundary()) {
			return TYPE_BOUNDARY
		}
		if (subType != null) {
			if (ADMIN_SUBTYPES.contains(subType)) {
				return TYPE_ADMIN
			}
			if (PLACE_SUBTYPES.contains(subType)) {
				return if ("city" == subType || "town" == subType) TYPE_CITY else TYPE_VILLAGE
			}
			if (NAME_ALIKE_PART_SUBTYPES.contains(subType)) {
				return TYPE_NAME_ALIKE_PART
			}
			if (NAME_ALIKE_SUBTYPES.contains(subType)) {
				return TYPE_NAME_ALIKE
			}
			if (LANDMARK_SUBTYPES.contains(subType)) {
				return TYPE_LANDMARK
			}
		}
		return TYPE_POI
	}

	/**
	 * How many things the answer is stitched from: one object beats the same words found in two,
	 * which keeps "Dr Lucas" off the 74th row. Two corrections, per RESULT so the comparator stays
	 * transitive: "<object> in <city>" counts as one when the city was named by its own name
	 * (pref-0117), and a node named after what it serves never counts as one (pref-0087).
	 */
	fun answerParts(r: SpatialSearchResult): Int {
		var parts = r.objs.size
		if (parts == 2) {
			val ref = r.objs[1]
			val second = ref.atom
			// named, not reached through an alias ("apple" finds New York): pref-0121
			if ((second.isCity() || second.isCityVillage() || second.isBoundary())
				&& matchesWholeName(ref) && !isDistrictOfPlace(r.objs[0].atom, second)
			) {
				parts = 1
			}
		}
		if (parts < 2 && isSubordinateNode(r) && !namedByKind(r.getFirstRef()!!)) {
			parts = 2
		}
		return parts
	}

	/**
	 * A place in a district that lies inside it adds nothing to the place: "Stuttgart Hauptbahnhof" is the
	 * station, not the city in its own district "Hauptbahnhof" (pref-0163, pref-0168). A city in its state
	 * ("Portland Oregon") still reads as one answer: the state is not near the city's centre.
	 */
	private fun isDistrictOfPlace(first: NameIndexAtom, second: NameIndexAtom): Boolean {
		if (!second.isBoundary() || !(first.isGeoArea() || isPlace(first, subType(first)))) {
			return false
		}
		val a = first.`object`?.getLocation()
		val b = second.`object`?.getLocation()
		val km = if (a != null && b != null) KMapUtils.getDistance(a, b) / 1000.0
		else KMapUtils.squareRootDist31(
			first.coords.x16 shl 15, first.coords.y16 shl 15,
			second.coords.x16 shl 15, second.coords.y16 shl 15
		) / 1000.0
		return km <= DISTRICT_OF_PLACE_KM
	}

	/** a query word is the name of a POI category ("farm", "furt", "parkplatz") */
	private fun queryIsKind(ref: SpatialSearchResultRef): Boolean {
		for (t in ref.tokens) {
			if (t.hasPoiCategoryKeys()) {
				return true
			}
		}
		return false
	}

	/** the query word is a category of the object: "parkplatz" says what a car park named "Parkplatz Edeka" is */
	private fun namedByKind(ref: SpatialSearchResultRef): Boolean {
		for (t in ref.tokens) {
			val poiTypes = ref.atom.poiTypes
			if (poiTypes != null && t.matchPoiCategoryKeys(poiTypes)) {
				return true
			}
		}
		return false
	}

	/** the query said how many and what kind, but never which street: "4 av" is 4th Avenue, not
	 *  house 4 on any avenue. Whether a word only says what kind ("avenue", "sokak", "вулиця") comes
	 *  from the common words of the map that holds the street, not from a list kept here - and that
	 *  statistic takes frequent street names for kinds too ("centre", "norte"), so this costs a house
	 *  wKindOnly of its score rather than a tier: the house at the point still comes first.
	 *  Only a house that is the whole answer: "76 North Street Waverly" names the street by its city. */
	private fun kindOnlyAddress(r: SpatialSearchResult): Boolean {
		val head = r.getFirstRef()
		return r.objs.size == 1 && head != null && head.atom.isBuilding() && head.atom.distinctFoundCnt == 0
	}

	/** the query named this object, rather than reaching it through an alias or a category */
	fun matchesOwnName(ref: SpatialSearchResultRef): Boolean = nameScore(ref) >= NAME_PREFIX

	/** the query spelled the whole name, not the start of a longer one: "rue de la" is not the
	 *  boundary "Rue de la République", and an object behind it is not what the query asked for */
	fun matchesWholeName(ref: SpatialSearchResultRef): Boolean = nameScore(ref) >= NAME_EXACT

	/** a settlement or an administrative area, however the map happens to store it */
	private fun isPlace(atom: NameIndexAtom, subType: String?): Boolean =
		atom.isCity() || atom.isCityVillage() ||
				subType != null && (ADMIN_SUBTYPES.contains(subType) || PLACE_SUBTYPES.contains(subType))

	/** carries a wikipedia article or a travel rating, so the name is its own, not a coincidence */
	private fun isNotable(r: SpatialSearchResult): Boolean {
		if (isProminent(r)) {
			return true
		}
		val o = r.getFirstRef()?.atom?.`object`
		return o is Amenity && !KAlgorithms.isEmpty(o.getAdditionalInfo(Amenity.WIKIDATA))
	}

	private fun queriedWords(ref: SpatialSearchResultRef, dropIncompleteDot: Boolean): String {
		val tokens = ref.tokens
		if (tokens.isEmpty()) {
			return ""
		}
		val sb = StringBuilder()
		for (t in tokens) {
			val w = if (dropIncompleteDot && t.incomplete && t.word.endsWith(SpatialSearchToken.DOT_INCOMPLETE_STRING))
				t.word.substring(0, t.word.length - 1) else t.word
			if (w.isNotEmpty()) {
				if (sb.isNotEmpty()) {
					sb.append(' ')
				}
				sb.append(w)
			}
		}
		return normalizeName(sb.toString())
	}

	/** drops the "(district)" suffix deduplication adds, so a street still matches its own name */
	private fun normalizeName(name: String): String {
		var s = name
		val bracket = s.lastIndexOf(" (")
		if (bracket > 0 && s.endsWith(")")) {
			s = s.substring(0, bracket)
		}
		val n = KSearchAlgorithms.normalizeToken(KSearchAlgorithms.alignChars(s))
		return n.trim().lowercase()
	}

	companion object {
		// name
		private const val NAME_EXACT = 1.00
		private const val NAME_PREFIX = 0.85
		private const val NAME_CONTAINS = 0.65
		private const val NAME_OTHER = 0.50
		private const val NAME_KIND_ONLY = 0.15

		// type
		private const val TYPE_ADMIN = 1.00 // a country or a region IS the place it names
		private const val TYPE_CITY = 0.95
		private const val TYPE_VILLAGE = 0.88
		private const val TYPE_LANDMARK = 0.80
		private const val TYPE_BUILDING = 0.75
		private const val TYPE_BOUNDARY = 0.60
		/** a boundary whose centre is this close to a place's is a district of that place */
		private const val DISTRICT_OF_PLACE_KM = 15.0
		private const val TYPE_STREET = 0.55 // above a stop, below a village: pref-0106
		private const val TYPE_POI = 0.50
		private const val TYPE_POSTCODE = 0.40
		private const val TYPE_NAME_ALIKE = 0.35
		private const val TYPE_NAME_ALIKE_PART = 0.10

		/** distance from which a town or a village, named by a piece of its name, starts to lose its weight */
		private const val PLACE_FAR_FROM_KM = 60.0
		/** ... and at which, past that, it keeps half of it */
		private const val PLACE_FAR_HALF_KM = 150.0

		/** elo above the floor at which fame alone makes an object a landmark: pref-0134 */
		private const val LANDMARK_RATING = 1000.0
		/** ... but only where a person can go to it - 1/(1+9/3): a famous church 80 km away is not
		 *  the answer to "christian church": pref-0086 */
		private const val LANDMARK_NEAR = 0.25

		/**
		 * Named alike the place they stand at - a stop, a bike dock, a car park called after the street, the square or the
		 * station. Searched by name, such an object is absorbed by the same-named place within 400 m and its weight keeps
		 * that place above it; searched by kind ("parking") each one is a row of its own.
		 */
		internal val NAME_ALIKE_SUBTYPES: Set<String> = hashSetOf(
			"bus_stop", "tram_stop", "railway_halt", "taxi", "bicycle_rental", "parking", "parking_entrance",
			"bicycle_parking"
		)

		/** pieces of what they are named after: the parts a stop or a station is stored as (a metro platform with wi-fi is
		 *  stored as internet access too), the parts and the signs of a street, street furniture */
		internal val NAME_ALIKE_PART_SUBTYPES: Set<String> = hashSetOf(
			"public_transport_platform", "public_transport_stop_position", "subway_entrance", "elevator",
			"ticket_validator", "entrance", "level_crossing", "motorway_junction", "internet_access_yes",
			"bridge", "tunnel", "viaduct", "ford", "highway_steps", "traffic_signals",
			"traffic_calming_bump", "traffic_calming_hump", "traffic_calming_cushion", "traffic_calming_chicane",
			"traffic_calming_rumble_strip", "traffic_calming_table", "traffic_calming_choker", "traffic_calming_island",
			"hazard_children", "hazard_school_zone", "hazard_animal_crossing", "hazard_pedestrians", "hazard_cyclists",
			"hazard_curve", "hazard_curves", "hazard_dangerous_junction", "hazard_slippery_road",
			"boundary_stone", "street_lamp", "waste_basket", "bench", "vending_machine"
		)

		internal val ADMIN_SUBTYPES: Set<String> = hashSetOf(
			"country", "state", "region", "province", "county"
		)

		/** a settlement stored as a POI - the world basemap has New York only so: pref-0127 */
		internal val PLACE_SUBTYPES: Set<String> = hashSetOf(
			"city", "town", "village", "hamlet", "borough"
		)

		/** objects a person travels TO by name; a hospital is a service, not a landmark: pref-0068 */
		internal val LANDMARK_SUBTYPES: Set<String> = hashSetOf(
			"railway_station", "public_transport_station", "bus_station", "aerodrome",
			"castle", "museum", "attraction", "memorial", "monument", "theatre", "stadium",
			"townhall", "zoo", "peak", "mountain_pass", "wiki_place",
			"marketplace", "square", "park", "cathedral", "monastery"
		)

		/** a node describing something else; its name is not evidence that it IS it: pref-0023 */
		fun isSubordinateNode(r: SpatialSearchResult?): Boolean {
			val head = r?.getFirstRef() ?: return false
			val a = head.atom.`object` as? Amenity ?: return false
			val subType = a.getSubType()
			return subType != null && (NAME_ALIKE_SUBTYPES.contains(subType) || NAME_ALIKE_PART_SUBTYPES.contains(subType))
		}

		/** a travel rating above the floor: known well enough to be a destination, not a detail */
		fun isProminent(r: SpatialSearchResult): Boolean = r.getTotalRating() > r.parent.MIN_ELO_RATING

		private fun subType(atom: NameIndexAtom): String? = (atom.`object` as? Amenity)?.getSubType()

		private fun distKm(x: Int, y: Int, left: Int, top: Int, right: Int, bottom: Int): Double =
			KMapUtils.squareRootDist31(x, y, max(left, min(right, x)), max(top, min(bottom, y))) / 1000.0
	}
}

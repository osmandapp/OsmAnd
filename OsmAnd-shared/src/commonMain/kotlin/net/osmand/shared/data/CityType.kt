package net.osmand.shared.data

/**
 * What kind of settlement a `place` tag names, and how big it is taken to be.
 *
 * A copy of `City.CityType` in OsmAnd-java, lifted out of `City`, which belongs to the address
 * section and is not copied yet. An amenity needs it to say which of the administrative areas it
 * sits in is worth showing.
 */
enum class CityType(private val radius: Double, private val population: Int) {

	// that's a tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
	CITY(10000.0, 100000), // 0. City
	TOWN(4000.0, 20000), // 1. Town
	VILLAGE(1300.0, 1000), // 2. Village
	HAMLET(1000.0, 100), // 3. Hamlet - Small village
	SUBURB(1500.0, 5000), // 4. Mostly district of the city (introduced to avoid duplicate streets in city) -
	// however BOROUGH, DISTRICT, NEIGHBOURHOOD could be used as well for that purpose
	// Main difference stores own streets to search and list by it
	// 5.2 stored in city / villages sections written as city type
	BOUNDARY(0.0, 0), // 5. boundary no streets
	// 5.3 stored in city / villages sections written as city type
	POSTCODE(500.0, 1000), // 6. write this could be activated after 5.2 release

	// not stored entities but registered to uniquely identify streets as SUBURB
	BOROUGH(2000.0, 2500),
	DISTRICT(1000.0, 10000),
	NEIGHBOURHOOD(500.0, 500),
	CENSUS(2000.0, 2500);

	fun getRadius(): Double = radius

	fun getPopulation(): Int = population

	fun storedAsSeparateAdminEntity(): Boolean =
		this == CITY || this == TOWN || this == VILLAGE || this == HAMLET || this == SUBURB

	companion object {

		private val VALUES = entries

		fun valueToString(t: CityType): String = t.toString().lowercase()

		// to be used only by amenity
		fun valueFromString(place: String?): CityType? {
			if (place == null) {
				return null
			}
			if ("township" == place) {
				return TOWN
			}
			if ("allotments" == place) {
				return SUBURB
			}
			for (type in VALUES) {
				if (type != BOUNDARY && type != POSTCODE && type.name.equals(place, ignoreCase = true)) {
					return type
				}
			}
			return null
		}
	}
}

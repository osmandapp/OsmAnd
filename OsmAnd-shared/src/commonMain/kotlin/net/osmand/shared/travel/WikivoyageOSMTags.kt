package net.osmand.shared.travel

/**
 * The osm tags a wikivoyage point carries over into the gpx it is written to.
 *
 * A copy of `net.osmand.wiki.WikivoyageOSMTags` in OsmAnd-java.
 */
enum class WikivoyageOSMTags(private val tg: String) {

	TAG_WIKIDATA("wikidata"),
	TAG_WIKIPEDIA("wikipedia"),
	TAG_OPENING_HOURS("opening_hours"),
	TAG_ADDRESS("address"),
	TAG_EMAIL("email"),
	TAG_FAX("fax"),
	TAG_DIRECTIONS("directions"),
	TAG_PRICE("price"),
	TAG_PHONE("phone");

	fun tag(): String = tg

	companion object {

		fun contains(string: String?): Boolean {
			for (tag in entries) {
				if (tag.tg == string) {
					return true
				}
			}
			return false
		}
	}
}

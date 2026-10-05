package net.osmand.shared.travel

import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import kotlin.math.abs

/**
 * What names an article across the files it may appear in: where it came from, where it is, and
 * the route id it was written with.
 *
 * A copy of `TravelArticle.TravelArticleIdentifier` in the android app, lifted out of its outer
 * class and without `Parcelable`, which is how android passes it between screens.
 *
 * Coordinates are compared with a tolerance, because the same route read out of two files does not
 * land on exactly the same point.
 */
class TravelArticleIdentifier(
	val file: KFile?,
	val lat: Double,
	val lon: Double,
	val title: String?,
	val routeId: String?,
	val routeSource: String?
) {

	var wikidata: String? = null

	internal constructor(article: TravelArticle) : this(
		article.file, article.lat, article.lon, article.title, article.routeId, article.routeSource
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null || other !is TravelArticleIdentifier) {
			return false
		}
		return areLatLonEqual(other.lat, other.lon, lat, lon) &&
				file == other.file &&
				KAlgorithms.stringsEqual(routeId, other.routeId) &&
				KAlgorithms.stringsEqual(routeSource, other.routeSource)
	}

	override fun hashCode(): Int = KAlgorithms.hash(file, lat, lon, routeId, routeSource)

	companion object {

		/** Within about a metre, and two missing coordinates count as equal. */
		fun areLatLonEqual(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
			val latEqual = (lat1.isNaN() && lat2.isNaN()) || abs(lat1 - lat2) < 0.00001
			val lonEqual = (lon1.isNaN() && lon2.isNaN()) || abs(lon1 - lon2) < 0.00001
			return latEqual && lonEqual
		}
	}
}

package net.osmand.shared.routing.details

/**
 * Renderer-independent input prepared exactly like Android
 * `RouteStatisticsHelper.searchRenderingAttribute()`.
 *
 * Platform implementations evaluate [attributeName] with [mainTag], [mainValue], and [additional]
 * against their current renderer, then their default renderer. Renderer objects remain outside
 * common code.
 */
data class RouteAttributeClassificationRequest(
	val attributeName: String,
	val mainTag: String?,
	val mainValue: String?,
	val additional: String,
)

/** Exact string and color values returned by a platform rendering attribute search. */
data class RouteAttributeClassification(
	val propertyName: String?,
	val color: Int,
)

/** Platform boundary for rendering-rule lookup used by shared route-statistics calculations. */
interface RouteAttributeClassifier {
	fun classify(request: RouteAttributeClassificationRequest): RouteAttributeClassification?
}

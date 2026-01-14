package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.filters.TrackFolderAnalysis

interface ComparableTracksGroup {
	fun getFolderAnalysis(): TrackFolderAnalysis
	fun getDirName(includingSubdirs: Boolean): String
	fun lastModified(): Long
	fun getDefaultOrder(): Int = -1

	/**
	 * Returns the metric used for sorting in VALUE_ASCENDING and VALUE_DESCENDING modes.
	 *
	 * The [Double] type is used as a unified format to support disparate metrics:
	 * - **Timestamps** (e.g. creation date) which are originally [Long] and exceed [Int] range.
	 * - **Physical quantities** (e.g. distance, altitude) which are typically [Double] or [Float].
	 *
	 * Defaults to `0.0` for groups that do not support these sorting modes.
	 */
	fun getComparisonValue(): Double = 0.0
}
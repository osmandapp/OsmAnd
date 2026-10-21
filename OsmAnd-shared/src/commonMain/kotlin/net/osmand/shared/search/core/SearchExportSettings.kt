package net.osmand.shared.search.core

/**
 * What a search collects besides its results, for the tests that are generated from it.
 *
 * A copy of `SearchExportSettings` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS.
 */
class SearchExportSettings {
	private val exportEmptyCities: Boolean
	private val exportBuildings: Boolean
	private val maxDistance: Double

	constructor() {
		exportEmptyCities = true
		exportBuildings = true
		maxDistance = -1.0
	}

	constructor(exportEmptyCities: Boolean, exportBuildings: Boolean, maxDistance: Double) {
		this.exportEmptyCities = exportEmptyCities
		this.exportBuildings = exportBuildings
		this.maxDistance = maxDistance
	}

	fun isExportEmptyCities(): Boolean = exportEmptyCities

	fun isExportBuildings(): Boolean = exportBuildings

	fun getMaxDistance(): Double = maxDistance
}

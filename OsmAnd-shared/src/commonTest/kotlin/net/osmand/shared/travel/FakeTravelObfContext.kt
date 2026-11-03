package net.osmand.shared.travel

import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.data.KLatLon

/** The platform side of [TravelObfHelper], with the files and the map position fixed by a test. */
class FakeTravelObfContext(
	private val wikivoyage: List<AmenityIndexRepository> = emptyList(),
	private val travelGpx: List<AmenityIndexRepository> = emptyList(),
	private val language: String = "en",
	private val mapLocation: KLatLon? = KLatLon(50.0, 14.0)
) : TravelObfContext {

	override fun getWikivoyageRepositories(): List<AmenityIndexRepository> = wikivoyage

	override fun getTravelGpxRepositories(): List<AmenityIndexRepository> = travelGpx

	override fun getLanguage(): String = language

	override fun getMapLocation(): KLatLon? = mapLocation

	override fun getAppVersion(): String = "OsmAnd test"
}

package net.osmand.plus.plugins.fuelprices.gb

import net.osmand.map.WorldRegion
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.fuelprices.FuelPriceProvider
import net.osmand.plus.plugins.fuelprices.ProviderPrices
import net.osmand.plus.utils.AndroidNetworkUtils
import net.osmand.util.MapUtils
import java.io.File
import java.io.IOException

class FuelFinderProvider(private val app: OsmandApplication) : FuelPriceProvider {

	companion object {
		private const val REFRESH_MS = 30 * 60 * 1000L
		private const val RETRY_MS = 5 * 60 * 1000L
	}

	override val name = "Fuel Finder"
	override val countryRegionId = WorldRegion.UNITED_KINGDOM_REGION_ID
	override val currency = "GBP"
	override val refTag = "ref:GB:fuel_finder"

	private val file = File(app.cacheDir, "fuel_prices/fuel_finder.csv")
	private var stations = emptyList<ProviderPrices>()
	private var stationsFileTime = 0L
	private var lastDownloadAttempt = 0L

	@Synchronized
	override fun stationByRef(ref: String): ProviderPrices? =
		loadStations().firstOrNull { it.id == ref }

	@Synchronized
	override fun stationsNear(lat: Double, lon: Double, radiusMeters: Double): List<ProviderPrices> =
		loadStations().filter { MapUtils.getDistance(lat, lon, it.lat, it.lon) <= radiusMeters }

	private fun loadStations(): List<ProviderPrices> {
		val now = System.currentTimeMillis()
		if (now - file.lastModified() > REFRESH_MS && now - lastDownloadAttempt > RETRY_MS) {
			lastDownloadAttempt = now
			download()
		}
		if (!file.exists()) {
			throw IOException("No Fuel Finder prices downloaded yet")
		}
		if (file.lastModified() != stationsFileTime) {
			stationsFileTime = file.lastModified()
			stations = file.bufferedReader().use { FuelFinderParser.parse(it) }
		}
		return stations
	}

	private fun download() {
		val tmp = File(file.parentFile, file.name + ".tmp")
		if (AndroidNetworkUtils.downloadFile(
			"https://www.fuel-finder.service.gov.uk/internal/v1.0.2/csv/get-latest-fuel-prices-csv",
			tmp, false, null) == null) {
				tmp.renameTo(file)
		}
	}
}

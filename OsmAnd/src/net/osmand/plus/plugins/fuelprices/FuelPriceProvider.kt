package net.osmand.plus.plugins.fuelprices

import androidx.annotation.StringRes
import net.osmand.plus.R

enum class FuelType(@StringRes val labelId: Int) {
	E10(R.string.fuel_prices_type_e10),
	E5(R.string.fuel_prices_type_e5),
	DIESEL(R.string.fuel_prices_type_diesel),
	PREMIUM_DIESEL(R.string.fuel_prices_type_premium_diesel),
	B10(R.string.fuel_prices_type_b10),
	HVO(R.string.fuel_prices_type_hvo),
}

data class FuelStation(
	val ref: String?,
	val lat: Double,
	val lon: Double,
	val countryRegionId: String?,
)

interface FuelPriceProvider {
	val name: String
	val countryRegionId: String
	val currency: String
	val refTag: String?

	fun stationByRef(ref: String): ProviderPrices?
	fun stationsNear(lat: Double, lon: Double, radiusMeters: Double): List<ProviderPrices>
}

data class ProviderPrices(
	val id: String,
	val lat: Double,
	val lon: Double,
	val prices: Map<FuelType, Double>,
	val updatedAtMillis: Long,
)


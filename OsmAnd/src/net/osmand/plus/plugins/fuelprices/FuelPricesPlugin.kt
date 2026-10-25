package net.osmand.plus.plugins.fuelprices

import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.data.Amenity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.mapcontextmenu.BuildRowAttrs
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.mapcontextmenu.MenuController
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.fuelprices.gb.FuelFinderProvider
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Currency
import java.util.concurrent.Executors

class FuelPricesPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	private val providers = listOf<FuelPriceProvider>(FuelFinderProvider(app))
	private val executor = Executors.newSingleThreadExecutor()

	override fun getId(): String = OsmAndCustomizationConstants.PLUGIN_FUEL_PRICES

	override fun getName(): String = app.getString(R.string.fuel_prices_plugin_name)

	override fun getDescription(linksEnabled: Boolean): CharSequence =
		app.getString(R.string.fuel_prices_plugin_description)

	override fun getLogoResourceId(): Int = R.drawable.ic_action_fuel

	override fun isMenuControllerSupported(menuController: MenuController): Boolean =
		menuController.builder.amenity?.takeIf { it.subType == "fuel" }
			?.let { providerFor(toStation(it).countryRegionId) } != null

	override fun buildContextMenuRows(
		menuBuilder: MenuBuilder, view: View, `object`: Any?, amenity: Amenity?
	) {
		val station = amenity?.let { toStation(it) } ?: return
		val provider = providerFor(station.countryRegionId) ?: return
		val row = menuBuilder.buildRow(
			view, BuildRowAttrs.Builder()
				.setIcon(menuBuilder.getRowIcon(R.drawable.ic_action_fuel))
				.setTextPrefix(app.getString(R.string.fuel_prices_title))
				.setText(app.getString(R.string.fuel_prices_loading))
				.build()
		)
		val rowRef = WeakReference(row)
		fetch(provider, station) { source ->
			val textView = rowRef.get()?.findViewById<TextView>(R.id.text) ?: return@fetch
			textView.text = source?.let { formatPrices(it, provider.currency) }
				?: app.getString(R.string.fuel_prices_unavailable)
		}
	}

	private fun providerFor(countryRegionId: String?): FuelPriceProvider? =
		providers.firstOrNull { it.countryRegionId == countryRegionId }

	private fun fetch(provider: FuelPriceProvider, station: FuelStation, callback: (ProviderPrices?) -> Unit) {
		executor.execute {
			val result = try {
				station.ref?.let { provider.stationByRef(it) }
					?: provider.stationsNear(station.lat, station.lon, 75.0).singleOrNull()
			} catch (e: Exception) {
				null
			}
			app.runInUIThread { callback(result) }
		}
	}

	private fun countryRegionId(lat: Double, lon: Double): String? {
		return try {
			app.regions.getRegionsToDownload(lat, lon, ArrayList())
				.firstNotNullOfOrNull { app.regions.getCountryRegionDataByDownloadName(it)?.regionId }
		} catch (e: IOException) {
			null
		}
	}

	private fun toStation(amenity: Amenity): FuelStation {
		val countryRegionId = countryRegionId(amenity.location.latitude, amenity.location.longitude)
		return FuelStation(
			lat = amenity.location.latitude,
			lon = amenity.location.longitude,
			countryRegionId = countryRegionId,
			ref = providerFor(countryRegionId)?.refKey?.let { amenity.getAdditionalInfo(it) }, // TODO
		)
	}

	private fun formatPrices(source: ProviderPrices, currency: String): String {
		val format = NumberFormat.getCurrencyInstance().apply {
			this.currency = Currency.getInstance(currency)
			minimumFractionDigits = 3
			maximumFractionDigits = 3
		}
		val lines = FuelType.entries.mapNotNull { type ->
			source.prices[type]?.let { "${app.getString(type.labelId)}: ${format.format(it)}" }
		}
		if (source.updatedAtMillis <= 0) {
			return lines.joinToString("\n")
		}
		val updated = DateUtils.getRelativeTimeSpanString(
			source.updatedAtMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
		)
		return (lines + app.getString(R.string.fuel_prices_updated, updated)).joinToString("\n")
	}
}

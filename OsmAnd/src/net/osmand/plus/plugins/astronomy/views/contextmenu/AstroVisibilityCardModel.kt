package net.osmand.plus.plugins.astronomy.views.contextmenu

import net.osmand.data.LatLon
import net.osmand.plus.GeocodingLookupService
import io.github.cosinekitty.astronomy.Observer
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.util.Algorithms
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class AstroVisibilityCardModel(app: OsmandApplication) : AstroContextCard(app) {

	var skyObject: SkyObject? = null
	var observer: Observer? = null
	var date: LocalDate = LocalDate.now()
	var zoneId: ZoneId = ZoneId.systemDefault()

	var riseTime: String? = null
	var culminationTime: String? = null
	var setTime: String? = null
	var locationText: String = ""
	var culminationColor: Int = 0
	var onDataChanged: (() -> Unit)? = null

	private var lastLocationLatLon: LatLon? = null
	private var locationLookupRequest: GeocodingLookupService.AddressLookupRequest? = null
	private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

	fun updateCard(
		skyObject: SkyObject?,
		observer: Observer?,
		date: LocalDate,
		zoneId: ZoneId,
	) {
		this.skyObject = skyObject
		this.observer = observer
		this.date = date
		this.zoneId = zoneId

		if (skyObject == null || observer == null) {
			cancelPendingLookups()
			riseTime = null
			culminationTime = null
			setTime = null
			locationText = ""
			culminationColor = 0
			return
		}

		val startLocal = date.atTime(12, 0).atZone(zoneId)
		val endLocal = startLocal.plusDays(1)
		val (rise, set) = AstroUtils.nextRiseSet(
			skyObject,
			startLocal,
			observer,
			startLocal,
			endLocal
		)
		val culmination = AstroChartMath.findCulmination(skyObject, observer, startLocal, endLocal)

		riseTime = rise?.format(timeFormatter)
		culminationTime = culmination.time?.format(timeFormatter)
		setTime = set?.format(timeFormatter)
		culminationColor = AstroChartColorPalette.fromContext(app)
			.colorForObjectAltitude(culmination.altitude ?: 0.0)

		val location = resolveLocationTarget()
		lastLocationLatLon = location
		locationText = formatCoordinates(location.latitude, location.longitude)
		requestAddress(location)
	}

	fun cancelPendingLookups() {
		locationLookupRequest?.let { request ->
			app.geocodingLookupService.cancel(request)
		}
		locationLookupRequest = null
	}

	private fun resolveLocationTarget(): LatLon {
		val useCurrentLocation = app.mapViewTrackingUtilities.isMapLinkedToLocation
		val knownLocation = if (useCurrentLocation) app.locationProvider.lastKnownLocation else null
		return if (knownLocation != null) {
			LatLon(knownLocation.latitude, knownLocation.longitude)
		} else {
			val center = app.osmandMap.mapView.currentRotatedTileBox.centerLatLon
			LatLon(center.latitude, center.longitude)
		}
	}

	private fun requestAddress(latLon: LatLon) {
		cancelPendingLookups()

		var createdRequest: GeocodingLookupService.AddressLookupRequest? = null
		val resultCallback = GeocodingLookupService.OnAddressLookupResult { address ->
			if (locationLookupRequest !== createdRequest) {
				return@OnAddressLookupResult
			}
			val currentLocation = lastLocationLatLon ?: latLon
			val coords = formatCoordinates(currentLocation.latitude, currentLocation.longitude)
			locationText = if (Algorithms.isEmpty(address)) coords else "$address ($coords)"
			onDataChanged?.invoke()
		}
		val progressCallback = GeocodingLookupService.OnAddressLookupProgress {

		}
		createdRequest =
			GeocodingLookupService.AddressLookupRequest(latLon, resultCallback, progressCallback)
		locationLookupRequest = createdRequest
		app.geocodingLookupService.lookupAddress(createdRequest)
	}

	private fun formatCoordinates(latitude: Double, longitude: Double): String {
		val latDir =
			app.getString(if (latitude >= 0.0) R.string.north_abbreviation else R.string.south_abbreviation)
		val lonDir =
			app.getString(if (longitude >= 0.0) R.string.east_abbreviation else R.string.west_abbreviation)
		return String.format(
			Locale.US,
			"%.2f° %s, %.2f° %s",
			abs(latitude),
			latDir,
			abs(longitude),
			lonDir
		)
	}
}

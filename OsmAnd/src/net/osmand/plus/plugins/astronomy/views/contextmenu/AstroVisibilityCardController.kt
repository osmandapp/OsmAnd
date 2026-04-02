package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.text.format.DateFormat
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter
import net.osmand.data.Amenity
import net.osmand.data.City.CityType
import net.osmand.data.LatLon
import net.osmand.plus.GeocodingLookupService
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.osm.PoiCategory
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class AstroVisibilityCardController(
	private val app: OsmandApplication
) {

	companion object {
		private const val CITY_SEARCH_RADIUS_METERS = 50 * 1000
	}

	var skyObject: SkyObject? = null
		private set
	var observer: Observer? = null
		private set
	var date: LocalDate = LocalDate.now()
		private set
	var zoneId: ZoneId = ZoneId.systemDefault()
		private set

	var riseTime: String? = null
		private set
	var culminationTime: String? = null
		private set
	var setTime: String? = null
		private set
	var locationText: String = ""
		private set
	var culminationColor: Int = 0
		private set
	var titleText: String = app.getString(R.string.astro_today_visibility)
		private set
	var showResetButton: Boolean = false
		private set
	var cursorReferenceTimeMillis: Long = 0L
		private set
	var onDataChanged: (() -> Unit)? = null

	private var lastLocationLatLon: LatLon? = null
	private var locationLookupRequest: GeocodingLookupService.AddressLookupRequest? = null
	private var locationResolveJob: Job? = null
	private var graphSnapshot: AstroVisibilityGraphSnapshot? = null
	private var graphObjectId: String? = null
	private var graphObserverLat = Double.NaN
	private var graphObserverLon = Double.NaN
	private var graphObserverHeight = Double.NaN
	private var computeScope = createScope()
	private var computeJob: Job? = null
	private val titleDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
	private val cityTypes = CityType.entries.associateBy { it.name.lowercase(Locale.ROOT) }
	private val cityFilter = object : SearchPoiTypeFilter {
		override fun accept(type: PoiCategory, subcategory: String): Boolean {
			return cityTypes.containsKey(subcategory)
		}

		override fun isEmpty(): Boolean {
			return false
		}
	}

	fun update(
		skyObject: SkyObject?,
		observer: Observer?,
		date: LocalDate,
		zoneId: ZoneId,
		cursorReferenceTimeMillis: Long,
		isTodayVisibility: Boolean
	) {
		this.skyObject = skyObject
		this.observer = observer
		this.date = date
		this.zoneId = zoneId
		this.cursorReferenceTimeMillis = cursorReferenceTimeMillis
		titleText = if (isTodayVisibility) {
			app.getString(R.string.astro_today_visibility)
		} else {
			titleDateFormatter.format(date)
		}
		showResetButton = !isTodayVisibility

		if (skyObject == null || observer == null) {
			cancelPendingWork()
			riseTime = null
			culminationTime = null
			setTime = null
			locationText = ""
			culminationColor = 0
			graphSnapshot = null
			graphObjectId = null
			graphObserverLat = Double.NaN
			graphObserverLon = Double.NaN
			graphObserverHeight = Double.NaN
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
		val timeFormatter = createTimeFormatter()

		riseTime = rise?.format(timeFormatter)
		culminationTime = culmination.time?.format(timeFormatter)
		setTime = set?.format(timeFormatter)
		culminationColor = AstroChartColorPalette.fromContext(app)
			.colorForObjectAltitude(culmination.altitude ?: 0.0)
		maybeRecomputeGraph(skyObject, observer, date, zoneId)

		val location = resolveLocationTarget()
		val locationChanged = lastLocationLatLon != location
		lastLocationLatLon = location
		if (locationChanged) {
			requestLocationText(location)
		} else if (Algorithms.isEmpty(locationText) && locationLookupRequest == null && locationResolveJob == null) {
			requestLocationText(location)
		}
	}

	fun buildItem(): AstroVisibilityCardItem? {
		if (skyObject == null || observer == null) {
			return null
		}
		return AstroVisibilityCardItem(
			graph = graphSnapshot,
			cursorReferenceTimeMillis = cursorReferenceTimeMillis,
			riseTime = riseTime,
			culminationTime = culminationTime,
			setTime = setTime,
			locationText = locationText,
			culminationColor = culminationColor,
			titleText = titleText,
			showResetButton = showResetButton
		)
	}

	fun cancelPendingWork() {
		computeJob?.cancel()
		computeJob = null
		cancelPendingLookups()
	}

	private fun cancelPendingLookups() {
		locationResolveJob?.cancel()
		locationResolveJob = null
		locationLookupRequest?.let { request ->
			app.geocodingLookupService.cancel(request)
		}
		locationLookupRequest = null
	}

	private fun maybeRecomputeGraph(
		skyObject: SkyObject,
		observer: Observer,
		date: LocalDate,
		zoneId: ZoneId
	) {
		val graphStartMillis = date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
		val graphMatchesState = graphSnapshot?.let {
			it.zoneId == zoneId &&
				it.startMillis == graphStartMillis &&
				graphObjectId == skyObject.id &&
				graphObserverLat == observer.latitude &&
				graphObserverLon == observer.longitude &&
				graphObserverHeight == observer.height
		} == true
		if (graphMatchesState) {
			return
		}
		computeJob?.cancel()
		ensureScope()
		computeJob = computeScope.launch {
			val snapshot = withContext(Dispatchers.Default) {
				computeGraphSnapshot(skyObject, observer, date, zoneId)
			}
			if (!isActive) {
				return@launch
			}
			if (
				this@AstroVisibilityCardController.skyObject?.id != skyObject.id ||
				this@AstroVisibilityCardController.observer?.latitude != observer.latitude ||
				this@AstroVisibilityCardController.observer?.longitude != observer.longitude ||
				this@AstroVisibilityCardController.observer?.height != observer.height ||
				this@AstroVisibilityCardController.date != date ||
				this@AstroVisibilityCardController.zoneId != zoneId
			) {
				return@launch
			}
			if (graphSnapshot != snapshot) {
				graphSnapshot = snapshot
			}
			graphObjectId = skyObject.id
			graphObserverLat = observer.latitude
			graphObserverLon = observer.longitude
			graphObserverHeight = observer.height
			onDataChanged?.invoke()
		}
	}

	private fun computeGraphSnapshot(
		skyObject: SkyObject,
		observer: Observer,
		date: LocalDate,
		zoneId: ZoneId
	): AstroVisibilityGraphSnapshot {
		val startLocal = date.atTime(12, 0).atZone(zoneId)
		val endLocal = startLocal.plusDays(1)
		val samples = AstroChartMath.computeDaySamples(
			objectToRender = skyObject,
			observer = observer,
			startLocal = startLocal,
			endLocal = endLocal,
			sampleCount = AstroChartMath.VISIBILITY_SAMPLE_COUNT,
			includeAzimuth = true
		)
		return AstroVisibilityGraphSnapshot(
			startMillis = samples.startMillis,
			endMillis = samples.endMillis,
			zoneId = zoneId,
			objectAltitudes = samples.objectAltitudes,
			objectAzimuths = samples.objectAzimuths ?: DoubleArray(samples.objectAltitudes.size),
			sunAltitudes = samples.sunAltitudes
		)
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

	private fun requestLocationText(latLon: LatLon) {
		cancelPendingLookups()
		ensureScope()
		locationResolveJob = computeScope.launch {
			val coords = formatCoordinates(latLon.latitude, latLon.longitude)
			val hasDetailedMap = withContext(Dispatchers.Default) {
				hasDetailedMap(latLon)
			}
			if (!isActive || lastLocationLatLon != latLon) {
				return@launch
			}
			if (hasDetailedMap) {
				locationResolveJob = null
				requestAddress(latLon)
				return@launch
			}
			val resolvedText = withContext(Dispatchers.Default) {
				findNearestBasemapCity(latLon)?.let { nearbyCity ->
					formatNearbyCity(nearbyCity)
				} ?: coords
			}
			if (!isActive || lastLocationLatLon != latLon) {
				return@launch
			}
			updateLocationText(resolvedText)
			locationResolveJob = null
		}
	}

	private fun requestAddress(latLon: LatLon) {
		var createdRequest: GeocodingLookupService.AddressLookupRequest? = null
		val resultCallback = GeocodingLookupService.OnAddressLookupResult { address ->
			if (locationLookupRequest !== createdRequest) {
				return@OnAddressLookupResult
			}
			val currentLocation = lastLocationLatLon ?: latLon
			val coords = formatCoordinates(currentLocation.latitude, currentLocation.longitude)
			val resolvedText = extractCity(address) ?: coords
			updateLocationText(resolvedText)
			locationLookupRequest = null
		}
		val progressCallback = GeocodingLookupService.OnAddressLookupProgress {

		}
		createdRequest =
			GeocodingLookupService.AddressLookupRequest(latLon, resultCallback, progressCallback)
		locationLookupRequest = createdRequest
		app.geocodingLookupService.lookupAddress(createdRequest)
	}

	private fun hasDetailedMap(latLon: LatLon): Boolean {
		val x31 = MapUtils.get31TileNumberX(latLon.longitude)
		val y31 = MapUtils.get31TileNumberY(latLon.latitude)
		for (resource in app.resourceManager.fileReaders) {
			val shallowReader = resource.shallowReader ?: continue
			if (!shallowReader.isBasemap &&
				shallowReader.containsMapData(
					x31,
					y31,
					x31,
					y31,
					BinaryMapIndexReader.DETAILED_MAP_MIN_ZOOM
				)
			) {
				return true
			}
		}
		return false
	}

	private fun findNearestBasemapCity(latLon: LatLon): NearbyCity? {
		val cities = searchBasemapCities(latLon)
		if (cities.isEmpty()) {
			return null
		}
		sortCities(cities, latLon)
		val city = cities.first()
		val lang = app.settings.MAP_PREFERRED_LOCALE.get()
		val transliterate = app.settings.MAP_TRANSLITERATE_NAMES.get()
		val cityName = city.getName(lang, transliterate).trim()
			.ifEmpty { city.name.trim() }
			.ifEmpty { return null }
		return NearbyCity(
			name = cityName,
			distanceMeters = MapUtils.getDistance(latLon, city.location)
		)
	}

	private fun searchBasemapCities(latLon: LatLon): MutableList<Amenity> {
		val rect = MapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, CITY_SEARCH_RADIUS_METERS)
		val top31 = MapUtils.get31TileNumberY(rect.top)
		val left31 = MapUtils.get31TileNumberX(rect.left)
		val bottom31 = MapUtils.get31TileNumberY(rect.bottom)
		val right31 = MapUtils.get31TileNumberX(rect.right)
		val closedAmenities = HashSet<Long>()
		val cities = ArrayList<Amenity>()
		val repositories = app.resourceManager.amenitySearcher.getAmenityRepositories(false, null)

		for (repository in repositories) {
			if (!repository.isWorldMap || !repository.checkContainsInt(top31, left31, bottom31, right31)) {
				continue
			}
			val foundAmenities = repository.searchAmenities(
				top31,
				left31,
				bottom31,
				right31,
				-1,
				cityFilter,
				null,
				null
			) ?: continue
			for (amenity in foundAmenities) {
				val amenityId = amenity.id
				if (amenity.isClosed) {
					if (amenityId != null) {
						closedAmenities.add(amenityId)
					}
				} else if (amenityId == null || !closedAmenities.contains(amenityId)) {
					cities.add(amenity)
				}
			}
		}
		return cities
	}

	private fun sortCities(cities: MutableList<Amenity>, latLon: LatLon) {
		cities.sortWith { first, second ->
			val firstRadius = cityTypes[first.subType]?.radius ?: 1000.0
			val secondRadius = cityTypes[second.subType]?.radius ?: 1000.0
			val firstDistance = MapUtils.getDistance(latLon, first.location) / firstRadius
			val secondDistance = MapUtils.getDistance(latLon, second.location) / secondRadius
			firstDistance.compareTo(secondDistance)
		}
	}

	private fun formatNearbyCity(city: NearbyCity): String {
		val formattedDistance = OsmAndFormatter.getFormattedDistance(
			city.distanceMeters.toFloat(),
			app,
			OsmAndFormatterParams.NO_TRAILING_ZEROS
		)
		return "${city.name} ($formattedDistance)"
	}

	private fun updateLocationText(resolvedText: String) {
		if (locationText != resolvedText) {
			locationText = resolvedText
			onDataChanged?.invoke()
		}
	}

	private fun createTimeFormatter(): DateTimeFormatter {
		return if (DateFormat.is24HourFormat(app)) {
			DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
		} else {
			DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
		}
	}

	private fun extractCity(address: String?): String? {
		if (Algorithms.isEmpty(address)) {
			return null
		}
		val nearPrefix = "${app.getString(R.string.shared_string_near)} "
		var normalized = address!!.trim()
		if (normalized.startsWith(nearPrefix)) {
			normalized = normalized.removePrefix(nearPrefix).trim()
		}
		return normalized.substringAfterLast(',', normalized).trim().takeIf { it.isNotEmpty() }
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

	private fun createScope(): CoroutineScope {
		return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	}

	private fun ensureScope() {
		val scopeJob = computeScope.coroutineContext[Job]
		if (scopeJob == null || !scopeJob.isActive) {
			computeScope = createScope()
		}
	}

	private data class NearbyCity(
		val name: String,
		val distanceMeters: Double
	)
}

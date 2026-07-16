package net.osmand.plus.plugins.astronomy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.GlobalSolarEclipseInfo
import io.github.cosinekitty.astronomy.GlobalSolarEclipseWindow
import io.github.cosinekitty.astronomy.LunarEclipseInfo
import io.github.cosinekitty.astronomy.LunarEclipseMapFrame
import io.github.cosinekitty.astronomy.LunarEclipseState
import io.github.cosinekitty.astronomy.LunarEclipseWindow
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.SolarEclipseMapFrame
import io.github.cosinekitty.astronomy.SolarEclipseMapTrack
import io.github.cosinekitty.astronomy.SolarEclipseShadowPoint
import io.github.cosinekitty.astronomy.SolarEclipseState
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.globalSolarEclipseWindow
import io.github.cosinekitty.astronomy.lunarEclipseMapFrame
import io.github.cosinekitty.astronomy.lunarEclipseState
import io.github.cosinekitty.astronomy.lunarEclipseWindow
import io.github.cosinekitty.astronomy.nextGlobalSolarEclipse
import io.github.cosinekitty.astronomy.nextLunarEclipse
import io.github.cosinekitty.astronomy.previousGlobalSolarEclipse
import io.github.cosinekitty.astronomy.previousLunarEclipse
import io.github.cosinekitty.astronomy.searchGlobalSolarEclipse
import io.github.cosinekitty.astronomy.searchLunarEclipse
import io.github.cosinekitty.astronomy.solarEclipseMapFrame
import io.github.cosinekitty.astronomy.solarEclipseMapTrack
import io.github.cosinekitty.astronomy.solarEclipseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.PlatformUtil
import java.util.Calendar
import java.util.TimeZone

enum class SolarEclipseNavigationDirection {
	Initial,
	Previous,
	Next
}

data class SolarEclipseModeState(
	val active: Boolean = false,
	val requestId: Long = 0L,
	val loading: Boolean = false,
	val observer: Observer? = null,
	val event: GlobalSolarEclipseInfo? = null,
	val window: GlobalSolarEclipseWindow? = null,
	val selectedTime: Time? = null,
	val localState: SolarEclipseState? = null,
	val error: Boolean = false,
	val navigationDirection: SolarEclipseNavigationDirection = SolarEclipseNavigationDirection.Initial,
	val trackLoading: Boolean = false,
	val track: SolarEclipseMapTrack? = null,
	val trackError: Boolean = false,
	val mapFrame: SolarEclipseMapFrame? = null,
	val mapRequestId: Long = 0L,
	val mapLoading: Boolean = false,
	val shadowPoint: SolarEclipseShadowPoint? = null,
	val mapError: Boolean = false
)

enum class LunarEclipseNavigationDirection {
	Initial,
	Previous,
	Next
}

data class LunarEclipseModeState(
	val active: Boolean = false,
	val requestId: Long = 0L,
	val loading: Boolean = false,
	val observer: Observer? = null,
	val event: LunarEclipseInfo? = null,
	val window: LunarEclipseWindow? = null,
	val selectedTime: Time? = null,
	val localState: LunarEclipseState? = null,
	val localStateObserver: Observer? = null,
	val error: Boolean = false,
	val navigationDirection: LunarEclipseNavigationDirection = LunarEclipseNavigationDirection.Initial,
	val mapFrame: LunarEclipseMapFrame? = null,
	val mapRequestId: Long = 0L,
	val mapLoading: Boolean = false,
	val fitFrame: LunarEclipseMapFrame? = null,
	val mapError: Boolean = false
)

class StarObjectsViewModel(
	private val app: Application,
	private val settings: AstronomyPluginSettings,
	private val dataProvider: AstroDataProvider,
) : AndroidViewModel(app) {

	private val _skyObjects = MutableLiveData<List<SkyObject>>()
	val skyObjects: LiveData<List<SkyObject>> = _skyObjects

	private val _constellations = MutableLiveData<List<Constellation>>()
	val constellations: LiveData<List<Constellation>> = _constellations

	private val _currentTime = MutableLiveData<Time>()
	val currentTime: LiveData<Time> = _currentTime

	// Using Calendar for UI interaction, converted to Time for internal logic
	private val _currentCalendar = MutableLiveData<Calendar>()
	val currentCalendar: LiveData<Calendar> = _currentCalendar

	private val _isTimeAutoUpdateEnabled = MutableLiveData(true)
	val isTimeAutoUpdateEnabled: LiveData<Boolean> = _isTimeAutoUpdateEnabled

	private val _solarEclipseModeState = MutableLiveData(SolarEclipseModeState())
	val solarEclipseModeState: LiveData<SolarEclipseModeState> = _solarEclipseModeState
	private val _lunarEclipseModeState = MutableLiveData(LunarEclipseModeState())
	val lunarEclipseModeState: LiveData<LunarEclipseModeState> = _lunarEclipseModeState

	private var eclipseSearchJob: Job? = null
	private var eclipseLocalStateJob: Job? = null
	private var eclipseTrackJob: Job? = null
	private var eclipseFrameJob: Job? = null
	private var eclipseRequestId = 0L
	private var eclipseMapRequestId = 0L
	private var eclipseLocalRequestId = 0L
	private var eclipseFrameRequestId = 0L
	private var pendingEclipseLocalRequest: LocalEclipseRequest? = null
	private var pendingEclipseTrackRequest: TrackRequest? = null
	private var pendingEclipseFrameTime: Time? = null
	private val eclipseTrackCache = mutableMapOf<Double, SolarEclipseMapTrack>()
	private var lunarEclipseSearchJob: Job? = null
	private var lunarEclipseStateJob: Job? = null
	private var lunarEclipseFrameJob: Job? = null
	private var lunarEclipseRequestId = 0L
	private var lunarEclipseStateRequestId = 0L
	private var lunarEclipseFrameRequestId = 0L
	private var lunarEclipseMapRequestId = 0L
	private var pendingLunarEclipseStateRequest: LunarStateRequest? = null
	private var pendingLunarEclipseFrameTime: Time? = null

	class Factory(
		private val application: Application,
		private val settings: AstronomyPluginSettings,
		private val dataProvider: AstroDataProvider
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return StarObjectsViewModel(application, settings, dataProvider) as T
		}
	}

	init {
		loadData()
		resetTime()
	}

	fun loadData() {
		viewModelScope.launch(Dispatchers.Default) {
			val starMapConfig = settings.getStarMapConfig()
			val favorites = starMapConfig.favorites
			val directions = starMapConfig.directions
			val celestialPaths = starMapConfig.celestialPaths
			val favoritesMap = favorites.associateBy { it.id }
			val directionsMap = directions.associateBy { it.id }
			val celestialPathsMap = celestialPaths.associateBy { it.id }
			val indexMap = favorites.withIndex().associate { it.value.id to it.index }

			fun SkyObject.withUserConfig(): SkyObject {
				return SkyObject(
					id = id,
					hip = hip,
					catalogs = catalogs,
					wid = wid,
					centerWId = centerWId,
					type = type,
					body = body,
					name = name,
					ra = ra,
					dec = dec,
					magnitude = magnitude,
					color = color,
					radius = radius,
					distance = distance,
					mass = mass,
					localizedName = localizedName,
					azimuth = azimuth,
					altitude = altitude,
					distAu = distAu,
					isFavorite = favoritesMap.containsKey(id),
					showDirection = directionsMap.containsKey(id),
					showCelestialPath = celestialPathsMap.containsKey(id),
					colorIndex = directionsMap[id]?.colorIndex ?: 0,
					startAzimuth = startAzimuth,
					startAltitude = startAltitude,
					targetAzimuth = targetAzimuth,
					targetAltitude = targetAltitude,
					lastUpdateTime = lastUpdateTime
				)
			}

			val objects = dataProvider.getSkyObjects(app).map { it.withUserConfig() }.toMutableList()
			val constellations = dataProvider.getConstellations(app).map { constellation ->
				constellation.copy().apply {
					isFavorite = favoritesMap.containsKey(id)
					showDirection = directionsMap.containsKey(id)
					showCelestialPath = celestialPathsMap.containsKey(id)
					colorIndex = directionsMap[id]?.colorIndex ?: 0
				}
			}.toMutableList()
			objects.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }
			constellations.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }

			_skyObjects.postValue(objects)
			_constellations.postValue(constellations)
		}
	}

	fun updateTime(calendar: Calendar) {
		_currentCalendar.value = calendar.clone() as Calendar

		// Convert to Astronomy Time (UTC)
		val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
		utcCal.timeInMillis = calendar.timeInMillis

		val t = Time(
			utcCal.get(Calendar.YEAR),
			utcCal.get(Calendar.MONTH) + 1,
			utcCal.get(Calendar.DAY_OF_MONTH),
			utcCal.get(Calendar.HOUR_OF_DAY),
			utcCal.get(Calendar.MINUTE),
			0.0
		)
		_currentTime.value = t
	}

	fun updateTime(time: Time) {
		_currentTime.value = time
		_currentCalendar.value = Calendar.getInstance(TimeZone.getDefault()).apply {
			timeInMillis = time.toMillisecondsSince1970()
		}
	}

	fun resetTime() {
		val now = Calendar.getInstance(TimeZone.getDefault())
		updateTime(now)
	}

	fun setTimeAutoUpdateEnabled(enabled: Boolean) {
		_isTimeAutoUpdateEnabled.value = enabled
	}

	fun enterSolarEclipseMode(observer: Observer, displayedTime: Time) {
		searchSolarEclipse(
			observer,
			SolarEclipseNavigationDirection.Initial
		) {
			var event = searchGlobalSolarEclipse(displayedTime.addDays(-INITIAL_SEARCH_LOOKBACK_DAYS))
			var window = globalSolarEclipseWindow(event)
			if (displayedTime.ut > window.end.ut) {
				event = nextGlobalSolarEclipse(event.peak)
				window = globalSolarEclipseWindow(event)
			}
			val selectedTime = if (displayedTime.ut in window.start.ut..window.end.ut) {
				displayedTime
			} else {
				event.peak
			}
			SearchResult(event, window, selectedTime)
		}
	}

	fun loadPreviousSolarEclipse() {
		val state = _solarEclipseModeState.value ?: return
		if (state.loading) return
		val current = state.event ?: return
		val observer = state.observer ?: return
		searchSolarEclipse(observer, SolarEclipseNavigationDirection.Previous) {
			val event = previousGlobalSolarEclipse(current.peak)
			SearchResult(event, globalSolarEclipseWindow(event), event.peak)
		}
	}

	fun loadNextSolarEclipse() {
		val state = _solarEclipseModeState.value ?: return
		if (state.loading) return
		val current = state.event ?: return
		val observer = state.observer ?: return
		searchSolarEclipse(observer, SolarEclipseNavigationDirection.Next) {
			val event = nextGlobalSolarEclipse(current.peak)
			SearchResult(event, globalSolarEclipseWindow(event), event.peak)
		}
	}

	fun retrySolarEclipseSearch(displayedTime: Time) {
		val state = _solarEclipseModeState.value ?: return
		val observer = state.observer ?: return
		when (state.navigationDirection) {
			SolarEclipseNavigationDirection.Previous -> loadPreviousSolarEclipse()
			SolarEclipseNavigationDirection.Next -> loadNextSolarEclipse()
			SolarEclipseNavigationDirection.Initial -> enterSolarEclipseMode(observer, displayedTime)
		}
	}

	fun selectSolarEclipseTime(time: Time) {
		val state = _solarEclipseModeState.value ?: return
		if (!state.active || state.window == null) return
		eclipseMapRequestId++
		val clamped = Time(time.ut.coerceIn(state.window.start.ut, state.window.end.ut))
		updateTime(clamped)
		_solarEclipseModeState.value = state.copy(
			selectedTime = clamped,
			mapLoading = false,
			shadowPoint = null,
			mapError = false
		)
		calculateLocalEclipseState(state.observer, clamped)
		calculateSolarEclipseMapFrame(clamped)
	}

	fun updateSolarEclipseObserver(observer: Observer) {
		val state = _solarEclipseModeState.value ?: return
		if (!state.active || sameObserver(state.observer, observer)) return
		_solarEclipseModeState.value = state.copy(observer = observer)
		calculateLocalEclipseState(observer, state.selectedTime)
	}

	fun requestSolarEclipseShadowPoint() {
		val state = _solarEclipseModeState.value ?: return
		val time = state.selectedTime ?: return
		val window = state.window ?: return
		if (!state.active || state.mapLoading || time.ut !in window.start.ut..window.end.ut) return

		val requestId = ++eclipseMapRequestId
		val readyPoint = state.mapFrame
			?.takeIf { it.time.ut == time.ut }
			?.shadowPoint
		if (readyPoint != null) {
			_solarEclipseModeState.value = state.copy(
				mapRequestId = requestId,
				mapLoading = false,
				shadowPoint = readyPoint,
				mapError = false
			)
			return
		}
		_solarEclipseModeState.value = state.copy(
			mapRequestId = requestId,
			mapLoading = true,
			shadowPoint = null,
			mapError = false
		)
		if (eclipseFrameJob?.isActive != true) {
			calculateSolarEclipseMapFrame(time)
		}
	}

	fun consumeSolarEclipseShadowPoint(mapRequestId: Long) {
		val state = _solarEclipseModeState.value ?: return
		if (state.mapRequestId == mapRequestId && (state.shadowPoint != null || state.mapError)) {
			_solarEclipseModeState.value = state.copy(shadowPoint = null, mapError = false)
		}
	}

	fun exitSolarEclipseMode() {
		eclipseRequestId++
		eclipseMapRequestId++
		eclipseLocalRequestId++
		eclipseFrameRequestId++
		pendingEclipseLocalRequest = null
		pendingEclipseTrackRequest = null
		pendingEclipseFrameTime = null
		eclipseSearchJob?.cancel()
		eclipseLocalStateJob?.cancel()
		eclipseTrackJob?.cancel()
		eclipseFrameJob?.cancel()
		_solarEclipseModeState.value = SolarEclipseModeState()
	}

	fun enterLunarEclipseMode(observer: Observer, displayedTime: Time) {
		searchLunarEclipse(
			observer,
			LunarEclipseNavigationDirection.Initial
		) {
			var event = searchLunarEclipse(displayedTime.addDays(-INITIAL_SEARCH_LOOKBACK_DAYS))
			var window = lunarEclipseWindow(event)
			if (displayedTime.ut > window.p4.ut) {
				event = nextLunarEclipse(event.peak)
				window = lunarEclipseWindow(event)
			}
			val selectedTime = if (displayedTime.ut in window.p1.ut..window.p4.ut) {
				displayedTime
			} else {
				event.peak
			}
			LunarSearchResult(event, window, selectedTime)
		}
	}

	fun loadPreviousLunarEclipse() {
		val state = _lunarEclipseModeState.value ?: return
		if (state.loading) return
		val current = state.event ?: return
		val observer = state.observer ?: return
		searchLunarEclipse(observer, LunarEclipseNavigationDirection.Previous) {
			val event = previousLunarEclipse(current.peak)
			LunarSearchResult(event, lunarEclipseWindow(event), event.peak)
		}
	}

	fun loadNextLunarEclipse() {
		val state = _lunarEclipseModeState.value ?: return
		if (state.loading) return
		val current = state.event ?: return
		val observer = state.observer ?: return
		searchLunarEclipse(observer, LunarEclipseNavigationDirection.Next) {
			val event = nextLunarEclipse(current.peak)
			LunarSearchResult(event, lunarEclipseWindow(event), event.peak)
		}
	}

	fun retryLunarEclipseSearch(displayedTime: Time) {
		val state = _lunarEclipseModeState.value ?: return
		val observer = state.observer ?: return
		when (state.navigationDirection) {
			LunarEclipseNavigationDirection.Previous -> loadPreviousLunarEclipse()
			LunarEclipseNavigationDirection.Next -> loadNextLunarEclipse()
			LunarEclipseNavigationDirection.Initial -> enterLunarEclipseMode(observer, displayedTime)
		}
	}

	fun selectLunarEclipseTime(time: Time) {
		val state = _lunarEclipseModeState.value ?: return
		val window = state.window ?: return
		if (!state.active) return
		lunarEclipseMapRequestId++
		val clamped = Time(time.ut.coerceIn(window.p1.ut, window.p4.ut))
		_lunarEclipseModeState.value = state.copy(
			selectedTime = clamped,
			mapLoading = false,
			fitFrame = null,
			mapError = false
		)
		calculateLunarEclipseState(state.observer, clamped)
		calculateLunarEclipseMapFrame(clamped)
	}

	fun updateLunarEclipseObserver(observer: Observer) {
		val state = _lunarEclipseModeState.value ?: return
		if (!state.active || sameObserver(state.observer, observer)) return
		_lunarEclipseModeState.value = state.copy(observer = observer)
		calculateLunarEclipseState(observer, state.selectedTime)
	}

	fun requestLunarEclipseVisibilityFit() {
		val state = _lunarEclipseModeState.value ?: return
		val time = state.selectedTime ?: return
		if (!state.active || state.mapLoading) return
		val requestId = ++lunarEclipseMapRequestId
		val readyFrame = state.mapFrame?.takeIf { it.time.ut == time.ut }
		if (readyFrame != null) {
			_lunarEclipseModeState.value = state.copy(
				mapRequestId = requestId,
				mapLoading = false,
				fitFrame = readyFrame,
				mapError = false
			)
			return
		}
		_lunarEclipseModeState.value = state.copy(
			mapRequestId = requestId,
			mapLoading = true,
			fitFrame = null,
			mapError = false
		)
		if (lunarEclipseFrameJob?.isActive != true) {
			calculateLunarEclipseMapFrame(time)
		}
	}

	fun consumeLunarEclipseVisibilityFit(mapRequestId: Long) {
		val state = _lunarEclipseModeState.value ?: return
		if (state.mapRequestId == mapRequestId && (state.fitFrame != null || state.mapError)) {
			_lunarEclipseModeState.value = state.copy(fitFrame = null, mapError = false)
		}
	}

	fun exitLunarEclipseMode() {
		lunarEclipseRequestId++
		lunarEclipseStateRequestId++
		lunarEclipseFrameRequestId++
		lunarEclipseMapRequestId++
		pendingLunarEclipseStateRequest = null
		pendingLunarEclipseFrameTime = null
		lunarEclipseSearchJob?.cancel()
		lunarEclipseStateJob?.cancel()
		lunarEclipseFrameJob?.cancel()
		_lunarEclipseModeState.value = LunarEclipseModeState()
	}

	private fun searchLunarEclipse(
		observer: Observer,
		direction: LunarEclipseNavigationDirection,
		searchBlock: () -> LunarSearchResult
	) {
		val requestId = ++lunarEclipseRequestId
		lunarEclipseSearchJob?.cancel()
		lunarEclipseStateRequestId++
		lunarEclipseFrameRequestId++
		lunarEclipseMapRequestId++
		pendingLunarEclipseStateRequest = null
		pendingLunarEclipseFrameTime = null
		val previous = _lunarEclipseModeState.value ?: LunarEclipseModeState()
		_lunarEclipseModeState.value = previous.copy(
			active = true,
			requestId = requestId,
			loading = true,
			observer = observer,
			error = false,
			navigationDirection = direction,
			mapFrame = null,
			mapLoading = false,
			fitFrame = null,
			mapError = false
		)
		lunarEclipseSearchJob = viewModelScope.launch {
			try {
				val result = withContext(Dispatchers.Default) { searchBlock() }
				if (requestId != lunarEclipseRequestId) return@launch
				_isTimeAutoUpdateEnabled.value = false
				updateTime(result.selectedTime)
				val currentObserver = _lunarEclipseModeState.value?.observer ?: observer
				_lunarEclipseModeState.value = LunarEclipseModeState(
					active = true,
					requestId = requestId,
					observer = currentObserver,
					event = result.event,
					window = result.window,
					selectedTime = result.selectedTime,
					navigationDirection = direction
				)
				calculateLunarEclipseState(currentObserver, result.selectedTime)
				calculateLunarEclipseMapFrame(result.selectedTime)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				LOG.error("Unable to calculate a lunar eclipse", e)
				if (requestId == lunarEclipseRequestId) {
					val latest = _lunarEclipseModeState.value ?: LunarEclipseModeState()
					_lunarEclipseModeState.value = latest.copy(
						active = true,
						loading = false,
						error = true,
						navigationDirection = direction
					)
				}
			}
		}
	}

	private fun calculateLunarEclipseState(observer: Observer?, time: Time?) {
		if (observer == null || time == null) return
		pendingLunarEclipseStateRequest = LunarStateRequest(
			requestId = ++lunarEclipseStateRequestId,
			observer = observer,
			time = time
		)
		if (lunarEclipseStateJob?.isActive == true) return
		lunarEclipseStateJob = viewModelScope.launch {
			while (true) {
				val request = pendingLunarEclipseStateRequest ?: break
				pendingLunarEclipseStateRequest = null
				try {
					val lunarState = withContext(Dispatchers.Default) {
						lunarEclipseState(request.time, request.observer)
					}
					if (request.requestId != lunarEclipseStateRequestId) continue
					val latest = _lunarEclipseModeState.value ?: continue
					if (!latest.active || latest.selectedTime?.ut != request.time.ut ||
						!sameObserver(latest.observer, request.observer)) continue
					if (_currentTime.value?.ut != request.time.ut) updateTime(request.time)
					_lunarEclipseModeState.value = latest.copy(
						localState = lunarState,
						localStateObserver = request.observer
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					LOG.error("Unable to calculate local lunar eclipse state", e)
				}
			}
		}
	}

	private fun calculateLunarEclipseMapFrame(time: Time) {
		++lunarEclipseFrameRequestId
		pendingLunarEclipseFrameTime = time
		if (lunarEclipseFrameJob?.isActive == true) return
		lunarEclipseFrameJob = viewModelScope.launch {
			while (true) {
				val requestedTime = pendingLunarEclipseFrameTime ?: break
				pendingLunarEclipseFrameTime = null
				val requestId = lunarEclipseFrameRequestId
				try {
					val frame = withContext(Dispatchers.Default) {
						lunarEclipseMapFrame(requestedTime)
					}
					if (requestId != lunarEclipseFrameRequestId) continue
					val latest = _lunarEclipseModeState.value ?: continue
					if (!latest.active || latest.selectedTime?.ut != requestedTime.ut) continue
					val mapRequested = latest.mapLoading
					_lunarEclipseModeState.value = latest.copy(
						mapFrame = frame,
						mapLoading = false,
						fitFrame = if (mapRequested) frame else latest.fitFrame,
						mapError = false
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					LOG.error("Unable to calculate lunar eclipse visibility", e)
					val latest = _lunarEclipseModeState.value ?: continue
					if (latest.mapLoading && latest.selectedTime?.ut == requestedTime.ut) {
						_lunarEclipseModeState.value = latest.copy(mapLoading = false, mapError = true)
					}
				}
			}
		}
	}

	private fun searchSolarEclipse(
		observer: Observer,
		direction: SolarEclipseNavigationDirection,
		searchBlock: () -> SearchResult
	) {
		val requestId = ++eclipseRequestId
		eclipseSearchJob?.cancel()
		eclipseLocalRequestId++
		pendingEclipseLocalRequest = null
		eclipseFrameRequestId++
		pendingEclipseFrameTime = null
		pendingEclipseTrackRequest = null
		eclipseMapRequestId++
		val previous = _solarEclipseModeState.value ?: SolarEclipseModeState()
		_solarEclipseModeState.value = previous.copy(
			active = true,
			requestId = requestId,
			loading = true,
			observer = observer,
			error = false,
			navigationDirection = direction,
			shadowPoint = null,
			mapLoading = false,
			mapError = false,
			trackLoading = false,
			track = null,
			trackError = false,
			mapFrame = null
		)

		eclipseSearchJob = viewModelScope.launch {
			try {
				val result = withContext(Dispatchers.Default) { searchBlock() }
				if (requestId != eclipseRequestId) return@launch

				_isTimeAutoUpdateEnabled.value = false
				updateTime(result.selectedTime)
				val currentObserver = _solarEclipseModeState.value?.observer ?: observer
				_solarEclipseModeState.value = SolarEclipseModeState(
					active = true,
					requestId = requestId,
					observer = currentObserver,
					event = result.event,
					window = result.window,
					selectedTime = result.selectedTime,
					navigationDirection = direction,
					trackLoading = true
				)
				calculateLocalEclipseState(currentObserver, result.selectedTime)
				calculateSolarEclipseTrack(requestId, result.window)
				calculateSolarEclipseMapFrame(result.selectedTime)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				LOG.error("Unable to calculate a global solar eclipse", e)
				if (requestId == eclipseRequestId) {
					val latest = _solarEclipseModeState.value ?: SolarEclipseModeState()
					_solarEclipseModeState.value = latest.copy(
						active = true,
						loading = false,
						error = true,
						navigationDirection = direction
					)
				}
			}
		}
	}

	private fun calculateSolarEclipseTrack(requestId: Long, window: GlobalSolarEclipseWindow) {
		val cached = eclipseTrackCache[window.event.peak.ut]
		if (cached != null) {
			val state = _solarEclipseModeState.value ?: return
			if (state.requestId == requestId && state.event?.peak?.ut == window.event.peak.ut) {
				_solarEclipseModeState.value = state.copy(
					trackLoading = false,
					track = cached,
					trackError = false
				)
			}
			return
		}
		pendingEclipseTrackRequest = TrackRequest(requestId, window)
		if (eclipseTrackJob?.isActive == true) return
		eclipseTrackJob = viewModelScope.launch {
			while (true) {
				val request = pendingEclipseTrackRequest ?: break
				pendingEclipseTrackRequest = null
				try {
					val track = withContext(Dispatchers.Default) {
						solarEclipseMapTrack(request.window)
					}
					val latest = _solarEclipseModeState.value ?: continue
					if (latest.requestId != request.requestId ||
						latest.event?.peak?.ut != request.window.event.peak.ut
					) {
						continue
					}
					eclipseTrackCache[request.window.event.peak.ut] = track
					while (eclipseTrackCache.size > ECLIPSE_TRACK_CACHE_SIZE) {
						eclipseTrackCache.remove(eclipseTrackCache.keys.first())
					}
					_solarEclipseModeState.value = latest.copy(
						trackLoading = false,
						track = track,
						trackError = false
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					LOG.error("Unable to calculate a solar eclipse map track", e)
					val latest = _solarEclipseModeState.value ?: continue
					if (latest.requestId == request.requestId) {
						_solarEclipseModeState.value = latest.copy(
							trackLoading = false,
							trackError = true
						)
					}
				}
			}
		}
	}

	private fun calculateSolarEclipseMapFrame(time: Time) {
		++eclipseFrameRequestId
		pendingEclipseFrameTime = time
		if (eclipseFrameJob?.isActive == true) return
		eclipseFrameJob = viewModelScope.launch {
			while (true) {
				val requestedTime = pendingEclipseFrameTime ?: break
				pendingEclipseFrameTime = null
				val requestId = eclipseFrameRequestId
				val includeFootprint =
					_solarEclipseModeState.value?.event?.kind == EclipseKind.Partial
				try {
					val frame = withContext(Dispatchers.Default) {
						solarEclipseMapFrame(requestedTime, includeFootprint)
					}
					if (requestId != eclipseFrameRequestId) continue
					val latest = _solarEclipseModeState.value ?: continue
					if (!latest.active || latest.selectedTime?.ut != requestedTime.ut) continue
					val mapRequested = latest.mapLoading
					_solarEclipseModeState.value = latest.copy(
						mapFrame = frame,
						mapLoading = false,
						shadowPoint = if (mapRequested) frame.shadowPoint else latest.shadowPoint,
						mapError = if (mapRequested) frame.shadowPoint == null else latest.mapError
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					LOG.error("Unable to calculate a solar eclipse map frame", e)
					val latest = _solarEclipseModeState.value ?: continue
					if (latest.mapLoading && latest.selectedTime?.ut == requestedTime.ut) {
						_solarEclipseModeState.value = latest.copy(mapLoading = false, mapError = true)
					}
				}
			}
		}
	}

	private fun calculateLocalEclipseState(observer: Observer?, time: Time?) {
		if (observer == null || time == null) return
		pendingEclipseLocalRequest = LocalEclipseRequest(
			requestId = ++eclipseLocalRequestId,
			observer = observer,
			time = time
		)
		if (eclipseLocalStateJob?.isActive == true) return
		eclipseLocalStateJob = viewModelScope.launch {
			while (true) {
				val request = pendingEclipseLocalRequest ?: break
				pendingEclipseLocalRequest = null
				try {
					val localState = withContext(Dispatchers.Default) {
						solarEclipseState(request.time, request.observer)
					}
					if (request.requestId != eclipseLocalRequestId) continue
					val latest = _solarEclipseModeState.value ?: continue
					if (!latest.active || latest.selectedTime?.ut != request.time.ut ||
						!sameObserver(latest.observer, request.observer)) continue
					_solarEclipseModeState.value = latest.copy(localState = localState)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					LOG.error("Unable to calculate local solar eclipse state", e)
				}
			}
		}
	}

	fun refreshSkyObjects() {
		_skyObjects.value = _skyObjects.value
		_constellations.value = _constellations.value
	}

	companion object {
		private val LOG = PlatformUtil.getLog(StarObjectsViewModel::class.java)
		private const val INITIAL_SEARCH_LOOKBACK_DAYS = 1.0
		private const val ECLIPSE_TRACK_CACHE_SIZE = 8
	}

	private data class SearchResult(
		val event: GlobalSolarEclipseInfo,
		val window: GlobalSolarEclipseWindow,
		val selectedTime: Time
	)

	private data class LunarSearchResult(
		val event: LunarEclipseInfo,
		val window: LunarEclipseWindow,
		val selectedTime: Time
	)

	private data class LocalEclipseRequest(
		val requestId: Long,
		val observer: Observer,
		val time: Time
	)

	private data class TrackRequest(
		val requestId: Long,
		val window: GlobalSolarEclipseWindow
	)

	private data class LunarStateRequest(
		val requestId: Long,
		val observer: Observer,
		val time: Time
	)

	private fun sameObserver(first: Observer?, second: Observer?): Boolean =
		first != null && second != null &&
			first.latitude == second.latitude &&
			first.longitude == second.longitude &&
			first.height == second.height
}

package net.osmand.plus.plugins.astronomy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.cosinekitty.astronomy.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

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
			val objects = dataProvider.getSkyObjects(app).toMutableList()
			val constellations = dataProvider.getConstellations(app).toMutableList()
			val starMapConfig = settings.getStarMapConfig()
			val favorites = starMapConfig.favorites
			val directions = starMapConfig.directions
			val celestialPaths = starMapConfig.celestialPaths
			val favoritesMap = favorites.associateBy { it.id }
			val directionsMap = directions.associateBy { it.id }
			val celestialPathsMap = celestialPaths.associateBy { it.id }
			val indexMap = favorites.withIndex().associate { it.value.id to it.index }

			fun applyConfig(obj: SkyObject) {
				obj.isFavorite = favoritesMap.contains(obj.id)
				obj.showDirection = directionsMap.contains(obj.id)
				obj.colorIndex = directionsMap[obj.id]?.colorIndex ?: 0
				obj.showCelestialPath = celestialPathsMap.contains(obj.id)
			}
			objects.forEach(::applyConfig)
			constellations.forEach(::applyConfig)
			objects.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }
			constellations.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }

			_skyObjects.postValue(objects)
			_constellations.postValue(constellations)
		}
	}

	fun updateTime(calendar: Calendar) {
		_currentCalendar.value = calendar

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

	fun resetTime() {
		val now = Calendar.getInstance(TimeZone.getDefault())
		updateTime(now)
	}

	fun setTimeAutoUpdateEnabled(enabled: Boolean) {
		_isTimeAutoUpdateEnabled.value = enabled
	}

	fun refreshSkyObjects() {
		_skyObjects.value = _skyObjects.value
		_constellations.value = _constellations.value
	}
}

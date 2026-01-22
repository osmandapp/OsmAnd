package net.osmand.plus.plugins.astro

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
	private val settings: StarWatcherSettings,
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

	class Factory(
		private val application: Application,
		private val settings: StarWatcherSettings,
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
			val favorites = settings.getStarMapConfig().favorites
			// Create lookup map for config items
			val favoritesMap = favorites.associateBy { it.id }
			val indexMap = favorites.withIndex().associate { it.value.id to it.index }

			objects.forEach { obj ->
				obj.isFavorite = favoritesMap.contains(obj.id)
			}
			objects.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }

			val constellations = dataProvider.getConstellations(app).toMutableList()

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

	fun refreshSkyObjects() {
		val objects = _skyObjects.value ?: return
		_skyObjects.value = objects
	}
}
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
import net.osmand.plus.plugins.astro.views.SkyObject
import java.util.Calendar
import java.util.TimeZone

open class StarObjectsViewModel(
	private val app: Application,
	private val settings: StarWatcherSettings,
	val viewType: StarObjectsViewType
) : AndroidViewModel(app) {

	enum class StarObjectsViewType { MAP, CHART }

	private val _skyObjects = MutableLiveData<List<SkyObject>>()
	val skyObjects: LiveData<List<SkyObject>> = _skyObjects

	private val _currentTime = MutableLiveData<Time>()
	val currentTime: LiveData<Time> = _currentTime

	// Using Calendar for UI interaction, converted to Time for internal logic
	private val _currentCalendar = MutableLiveData<Calendar>()
	val currentCalendar: LiveData<Calendar> = _currentCalendar

	init {
		loadData()
		resetTime()
	}

	fun loadData() {
		viewModelScope.launch(Dispatchers.Default) {
			val objects = AstroDataProvider.getInitialSkyObjects(app).toMutableList()
			val items = if (viewType == StarObjectsViewType.MAP)
				settings.getStarMapConfig().items else settings.getStarChartConfig().items
			// Create lookup map for config items
			val itemMap = items.associateBy { it.id }
			val indexMap = items.withIndex().associate { it.value.id to it.index }

			objects.forEach { obj ->
				val itemConfig = itemMap[obj.id]
				obj.isVisible = itemConfig?.isVisible ?: false
			}
			objects.sortBy { indexMap[it.id] ?: Int.MAX_VALUE }

			_skyObjects.postValue(objects)
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
}

class StarMapObjectsViewModel(app: Application, settings: StarWatcherSettings)
	: StarObjectsViewModel(app, settings, StarObjectsViewType.MAP) {

	class Factory(
		private val application: Application,
		private val settings: StarWatcherSettings,
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return StarObjectsViewModel(application, settings, StarObjectsViewType.MAP) as T
		}
	}
}

class StarChartObjectsViewModel(app: Application, settings: StarWatcherSettings)
	: StarObjectsViewModel(app, settings, StarObjectsViewType.CHART) {

	class Factory(
		private val application: Application,
		private val settings: StarWatcherSettings,
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return StarObjectsViewModel(application, settings, StarObjectsViewType.CHART) as T
		}
	}
}
package net.osmand.plus.plugins.astro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cosinekitty.astronomy.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.osmand.plus.plugins.astro.views.SkyObject
import java.util.Calendar
import java.util.TimeZone

class StarMapViewModel(private val app: Application) : AndroidViewModel(app) {

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

	private fun loadData() {
		viewModelScope.launch(Dispatchers.Default) {
			val objects = AstroDataProvider.getInitialSkyObjects(app)
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
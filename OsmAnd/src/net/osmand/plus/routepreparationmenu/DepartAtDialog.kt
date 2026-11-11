package net.osmand.plus.routepreparationmenu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.text.format.DateFormat
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import java.util.Calendar

/**
 * Asks for the date and time to depart at and stores it in the transport routing helper
 * (null means now). The time is entered in the time zone of the device.
 */
class DepartAtDialog(private val mapActivity: MapActivity) {

	private val transportRoutingHelper = mapActivity.app.transportRoutingHelper

	fun show(onChanged: Runnable) {
		val current = Calendar.getInstance()
		transportRoutingHelper.departureTimeSec?.let { current.timeInMillis = it * 1000 }
		val dateDialog = DatePickerDialog(mapActivity, { _, year, month, day ->
			showTimeDialog(year, month, day, current, onChanged)
		}, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH))
		dateDialog.setButton(DialogInterface.BUTTON_NEUTRAL, mapActivity.getString(R.string.depart_now)) { _, _ ->
			transportRoutingHelper.departureTimeSec = null
			onChanged.run()
		}
		dateDialog.show()
	}

	private fun showTimeDialog(year: Int, month: Int, day: Int, current: Calendar, onChanged: Runnable) {
		TimePickerDialog(mapActivity, { _, hour, minute ->
			val picked = Calendar.getInstance()
			picked.clear()
			picked.set(year, month, day, hour, minute, 0)
			transportRoutingHelper.departureTimeSec = picked.timeInMillis / 1000
			onChanged.run()
		}, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), DateFormat.is24HourFormat(mapActivity)).show()
	}
}

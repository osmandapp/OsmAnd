package net.osmand.plus.helpers

import android.hardware.SensorManager
import java.util.function.Consumer

private const val LIGHT_MARGIN_LUX = 20f

internal class AmbientLightMapTheme(
	private val schedule: Consumer<Runnable>,
	private val cancel: Consumer<Runnable>,
	private val onChange: Consumer<Boolean>
) {
	private var night: Boolean? = null
	private var pending: Runnable? = null

	fun onLight(lux: Float) {
		if (!lux.isFinite() || lux < 0) return

		val current = night
		if (current == null) {
			setNight(lux <= SensorManager.LIGHT_CLOUDY)
			return
		}

		// Leave a band around the initial threshold so small changes at dusk keep the map stable.
		val shouldChange = if (current) {
			lux > SensorManager.LIGHT_CLOUDY + LIGHT_MARGIN_LUX
		} else {
			lux < SensorManager.LIGHT_CLOUDY - LIGHT_MARGIN_LUX
		}
		if (!shouldChange) {
			cancelPending()
		} else if (pending == null) {
			// Light is an on-change sensor: a stable new reading may be the last event we receive.
			val change = object : Runnable {
				override fun run() {
					if (pending === this) {
						pending = null
						setNight(!current)
					}
				}
			}
			pending = change
			schedule.accept(change)
		}
	}

	fun reset() {
		cancelPending()
		night = null
	}

	private fun cancelPending() {
		pending?.let { cancel.accept(it) }
		pending = null
	}

	private fun setNight(value: Boolean) {
		night = value
		onChange.accept(value)
	}
}

package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.R
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MapMagnifierScreen(
	carContext: CarContext
) : BaseAndroidAutoScreen(carContext) {

	init {
		lifecycle.addObserver(this)
	}

	private var magnifierValues = DEFAULT_VALUES
	private var selectedIndex = 0
	private var initialAAMapDensity = 0f
	private var isApplied = false

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		if (!isApplied) {
			val settings = app.settings
			if (isInitialValueSet()) {
				settings.AA_MAP_DENSITY.set(initialAAMapDensity)
			} else {
				settings.AA_MAP_DENSITY.resetToDefault()
			}
			app.osmandMap.mapView.applyDisplayScaleSettings()
		}
	}

	override fun onFirstGetTemplate() {
		super.onFirstGetTemplate()
		val settings = app.settings
		initialAAMapDensity = if (settings.AA_MAP_DENSITY.isSet) {
			settings.AA_MAP_DENSITY.get()
		} else {
			0f
		}
		// the value in use right now: the Android Auto one if it was ever changed here,
		// the phone one otherwise
		val currentValue =
			((if (isInitialValueSet()) initialAAMapDensity else settings.MAP_DENSITY.get()) * 100)
				.roundToInt()
		magnifierValues = buildMagnifierValues(currentValue)
		selectedIndex = max(0, magnifierValues.indexOf(currentValue))
	}

	/**
	 * The phone offers values this screen does not (25 %, 400 % ...). Such a value has to stay
	 * visible and selected, otherwise the head unit shows a selection the map does not use.
	 * The list is kept within the host content limit around the selected item.
	 */
	private fun buildMagnifierValues(currentValue: Int): List<Int> {
		val values = DEFAULT_VALUES.toMutableList()
		if (!values.contains(currentValue)) {
			values.add(currentValue)
			values.sort()
		}
		val limit = if (contentLimit > 0) contentLimit else values.size
		if (values.size <= limit) {
			return values
		}
		var from = max(0, values.indexOf(currentValue) - limit / 2)
		from = min(from, values.size - limit)
		return values.subList(from, from + limit)
	}

	private fun isInitialValueSet(): Boolean {
		return initialAAMapDensity > 0
	}

	override fun getTemplate(): Template {
		val listBuilder = ItemList.Builder()
		for (value in magnifierValues) {
			listBuilder.addItem(
				Row.Builder()
					.setTitle(String.format(Locale.getDefault(), "%d%%", value))
					.build()
			)
		}

		listBuilder.setOnSelectedListener { index ->
			if (index >= 0 && index < magnifierValues.size) {
				val value = magnifierValues[index].toFloat() / 100
				app.settings.AA_MAP_DENSITY.set(value)
				app.osmandMap.mapView.applyDisplayScaleSettings()
				selectedIndex = index
			}
		}
		listBuilder.setSelectedIndex(selectedIndex)

		val header = Header.Builder()
			.setTitle(app.getString(R.string.map_magnifier))
			.setStartHeaderAction(Action.BACK)
			.build()

		val listTemplate = ListTemplate.Builder()
			.setHeader(header)
			.setSingleList(listBuilder.build())
			.build()

		val actionStrip = ActionStrip.Builder()
			.addAction(
				Action.Builder()
					.setTitle(app.getString(R.string.shared_string_apply))
					.setOnClickListener {
						isApplied = true
						finish()
					}
					.build()
			)
			.build()

		return MapWithContentTemplate.Builder()
			.setContentTemplate(listTemplate)
			.setActionStrip(actionStrip)
			.build()
	}

	companion object {
		private val DEFAULT_VALUES = listOf(50, 75, 100, 125, 150, 200)
	}
}

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
import kotlin.math.roundToInt

class MapMagnifierScreen(
	carContext: CarContext
) : BaseAndroidAutoScreen(carContext) {

	init {
		lifecycle.addObserver(this)
	}

	private val magnifierValues = listOf(50, 75, 100, 125, 150, 200)
	private var selectedIndex = 2
	private var initialAAMapDensitySet = false
	private var initialAAMapDensity = 1f
	private var isApplied = false

	override fun onCreate(owner: LifecycleOwner) {
		super.onCreate(owner)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		if (!isApplied) {
			val settings = app.settings
			settings.AA_MAP_DENSITY.set(initialAAMapDensity)
			settings.AA_MAP_DENSITY_SET.set(initialAAMapDensitySet)
			refreshMapScale()
		}
	}

	override fun onFirstGetTemplate() {
		super.onFirstGetTemplate()
		val settings = app.settings
		val mapDensity: Float = settings.MAP_DENSITY.get()
		initialAAMapDensity = settings.AA_MAP_DENSITY.get()
		initialAAMapDensitySet = settings.AA_MAP_DENSITY_SET.get()
		val selectedValue =
			((if (initialAAMapDensitySet) initialAAMapDensity else mapDensity) * 100).roundToInt()
		for (value in magnifierValues) {
			if (selectedValue == value) {
				selectedIndex = magnifierValues.indexOf(value)
				break
			}
		}
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
				val settings = app.settings
				settings.AA_MAP_DENSITY.set(value)
				settings.AA_MAP_DENSITY_SET.set(true)
				refreshMapScale()
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

	private fun refreshMapScale() {
		val mapView = app.osmandMap.mapView
		mapView.setMapDensity(mapView.getSettingsMapDensity())
		app.osmandMap.refreshMap(true)
	}
}
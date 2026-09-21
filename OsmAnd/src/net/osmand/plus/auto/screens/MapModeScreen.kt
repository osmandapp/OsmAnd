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
import net.osmand.plus.settings.enums.AndroidAutoMapMode

class MapModeScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext) {

	init {
		lifecycle.addObserver(this)
	}

	private val mapModes = AndroidAutoMapMode.entries
	private var initialMapMode = AndroidAutoMapMode.AUTOMATIC
	private var selectedIndex = initialMapMode.ordinal
	private var isApplied = false

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		if (!isApplied) {
			app.settings.AA_MAP_NIGHT_MODE.set(initialMapMode)
			refreshMapMode()
		}
	}

	override fun onFirstGetTemplate() {
		super.onFirstGetTemplate()
		initialMapMode = app.settings.AA_MAP_NIGHT_MODE.get()
		selectedIndex = mapModes.indexOf(initialMapMode).takeIf { it >= 0 }
			?: AndroidAutoMapMode.AUTOMATIC.ordinal
	}

	override fun getTemplate(): Template {
		val listBuilder = ItemList.Builder()
		for (mode in mapModes) {
			listBuilder.addItem(
				Row.Builder()
					.setTitle(app.getString(mode.titleId))
					.build()
			)
		}

		listBuilder.setOnSelectedListener { index ->
			if (index >= 0 && index < mapModes.size) {
				app.settings.AA_MAP_NIGHT_MODE.set(mapModes[index])
				selectedIndex = index
				refreshMapMode()
			}
		}
		listBuilder.setSelectedIndex(selectedIndex)

		val header = Header.Builder()
			.setTitle(app.getString(R.string.map_mode))
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

	private fun refreshMapMode() {
		app.osmandMap.mapView.refreshMap(true)
		app.carNavigationSession?.navigationCarSurface?.onCarConfigurationChanged()
	}
}

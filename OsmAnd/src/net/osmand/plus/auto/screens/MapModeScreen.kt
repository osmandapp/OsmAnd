package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.plus.R
import net.osmand.plus.settings.enums.AndroidAutoMapMode

class MapModeScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext) {

	init {
		lifecycle.addObserver(this)
	}

	private val mapModes = AndroidAutoMapMode.entries
	private var selectedMapMode = AndroidAutoMapMode.AUTOMATIC
	private var closing = false

	override fun onFirstGetTemplate() {
		super.onFirstGetTemplate()
		selectedMapMode = app.settings.AA_MAP_NIGHT_MODE.get()
	}

	override fun getTemplate(): Template {
		// The selection is saved immediately and the screen closes right away: there is
		// no Apply button, because the host hides the map action strip while navigation
		// is active and a selectable list cannot contain an extra "Apply" row (#21448).
		val listBuilder = ItemList.Builder()
		for (mode in mapModes) {
			listBuilder.addItem(
				Row.Builder()
					.setTitle(app.getString(mode.titleId))
					.setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, mode.iconId)).build())
					.build()
			)
		}
		listBuilder.setOnSelectedListener { index -> onMapModeSelected(index) }
		listBuilder.setSelectedIndex(mapModes.indexOf(selectedMapMode))

		val header = Header.Builder()
			.setTitle(app.getString(R.string.map_mode))
			.setStartHeaderAction(Action.BACK)
			.build()

		val listTemplate = ListTemplate.Builder()
			.setHeader(header)
			.setSingleList(listBuilder.build())
			.build()

		return MapWithContentTemplate.Builder()
			.setContentTemplate(listTemplate)
			.build()
	}

	private fun onMapModeSelected(index: Int) {
		val mode = mapModes.getOrNull(index) ?: return
		if (mode == selectedMapMode || closing) {
			return
		}
		selectedMapMode = mode
		app.settings.AA_MAP_NIGHT_MODE.set(mode)
		app.osmandMap.mapView.refreshMap(true)
		app.carNavigationSession?.navigationCarSurface?.onCarConfigurationChanged()
		closing = true
		finish()
	}
}

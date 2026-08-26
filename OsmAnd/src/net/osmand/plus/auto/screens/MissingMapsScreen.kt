package net.osmand.plus.auto.screens

import android.os.Bundle
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.routepreparationmenu.RequiredMapsFragment
import net.osmand.plus.settings.backend.OsmandSettings


class MissingMapsScreen(carContext: CarContext, screenType: MissingMapsScreenType) :
	BaseAndroidAutoScreen(carContext) {
	var screenType: MissingMapsScreenType = screenType
		private set

	fun updateScreenType(screenType: MissingMapsScreenType) {
		this.screenType = screenType
		invalidate()
	}

	override fun getTemplate(): Template {
		val title = app.getString(screenType.titleId)
		val message = app.getString(screenType.messageId)
		val builder = MessageTemplate.Builder(message).setTitle(title)

		if (screenType == MissingMapsScreenType.POSSIBLE_MISSING_MAPS) {
			builder.addAction(
				Action.Builder()
					.setTitle(app.getString(R.string.route_calculation_use_existing_maps))
					.setOnClickListener {
						app.getSettings().setStopOnMissingMaps(false)
						OsmandSettings.IGNORE_MISSING_MAPS = true
						app.getRoutingHelper().onSettingsChanged(true)
						finish()
					}
					.build()
			)
		}

		return builder
			.addAction(
				Action.Builder()
					.setTitle(app.getString(R.string.view_on_phone))
					.setOnClickListener {
						val app = carContext.applicationContext as OsmandApplication
						val params = Bundle()
						params.putBoolean(RequiredMapsFragment.OPEN_FRAGMENT_KEY, true)
						MapActivity.launchMapActivityMoveToTop(app, null, null, params)
						app.getRoutingHelper().stopCalculationImmediately()
						app.getSettings().setStopOnMissingMaps(true)
						app.carNavigationSession?.stopNavigation()
						finish()
					}
					.build()
			)
			.setHeaderAction(Action.BACK)
			.build()
	}
}

enum class MissingMapsScreenType(val titleId: Int, val messageId: Int) {
	MISSING_MAPS(
		R.string.route_calculation_missing_maps,
		R.string.android_auto_missing_maps_desc
	),
	POSSIBLE_MISSING_MAPS(
		R.string.route_calculation_missing_maps,
		R.string.android_auto_possible_missing_maps_desc
	)
}
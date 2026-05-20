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


class MissingMapsScreen(carContext: CarContext, private val allowContinue: Boolean) :
	BaseAndroidAutoScreen(carContext) {
	override fun getTemplate(): Template {
		val title = app.getString(R.string.missing_maps_header)
		val message = app.getString(R.string.missing_maps_description)
		val continueNavigation = R.string.continue_navigation
		val stopNavigationService = R.string.stop_navigation_service
		val firstButtonText = if (allowContinue) continueNavigation else stopNavigationService

		return MessageTemplate.Builder(message)
			.setTitle(title)
			.addAction(
				Action.Builder()
					.setTitle(app.getString(firstButtonText))
					.setOnClickListener {
						if (!allowContinue) {
							app.carNavigationSession?.stopNavigation()
						}
						finish()
					}
					.build()
			)
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

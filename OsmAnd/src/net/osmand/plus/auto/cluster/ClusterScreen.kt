package net.osmand.plus.auto.cluster

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import net.osmand.plus.R

/**
 * The cluster accepts only a NavigationTemplate and ignores its actions,
 * but the template can't be built without an action strip.
 */
class ClusterScreen(carContext: CarContext) : Screen(carContext) {

	override fun onGetTemplate(): Template {
		val action = Action.Builder()
			.setTitle(carContext.getString(R.string.app_name))
			.build()
		return NavigationTemplate.Builder()
			.setActionStrip(ActionStrip.Builder().addAction(action).build())
			.build()
	}
}
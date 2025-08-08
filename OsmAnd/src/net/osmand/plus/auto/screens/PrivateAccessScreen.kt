package net.osmand.plus.auto.screens


import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference

@OptIn(ExperimentalCarApi::class)
class PrivateAccessScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext) {

	override fun getTemplate(): Template {
		val yesAction = Action.Builder()
			.setTitle(app.getString(R.string.shared_string_yes))
			.setOnClickListener {
				val routingHelper = app.routingHelper
				val settings = app.settings
				val modes = ApplicationMode.values(app)
				for (mode in modes) {
					val preference: OsmandPreference<Boolean> =
						settings.getAllowPrivatePreference(mode)
					if (!preference.getModeValue(mode)) {
						preference.setModeValue(mode, true)
					}
				}
				routingHelper.onSettingsChanged(null, true)
				setResult(true)
				finish()
			}
			.build()

		val cancelAction = Action.Builder()
			.setTitle(app.getString(R.string.shared_string_cancel))
			.setOnClickListener {
				setResult(false)
				finish()
			}
			.build()

		val paneTemplate = PaneTemplate.Builder(
			Pane.Builder()
				.addRow(
					Row.Builder()
						.setTitle(app.getString(R.string.private_access_routing_req_short))
						.build()
				)
				.addAction(yesAction)
				.addAction(cancelAction)
				.build()
		)
			.setTitle(app.getString(R.string.private_access))
			.setHeaderAction(Action.BACK)
			.build()

		return MapWithContentTemplate.Builder()
			.setContentTemplate(paneTemplate)
			.build()
	}
}
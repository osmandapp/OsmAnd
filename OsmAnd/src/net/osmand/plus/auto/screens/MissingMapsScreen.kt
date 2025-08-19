package net.osmand.plus.auto.screens

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import net.osmand.plus.R
import net.osmand.plus.receivers.AndroidAutoActionReceiver
import net.osmand.plus.routepreparationmenu.RequiredMapsFragment
import net.osmand.plus.settings.backend.OsmandSettings


class MissingMapsScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext) {
	override fun getTemplate(): Template {
		val title = app.getString(R.string.missing_maps_header)
		val message = app.getString(R.string.missing_maps_description)

		return MessageTemplate.Builder(message)
			.setTitle(title)
			.addAction(
				Action.Builder()
					.setTitle(app.getString(R.string.missing_maps_ignore))
					.setOnClickListener {
						OsmandSettings.IGNORE_MISSING_MAPS = true
						app.routingHelper.onSettingsChanged(true)
						finish()
					}
					.build()
			)
			.addAction(
				Action.Builder()
					.setTitle(app.getString(R.string.view_on_phone))
					.setOnClickListener {
						val intent = Intent(AndroidAutoActionReceiver.INTENT_SHOW_FRAGMENT)
						intent.putExtra(
							AndroidAutoActionReceiver.INTENT_KEY_SHOW_FRAGMENT_NAME,
							RequiredMapsFragment::class.java.simpleName)
						app.sendBroadcast(intent)
						finish()
					}
					.build()
			)
			.setHeaderAction(Action.BACK)
			.build()
	}
}
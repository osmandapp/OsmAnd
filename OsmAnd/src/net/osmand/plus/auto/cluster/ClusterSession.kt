package net.osmand.plus.auto.cluster

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class ClusterSession : Session() {

	override fun onCreateScreen(intent: Intent): Screen {
		ClusterRenderer(carContext, lifecycle)
		return ClusterScreen(carContext)
	}
}
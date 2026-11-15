package net.osmand.plus.auto.cluster

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.OsmandApplication

class ClusterSession : Session(), DefaultLifecycleObserver {

	private var renderer: ClusterRenderer? = null

	init {
		lifecycle.addObserver(this)
	}

	override fun onCreateScreen(intent: Intent): Screen {
		val renderer = ClusterRenderer(carContext, lifecycle)
		this.renderer = renderer
		getApp().carClusterRenderer = renderer
		return ClusterScreen(carContext)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		val app = getApp()
		if (renderer != null && app.carClusterRenderer === renderer) {
			app.carClusterRenderer = null
		}
		renderer = null
	}

	private fun getApp(): OsmandApplication = carContext.applicationContext as OsmandApplication
}
package net.osmand.plus.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.routepreparationmenu.RequiredMapsFragment
import net.osmand.util.Algorithms

class AndroidAutoActionReceiver : BroadcastReceiver() {
	companion object {
		const val INTENT_SHOW_FRAGMENT: String = "net.osmand.CAR_ACTION_SHOW_FRAGMENT"
		const val INTENT_KEY_SHOW_FRAGMENT_NAME: String = "INTENT_KEY_SHOW_FRAGMENT_NAME"
	}

	var activity: MapActivity? = null

	override fun onReceive(context: Context, intent: Intent) {
		activity?.apply {
			if (INTENT_SHOW_FRAGMENT == intent.action && intent.hasExtra(INTENT_KEY_SHOW_FRAGMENT_NAME)) {
				val name = intent.getStringExtra(INTENT_KEY_SHOW_FRAGMENT_NAME)
				if (Algorithms.stringsEqual(name, RequiredMapsFragment::class.java.simpleName)) {
					RequiredMapsFragment.showInstance(supportFragmentManager)
				}
			}
		}
	}
}
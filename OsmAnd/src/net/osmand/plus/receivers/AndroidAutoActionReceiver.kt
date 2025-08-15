package net.osmand.plus.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.routepreparationmenu.RequiredMapsFragment

class AndroidAutoActionReceiver : BroadcastReceiver() {
	var activity: MapActivity? = null
	override fun onReceive(context: Context, intent: Intent) {
		activity?.apply {
			if (MapActivity.INTENT_SHOW_FRAGMENT == intent.action) {
				if (intent.hasExtra(MapActivity.INTENT_KEY_SHOW_FRAGMENT_NAME)) {
					val fragmentName =
						intent.getStringExtra(MapActivity.INTENT_KEY_SHOW_FRAGMENT_NAME)
					if (fragmentName != null) {
						if (fragmentName == RequiredMapsFragment::class.java.simpleName) {
							RequiredMapsFragment.showInstance(supportFragmentManager)
						}
					}
				}
			}
		}
	}
}
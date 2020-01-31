package net.osmand.telegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InitAppBroadcastReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		// check if aidl connection was lost
		val app = context.applicationContext as TelegramApplication
		val aidlHelper = app.osmandAidlHelper
		if (aidlHelper.isOsmandBound() && !aidlHelper.isOsmandConnected()) {
			aidlHelper.connectOsmand()
		}
	}
}
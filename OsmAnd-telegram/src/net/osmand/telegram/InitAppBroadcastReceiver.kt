package net.osmand.telegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InitAppBroadcastReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		// do nothing, TelegramApplication already initialized
	}
}

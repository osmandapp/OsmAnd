package net.osmand.aiconnector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Debug builds only: drives the on-device assistant from adb, for speed and quality runs on a phone.
 *   adb shell am broadcast -n net.osmand.aiconnector.debug/net.osmand.aiconnector.AssistantProbe \
 *       --es model gemma-4-e2b-litert --ez clear true --es text "Turn on night mode"
 * Tools that change OsmAnd are not run unless --ez dry false. Events go to logcat with the tag OsmAndAssistant.
 */
class AssistantProbe : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		val assistant = (context.applicationContext as AssistantApplication).assistant
		intent.getStringExtra("model")?.let { id ->
			val model = assistant.models.all.firstOrNull { it.id == id }
			if (model == null) {
				Log.w("OsmAndAssistant", "Unknown model $id: ${assistant.models.all.joinToString { it.id }}")
				return
			}
			assistant.models.selected = model
		}
		assistant.remind = intent.getBooleanExtra("remind", true)
		if (intent.hasExtra("cpu")) {
			assistant.forceCpu = intent.getBooleanExtra("cpu", assistant.forceCpu)
			assistant.unload()
		}
		// test chats are not saved in the user's chat list
		if (intent.getBooleanExtra("clear", false)) assistant.newChat(save = false)
		intent.getStringExtra("text")?.let { assistant.send(it, intent.getBooleanExtra("dry", true)) }
		// a tool without the model, e.g. to undo what a test run changed: --es tool osmand_stop_recording --es args '{}'
		intent.getStringExtra("tool")?.let { assistant.runTool(it, intent.getStringExtra("args") ?: "{}") }
	}
}

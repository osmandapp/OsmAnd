package net.osmand.aiconnector

import android.app.Application
import net.osmand.aiconnector.assistant.LocalAssistant

/** Owns the on-device assistant, so a loaded model outlives the chat screen. */
class AssistantApplication : Application() {

	val assistant by lazy { LocalAssistant(this) }
}

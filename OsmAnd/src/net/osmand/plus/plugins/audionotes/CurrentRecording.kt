package net.osmand.plus.plugins.audionotes

import net.osmand.CallbackWithObject
import java.io.File

data class CurrentRecording @JvmOverloads constructor(
	val type: AVActionType,
	val file: File? = null,
	val resultCallback: CallbackWithObject<File>? = null,
	val showInDialog: Boolean = false
) {
	fun isAttachedMediaRecording(): Boolean = file != null && resultCallback != null
}

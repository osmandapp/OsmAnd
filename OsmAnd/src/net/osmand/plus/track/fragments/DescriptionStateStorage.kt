package net.osmand.plus.track.fragments

import android.content.Context
import android.os.Bundle
import android.util.AtomicFile
import androidx.fragment.app.Fragment
import net.osmand.PlatformUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Keeps a description in the saved state of a fragment.
 *
 * The state of all fragments is sent to the system in one Binder transaction, which is limited to about 1 MB.
 * A long description does not fit there, so it is written to a file and only the file name goes to the state.
 *
 * @param key key in the state and prefix of the file name, must be unique among the screens open at the same time
 */
class DescriptionStateStorage(context: Context, private val key: String) {

	companion object {
		private val LOG = PlatformUtil.getLog(DescriptionStateStorage::class.java)

		const val MAX_LENGTH_IN_STATE = 10_000

		private const val DIR_NAME = "description_state"
		private const val FILE_KEY_SUFFIX = "_file"
	}

	private val dir = File(context.cacheDir, DIR_NAME)
	private val fileKey = key + FILE_KEY_SUFFIX

	private var fileName: String? = null
	private var writtenText: String? = null

	fun save(outState: Bundle, text: String) {
		if (text.length <= MAX_LENGTH_IN_STATE) {
			outState.putString(key, text)
		} else if (text == writtenText || write(text)) {
			outState.putString(fileKey, fileName)
		}
	}

	/** Returns null if nothing was saved, or if the file can't be read, e.g. the system has cleared the cache. */
	fun restore(state: Bundle): String? {
		val name = state.getString(fileKey) ?: return state.getString(key)
		return try {
			val text = String(AtomicFile(File(dir, name)).readFully(), Charsets.UTF_8)
			fileName = name
			writtenText = text
			text
		} catch (e: IOException) {
			LOG.error("Failed to read description from $name", e)
			null
		}
	}

	/** Must be called from [Fragment.onDestroy]: deletes the file if the state of the screen will not be restored. */
	fun clearIfClosed(fragment: Fragment) {
		// a fragment from the back stack is also "removing" when its activity is recreated, but its state is saved
		val closed = fragment.isRemoving && !fragment.isStateSaved
		if (closed || fragment.requireActivity().isFinishing) {
			clear()
		}
	}

	fun clear() {
		fileName?.let { AtomicFile(File(dir, it)).delete() }
		fileName = null
		writtenText = null
	}

	private fun write(text: String): Boolean {
		val name = fileName ?: "$key-${UUID.randomUUID()}"
		val file = AtomicFile(File(dir, name))
		var stream: FileOutputStream? = null
		return try {
			dir.mkdirs()
			if (fileName == null) {
				deleteAbandonedFiles()
			}
			stream = file.startWrite()
			stream.write(text.toByteArray(Charsets.UTF_8))
			file.finishWrite(stream)
			fileName = name
			writtenText = text
			true
		} catch (e: IOException) {
			LOG.error("Failed to write description to $name", e)
			file.failWrite(stream)
			false
		}
	}

	// files of the screens that were not restored after the process was killed
	private fun deleteAbandonedFiles() {
		dir.listFiles { _, name -> name.startsWith("$key-") }?.forEach { it.delete() }
	}
}

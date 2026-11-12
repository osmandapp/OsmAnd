package net.osmand.wear.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import java.io.DataInputStream
import java.io.InputStream

/**
 * The latest map frame the phone has sent, and nothing else.
 *
 * Only the newest frame matters: if the watch cannot keep up with decoding, the right thing is
 * to skip ahead rather than to play a backlog of stale map.
 */
object MapFrames {

	/**
	 * A frame and the gesture it answers. [seq] is the last gesture the phone had applied when
	 * it drew this; the screen compares it against the last one it asked for, and a frame that
	 * was already in flight when the gesture was made is dropped rather than shown.
	 */
	data class Frame(val image: ImageBitmap, val seq: Int)

	private val _frame = MutableStateFlow<Frame?>(null)
	val frame: StateFlow<Frame?> = _frame

	/**
	 * Reads length-prefixed WebP frames until the stream ends or the thread is interrupted.
	 * Runs on the caller's thread, which is the channel callback's, never the main one.
	 */
	fun consume(input: InputStream) {
		val data = DataInputStream(input)
		val options = BitmapFactory.Options().apply { inMutable = true }
		// Two buffers in rotation: the one just handed to the UI must not be decoded into while it
		// is still on screen, so the frame before it is the one reused.
		val buffers = arrayOfNulls<Bitmap>(2)
		var next = 0
		try {
			while (!Thread.currentThread().isInterrupted) {
				val size = data.readInt()
				if (size <= 0 || size > MAX_FRAME_BYTES) {
					// A length this far out means the stream is out of step; there is no way to
					// resynchronise inside it, so stop rather than decode rubbish.
					return
				}
				val seq = data.readInt()
				val bytes = ByteArray(size)
				data.readFully(bytes)
				// Reusing the allocation rather than taking a megabyte a frame: four a second
				// otherwise has the watch collecting garbage in the middle of a gesture.
				options.inBitmap = buffers[next]
				val bitmap = try {
					BitmapFactory.decodeByteArray(bytes, 0, size, options)
				} catch (mismatch: IllegalArgumentException) {
					options.inBitmap = null
					BitmapFactory.decodeByteArray(bytes, 0, size, options)
				} ?: continue
				buffers[next] = bitmap
				next = 1 - next
				_frame.value = Frame(bitmap.asImageBitmap(), seq)
			}
		} catch (end: Exception) {
			// The phone closed the channel, or the link dropped.
		} finally {
			_frame.value = null
		}
	}

	fun clear() {
		_frame.value = null
	}

	private const val MAX_FRAME_BYTES = 2 * 1024 * 1024
}

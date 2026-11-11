package net.osmand.shared.io

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants.O_CLOEXEC
import android.system.OsConstants.O_CREAT
import android.system.OsConstants.O_EXCL
import android.system.OsConstants.O_WRONLY
import okio.ForwardingSink
import okio.IOException
import okio.Sink
import okio.sink
import java.io.FileOutputStream

// android.system.Os is available from API 21, below the shared library's minSdk.
internal actual fun KFile.exclusiveSink(): Sink {
	val descriptor = try {
		Os.open(path(), O_WRONLY or O_CREAT or O_EXCL or O_CLOEXEC, 384 /* 0600 */)
	} catch (e: ErrnoException) {
		throw IOException("Cannot create $path", e)
	}
	return object : ForwardingSink(FileOutputStream(descriptor).sink()) {
		private var closed = false

		override fun close() {
			if (closed) return
			closed = true
			try {
				super.close()
			} finally {
				// Android's FileOutputStream(FileDescriptor) does not own/close the descriptor.
				try {
					Os.close(descriptor)
				} catch (e: ErrnoException) {
					throw IOException("Cannot close $path", e)
				}
			}
		}
	}
}

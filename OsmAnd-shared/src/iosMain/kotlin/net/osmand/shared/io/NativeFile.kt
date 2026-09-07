package net.osmand.shared.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSFileManager
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.stat

@OptIn(ExperimentalForeignApi::class)
actual class NativeFile actual constructor(actual val file: KFile) {

	private val filePath = file.path()

	actual fun absolutePath(): String = filePath

	actual fun isDirectory(): Boolean = memScoped {
		val st = alloc<stat>()
		if (stat(filePath, st.ptr) != 0) return@memScoped false
		(st.st_mode.toInt() and S_IFMT) == S_IFDIR
	}

	actual fun exists(): Boolean = memScoped {
		val st = alloc<stat>()
		stat(filePath, st.ptr) == 0
	}

	actual fun length(): Long = memScoped {
		val st = alloc<stat>()
		if (stat(filePath, st.ptr) != 0) return@memScoped 0L
		st.st_size
	}

	actual fun lastModified(): Long = memScoped {
		val st = alloc<stat>()
		if (stat(filePath, st.ptr) != 0) return@memScoped 0L
		st.st_mtimespec.tv_sec * 1000L + st.st_mtimespec.tv_nsec / 1_000_000L
	}

	actual fun listFiles(): List<KFile>? {
		val files = NSFileManager.defaultManager.contentsOfDirectoryAtPath(filePath, null) ?: return null
		return files.mapNotNull { fileName ->
			val fullPath = "$filePath/$fileName"
			KFile(fullPath)
		}
	}

	actual fun renameTo(toFile: KFile): Boolean {
		return NSFileManager.defaultManager.moveItemAtPath(filePath, toFile.path(), null)
	}

	actual fun renameTo(toFilePath: String): Boolean {
		return NSFileManager.defaultManager.moveItemAtPath(filePath, toFilePath, null)
	}
}

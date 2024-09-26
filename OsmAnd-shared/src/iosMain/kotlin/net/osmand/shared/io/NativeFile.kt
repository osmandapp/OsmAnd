package net.osmand.shared.io

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual class NativeFile actual constructor(actual val file: KFile) {

	private val filePath = file.path()

	actual fun absolutePath(): String = filePath

	actual fun isDirectory(): Boolean {
		return memScoped {
			val isDirectory = alloc<BooleanVar>()
			NSFileManager.defaultManager.fileExistsAtPath(filePath, isDirectory.ptr)
			isDirectory.value
		}
	}

	actual fun exists(): Boolean {
		return NSFileManager.defaultManager.fileExistsAtPath(filePath)
	}

	actual fun length(): Long {
		val attr: Map<Any?, *>? = NSFileManager.defaultManager.attributesOfItemAtPath(filePath, null)
		if (attr == null || attr.isEmpty()) {
			return 0
		}
		return (attr[NSFileSize] as NSNumber).longLongValue
	}

	actual fun lastModified(): Long {
		val attr: Map<Any?, *>? = NSFileManager.defaultManager.attributesOfItemAtPath(filePath, null)
		if (attr == null || attr.isEmpty()) {
			return 0
		}
		return ((attr[NSFileModificationDate] as NSDate).timeIntervalSince1970 * 1000.0).toLong()
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
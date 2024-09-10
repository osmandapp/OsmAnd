package net.osmand.shared.io

import net.osmand.shared.extensions.jFile
import net.osmand.shared.extensions.kFile
import java.io.File

actual class NativeFile actual constructor(actual val file: KFile) {

	private var jFile = file.jFile()

	actual fun length(): Long = jFile.length()

	actual fun lastModified(): Long = jFile.lastModified()

	actual fun listFiles(): List<KFile>? = jFile.listFiles()?.map { it.kFile() }

	actual fun renameTo(toFile: KFile): Boolean = jFile.renameTo(toFile.jFile())

	actual fun renameTo(toFilePath: String): Boolean = jFile.renameTo(File(toFilePath))
}
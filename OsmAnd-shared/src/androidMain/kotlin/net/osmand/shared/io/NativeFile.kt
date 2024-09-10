package net.osmand.shared.io

import net.osmand.shared.extensions.jFile
import net.osmand.shared.extensions.kFile

actual class NativeFile actual constructor(actual val file: KFile) {

	private var jFile = file.jFile()

	actual fun length(): Long = jFile.length()

	actual fun lastModified(): Long = jFile.lastModified()

	actual fun listFiles(): List<KFile>? = jFile.listFiles()?.map { it.kFile() }
}
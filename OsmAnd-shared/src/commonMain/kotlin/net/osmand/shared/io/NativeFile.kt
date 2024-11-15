package net.osmand.shared.io

expect class NativeFile(file: KFile) {

	val file: KFile

	fun absolutePath(): String

	fun isDirectory(): Boolean

	fun exists(): Boolean

	fun length(): Long

	fun lastModified(): Long

	fun listFiles(): List<KFile>?

	fun renameTo(toFile: KFile): Boolean

	fun renameTo(toFilePath: String): Boolean
}
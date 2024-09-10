package net.osmand.shared.io

expect class NativeFile(file: KFile) {

	val file: KFile

	fun length(): Long

	fun lastModified(): Long

	fun listFiles(): List<KFile>?
}
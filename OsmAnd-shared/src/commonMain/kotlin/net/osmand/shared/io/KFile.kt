package net.osmand.shared.io

import net.osmand.shared.util.KAlgorithms.isEmpty
import okio.FileMetadata
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.Sink
import okio.Source
import okio.buffer
import okio.use

class KFile {
	val path: Path

	constructor(filePath: String) {
		this.path = filePath.toPath()
	}

	constructor(path: Path) {
		this.path = path
	}

	constructor(file: KFile, fileName: String) {
		this.path = file.path.resolve(fileName)
	}

	fun name(): String = path.name

	fun parent(): Path? = path.parent

	fun getParentFile(): KFile? {
		val parent = path.parent
		return if (parent != null) KFile(parent) else null
	}

	fun exists(): Boolean = FileSystem.SYSTEM.exists(path)

	fun isAbsolute(): Boolean = path.isAbsolute

	fun isDirectory(): Boolean =
		FileSystem.SYSTEM.metadataOrNull(path)?.isDirectory == true

	fun lastModified(): Long {
		return try {
			val metadata: FileMetadata? = FileSystem.SYSTEM.metadataOrNull(path)
			metadata?.lastModifiedAtMillis ?: 0
		} catch (e: IOException) {
			0
		}
	}

	fun path(): String = path.toString()

	fun absolutePath(): String =
		if (exists()) FileSystem.SYSTEM.canonicalize(path).toString() else path.toString()

	@Throws(IOException::class)
	fun source(): Source = FileSystem.SYSTEM.source(path)

	@Throws(IOException::class)
	fun sink(): Sink = FileSystem.SYSTEM.sink(path)

	@Throws(IOException::class)
	fun createDirectories() = FileSystem.SYSTEM.createDirectories(path)

	@Throws(IOException::class)
	fun readText(): String {
		return FileSystem.SYSTEM.source(path).buffer().use {
			it.readUtf8()
		}
	}

	@Throws(IOException::class)
	fun writeText(text: String) {
		FileSystem.SYSTEM.sink(path).buffer().use {
			it.writeUtf8(text)
		}
	}

	fun listFiles(): Array<KFile>? {
		try {
			val pathList = FileSystem.SYSTEM.list(path)
			return pathList.map { KFile(it) }.toTypedArray()
		} catch (e: IOException) {
			return null
		}
	}

	fun delete(): Boolean {
		if (!exists()) {
			return false
		}
		FileSystem.SYSTEM.delete(path, false)
		return !exists()
	}

	override fun hashCode(): Int {
		return path.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KFile) return false
		return path == other.path
	}

	override fun toString(): String {
		return path.toString()
	}

	fun length(): Long {
		return try {
			val metadata: FileMetadata? = FileSystem.SYSTEM.metadataOrNull(path)
			metadata?.size ?: 0
		} catch (e: IOException) {
			0
		}
	}

	fun renameTo(toFile: KFile): Boolean {
		if (!exists()) {
			return false
		}
		FileSystem.SYSTEM.atomicMove(path, toFile.path)
		return toFile.exists()
	}

	fun renameTo(toFilePath: String): Boolean {
		return renameTo(KFile(toFilePath))
	}

	fun getFileNameWithoutExtension(): String? {
		return Companion.getFileNameWithoutExtension(this.name())
	}

	companion object {
		fun getFileNameWithoutExtension(name: String?): String? {
			return name?.substringBeforeLast(".");
		}

		fun removeAllFiles(file: KFile): Boolean {
			return if (file.isDirectory()) {
				val files = file.listFiles()
				if (!isEmpty(files)) {
					for (f in files!!) {
						removeAllFiles(f)
					}
				}
				file.delete()
			} else {
				file.delete()
			}
		}
	}
}
package net.osmand.shared.io

import net.osmand.shared.util.KAlgorithms.isEmpty
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

	private var nativeFile: NativeFile
	private var directory: Boolean? = null
	private var absolutePath: String? = null

	constructor(filePath: String) {
		this.path = filePath.toPath()
		nativeFile = NativeFile(this)
	}

	constructor(path: Path) {
		this.path = path
		nativeFile = NativeFile(this)
	}

	constructor(file: KFile, fileName: String) {
		this.path = file.path.resolve(fileName)
		nativeFile = NativeFile(this)
	}

	fun name(): String = path.name

	fun parent(): Path? = path.parent

	fun getParentFile(): KFile? {
		val parent = path.parent
		return if (parent != null) KFile(parent) else null
	}

	fun isPathEmpty():Boolean {
		val path = path()
		return path.isEmpty() || path == "." || path == Path.DIRECTORY_SEPARATOR
	}

	fun exists(): Boolean = nativeFile.exists()

	fun isAbsolute(): Boolean = path.isAbsolute

	fun isDirectory(): Boolean {
		var directory = this.directory
		if (directory == null) {
			directory = nativeFile.isDirectory()
			this.directory = directory
		}
		return directory
	}

	fun lastModified(): Long = nativeFile.lastModified()

	fun path(): String = path.toString()

	fun absolutePath(): String {
		var absolutePath = this.absolutePath
		if (absolutePath == null) {
			absolutePath = nativeFile.absolutePath()
			this.absolutePath = absolutePath
		}
		return absolutePath
	}

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

	fun listFiles(): List<KFile>? = nativeFile.listFiles()

	fun delete(): Boolean {
		if (!exists()) {
			return false
		}
		FileSystem.SYSTEM.delete(path, false)
		return !exists()
	}

	override fun hashCode(): Int = path.hashCode()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KFile) return false
		return path == other.path
	}

	override fun toString(): String = path.toString()

	fun length(): Long = nativeFile.length()

	fun renameTo(toFile: KFile): Boolean = nativeFile.renameTo(toFile)

	fun renameTo(toFilePath: String): Boolean = nativeFile.renameTo(toFilePath)

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
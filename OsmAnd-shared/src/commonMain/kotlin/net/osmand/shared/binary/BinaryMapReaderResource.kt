package net.osmand.shared.binary

import net.osmand.shared.IndexConstants
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KLock
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.synchronized
import okio.IOException

/**
 * The readers of one obf file: the first one walks the file's structure, and each
 * [BinaryMapReaderResourceType] gets a reader of its own over that structure, so that the searches,
 * the routing and the poi of a file read it at the same time through file handles of their own.
 * A copy of `BinaryMapReaderResource` in the android app.
 *
 * Android is handed the first reader when it indexes the file; here it is opened the first time a
 * reader is asked for, and a file whose format version is not [IndexConstants.BINARY_MAP_VERSION]
 * gets none, as android leaves such a file out when indexing. Files are walked and opened outside
 * the lock, so that closing a file or asking what it holds does not wait for another thread.
 *
 * A failed initial read or a copy detecting a changed file size prevents further opens until the
 * registry replaces this resource. Readers already published remain available until [close]. A
 * copy that fails to open for another reason is retried on the next request.
 */
class BinaryMapReaderResource internal constructor(
	private val file: KFile,
	private val openReader: (BinaryMapIndexReader?) -> BinaryMapIndexReader
) {

	constructor(file: KFile) : this(file, { reference ->
		if (reference == null) BinaryMapIndexReader(file) else BinaryMapIndexReader(reference)
	})

	private val lock = KLock()
	private val readers = arrayOfNulls<BinaryMapIndexReader>(BinaryMapReaderResourceType.entries.size)
	private var initialReader: BinaryMapIndexReader? = null
	private var failed = false
	private var closed = false

	private var useForRouting = false
	private var useForPublicTransport = false

	constructor(path: String) : this(KFile(path))

	/** The reader of [type], opened the first time it is asked for; null when the file cannot be read. */
	fun getReader(type: BinaryMapReaderResourceType): BinaryMapIndexReader? {
		synchronized(lock) {
			val opened = readers[type.ordinal]
			if (opened != null || closed || failed) {
				return opened
			}
		}
		val initialReader = getShallowReader() ?: return null
		return open(
			open = { openReader(initialReader) },
			remembered = { it is ObfFileChangedException },
			opened = { readers[type.ordinal] },
			keep = { readers[type.ordinal] = it }
		)
	}

	/** The reader that walked the file, to ask what the file holds - its sections, their boxes - not to read with. */
	fun getShallowReader(): BinaryMapIndexReader? {
		synchronized(lock) {
			if (initialReader != null || closed || failed) {
				return initialReader
			}
		}
		return open(
			open = { walk() },
			remembered = { true },
			opened = { initialReader },
			keep = { initialReader = it }
		)
	}

	fun getFile(): KFile = file

	fun getFileName(): String = file.name()

	fun getFileLastModified(): Long = file.lastModified()

	fun close() {
		val closing = ArrayList<BinaryMapIndexReader>()
		synchronized(lock) {
			closed = true
			initialReader?.let { closing.add(it) }
			for (i in readers.indices) {
				readers[i]?.let { closing.add(it) }
				readers[i] = null
			}
			initialReader = null
		}
		for (reader in closing) {
			close(reader)
		}
	}

	fun isClosed(): Boolean = synchronized(lock) { closed }

	fun setUseForRouting(useForRouting: Boolean) = synchronized(lock) {
		this.useForRouting = useForRouting
	}

	fun isUseForRouting(): Boolean = synchronized(lock) { useForRouting }

	fun setUseForPublicTransport(useForPublicTransport: Boolean) = synchronized(lock) {
		this.useForPublicTransport = useForPublicTransport
	}

	fun isUseForPublicTransport(): Boolean = synchronized(lock) { useForPublicTransport }

	private fun walk(): BinaryMapIndexReader {
		val reader = openReader(null)
		if (reader.getVersion() != IndexConstants.BINARY_MAP_VERSION) {
			close(reader)
			throw IOException("${file.name()} has version ${reader.getVersion()}, not ${IndexConstants.BINARY_MAP_VERSION}")
		}
		return reader
	}

	/**
	 * Opens a reader without the lock - a walk of a big file takes hundreds of milliseconds - and
	 * keeps it, unless the file was closed meanwhile or another thread opened the same reader first,
	 * in which case that one is returned and this one closed. An error [remembered] prevents any
	 * further readers from being published, including opens already in progress.
	 */
	private fun open(
		open: () -> BinaryMapIndexReader,
		remembered: (Exception) -> Boolean,
		opened: () -> BinaryMapIndexReader?,
		keep: (BinaryMapIndexReader) -> Unit
	): BinaryMapIndexReader? {
		val reader = try {
			open()
		} catch (e: Exception) {
			val report = if (remembered(e)) {
				synchronized(lock) {
					val firstFailure = !failed && !closed
					failed = true
					firstFailure
				}
			} else {
				true
			}
			if (report) log.error("Fail to initialize ${file.name()}", e)
			return null
		}
		var unused: BinaryMapIndexReader? = null
		val result = synchronized(lock) {
			val other = opened()
			if (closed || failed || other != null) {
				unused = reader
				other
			} else {
				keep(reader)
				reader
			}
		}
		unused?.let { close(it) }
		return result
	}

	private fun close(reader: BinaryMapIndexReader) {
		try {
			reader.close()
		} catch (e: Exception) {
			log.error("Fail to close ${file.name()}", e)
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger("BinaryMapReaderResource")
	}
}

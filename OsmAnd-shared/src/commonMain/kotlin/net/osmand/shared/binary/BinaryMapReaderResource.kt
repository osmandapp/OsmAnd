package net.osmand.shared.binary

import net.osmand.shared.io.KFile
import net.osmand.shared.util.KLock
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.synchronized

/**
 * The readers of one obf file, one for each [BinaryMapReaderResourceType], so that the searches, the
 * routing and the poi of a file each read it through a file handle of their own.
 *
 * A copy of `BinaryMapReaderResource` in the android app, which stays there; this copy is for iOS.
 * It differs in two things:
 * - every reader walks the file's structure itself, rather than sharing the structure the first
 *   reader walked: reading fills that structure in as it goes (the poi categories, the map encoding
 *   rules, the routing trees), so readers on different threads must not share it;
 * - the first reader is opened when a reader is first asked for, not when the file is indexed, and
 *   it is then the reader of the first type asked for, so a file read for one purpose is walked once.
 *
 * A file is walked outside the lock, so that closing the file or asking what it holds does not wait
 * for a walk on another thread. A file that cannot be read - missing, being replaced, damaged - has
 * no readers: the error is logged once and every reader asked for is null, until the file is handed
 * over again as a new resource.
 */
class BinaryMapReaderResource(private val file: KFile) {

	private val lock = KLock()
	private val readers = arrayOfNulls<BinaryMapIndexReader>(BinaryMapReaderResourceType.entries.size)
	private var initialReader: BinaryMapIndexReader? = null
	private var initialReaderTaken = false
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
			val initialReader = initialReader
			if (initialReader != null && !initialReaderTaken) {
				initialReaderTaken = true
				readers[type.ordinal] = initialReader
				return initialReader
			}
		}
		val reader = open() ?: return null
		var unused: BinaryMapIndexReader? = null
		val result = synchronized(lock) {
			val opened = readers[type.ordinal]
			if (closed || opened != null) {
				// closed meanwhile, or another thread opened the same type first
				unused = reader
				opened
			} else {
				readers[type.ordinal] = reader
				if (initialReader == null) {
					initialReader = reader
					initialReaderTaken = true
				}
				reader
			}
		}
		unused?.let { close(it) }
		return result
	}

	/**
	 * A reader to ask what the file holds - its sections, their boxes - but not to read with: it is
	 * also the reader of the first type asked for, which may be reading on another thread.
	 */
	fun getShallowReader(): BinaryMapIndexReader? {
		synchronized(lock) {
			if (initialReader != null || closed || failed) {
				return initialReader
			}
		}
		val reader = open() ?: return null
		var unused: BinaryMapIndexReader? = null
		val result = synchronized(lock) {
			val initialReader = initialReader
			if (closed || initialReader != null) {
				unused = reader
				initialReader
			} else {
				this.initialReader = reader
				reader
			}
		}
		unused?.let { close(it) }
		return result
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
				val reader = readers[i]
				if (reader != null && reader !== initialReader) {
					closing.add(reader)
				}
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

	// without the lock: a walk of a big file takes hundreds of milliseconds
	private fun open(): BinaryMapIndexReader? {
		return try {
			BinaryMapIndexReader(file)
		} catch (e: Exception) {
			log.error("Fail to initialize ${file.name()}", e)
			synchronized(lock) { failed = true }
			null
		}
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

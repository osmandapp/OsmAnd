package net.osmand.shared.binary

import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KLock
import net.osmand.shared.util.synchronized

/**
 * The obf files of the app and their readers, kept open between requests: the part of
 * `ResourceManager` in the android app that holds `fileReaders`, so that the app gets its readers
 * from here and does not open, read or close one itself.
 *
 * The platform hands over the files it has with [setFiles], at start and whenever they change, with
 * what each may be used for, as android decides when it indexes a file. A file it no longer has is
 * closed, and so is one whose time stamp or size changed, since an update writes the new file at the
 * path of the old one; the next request opens it again. As in android's `closeFile`, a closed file's
 * readers are closed at once: a search or a route still reading through one fails inside the shared
 * code, which catches it, and the next one reads the new file. The files are kept in android's
 * order, [KAlgorithms.compareFileVersions] of their names.
 *
 * A file is opened the first time something asks about it, on the thread that asks, so the first
 * [getQuickSearchFiles], [getRoutingMapFiles] or [getAmenityRepositories] walks every file it looks
 * at: not for the main thread. Android walks them when it indexes the files, from a cache of their
 * structure.
 */
class ObfReaderRegistry {

	/**
	 * A file the platform has: [useForRouting] when routes may be calculated over it,
	 * [useForPublicTransport] when public transport routes may.
	 */
	class ObfFile(val path: String, val useForRouting: Boolean, val useForPublicTransport: Boolean)

	private data class FileStamp(val lastModified: Long, val length: Long)

	private class Entry(val path: String, val resource: BinaryMapReaderResource, val stamp: FileStamp)

	private val lock = KLock()
	private var entries: List<Entry> = emptyList()
	private var entriesByPath: Map<String, Entry> = emptyMap()
	private val amenityRepositories = HashMap<String, BinaryAmenityIndexRepository>()

	fun setFiles(files: List<ObfFile>) {
		val closing = ArrayList<BinaryMapReaderResource>()
		synchronized(lock) {
			val next = ArrayList<Entry>(files.size)
			val nextByPath = HashMap<String, Entry>()
			for (obfFile in files) {
				if (nextByPath.containsKey(obfFile.path)) {
					continue
				}
				val file = KFile(obfFile.path)
				val stamp = FileStamp(file.lastModified(), file.length())
				val old = entriesByPath[obfFile.path]
				val resource = if (old != null && old.stamp == stamp) old.resource else BinaryMapReaderResource(file)
				resource.setUseForRouting(obfFile.useForRouting)
				resource.setUseForPublicTransport(obfFile.useForPublicTransport)
				val entry = Entry(obfFile.path, resource, stamp)
				next.add(entry)
				nextByPath[obfFile.path] = entry
			}
			for (old in entries) {
				if (nextByPath[old.path]?.resource !== old.resource) {
					closing.add(old.resource)
					amenityRepositories.remove(old.path)
				}
			}
			entries = next.sortedWith { e1, e2 ->
				KAlgorithms.compareFileVersions(e1.resource.getFileName(), e2.resource.getFileName())
			}
			entriesByPath = nextByPath
		}
		for (resource in closing) {
			resource.close()
		}
	}

	/** The files in android's order. */
	fun getFileReaders(): List<BinaryMapReaderResource> = synchronized(lock) { entries }.map { it.resource }

	fun getFileReader(path: String): BinaryMapReaderResource? = synchronized(lock) { entriesByPath[path]?.resource }

	fun getRoutingMapFiles(): List<BinaryMapIndexReader> {
		val readers = ArrayList<BinaryMapIndexReader>()
		for (resource in getFileReaders()) {
			if (resource.isUseForRouting() && resource.getShallowReader()?.containsRouteData() == true) {
				resource.getReader(BinaryMapReaderResourceType.ROUTING)?.let { readers.add(it) }
			}
		}
		return readers
	}

	fun getReverseGeocodingMapFiles(): List<BinaryMapIndexReader> =
		getFileReaders().mapNotNull { it.getReader(BinaryMapReaderResourceType.REVERSE_GEOCODING) }

	/** The readers of the files with pois or addresses, but those whose name ends with one of [ignoreExtensions]. */
	fun getQuickSearchFiles(ignoreExtensions: List<String>): List<BinaryMapIndexReader> {
		val readers = ArrayList<BinaryMapIndexReader>()
		for (resource in getFileReaders()) {
			if (ignoreExtensions.any { resource.getFileName().endsWith(it) }) {
				continue
			}
			val shallowReader = resource.getShallowReader()
			if (shallowReader != null && (shallowReader.containsPoiData() || shallowReader.containsAddressData())) {
				resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)?.let { readers.add(it) }
			}
		}
		return readers
	}

	/**
	 * The poi of the file at [path], read through its [BinaryMapReaderResourceType.POI] reader; one
	 * repository for each file, so that the threads reading its pois take turns. Null when the
	 * file was not handed over.
	 */
	fun getAmenityRepository(path: String): BinaryAmenityIndexRepository? = synchronized(lock) {
		val resource = entriesByPath[path]?.resource ?: return@synchronized null
		amenityRepositories.getOrPut(path) {
			BinaryAmenityIndexRepository(resource.getFile()) { resource.getReader(BinaryMapReaderResourceType.POI) }
		}
	}

	/** The repositories of the files with pois, in android's order of the files. */
	fun getAmenityRepositories(): List<BinaryAmenityIndexRepository> {
		val repositories = ArrayList<BinaryAmenityIndexRepository>()
		for (entry in synchronized(lock) { entries }) {
			if (entry.resource.getShallowReader()?.containsPoiData() == true) {
				getAmenityRepository(entry.path)?.let { repositories.add(it) }
			}
		}
		return repositories
	}

	fun close() {
		setFiles(emptyList())
	}
}

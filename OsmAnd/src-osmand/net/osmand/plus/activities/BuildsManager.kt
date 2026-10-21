package net.osmand.plus.activities

import android.os.SystemClock
import net.osmand.PlatformUtil
import net.osmand.osm.io.NetworkUtils
import net.osmand.plus.OsmandApplication
import net.osmand.plus.Version
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_META_NAME
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_NAME
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_PART_NAME
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.text.ParseException
import java.util.Date
import java.util.Properties
import java.util.concurrent.Executors

/**
 * The list of OsmAnd builds and the download of one of them. Lives in the application, not in
 * the screen: a rotation or leaving the screen neither restarts the list nor stops the download.
 *
 * The download is resumable. The partial file is kept with the validator of the response it
 * came from (ETag, or Last-Modified), and the next attempt asks for the rest with Range +
 * If-Range. The builds in latest-night-build/ are replaced every night under the same path, so
 * without If-Range a resume could glue the start of one build to the end of another.
 */
class BuildsManager private constructor(private val app: OsmandApplication) {

	sealed class ListState {
		object Loading : ListState()
		class Loaded(val builds: List<OsmAndBuild>) : ListState()
		class Failed(val message: String?) : ListState()
	}

	sealed class DownloadState {
		object Idle : DownloadState()
		class Running(val build: OsmAndBuild, val downloaded: Long, val total: Long) : DownloadState()
		/** Stopped by the user or after the retries ran out; [error] is null for a pause. */
		class Stopped(val build: OsmAndBuild, val downloaded: Long, val total: Long, val error: String?) : DownloadState()
		class Completed(val build: OsmAndBuild, val file: File) : DownloadState()
	}

	interface Listener {
		fun onListStateChanged(state: ListState) = Unit
		fun onDownloadStateChanged(state: DownloadState) = Unit
	}

	private val listeners = mutableListOf<Listener>()
	private val listExecutor = Executors.newSingleThreadExecutor()
	private val downloadExecutor = Executors.newSingleThreadExecutor()

	private val apkFile: File get() = app.getAppPath(DOWNLOAD_BUILD_NAME)
	private val partFile: File get() = app.getAppPath(DOWNLOAD_BUILD_PART_NAME)
	private val metaFile: File get() = app.getAppPath(DOWNLOAD_BUILD_META_NAME)

	var listState: ListState? = null
		private set
	var downloadState: DownloadState = DownloadState.Idle
		private set

	@Volatile
	private var downloadId = 0
	@Volatile
	private var connection: HttpURLConnection? = null

	private var pendingInstall = false
	private var previousInstallDate: String? = null

	init {
		restoreStoppedDownload()
	}

	fun addListener(listener: Listener) {
		listeners.add(listener)
	}

	fun removeListener(listener: Listener) {
		listeners.remove(listener)
	}

	fun loadBuilds(force: Boolean) {
		val state = listState
		if (state is ListState.Loading || state is ListState.Loaded && !force) {
			return
		}
		setListState(ListState.Loading)
		listExecutor.execute {
			val newState = try {
				ListState.Loaded(fetchBuilds())
			} catch (e: Exception) {
				LOG.error("Could not load the list of builds", e)
				ListState.Failed(e.message)
			}
			app.runInUIThread { setListState(newState) }
		}
	}

	fun startDownload(build: OsmAndBuild) {
		val current = downloadState
		if (current is DownloadState.Running) {
			if (current.build.path == build.path) {
				return
			}
			stopDownload()
		}
		val id = ++downloadId
		// a resumed build starts from what is already on disk, a new one from zero
		val resumed = current is DownloadState.Stopped && current.build.path == build.path
		setDownloadState(DownloadState.Running(build,
			if (resumed) (current as DownloadState.Stopped).downloaded else 0,
			if (resumed) (current as DownloadState.Stopped).total else -1))
		downloadExecutor.execute { downloadWithRetries(id, build) }
	}

	/** Keeps the partial file, so [startDownload] of the same build resumes it. */
	fun stopDownload() {
		val state = downloadState as? DownloadState.Running ?: return
		downloadId++
		// closing a TLS socket may write to the network, which is not allowed on the main thread
		connection?.let { Thread { it.disconnect() }.start() }
		setDownloadState(DownloadState.Stopped(state.build, partFile.length(), state.total, null))
	}

	fun discardDownload() {
		stopDownload()
		downloadExecutor.execute {
			partFile.delete()
			metaFile.delete()
			apkFile.delete()
		}
		setDownloadState(DownloadState.Idle)
	}

	fun getInstalledDate(): Date? {
		val value = app.settings.CONTRIBUTION_INSTALL_APP_DATE.get() ?: return null
		return try {
			OsmAndBuild.DATE_FORMAT.parse(value)
		} catch (e: ParseException) {
			null
		}
	}

	/**
	 * Remembers the date of the build as the installed one before the installer opens: a
	 * successful update kills this process, so there would be no later moment to do it. Builds
	 * of other apps (tracker, samples) do not replace this app and are not remembered.
	 */
	fun onInstallStarted(build: OsmAndBuild, file: File) {
		val packageName = app.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName
		val date = build.date
		if (packageName == app.packageName && date != null) {
			val preference = app.settings.CONTRIBUTION_INSTALL_APP_DATE
			previousInstallDate = preference.get()
			pendingInstall = true
			preference.set(OsmAndBuild.DATE_FORMAT.format(date))
		}
	}

	fun onInstallResult(installed: Boolean) {
		if (pendingInstall && !installed) {
			app.settings.CONTRIBUTION_INSTALL_APP_DATE.set(previousInstallDate)
		}
		pendingInstall = false
	}

	private fun fetchBuilds(): List<OsmAndBuild> {
		val builds = ArrayList<OsmAndBuild>()
		val connection = NetworkUtils.getHttpURLConnection(URL_TO_RETRIEVE_BUILDS)
		connection.connectTimeout = CONNECT_TIMEOUT_MS
		connection.readTimeout = READ_TIMEOUT_MS
		try {
			val parser = XmlPullParserFactory.newInstance().newPullParser()
			parser.setInput(connection.inputStream, "UTF-8")
			val freeVersion = Version.isFreeVersion(app)
			while (true) {
				val next = parser.next()
				if (next == XmlPullParser.END_DOCUMENT) {
					break
				}
				if (next != XmlPullParser.START_TAG || parser.name != "build"
					|| !"osmand".equals(parser.getAttributeValue(null, "type"), true)) {
					continue
				}
				val path = parser.getAttributeValue(null, "path") ?: continue
				val date = parser.getAttributeValue(null, "timestamp")?.toLongOrNull()?.let { Date(it) }
				val build = OsmAndBuild(path, parser.getAttributeValue(null, "size"), date,
					parser.getAttributeValue(null, "tag"))
				if (!freeVersion || path.contains("default")) {
					builds.add(build)
				}
			}
		} finally {
			connection.disconnect()
		}
		return builds
	}

	private fun downloadWithRetries(id: Int, build: OsmAndBuild) {
		var attempt = 0
		while (true) {
			try {
				download(id, build)
				return
			} catch (e: IOException) {
				if (id != downloadId) {
					return
				}
				LOG.warn("Build download attempt $attempt failed: ${e.message}")
				if (e is HttpStatusException || attempt >= RETRY_DELAYS_MS.size) {
					val total = readMeta()?.total ?: -1
					app.runInUIThread {
						if (id == downloadId) {
							setDownloadState(DownloadState.Stopped(build, partFile.length(), total,
								e.message ?: e.javaClass.simpleName))
						}
					}
					return
				}
				SystemClock.sleep(RETRY_DELAYS_MS[attempt++])
				if (id != downloadId) {
					return
				}
			}
		}
	}

	private fun download(id: Int, build: OsmAndBuild) {
		val meta = readMeta()
		var offset = partFile.length()
		if (meta == null || meta.path != build.path || offset == 0L) {
			offset = 0
			partFile.delete()
		}
		val connection = NetworkUtils.getHttpURLConnection(URL_GET_BUILD + build.path)
		this.connection = connection
		try {
			connection.connectTimeout = CONNECT_TIMEOUT_MS
			connection.readTimeout = READ_TIMEOUT_MS
			// Content-Length and Content-Range must describe the file itself, not a gzip of it
			connection.setRequestProperty("Accept-Encoding", "identity")
			if (offset > 0 && meta != null) {
				connection.setRequestProperty("Range", "bytes=$offset-")
				connection.setRequestProperty("If-Range", meta.validator)
			}
			val code = connection.responseCode
			if (id != downloadId) {
				return
			}
			val total: Long
			when (code) {
				HttpURLConnection.HTTP_PARTIAL -> {
					val range = parseContentRange(connection.getHeaderField("Content-Range"))
					if (range == null || range.first != offset) {
						// the server sent another piece than asked for - start the file over
						discardPart()
						throw IOException("Unexpected Content-Range")
					}
					total = range.second
				}
				HttpURLConnection.HTTP_OK -> {
					// no range support, or the build behind the path has changed since
					offset = 0
					total = connection.contentLengthLong
					val validator = connection.getHeaderField("ETag")
						?: connection.getHeaderField("Last-Modified")
					if (validator != null) {
						writeMeta(Meta(build, validator, total))
					} else {
						metaFile.delete()
					}
				}
				HTTP_RANGE_NOT_SATISFIABLE -> {
					discardPart()
					throw IOException("HTTP $code")
				}
				else -> throw HttpStatusException(code)
			}
			copy(id, build, connection, offset, total)
		} finally {
			connection.disconnect()
			this.connection = null
		}
	}

	private fun copy(id: Int, build: OsmAndBuild, connection: HttpURLConnection, offset: Long, total: Long) {
		var downloaded = offset
		var lastNotification = 0L
		connection.inputStream.use { input ->
			FileOutputStream(partFile, offset > 0).use { output ->
				val buffer = ByteArray(BUFFER_SIZE)
				while (true) {
					val read = input.read(buffer)
					if (read == -1 || id != downloadId) {
						break
					}
					output.write(buffer, 0, read)
					downloaded += read
					val now = SystemClock.elapsedRealtime()
					if (now - lastNotification > PROGRESS_INTERVAL_MS) {
						lastNotification = now
						val progress = downloaded
						app.runInUIThread {
							if (id == downloadId) {
								setDownloadState(DownloadState.Running(build, progress, total))
							}
						}
					}
				}
			}
		}
		if (id != downloadId) {
			return
		}
		if (total > 0 && partFile.length() != total) {
			// a stream that ends early is not an error for HttpURLConnection - it is for an APK
			throw IOException("Incomplete download: ${partFile.length()} of $total bytes")
		}
		val apk = apkFile
		apk.delete()
		if (!partFile.renameTo(apk)) {
			throw IOException("Could not rename ${partFile.name}")
		}
		metaFile.delete()
		app.runInUIThread {
			if (id == downloadId) {
				setDownloadState(DownloadState.Completed(build, apk))
			}
		}
	}

	private fun discardPart() {
		partFile.delete()
		metaFile.delete()
	}

	/** After a restart of the app a stopped download is shown again, ready to resume. */
	private fun restoreStoppedDownload() {
		val meta = readMeta()
		val part = partFile
		if (meta != null && part.length() > 0) {
			val build = OsmAndBuild(meta.path, meta.size, meta.date?.let { Date(it) }, meta.tag)
			downloadState = DownloadState.Stopped(build, part.length(), meta.total, null)
		}
	}

	private fun readMeta(): Meta? {
		val file = metaFile
		if (!file.exists()) {
			return null
		}
		return try {
			val properties = Properties()
			FileInputStream(file).use { properties.load(it) }
			Meta(
				properties.getProperty("path") ?: return null,
				properties.getProperty("tag"),
				properties.getProperty("size"),
				properties.getProperty("date")?.toLongOrNull(),
				properties.getProperty("validator") ?: return null,
				properties.getProperty("total")?.toLongOrNull() ?: -1)
		} catch (e: IOException) {
			LOG.error("Could not read ${file.name}", e)
			null
		}
	}

	private fun writeMeta(meta: Meta) {
		val properties = Properties()
		properties.setProperty("path", meta.path)
		meta.tag?.let { properties.setProperty("tag", it) }
		meta.size?.let { properties.setProperty("size", it) }
		meta.date?.let { properties.setProperty("date", it.toString()) }
		properties.setProperty("validator", meta.validator)
		properties.setProperty("total", meta.total.toString())
		FileOutputStream(metaFile).use { properties.store(it, null) }
	}

	private fun setListState(state: ListState) {
		listState = state
		for (listener in listeners.toList()) {
			listener.onListStateChanged(state)
		}
	}

	private fun setDownloadState(state: DownloadState) {
		downloadState = state
		for (listener in listeners.toList()) {
			listener.onDownloadStateChanged(state)
		}
	}

	private class Meta(
		val path: String,
		val tag: String?,
		val size: String?,
		val date: Long?,
		val validator: String,
		val total: Long
	) {
		constructor(build: OsmAndBuild, validator: String, total: Long) :
				this(build.path, build.tag, build.size, build.date?.time, validator, total)
	}

	private class HttpStatusException(code: Int) : IOException("HTTP $code")

	companion object {

		private val LOG = PlatformUtil.getLog(BuildsManager::class.java)

		private const val URL_GET_BUILD = "https://osmand.net/"
		private const val URL_TO_RETRIEVE_BUILDS = "https://osmand.net/builds"
		const val LATEST_BUILDS_FOLDER = "latest-night-build/"

		private const val CONNECT_TIMEOUT_MS = 15_000
		private const val READ_TIMEOUT_MS = 30_000
		private const val BUFFER_SIZE = 64 * 1024
		private const val PROGRESS_INTERVAL_MS = 200L
		private const val HTTP_RANGE_NOT_SATISFIABLE = 416
		private val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000, 10_000)

		@Volatile
		private var instance: BuildsManager? = null

		@JvmStatic
		fun getInstance(app: OsmandApplication): BuildsManager =
			instance ?: synchronized(this) {
				instance ?: BuildsManager(app).also { instance = it }
			}

		/** "bytes 100-199/1000" -> (100, 1000); the total is -1 when the server sends "*". */
		@JvmStatic
		fun parseContentRange(value: String?): Pair<Long, Long>? {
			val match = value?.let { Regex("""bytes\s+(\d+)-\d+/(\d+|\*)""").find(it) } ?: return null
			val start = match.groupValues[1].toLong()
			val total = match.groupValues[2].toLongOrNull() ?: -1
			return Pair(start, total)
		}
	}
}

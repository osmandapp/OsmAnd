package net.osmand.shared.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import kotlinx.datetime.Instant
import net.osmand.shared.KException
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.io.KFile
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Locale

actual object PlatformUtil {

	private var context: WeakReference<Context>? = null
	private var appDir: File? = null
	private var gpxDir: File? = null
	private lateinit var osmAndContext: OsmAndContext

	private var sqliteApi: SQLiteAPI? = null

	fun initialize(context: Context, appDir: File, gpxDir: File, osmAndContext: OsmAndContext) {
		this.context = WeakReference(context)
		this.osmAndContext = osmAndContext
		this.appDir = appDir
		this.gpxDir = gpxDir

		sqliteApi = SQLiteAPIImpl(context)
		Localization.initialize(context)
	}

	actual fun getOsmAndContext(): OsmAndContext {
		return osmAndContext
	}

	actual fun currentTimeMillis(): Long {
		return System.currentTimeMillis()
	}

	actual fun getAppDir(): KFile {
		val dir = appDir
		if (dir == null) {
			throw IllegalStateException("App dir not initialized")
		} else {
			return KFile(dir.absolutePath)
		}
	}

	actual fun getGpxDir(): KFile {
		val dir = gpxDir
		if (dir == null) {
			throw IllegalStateException("Gpx dir not initialized")
		} else {
			return KFile(dir.absolutePath)
		}
	}

	actual fun getSQLiteAPI(): SQLiteAPI {
		val sqliteApi = sqliteApi
		if (sqliteApi == null) {
			throw IllegalStateException("SQLiteAPI not initialized")
		} else {
			return sqliteApi
		}
	}

	fun getKotlinException(e: java.lang.Exception): KException {
		return KException(e.message, e)
	}

	fun getJavaException(e: KException): java.lang.Exception {
		return Exception(e.message, e)
	}

	@SuppressLint("DiscouragedApi")
	actual fun getStringResource(stringId: String): String {
		val ctx = context?.get()
		ctx?.let {
			val id =  it.resources.getIdentifier(stringId, "string", it.packageName)
			if (id != 0) {
				return it.getString(id)
			}
		}
		throw Resources.NotFoundException()
	}


	actual fun formatDate(date:Instant, pattern: String):String {
		val formatter = SimpleDateFormat(pattern, Locale.getDefault())
		return formatter.format(date)
	}

	actual fun getFileSeparator(): String {
		return File.separator
	}

	actual fun getFileLength(file: KFile): Long{
		val jFile = File(file.absolutePath())
		return jFile.length()
	}

	actual fun renameFile(src: String, dest: String): Boolean {
		val file = File(src)
		val newFile = File(dest)
		return file.exists() && file.renameTo(newFile)
	}

	actual fun createFile(path: String): Boolean {
		val file = File(path)
		if(!file.exists()) {
			return file.createNewFile()
		}
		return false
	}

}
package net.osmand.test.junit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteReadOnlyDatabaseException
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.backup.SettingsHelper
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.plus.shared.SharedUtil
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.gpx.GpxDatabase
import net.osmand.shared.gpx.GpxDbHelper
import net.osmand.shared.gpx.GpxDbUtils
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.GpxUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * OsmAnd-Issues #3331: `SQLiteDatabaseLockedException` while importing many tracks.
 *
 * A read-only `GpxDatabase.openConnection()` used to get a read-write connection without
 * write-ahead logging on Android, so it ran in rollback-journal mode and switched the shared
 * database file out of WAL whenever it was the only connection. In rollback mode a transaction
 * holds the file exclusively and every other thread that opens the database waits for it - the
 * framework compiles `PRAGMA journal_mode` on each new connection, which needs a read lock - until
 * the busy timeout of 2.5 s expires with `SQLiteDatabaseLockedException`. During an import the
 * import thread, the `GpxReader`s and the main-thread progress callback open the database for
 * every file, so the timeout is reached and the exception is fatal on the main thread and in
 * `ImportFileItemsTask`.
 */
@RunWith(AndroidJUnit4::class)
class GpxDatabaseOpenConnectionTest {

	companion object {
		private const val WAIT_TIMEOUT_MS = 180_000L
		// Busy timeout of the Android SQLite framework
		private const val BUSY_TIMEOUT_MS = 2_500L
		// Longer than two busy timeouts: the open made by openConnection() and the one repeated
		// to capture its exception
		private const val TRANSACTION_HOLD_MS = 7_000L

		private const val IMPORT_TRACKS = 1000
		private const val IMPORT_TRACK_POINTS = 300
		private const val IMPORT_TRACK_PREFIX = "gpx_db_test_"
		private const val IMPORT_COLOR = "#ff3366"
	}

	private lateinit var app: OsmandApplication
	private lateinit var database: GpxDatabase

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		database = GpxDbHelper.getGPXDatabase()
		// Another open connection would keep the file in WAL mode and hide the defect
		waitUntil("application initialization") { !app.isApplicationInitializing }
		waitUntil("GPX readers") { !GpxDbHelper.isReading() }
	}

	@Test
	fun readOnlyOpenKeepsWriteAheadLogging() {
		assertEquals("wal", journalModeAfterOpen(readonly = false))
		assertEquals("wal", journalModeAfterOpen(readonly = true))
		assertEquals("wal", journalModeAfterOpen(readonly = false))
	}

	@Test
	fun readOnlyOpenIsReadOnly() {
		withConnection(readonly = true) { assertTrue(it.isReadOnly()) }
		withConnection(readonly = false) { assertFalse(it.isReadOnly()) }
	}

	/**
	 * Reproduces the crash: a transaction on a connection opened the read-only way must not block
	 * another thread from opening the database. Before the fix that connection is in rollback mode
	 * and the open waits for the busy timeout; the test then repeats the framework open and fails
	 * with the exception it throws, "database is locked (code 5 SQLITE_BUSY): , while compiling:
	 * PRAGMA journal_mode" from SQLiteConnection.setJournalMode(), as reported in the issue. After
	 * the fix the connection is read-only and refuses the transaction, so there is nothing to wait
	 * for.
	 */
	@Test
	fun openIsNotBlockedByTransactionOnReadOnlyConnection() {
		val transaction = holdTransaction(readonly = true)
		try {
			if (transaction.refused) {
				return
			}
			val start = SystemClock.elapsedRealtime()
			val db = database.openConnection(true)
			val elapsed = SystemClock.elapsedRealtime() - start
			if (db == null) {
				val message = "could not open the database after $elapsed ms " +
					"while another thread held a transaction"
				val failure = frameworkOpenFailure() ?: throw AssertionError(message)
				failure.addSuppressed(AssertionError(message))
				throw failure
			}
			try {
				assertTrue(rowCount(db) >= 0)
			} finally {
				db.close()
			}
		} finally {
			transaction.release()
		}
	}

	/**
	 * The guarantee the fix relies on: with write-ahead logging a transaction on a writable
	 * connection never blocks a reader. Fails within the busy timeout if writable opens ever stop
	 * requesting WAL, because in a rollback journal the transaction locks the whole file.
	 */
	@Test
	fun readIsNotBlockedByTransactionOnWritableConnection() {
		val transaction = holdTransaction(readonly = false)
		try {
			val start = SystemClock.elapsedRealtime()
			withConnection(readonly = true) { assertTrue(rowCount(it) >= 0) }
			val elapsed = SystemClock.elapsedRealtime() - start
			assertTrue("read waited $elapsed ms for a transaction on a writable connection",
				elapsed < BUSY_TIMEOUT_MS)
		} finally {
			transaction.release()
		}
	}

	/**
	 * The scenario of the issue: an .osf with [IMPORT_TRACKS] tracks imported through
	 * FileSettingsHelper, which has the import thread, the GpxReaders and the main-thread progress
	 * callback open the database for every file. The journal mode of the file is sampled all along
	 * through a genuinely read-only connection: before the fix it flipped between WAL and TRUNCATE
	 * thousands of times and the process usually crashed after a few hundred files; after it the
	 * file must stay in WAL and every track must be stored with its imported appearance and
	 * analysed. Takes a few minutes; the imported tracks are removed again at the end.
	 */
	@LargeTest
	@Test
	fun importsManyTracks() {
		val tracksDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR)
		val trackFiles = (1..IMPORT_TRACKS).map { File(tracksDir, trackName(it)) }
		val archive = app.getAppPath("gpx_database_test.osf")
		removeImportedTracks(archive, trackFiles) // left over by a crashed earlier run
		val journalModes = sampleJournalModes()
		try {
			writeArchive(archive)
			val items = collect(archive)
			assertEquals(IMPORT_TRACKS, items.size)
			assertTrue("import reported failure", import(archive, items))
			waitUntil("GPX readers after import") { !GpxDbHelper.isReading() }
			assertEquals("journal modes of the file seen during the import", setOf("wal"), journalModes.finish())

			val expectedColor = GpxUtilities.parseColor(IMPORT_COLOR)
			for (file in trackFiles) {
				val item = GpxDbHelper.getItem(SharedUtil.kFile(file), false)
					?: throw AssertionError("${file.name} is missing in the GPX database")
				assertEquals("${file.name} colour", expectedColor, item.getParameter(GpxParameter.COLOR))
				assertFalse("${file.name} is not analysed", GpxDbUtils.isAnalyseNeeded(item))
			}
		} finally {
			journalModes.finish()
			removeImportedTracks(archive, trackFiles)
		}
	}

	private class JournalModeSampler(private val path: String) : Thread("gpx-db-journal-mode") {
		private val running = AtomicBoolean(true)
		private val modes = Collections.synchronizedSet(mutableSetOf<String>())

		override fun run() {
			while (running.get()) {
				try {
					// A read-only connection never changes the journal mode itself
					val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
					try {
						db.rawQuery("PRAGMA journal_mode", null).use { cursor ->
							if (cursor.moveToFirst()) {
								modes.add(cursor.getString(0).lowercase())
							}
						}
					} finally {
						db.close()
					}
				} catch (e: SQLiteException) {
					// locked or not created yet: sample again
				}
				SystemClock.sleep(50L)
			}
		}

		fun finish(): Set<String> {
			running.set(false)
			join(WAIT_TIMEOUT_MS)
			return modes.toSet()
		}
	}

	private fun sampleJournalModes(): JournalModeSampler =
		JournalModeSampler(app.getDatabasePath(GpxDatabase.DB_NAME).path).apply { start() }

	private class HeldTransaction(
		val refused: Boolean,
		private val thread: Thread,
		private val releaseLatch: CountDownLatch,
		private val error: AtomicReference<Throwable?>
	) {
		fun release() {
			releaseLatch.countDown()
			thread.join(TRANSACTION_HOLD_MS + WAIT_TIMEOUT_MS)
			error.get()?.let { throw AssertionError("transaction thread failed", it) }
		}
	}

	/**
	 * Opens a connection on another thread and starts a transaction on it (BEGIN EXCLUSIVE),
	 * held until [HeldTransaction.release] or [TRANSACTION_HOLD_MS]. A read-only connection
	 * refuses the transaction, which is reported through [HeldTransaction.refused].
	 */
	private fun holdTransaction(readonly: Boolean): HeldTransaction {
		val started = CountDownLatch(1)
		val releaseLatch = CountDownLatch(1)
		val refused = AtomicReference(false)
		val error = AtomicReference<Throwable?>()
		val thread = Thread({
			try {
				withConnection(readonly) { db ->
					try {
						db.beginTransaction()
					} catch (e: SQLiteReadOnlyDatabaseException) {
						refused.set(true)
					}
					started.countDown()
					if (!refused.get()) {
						releaseLatch.await(TRANSACTION_HOLD_MS, TimeUnit.MILLISECONDS)
						db.endTransaction()
					}
				}
			} catch (t: Throwable) {
				error.set(t)
				started.countDown()
			}
		}, "gpx-db-transaction")
		thread.start()
		assertTrue("transaction did not start", started.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS))
		error.get()?.let { throw AssertionError("transaction thread failed", it) }
		return HeldTransaction(refused.get(), thread, releaseLatch, error)
	}

	/** The open that SQLiteAPIImpl.getOrCreateDatabase() makes, with the exception it swallowed. */
	private fun frameworkOpenFailure(): SQLiteException? {
		return try {
			app.openOrCreateDatabase(GpxDatabase.DB_NAME,
				Context.MODE_PRIVATE or Context.MODE_ENABLE_WRITE_AHEAD_LOGGING, null).close()
			null
		} catch (e: SQLiteException) {
			e
		}
	}

	private fun journalModeAfterOpen(readonly: Boolean): String =
		withConnection(readonly) { queryString(it, "PRAGMA journal_mode").lowercase() }

	private fun <T> withConnection(readonly: Boolean, action: (SQLiteConnection) -> T): T {
		val db = database.openConnection(readonly)
			?: throw AssertionError("could not open the GPX database readonly=$readonly")
		try {
			return action(db)
		} finally {
			db.close()
		}
	}

	private fun rowCount(db: SQLiteConnection): Int =
		queryString(db, "SELECT COUNT(*) FROM ${GpxDatabase.GPX_TABLE_NAME}").toInt()

	private fun queryString(db: SQLiteConnection, sql: String): String {
		val cursor = db.rawQuery(sql, null) ?: throw AssertionError("no cursor for: $sql")
		try {
			assertTrue("no row for: $sql", cursor.moveToFirst())
			return cursor.getString(0)
		} finally {
			cursor.close()
		}
	}

	private fun waitUntil(what: String, condition: () -> Boolean) {
		val deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MS
		while (!condition()) {
			assertTrue("timed out waiting for $what", SystemClock.elapsedRealtime() < deadline)
			SystemClock.sleep(100L)
		}
	}

	private fun collect(archive: File): List<SettingsItem> {
		val collected = CountDownLatch(1)
		var items: List<SettingsItem> = emptyList()
		app.fileSettingsHelper.collectSettings(archive, "", SettingsHelper.VERSION) { _, _, list ->
			items = list
			collected.countDown()
		}
		assertTrue("collect did not finish", collected.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS))
		return items
	}

	private fun import(archive: File, items: List<SettingsItem>): Boolean {
		val imported = CountDownLatch(1)
		var success = false
		app.fileSettingsHelper.importSettings(archive, items, "", SettingsHelper.VERSION,
			object : ImportListener {
				override fun onImportFinished(succeed: Boolean, needRestart: Boolean, items: List<SettingsItem>) {
					success = succeed
					imported.countDown()
				}
			})
		assertTrue("import did not finish", imported.await(30, TimeUnit.MINUTES))
		return success
	}

	private fun removeImportedTracks(archive: File, trackFiles: List<File>) {
		archive.delete()
		trackFiles.forEach { it.delete() }
		GpxDbHelper.remove(trackFiles.map { SharedUtil.kFile(it) })
	}

	private fun trackName(index: Int) = String.format(Locale.US, "%s%04d.gpx", IMPORT_TRACK_PREFIX, index)

	private fun writeArchive(archive: File) {
		val random = Random(3331)
		ZipOutputStream(FileOutputStream(archive).buffered()).use { zip ->
			val items = (1..IMPORT_TRACKS).joinToString(",") {
				"{\"type\":\"GPX\",\"file\":\"tracks/${trackName(it)}\",\"color\":\"$IMPORT_COLOR\"," +
					"\"width\":\"bold\",\"show_arrows\":true,\"show_start_finish\":true}"
			}
			zip.putNextEntry(ZipEntry("items.json"))
			zip.write("{\"version\":${SettingsHelper.VERSION},\"items\":[$items]}".toByteArray())
			zip.closeEntry()
			for (index in 1..IMPORT_TRACKS) {
				zip.putNextEntry(ZipEntry("tracks/" + trackName(index)))
				zip.write(gpx(index, random).toByteArray())
				zip.closeEntry()
			}
		}
	}

	private fun gpx(index: Int, random: Random): String {
		val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
			.apply { timeZone = TimeZone.getTimeZone("UTC") }
		val sb = StringBuilder(IMPORT_TRACK_POINTS * 120)
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		sb.append("<gpx version=\"1.1\" creator=\"GpxDatabaseOpenConnectionTest\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
		sb.append("<metadata><name>Test track ").append(index).append("</name></metadata>\n")
		sb.append("<trk><name>Test track ").append(index).append("</name><trkseg>\n")
		var lat = 48.0 + random.nextDouble() * 4.0
		var lon = 22.0 + random.nextDouble() * 18.0
		var ele = 100.0 + random.nextDouble() * 500.0
		var millis = 1_700_000_000_000L + index * 86_400_000L
		repeat(IMPORT_TRACK_POINTS) {
			lat += (random.nextDouble() - 0.4) * 0.0004
			lon += (random.nextDouble() - 0.4) * 0.0004
			ele += (random.nextDouble() - 0.5) * 4.0
			millis += 5_000L + random.nextInt(3_000)
			sb.append("<trkpt lat=\"").append(String.format(Locale.US, "%.6f", lat))
				.append("\" lon=\"").append(String.format(Locale.US, "%.6f", lon)).append("\">")
				.append("<ele>").append(String.format(Locale.US, "%.1f", ele)).append("</ele>")
				.append("<time>").append(time.format(Date(millis))).append("</time></trkpt>\n")
		}
		sb.append("</trkseg></trk></gpx>\n")
		return sb.toString()
	}
}

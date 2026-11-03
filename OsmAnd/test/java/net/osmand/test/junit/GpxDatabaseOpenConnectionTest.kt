package net.osmand.test.junit

import android.content.Context
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteReadOnlyDatabaseException
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.gpx.GpxDatabase
import net.osmand.shared.gpx.GpxDbHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
		// Longer than two busy timeouts (2.5 s each) of the Android SQLite framework: the open
		// made by openConnection() and the one repeated to capture its exception
		private const val TRANSACTION_HOLD_MS = 7_000L
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
	 * another thread from opening the database. Before the fix the open waited for the busy
	 * timeout and the framework threw the exception of the issue, "database is locked (code 5
	 * SQLITE_BUSY): , while compiling: PRAGMA journal_mode" from SQLiteConnection.setJournalMode().
	 * In the import the same lock hit the second pooled connection, opened by getVersion(), where
	 * nothing catches it; here it hits the first one, which SQLiteAPIImpl turns into null, so the
	 * failure repeats that open and fails with the exception it throws. After the fix the
	 * connection is really read-only and refuses the transaction, so there is nothing to wait for.
	 */
	@Test
	fun openIsNotBlockedByTransactionOnReadOnlyConnection() {
		val transactionStarted = CountDownLatch(1)
		val releaseTransaction = CountDownLatch(1)
		val transactionRefused = AtomicBoolean(false)
		val writerError = AtomicReference<Throwable?>()
		val writer = Thread({
			try {
				val db = database.openConnection(true)
					?: throw AssertionError("writer could not open the database")
				try {
					try {
						db.beginTransaction()
					} catch (e: SQLiteReadOnlyDatabaseException) {
						transactionRefused.set(true)
					}
					transactionStarted.countDown()
					if (!transactionRefused.get()) {
						releaseTransaction.await(TRANSACTION_HOLD_MS, TimeUnit.MILLISECONDS)
						db.endTransaction()
					}
				} finally {
					db.close()
				}
			} catch (t: Throwable) {
				writerError.set(t)
				transactionStarted.countDown()
			}
		}, "gpx-db-transaction")
		writer.start()
		try {
			assertTrue("transaction did not start",
				transactionStarted.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS))
			assertNull("writer failed: ${writerError.get()}", writerError.get())
			if (transactionRefused.get()) {
				return
			}

			val start = SystemClock.elapsedRealtime()
			val db = database.openConnection(true)
			val elapsed = SystemClock.elapsedRealtime() - start
			if (db == null) {
				val message = "could not open the database after $elapsed ms " +
					"while another thread held a transaction"
				// Fail with the exception of the issue itself, as the framework threw it
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
			releaseTransaction.countDown()
			writer.join(TRANSACTION_HOLD_MS + WAIT_TIMEOUT_MS)
		}
		assertNull("writer failed: ${writerError.get()}", writerError.get())
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
}

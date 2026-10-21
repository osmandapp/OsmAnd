package net.osmand.test.junit

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.helpers.OsmandBackupAgent
import net.osmand.plus.settings.backend.OsmandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A restore from the Android key/value backup must not bring back the install referrer.
 *
 * [OsmandBackupAgent] backs up the whole global preferences file, so a reinstall or a new device
 * got `install_referrer` and `install_referrer_checked` of the old install back. The checked flag
 * kept the new install from ever reading its own referrer, and the old campaign was then sent
 * together with the reset install age (`FIRST_INSTALLED` lives in a file that is not backed up).
 *
 * The agent runs on a context that prefixes preference file names, so the test never touches the
 * preferences of the app under test.
 */
@RunWith(AndroidJUnit4::class)
class OsmandBackupAgentRestoreTest {

	companion object {
		private const val PREFIX = "osmand_backup_agent_test_"
		private const val UNRELATED_KEY = "unrelated_key"
	}

	private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
	private val settings: OsmandSettings = (context.applicationContext as OsmandApplication).settings
	private val prefsName = PREFIX + OsmandSettings.getSharedPreferencesName(null)

	@After
	fun tearDown() {
		context.deleteSharedPreferences(prefsName)
	}

	@Test
	fun restoreDropsInstallReferrer() {
		val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
		prefs.edit()
			.putString(settings.INSTALL_REFERRER.id, "utm_source=old_install")
			.putBoolean(settings.INSTALL_REFERRER_CHECKED.id, true)
			.putString(UNRELATED_KEY, "restored")
			.commit()

		IsolatedBackupAgent(context).onRestoreFinished()

		assertFalse(prefs.contains(settings.INSTALL_REFERRER.id))
		assertFalse(prefs.contains(settings.INSTALL_REFERRER_CHECKED.id))
		assertEquals("restored", prefs.getString(UNRELATED_KEY, null))
	}

	private class IsolatedBackupAgent(base: Context) : OsmandBackupAgent() {
		init {
			attachBaseContext(PrefixedPreferencesContext(base))
		}
	}

	private class PrefixedPreferencesContext(base: Context) : ContextWrapper(base) {
		override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
			super.getSharedPreferences(PREFIX + name, mode)
	}
}

package net.osmand.test.junit

import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** A configuration change keeps the language OsmAnd applies itself when the system does not (#14894). */
@RunWith(AndroidJUnit4::class)
class AppLocaleConfigurationChangeTest {

	private val instrumentation = InstrumentationRegistry.getInstrumentation()
	private val app = instrumentation.targetContext.applicationContext as OsmandApplication
	private val savedLocale = app.settings.PREFERRED_LOCALE.get()

	@After
	fun tearDown() = setPreferredLocale(savedLocale)

	@Test
	fun appStringsKeepPreferredLocaleAfterConfigurationChange() {
		val systemLocale = Resources.getSystem().configuration.locales[0]
		val language = if (systemLocale.language == "de") "fr" else "de"
		setPreferredLocale(language)
		val expected = app.localeHelper.getLocalizedResources(Locale(language)).getString(R.string.shared_string_cancel)
		assertEquals(expected, app.getString(R.string.shared_string_cancel))

		// the system sends its own language with every configuration change
		val newConfig = Configuration(app.resources.configuration)
		newConfig.setLocale(systemLocale)
		instrumentation.runOnMainSync { app.onConfigurationChanged(newConfig) }

		assertEquals(expected, app.getString(R.string.shared_string_cancel))
	}

	private fun setPreferredLocale(locale: String) {
		// past the setting's listener, which hands the language to the system on Android 13+
		val settings = app.settings
		settings.settingsAPI.edit(settings.globalPreferences).putString(settings.PREFERRED_LOCALE.id, locale).commit()
		app.localeHelper.checkPreferredLocale()
	}
}

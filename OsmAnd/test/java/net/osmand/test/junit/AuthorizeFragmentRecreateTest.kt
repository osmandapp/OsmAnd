package net.osmand.test.junit

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.backup.ui.AuthorizeFragment
import net.osmand.plus.backup.ui.LoginDialogType
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The OsmAnd Cloud sign-in screen must keep what was entered and shown when the activity is
 * recreated, as on a rotation. Its steps share view ids, so their fields used to save nothing.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AuthorizeFragmentRecreateTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@Test
	fun signInKeepsTextErrorAndContinue() {
		show(LoginDialogType.SIGN_IN)
		onScreen { screen ->
			val step = screen.findViewById<View>(R.id.sign_in_container)
			step.field().setText("abc")
			screen.findViewById<View>(R.id.continue_button).findViewById<View>(R.id.button_wrapper).performClick()
			assertTrue("Not an e-mail, but no error", step.error().visibility == View.VISIBLE)
			step.field().setText("abcd") // editing enables Continue again, the error stays
		}
		scenarioRule.scenario.recreate()
		onScreen { screen ->
			val step = screen.findViewById<View>(R.id.sign_in_container)
			assertEquals("abcd", step.field().text.toString())
			assertTrue("The error is gone", step.error().visibility == View.VISIBLE)
			assertEquals(app.getString(R.string.osm_live_enter_email), step.error().text.toString())
			assertTrue("Continue is disabled", screen.findViewById<View>(R.id.continue_button).isEnabled)
		}
	}

	@Test
	fun verificationKeepsCodeWithoutChoosePlan() {
		show(LoginDialogType.VERIFY_EMAIL)
		onScreen { screen -> screen.findViewById<View>(R.id.verify_email_container).field().setText("123456") }
		scenarioRule.scenario.recreate()
		onScreen { screen ->
			assertEquals("123456", screen.findViewById<View>(R.id.verify_email_container).field().text.toString())
			assertFalse("\"Get\" is shown", screen.findViewById<View>(R.id.get_button).visibility == View.VISIBLE)
		}
	}

	private fun show(type: LoginDialogType) {
		scenarioRule.scenario.onActivity { activity ->
			AuthorizeFragment.showInstance(activity.supportFragmentManager, type)
			activity.supportFragmentManager.executePendingTransactions()
		}
	}

	private fun onScreen(action: (View) -> Unit) {
		scenarioRule.scenario.onActivity { activity ->
			action(activity.supportFragmentManager.findFragmentByTag(AuthorizeFragment.TAG)!!.requireView())
		}
	}

	private fun View.field(): EditText = findViewById(R.id.edit_text)

	private fun View.error(): TextView = findViewById(R.id.error_text)
}

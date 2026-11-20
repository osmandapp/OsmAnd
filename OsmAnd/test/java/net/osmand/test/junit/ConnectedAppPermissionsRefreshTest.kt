package net.osmand.test.junit

import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.aidl.AidlPermissionGroup.MAP
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.ConnectedAppPermissionsFragment
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The permissions screen of a connected app shows the groups changed while OsmAnd was in the background. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConnectedAppPermissionsRefreshTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@Test
	fun showsGroupsChangedInBackground() {
		val api = app.aidlApi
		// stores a connection of a package that is not installed: Plugins never lists it
		val connectedApp = api.getOrCreateConnectedApp("net.osmand.test.connected_app")
		api.setGroupGranted(connectedApp, MAP, true)
		scenarioRule.scenario.onActivity { activity ->
			ConnectedAppPermissionsFragment.showInstance(activity.supportFragmentManager, connectedApp)
			activity.supportFragmentManager.executePendingTransactions()
		}
		// paused, as while the app's permission request is shown
		scenarioRule.scenario.moveToState(Lifecycle.State.STARTED)
		api.setGroupGranted(connectedApp, MAP, false)
		scenarioRule.scenario.moveToState(Lifecycle.State.RESUMED)

		scenarioRule.scenario.onActivity { activity ->
			val screen = activity.supportFragmentManager
				.findFragmentByTag(ConnectedAppPermissionsFragment::class.java.simpleName)!!.requireView()
			val mapRow = screen.findViewById<ViewGroup>(R.id.groups).getChildAt(MAP.ordinal)
			assertFalse("Map is still shown as granted", mapRow.findViewById<CompoundButton>(R.id.switch_widget).isChecked)
		}
	}
}

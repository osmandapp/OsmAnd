package net.osmand.test.junit

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.ConnectedAppPermissionsFragment
import net.osmand.plus.plugins.PluginsFragment
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Back on the permissions screen of a connected app returns to Plugins, not to the map. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConnectedAppPermissionsBackTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@Test
	fun backReturnsToPlugins() {
		scenarioRule.scenario.onActivity { activity ->
			val manager = activity.supportFragmentManager
			PluginsFragment.showInstance(manager)
			// stores a connection of a package that is not installed: Plugins never lists it
			val connectedApp = app.aidlApi.getOrCreateConnectedApp("net.osmand.test.connected_app")
			ConnectedAppPermissionsFragment.showInstance(manager, connectedApp)
			manager.executePendingTransactions()

			activity.onBackPressedDispatcher.onBackPressed()
			manager.executePendingTransactions()

			assertNull(manager.findFragmentByTag(ConnectedAppPermissionsFragment::class.java.simpleName))
			assertNotNull("Back closed Plugins too", manager.findFragmentByTag(PluginsFragment.TAG))
		}
	}
}

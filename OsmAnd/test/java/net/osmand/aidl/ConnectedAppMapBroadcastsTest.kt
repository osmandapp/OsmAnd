package net.osmand.aidl

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.aidlapi.maplayer.AMapLayer
import net.osmand.aidlapi.mapwidget.AMapWidget
import net.osmand.plus.activities.MapActivity
import net.osmand.test.common.AndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Map layers and widgets that a connected app adds or removes through AIDL must reach the open map.
 * MapActivity's AIDL receivers are not exported, and since Android 14 a broadcast sent without a
 * package does not reach them; on Android 13 and lower this test passes either way.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConnectedAppMapBroadcastsTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@After
	fun removeTestObjects() {
		app.aidlApi.removeMapLayer(PACK, ID)
		app.aidlApi.removeMapWidget(PACK, ID)
	}

	@Test
	fun layerIsAddedToAndRemovedFromMap() {
		val connectedApp = app.aidlApi.getOrCreateConnectedApp(PACK)

		assertTrue(app.aidlApi.addMapLayer(PACK, AidlMapLayerWrapper(AMapLayer(ID, "Test layer", 5.5f, null))))
		assertTrue("The layer did not reach the map", waitFor { connectedApp.mapLayers.containsKey(ID) })

		assertTrue(app.aidlApi.removeMapLayer(PACK, ID))
		assertTrue("The layer stayed on the map", waitFor { !connectedApp.mapLayers.containsKey(ID) })
	}

	@Test
	fun widgetIsAddedToAndRemovedFromMap() {
		val connectedApp = app.aidlApi.getOrCreateConnectedApp(PACK)
		val widget = AMapWidget(ID, "ic_action_speed", "Test widget", "widget_speed_day",
			"widget_speed_night", "10", "km/h", 50, null)

		assertTrue(app.aidlApi.addMapWidget(PACK, AidlMapWidgetWrapper(widget)))
		assertTrue("The widget did not reach the map", waitFor { connectedApp.widgetControls.containsKey(ID) })

		assertTrue(app.aidlApi.removeMapWidget(PACK, ID))
		assertTrue("The widget stayed on the map", waitFor { !connectedApp.widgetControls.containsKey(ID) })
	}

	// the broadcast is delivered asynchronously to MapActivity's receiver on the main thread
	private fun waitFor(condition: () -> Boolean): Boolean {
		val deadline = System.currentTimeMillis() + TIMEOUT_MS
		while (!condition()) {
			if (System.currentTimeMillis() > deadline) {
				return false
			}
			Thread.sleep(50)
		}
		return true
	}

	companion object {
		private const val PACK = "net.osmand.test.connected_app"
		private const val ID = "connected_app_map_broadcasts_test"
		private const val TIMEOUT_MS = 5000L
	}
}

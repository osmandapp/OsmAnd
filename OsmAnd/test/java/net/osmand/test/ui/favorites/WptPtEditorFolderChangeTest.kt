package net.osmand.test.ui.favorites

import android.graphics.Color
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor
import net.osmand.plus.mapcontextmenu.editors.WptPtEditorFragment
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController
import net.osmand.plus.render.RenderingIcons
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Add track waypoint, folder change (#25404): the icon selected in Select icon is the icon
 * the waypoint gets from the new group, the same icon as on the point preview.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class WptPtEditorFolderChangeTest : AndroidTest() {

	companion object {
		private const val POI_TYPES_INIT_TIMEOUT_MS = 30_000L
		private const val FLAG_ICON = "special_flag"
	}

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private var editor: WptPtEditorFragment? = null

	@After
	fun closeEditor() {
		editor?.let { fragment -> scenarioRule.scenario.onActivity { fragment.dismiss() } }
	}

	@Test
	fun selectedIconFollowsTheGroupIcon() {
		skipAppStartDialogs(app)
		waitForPoiTypes()
		val (amenity, poiIcon) = supermarket()
		val editor = openAddWaypoint(amenity)
		assertEquals(poiIcon, editor.iconName)
		assertEquals(poiIcon, selectedIcon(editor))

		moveTo(editor, PointsGroup("Test 25404 star", null, null, Color.RED))
		assertEquals("A group without an icon gives the star", DEFAULT_ICON_NAME, editor.iconName)
		assertEquals("Select icon shows the icon of the point", DEFAULT_ICON_NAME, selectedIcon(editor))

		moveTo(editor, PointsGroup("Test 25404 flag", FLAG_ICON, null, Color.BLUE))
		assertEquals(FLAG_ICON, editor.iconName)
		assertEquals("Select icon shows the icon of the point", FLAG_ICON, selectedIcon(editor))
	}

	private fun supermarket(): Pair<Amenity, String> {
		val supermarket = app.poiTypes.getPoiTypeByKey("supermarket")
		assertNotNull("No POI type supermarket", supermarket)
		val amenity = Amenity().apply {
			type = supermarket.category
			subType = supermarket.keyName
		}
		val poiIcon = RenderingIcons.getBigIconName(RenderingIcons.getPreselectedIconId(app, amenity))
		assertNotNull("No icon for POI type supermarket", poiIcon)
		assertNotEquals(DEFAULT_ICON_NAME, poiIcon)
		return amenity to poiIcon!!
	}

	private fun openAddWaypoint(amenity: Amenity): WptPtEditorFragment {
		scenarioRule.scenario.onActivity {
			it.contextMenu.wptPtPointEditor?.add(GpxFile(null), LatLon(50.45269, 30.50240), "Novus", amenity)
		}
		Espresso.onIdle()
		scenarioRule.scenario.onActivity {
			editor = it.supportFragmentManager.findFragmentByTag(WptPtEditor.TAG) as? WptPtEditorFragment
		}
		assertNotNull("Add track waypoint is not open", editor)
		return editor!!
	}

	private fun moveTo(editor: WptPtEditorFragment, group: PointsGroup) {
		scenarioRule.scenario.onActivity { editor.setPointsGroup(group, true) }
		Espresso.onIdle()
	}

	private fun selectedIcon(editor: WptPtEditorFragment): String? {
		var iconKey: String? = null
		scenarioRule.scenario.onActivity {
			iconKey = EditorIconController.getInstance(app, editor, editor.iconName).selectedIconKey
		}
		return iconKey
	}

	private fun waitForPoiTypes() {
		val deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS
		while (!app.poiTypes.isInit && System.currentTimeMillis() < deadline) {
			Thread.sleep(50)
		}
		assertTrue("POI types are not initialized", app.poiTypes.isInit)
	}
}
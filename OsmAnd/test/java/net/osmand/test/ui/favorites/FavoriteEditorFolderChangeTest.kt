package net.osmand.test.ui.favorites

import android.graphics.Color
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.data.Amenity
import net.osmand.data.BackgroundType
import net.osmand.data.FavouritePoint
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.render.RenderingIcons
import net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Edit Favorite on a folder change, through the editor itself (#25404): what the user did not
 * change by hand follows the folder, a folder with the "Original" icon gives a POI favorite
 * its POI icon, as Add does, and an icon picked by hand is kept.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteEditorFolderChangeTest : AndroidTest() {

	companion object {
		private const val POI_TYPES_INIT_TIMEOUT_MS = 30_000L
		private const val HOUSE_ICON = "special_house"
		private const val FLAG_ICON = "special_flag"
		private const val PICKED_ICON = "special_star"
	}

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private val groups = mutableListOf<FavoriteGroup>()

	@After
	fun deleteTestGroups() {
		groups.forEach { app.favoritesHelper.deleteGroup(it, true) }
	}

	@Test
	fun editedFavoriteFollowsTheFolderExceptWhatWasPickedByHand() {
		skipAppStartDialogs(app)
		waitForPoiTypes()
		val folderA = addGroup("Test 25404 A", Color.RED, HOUSE_ICON, BackgroundType.CIRCLE)
		val folderB = addGroup("Test 25404 B", Color.BLUE, FLAG_ICON, BackgroundType.SQUARE)
		val folderC = addGroup("Test 25404 C", Color.GREEN, DEFAULT_ICON_NAME, BackgroundType.OCTAGON)
		val (favorite, poiIcon) = addPubFavorite(folderA)

		val editor = openEditor(favorite)

		moveTo(editor, folderC)
		assertEquals("A folder with the Original icon gives the POI icon", poiIcon, editor.iconName)
		assertEquals(Color.GREEN, editor.color)
		assertEquals(BackgroundType.OCTAGON, editor.backgroundType)

		moveTo(editor, folderB)
		assertEquals("The icon of an edited favorite follows the folder", FLAG_ICON, editor.iconName)
		assertEquals(Color.BLUE, editor.color)
		assertEquals(BackgroundType.SQUARE, editor.backgroundType)

		scenarioRule.scenario.onActivity { editor.onIconSelectedFromPalette(PICKED_ICON) }
		moveTo(editor, folderA)
		assertEquals("An icon picked by hand is kept", PICKED_ICON, editor.iconName)
		assertEquals(Color.RED, editor.color)
		assertEquals(BackgroundType.CIRCLE, editor.backgroundType)
	}

	private fun addGroup(name: String, color: Int, icon: String, shape: BackgroundType): FavoriteGroup {
		val group = app.favoritesHelper.addFavoriteGroup(name, color, icon, shape)
		groups.add(group)
		return group
	}

	/**
	 * A saved pub favorite with the folder icon, as it is after it was added to the folder.
	 */
	private fun addPubFavorite(folder: FavoriteGroup): Pair<FavouritePoint, String> {
		val pub = app.poiTypes.getPoiTypeByKey("pub")
		assertNotNull("No POI type pub", pub)
		val amenity = Amenity().apply {
			type = pub.category
			subType = pub.keyName
		}
		val poiIcon = RenderingIcons.getBigIconName(RenderingIcons.getPreselectedIconId(app, amenity))
		assertNotNull("No icon for POI type pub", poiIcon)

		val favorite = FavouritePoint(50.0, 30.0, "Pub", folder.name).apply {
			setIconIdFromName(folder.iconName)
			color = folder.color
			backgroundType = folder.backgroundType
			address = "Test"
			amenityExtensions = amenity.getAmenityExtensions(app.poiTypes, true, true, app.language)
		}
		assertTrue("The favorite was not added", app.favoritesHelper.addFavourite(favorite))
		return favorite to poiIcon!!
	}

	private fun openEditor(favorite: FavouritePoint): FavoritePointEditorFragment {
		var fragment: FavoritePointEditorFragment? = null
		scenarioRule.scenario.onActivity { it.contextMenu.favoritePointEditor?.edit(favorite) }
		Espresso.onIdle()
		scenarioRule.scenario.onActivity {
			fragment = it.supportFragmentManager.findFragmentByTag(FavoritePointEditor.TAG) as? FavoritePointEditorFragment
		}
		assertNotNull("Edit Favorite is not open", fragment)
		return fragment!!
	}

	private fun moveTo(editor: FavoritePointEditorFragment, folder: FavoriteGroup) {
		scenarioRule.scenario.onActivity { editor.setPointsGroup(folder.toPointsGroup(app), true) }
		Espresso.onIdle()
	}

	private fun waitForPoiTypes() {
		val deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS
		while (!app.poiTypes.isInit && System.currentTimeMillis() < deadline) {
			Thread.sleep(50)
		}
		assertTrue("POI types are not initialized", app.poiTypes.isInit)
	}
}
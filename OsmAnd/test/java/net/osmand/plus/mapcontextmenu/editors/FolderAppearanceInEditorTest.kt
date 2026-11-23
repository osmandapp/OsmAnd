package net.osmand.plus.mapcontextmenu.editors

import android.graphics.Color
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.Amenity
import net.osmand.data.BackgroundType
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.render.RenderingIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Add / Edit favorite on a folder change (#25404): what the user did not change by hand
 * is taken from the folder, an existing favorite offers the icon of its POI as "Original",
 * and Move gives a point " 2", " 3"... when the folder has its name already.
 */
@RunWith(AndroidJUnit4::class)
class FolderAppearanceInEditorTest {

	companion object {
		private const val POI_TYPES_INIT_TIMEOUT_MS = 30_000L
		private const val CURRENT_ICON = "special_star"
		private const val FOLDER_ICON = "special_house"
	}

	private lateinit var app: OsmandApplication

	@Before
	fun setUp() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
	}

	@Test
	fun untouchedAppearanceFollowsTheFolder() {
		val changes = ManualAppearanceChanges()
		assertEquals(FOLDER_ICON, changes.iconOnFolderChange(CURRENT_ICON, FOLDER_ICON))
		assertEquals(Color.BLUE, changes.colorOnFolderChange(Color.RED, Color.BLUE))
		assertEquals(BackgroundType.SQUARE, changes.shapeOnFolderChange(BackgroundType.CIRCLE, BackgroundType.SQUARE))
	}

	@Test
	fun eachChangedPartIsKeptOnItsOwn() {
		val iconChanged = ManualAppearanceChanges().apply { onIconChanged() }
		assertEquals(CURRENT_ICON, iconChanged.iconOnFolderChange(CURRENT_ICON, FOLDER_ICON))
		assertEquals(Color.BLUE, iconChanged.colorOnFolderChange(Color.RED, Color.BLUE))
		assertEquals(BackgroundType.SQUARE, iconChanged.shapeOnFolderChange(BackgroundType.CIRCLE, BackgroundType.SQUARE))

		val colorChanged = ManualAppearanceChanges().apply { onColorChanged() }
		assertEquals(FOLDER_ICON, colorChanged.iconOnFolderChange(CURRENT_ICON, FOLDER_ICON))
		assertEquals(Color.RED, colorChanged.colorOnFolderChange(Color.RED, Color.BLUE))
		assertEquals(BackgroundType.SQUARE, colorChanged.shapeOnFolderChange(BackgroundType.CIRCLE, BackgroundType.SQUARE))

		val shapeChanged = ManualAppearanceChanges().apply { onShapeChanged() }
		assertEquals(FOLDER_ICON, shapeChanged.iconOnFolderChange(CURRENT_ICON, FOLDER_ICON))
		assertEquals(Color.BLUE, shapeChanged.colorOnFolderChange(Color.RED, Color.BLUE))
		assertEquals(BackgroundType.CIRCLE, shapeChanged.shapeOnFolderChange(BackgroundType.CIRCLE, BackgroundType.SQUARE))
	}

	@Test
	fun changesSurviveRecreation() {
		val changes = ManualAppearanceChanges().apply {
			onColorChanged()
			onShapeChanged()
		}
		val state = Bundle()
		changes.saveState(state)

		val restored = ManualAppearanceChanges()
		restored.restoreState(state)
		assertFalse(restored.isIconChanged)
		assertTrue(restored.isColorChanged)
		assertTrue(restored.isShapeChanged)
	}

	@Test
	fun savedFavoriteOffersTheIconOfItsPoi() {
		waitForPoiTypes()
		val pub = app.poiTypes.getPoiTypeByKey("pub")
		assertNotNull("No POI type pub", pub)
		val amenity = Amenity().apply {
			type = pub.category
			subType = pub.keyName
		}
		val expectedIcon = RenderingIcons.getBigIconName(RenderingIcons.getPreselectedIconId(app, amenity))
		assertNotNull("No icon for POI type pub", expectedIcon)

		val favorite = FavouritePoint(50.0, 30.0, "Pub", "")
		favorite.amenityExtensions = amenity.getAmenityExtensions(app.poiTypes, true, true, app.language)
		assertEquals(expectedIcon, FavoritePointEditor(app).getPoiIconName(favorite))
	}

	@Test
	fun favoriteNotFromPoiHasNoOriginalIcon() {
		waitForPoiTypes()
		val favorite = FavouritePoint(50.0, 30.0, "Spot", "")
		assertNull(FavoritePointEditor(app).getPoiIconName(favorite))
	}

	@Test
	fun favoriteWithoutSubtypeValuesHasNoOriginalIcon() {
		waitForPoiTypes()
		val favorite = FavouritePoint(50.0, 30.0, "Spot", "")
		favorite.amenityExtensions["amenity_subtype"] = ";"
		assertNull(FavoritePointEditor(app).getPoiIconName(favorite))
	}

	@Test
	fun movedPointGetsTheNextFreeNumber() {
		val group = FavoriteGroup("Folder", mutableListOf(), Color.RED, true, false)
		assertEquals("Cafe", group.getUniquePointName("Cafe"))

		group.points.add(FavouritePoint(50.0, 30.0, "Cafe", "Folder"))
		assertEquals("Cafe 2", group.getUniquePointName("Cafe"))

		group.points.add(FavouritePoint(50.1, 30.1, "Cafe 2", "Folder"))
		assertEquals("Cafe 3", group.getUniquePointName("Cafe"))
	}

	private fun waitForPoiTypes() {
		val deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS
		while (!app.poiTypes.isInit && System.currentTimeMillis() < deadline) {
			Thread.sleep(50)
		}
		assertTrue("POI types are not initialized", app.poiTypes.isInit)
	}
}
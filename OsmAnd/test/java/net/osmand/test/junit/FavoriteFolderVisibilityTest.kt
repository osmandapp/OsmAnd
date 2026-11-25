package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.FavouritesHelper
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions
import net.osmand.plus.settings.backend.backup.items.FavoritesSettingsItem
import net.osmand.plus.shared.SharedUtil
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FavoriteFolderVisibilityTest {

	private lateinit var app: OsmandApplication
	private lateinit var helper: FavouritesHelper
	private var showFavoritesBefore = true
	private val prefix = "test-26433-${UUID.randomUUID()}"

	private val folderA get() = "$prefix/A"
	private val folderB get() = "$prefix/A/B"
	private val folderC get() = "$prefix/A/B/C"
	private val folderD get() = "$prefix/A/D"
	private val folderS get() = "$prefix/S"
	private val folderSC get() = "$prefix/S/C"

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
		assertFalse("application initialization timed out", app.isApplicationInitializing)
		helper = app.favoritesHelper
		showFavoritesBefore = app.settings.SHOW_FAVORITES.get()
	}

	@After
	fun cleanup() {
		if (!::helper.isInitialized) {
			return
		}
		for (group in helper.favoriteGroups.toList()) {
			val created = group.name.startsWith(prefix)
			group.points.toList()
				.filter { created || it.name.startsWith(prefix) }
				.forEach { helper.deleteFavourite(it, false) }
			if (created) {
				helper.deleteGroup(group, false)
			}
		}
		helper.saveCurrentPointsIntoFile(false)
		app.settings.SHOW_FAVORITES.set(showFavoritesBefore)
	}

	@Test
	fun hidingFolderHidesWholeSubtree() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)

		assertPointVisible("a", false)
		assertPointVisible("b", false)
		assertPointVisible("c", false)
		assertPointVisible("d", false)
		assertOwnVisible(folderA, false)
		assertOwnVisible(folderB, true)
		assertOwnVisible(folderC, false)
		assertOwnVisible(folderD, true)
		assertFalse(helper.isFolderVisible(folderA))
		assertFalse(helper.isFolderVisible(folderB))
		assertFalse(helper.isFolderVisible(folderC))
		assertFalse(helper.isFolderVisible(folderD))
	}

	@Test
	fun showingFolderRestoresSubfolderStates() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)
		helper.updateFolderVisibility(folderA, true, false)

		assertPointVisible("a", true)
		assertPointVisible("b", true)
		assertPointVisible("d", true)
		assertPointVisible("c", false)
		assertFalse(helper.isFolderVisible(folderC))
		assertTrue(helper.isFolderVisible(folderB))
	}

	@Test
	fun subfolderStatesSurviveReload() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()

		assertOwnVisible(folderA, false)
		assertOwnVisible(folderB, true)
		assertOwnVisible(folderC, false)
		assertOwnVisible(folderD, true)
		assertPointVisible("a", false)
		assertPointVisible("b", false)
		assertPointVisible("c", false)
		assertPointVisible("d", false)

		helper.updateFolderVisibility(folderA, true, false)

		assertPointVisible("b", true)
		assertPointVisible("d", true)
		assertPointVisible("c", false)
	}

	@Test
	fun hidingFolderWritesHiddenPointsOfSubfolders() {
		createTree()
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()
		helper.updateFolderVisibility(folderA, false, false)
		helper.saveCurrentPointsIntoFile(false)

		val file = helper.fileHelper.getExternalFile(group(folderB))
		val gpx = SharedUtil.loadGpxFile(file)
		val points = gpx.getPointsList().filter { it.name == "$prefix-b" }
		assertEquals(1, points.size)
		assertTrue(points.single().isHidden())
		assertFalse(gpx.pointsGroups.values.single().isHidden())
	}

	@Test
	fun reloadHidesVisiblePointsUnderHiddenFolder() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)
		findPoint("b").isVisible = true
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()

		assertOwnVisible(folderB, true)
		assertPointVisible("b", false)
	}

	@Test
	fun showingSubfolderUnhidesHiddenAncestors() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)
		helper.updateFolderVisibility(folderB, true, false)

		assertOwnVisible(folderA, true)
		assertOwnVisible(folderB, true)
		assertPointVisible("a", true)
		assertPointVisible("b", true)
		assertPointVisible("d", true)
		assertPointVisible("c", false)
	}

	@Test
	fun hidingVirtualFolderHidesItsSubtree() {
		createTree()
		assertNull(helper.getGroup(prefix))

		helper.updateFolderVisibility(prefix, false, false)

		val group = helper.getGroup(prefix)
		assertNotNull(group)
		assertFalse(group!!.isVisible)
		assertPointVisible("a", false)
		assertPointVisible("b", false)
		assertPointVisible("c", false)
		assertPointVisible("d", false)

		helper.updateFolderVisibility(prefix, true, false)

		assertPointVisible("a", true)
		assertPointVisible("b", true)
		assertPointVisible("c", true)
		assertPointVisible("d", true)
	}

	@Test
	fun newFavoriteInHiddenSubtreeIsHidden() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)

		val point = addPoint("new", folderB)

		assertFalse(point.isVisible)

		helper.updateFolderVisibility(folderA, true, false)

		assertTrue(point.isVisible)
	}

	@Test
	fun movingFolderIntoHiddenFolderHidesIt() {
		createTree()
		addPoint("e", "$prefix/E")
		helper.updateFolderVisibility(folderA, false, false)

		val moved = helper.moveFavoriteFolderSubtrees(listOf("$prefix/E"), folderA, false)

		assertTrue(moved)
		assertNotNull(helper.getGroup("$folderA/E"))
		assertOwnVisible("$folderA/E", true)
		assertPointVisible("e", false)

		helper.updateFolderVisibility(folderA, true, false)

		assertPointVisible("e", true)
	}

	@Test
	fun loadTakesFolderVisibilityFromFirstPointWithoutHiddenParent() {
		createTree()
		findPoint("d").isVisible = false
		group(folderB).isVisible = false
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()

		assertOwnVisible(folderD, false)
		assertOwnVisible(folderB, true)
		assertPointVisible("b", true)
	}

	@Test
	fun hiddenEmptyFolderSurvivesReload() {
		createTree()
		helper.updateFolderVisibility(prefix, false, false)
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()

		assertOwnVisible(prefix, false)
		assertOwnVisible(folderA, true)
		assertPointVisible("a", false)

		helper.updateFolderVisibility(prefix, true, false)

		assertPointVisible("a", true)
	}

	@Test
	fun movingFolderOutOfHiddenFolderShowsIt() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)

		val moved = helper.moveFavoriteFolderSubtrees(listOf(folderB), prefix, false)

		assertTrue(moved)
		assertPointVisible("b", true)
		assertPointVisible("c", false)
		assertPointVisible("a", false)
	}

	@Test
	fun movingFavoriteFromHiddenFolderIntoNewParentFolderKeepsItVisible() {
		createTree()
		addPoint("q", "$prefix/V/Q")
		helper.updateFolderVisibility(folderA, false, false)

		helper.editFavouritesGroup(listOf(findPoint("d")), "$prefix/V")

		assertOwnVisible("$prefix/V", true)
		assertPointVisible("d", true)
		assertPointVisible("q", true)
	}

	@Test
	fun addingFavoriteWithHiddenParentAppearanceHidesExistingSubfolders() {
		addPoint("b", folderB)
		val point = FavouritePoint(50.0, 30.0, "$prefix-a", folderA)
		point.altitude = 10.0
		val pointsGroup = PointsGroup(folderA, "special_star", "circle", 0, true, false)
		helper.addFavourite(point, pointsGroup, AddFavoriteOptions().setSortAndSave(false).setLookupAddress(false))

		assertOwnVisible(folderA, false)
		assertPointVisible("a", false)
		assertPointVisible("b", false)
		assertOwnVisible(folderB, true)
	}

	@Test
	fun editorSeesVisibilityOfFoldersWithoutOwnGroup() {
		createTree()

		assertTrue(helper.isGroupVisible(prefix))
		assertTrue(helper.isGroupVisible("$folderA/New"))

		helper.updateFolderVisibility(folderA, false, false)

		assertFalse(helper.isGroupVisible(folderB))
		assertFalse(helper.isGroupVisible("$folderA/New"))
		assertTrue(helper.isGroupVisible(prefix))
	}

	@Test
	fun renamingHiddenGroupShowsItsSubfolders() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)

		helper.updateGroupName(group(folderA), "$prefix/Renamed", false)

		assertPointVisible("a", false)
		assertPointVisible("b", true)
		assertPointVisible("d", true)
	}

	@Test
	fun deletingHiddenGroupShowsItsSubfolders() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)

		helper.deleteGroup(group(folderA), false)

		assertPointVisible("b", true)
		assertPointVisible("d", true)
		assertPointVisible("c", false)
		assertTrue(helper.isFolderVisible(folderB))
	}

	@Test
	fun bulkDeletingHiddenGroupShowsItsSubfolders() {
		createTree()
		helper.updateFolderVisibility(folderC, false, false)
		helper.updateFolderVisibility(folderA, false, false)

		helper.delete(setOf(group(folderA)), null)

		assertPointVisible("b", true)
		assertPointVisible("d", true)
		assertPointVisible("c", false)
	}

	@Test
	fun movingFavoriteFromHiddenFolderIntoNewFolderKeepsSourceFlag() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)

		helper.editFavouritesGroup(listOf(findPoint("a")), "$prefix/W")

		assertOwnVisible("$prefix/W", false)
		assertPointVisible("a", false)
	}

	@Test
	fun movingFavoriteHiddenByParentIntoNewFolderKeepsItVisible() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)

		helper.editFavouritesGroup(listOf(findPoint("d")), "$prefix/W")

		assertOwnVisible("$prefix/W", true)
		assertPointVisible("d", true)
	}

	@Test
	fun movingFavoriteFromHiddenFolderNextToGroupWithTrailingSlashTakesItsFlag() {
		createTree()
		helper.addFavoriteGroup("$prefix/T/", 0)
		addPoint("x", "$prefix/T/X")
		helper.updateFolderVisibility("$prefix/T", false, false)

		helper.editFavouritesGroup(listOf(findPoint("d")), "$prefix/T")

		assertOwnVisible("$prefix/T", false)
		assertPointVisible("d", false)
		assertPointVisible("x", false)
		assertFalse(helper.isFolderVisible("$prefix/T/X"))
	}

	@Test
	fun movingFavoriteFromHiddenFolderIntoFolderWithOnlySubfoldersKeepsThemVisible() {
		createTree()
		addPoint("y", "$prefix/T/Y")
		helper.updateFolderVisibility(folderA, false, false)

		helper.editFavouritesGroup(listOf(findPoint("a")), "$prefix/T")

		assertOwnVisible("$prefix/T", true)
		assertPointVisible("a", true)
		assertPointVisible("y", true)
	}

	@Test
	fun groupWithTrailingSlashIsResolvedByFolderPath() {
		helper.addFavoriteGroup("$prefix/T/", 0)
		addPoint("t", "$prefix/T/")
		addPoint("x", "$prefix/T/X")

		helper.updateFolderVisibility("$prefix/T", false, false)

		assertFalse(group("$prefix/T/").isVisible)
		assertNull(helper.getGroup("$prefix/T"))
		assertFalse(helper.isFolderVisible("$prefix/T"))
		assertFalse(helper.isFolderVisible("$prefix/T/X"))
		assertPointVisible("t", false)
		assertPointVisible("x", false)
		assertTrue(helper.canChangeFolderVisibility("$prefix/T"))

		helper.updateFolderVisibility("$prefix/T", true, false)

		assertPointVisible("t", true)
		assertPointVisible("x", true)
		assertNull(helper.getGroup("$prefix/T"))
	}

	@Test
	fun hiddenGroupWithTrailingSlashHidesSubfolderAfterReload() {
		helper.addFavoriteGroup("$prefix/T/", 0)
		addPoint("t", "$prefix/T/")
		addPoint("x", "$prefix/T/X")

		helper.updateFolderVisibility("$prefix/T", false, false)
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()

		assertPointVisible("x", false)
		assertOwnVisible("$prefix/T/X", true)
		assertOwnVisible("$prefix/T/", false)
	}

	@Test
	fun aidlUpdateWithSameVisibilityKeepsHiddenParent() {
		createTree()
		helper.updateFolderVisibility(folderA, false, false)

		helper.updateGroupVisibility(group(folderB), true, false)

		assertOwnVisible(folderA, false)
		assertPointVisible("b", false)
	}

	@Test
	fun settingsItemParentAppliedAfterChildHidesChild() {
		createSyncedTree()

		applyIncoming(incomingGroup(folderS, false, "s"))
		applyIncoming(incomingGroup(folderSC, true, "c"))

		assertSyncedTreeHidden()
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()
		assertSyncedTreeHidden()

		helper.updateFolderVisibility(folderS, true, false)

		assertPointVisible("s", true)
		assertPointVisible("c", true)
	}

	@Test
	fun settingsItemChildAppliedAfterParentStaysHidden() {
		createSyncedTree()

		applyIncoming(incomingGroup(folderSC, true, "c"))
		applyIncoming(incomingGroup(folderS, false, "s"))

		assertSyncedTreeHidden()
		helper.saveCurrentPointsIntoFile(false)
		helper.loadFavorites()
		assertSyncedTreeHidden()

		helper.updateFolderVisibility(folderS, true, false)

		assertPointVisible("s", true)
		assertPointVisible("c", true)
	}

	private fun createSyncedTree() {
		addPoint("s", folderS)
		addPoint("c", folderSC)
		assertOwnVisible(folderS, true)
		assertOwnVisible(folderSC, true)
	}

	private fun incomingGroup(path: String, ownVisible: Boolean, pointSuffix: String): FavoriteGroup {
		val point = FavouritePoint(50.0, 30.0, "$prefix-$pointSuffix", path, 10.0, 1_000L)
		point.isVisible = false
		return FavoriteGroup(path, arrayListOf(point), 0, ownVisible, false)
	}

	private fun applyIncoming(group: FavoriteGroup) {
		val item = FavoritesSettingsItem(app, mutableListOf(group))
		item.setShouldReplace(true)
		item.processDuplicateItems()
		item.apply()
	}

	private fun assertSyncedTreeHidden() {
		assertOwnVisible(folderS, false)
		assertOwnVisible(folderSC, true)
		assertTrue(group(folderS).points.isNotEmpty())
		assertTrue(group(folderSC).points.isNotEmpty())
		assertTrue(group(folderS).points.none { it.isVisible })
		assertTrue(group(folderSC).points.none { it.isVisible })
		assertFalse(helper.isFolderVisible(folderSC))
	}

	private fun createTree() {
		addPoint("a", folderA)
		addPoint("b", folderB)
		addPoint("c", folderC)
		addPoint("d", folderD)
	}

	private fun addPoint(suffix: String, category: String): FavouritePoint {
		val point = FavouritePoint(50.0, 30.0, "$prefix-$suffix", category)
		point.altitude = 10.0
		helper.addFavourite(point, AddFavoriteOptions().setSortAndSave(false).setLookupAddress(false))
		return point
	}

	private fun findPoint(suffix: String): FavouritePoint {
		val name = "$prefix-$suffix"
		val point = helper.favouritePoints.firstOrNull { it.name == name }
		assertNotNull("point $name not found", point)
		return point!!
	}

	private fun group(name: String): FavoriteGroup {
		val group = helper.getGroup(name)
		assertNotNull("group $name not found", group)
		return group!!
	}

	private fun assertPointVisible(suffix: String, expected: Boolean) {
		assertEquals("point $suffix visibility", expected, findPoint(suffix).isVisible)
	}

	private fun assertOwnVisible(path: String, expected: Boolean) {
		assertEquals("folder $path own visibility", expected, group(path).isVisible)
	}
}

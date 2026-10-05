package net.osmand.test.junit

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.SpecialPointType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.importfiles.tasks.FavoritesImportTask
import net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY
import net.osmand.plus.myplaces.favorites.FavouritesHelper
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.shared.gpx.primitives.WptPt
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FavoritesImportDestinationTest {

	private lateinit var app: OsmandApplication
	private lateinit var helper: FavouritesHelper
	private lateinit var groupsBefore: Set<String>
	private val prefix = "test-25775-${UUID.randomUUID()}"

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
		assertFalse("application initialization timed out", app.isApplicationInitializing)
		helper = app.favoritesHelper
		groupsBefore = helper.favoriteGroups.map { it.name }.toSet()
	}

	@After
	fun cleanup() {
		if (!::groupsBefore.isInitialized) return
		for (group in helper.favoriteGroups.toList()) {
			val created = group.name !in groupsBefore
			group.points.filter { created || it.name.startsWith(prefix) }.forEach { helper.deleteFavourite(it, false) }
			if (created) helper.deleteGroup(group, false)
		}
		helper.saveCurrentPointsIntoFile(false)
	}

	@Test
	fun preservesSourceHierarchyUnderTheChosenFolder() {
		val target = "$prefix/Archive/Old phone points"
		importNow(gpx(waypoint("travel", "Travel"), waypoint("paris", "Travel/Paris"),
			waypoint("ungrouped", null), waypoint("user", "user")), target)
		assertEquals(listOf("travel"), pointNames("$target/Travel"))
		assertEquals(listOf("paris"), pointNames("$target/Travel/Paris"))
		assertEquals(listOf("ungrouped"), pointNames(target))
		assertEquals(listOf("user"), pointNames("$target/user"))
	}

	@Test
	fun emptyTargetIsTheDefaultFolderWhileNullKeepsLegacyImport() {
		val fileName = "$prefix-file.gpx"
		importNow(gpx(waypoint("$prefix-default", null)), "", fileName)
		assertNull(helper.getGroup(fileName))
		assertTrue(helper.getGroup("")!!.points.any { it.name == "$prefix-default" })

		importNow(gpx(waypoint("$prefix-legacy", null)), null, fileName)
		assertEquals(listOf("$prefix-legacy"), pointNames(fileName))
	}

	@Test
	fun specialPointsStayCanonicalWhileOrdinaryPersonalPointsMove() {
		// The import writes to the real Home/Work/Parking group, so use a device that has none yet.
		assumeTrue("needs a device without Home/Work/Parking", helper.getGroup(PERSONAL_CATEGORY) == null)
		val target = "$prefix/destination"
		// Named "home" so that moving it into "personal" would infer HOME if point_type were not kept.
		val work = waypoint("home", "Travel").apply { setSpecialPointType(SpecialPointType.WORK.getName()) }
		val source = gpx(work, waypoint("home", PERSONAL_CATEGORY))
		source.pointsGroups["Travel"] = importedAppearance("Travel")
		importNow(source, target)

		val personal = helper.getGroup(PERSONAL_CATEGORY)!!
		assertEquals(SpecialPointType.WORK, personal.points.single().specialPointType)
		assertNotEquals(IMPORTED_COLOR, personal.color)
		assertNull(helper.getGroup("$target/$PERSONAL_CATEGORY")!!.points.single().specialPointType)
	}

	@Test
	fun onlyNewGroupsInTheChosenFolderTakeImportedAppearance() {
		val target = "$prefix/destination"
		val existing = "$target/Existing"
		helper.addFavoriteGroup(existing, LOCAL_COLOR)
		val source = gpx(waypoint("existing", "Existing"), waypoint("new", "New"))
		source.pointsGroups["Existing"] = importedAppearance("Existing")
		source.pointsGroups["New"] = importedAppearance("New")
		importNow(source, target)
		assertEquals(LOCAL_COLOR, helper.getGroup(existing)!!.color)
		assertEquals(IMPORTED_COLOR, helper.getGroup("$target/New")!!.color)

		// Imports without a chosen folder keep restyling existing groups, as before.
		val legacy = gpx(waypoint("legacy", existing))
		legacy.pointsGroups[existing] = importedAppearance(existing)
		importNow(legacy, null)
		assertEquals(IMPORTED_COLOR, helper.getGroup(existing)!!.color)
	}

	private fun pointNames(category: String) = helper.getGroup(category)!!.points.map { it.name }

	private fun importedAppearance(category: String) =
		PointsGroup(category, "special_star", "square", IMPORTED_COLOR, false, false)

	private fun waypoint(name: String, category: String?) = WptPt().apply {
		this.name = name
		this.category = category
		lat = 50.0
		lon = 30.0
		ele = 10.0 // skips the async altitude lookup in addFavourite()
	}

	private fun gpx(vararg points: WptPt) = GpxFile("test").apply { addPoints(points.toList()) }

	private fun importNow(gpx: GpxFile, target: String?, fileName: String = "$prefix-file.gpx") {
		// Activity's constructor creates a Handler, which needs a Looper; the test thread has none.
		lateinit var activity: TestActivity
		InstrumentationRegistry.getInstrumentation().runOnMainSync { activity = TestActivity(app) }
		TestImportTask(activity, gpx, fileName, target).importNow()
	}

	private class TestActivity(private val app: OsmandApplication) : FragmentActivity() {
		override fun getApplicationContext(): Context = app
	}

	private class TestImportTask(activity: FragmentActivity, gpx: GpxFile, fileName: String, target: String?) :
		FavoritesImportTask(activity, gpx, fileName, true, target) {
		fun importNow() = doInBackground()
	}

	private companion object {
		val LOCAL_COLOR = 0xff123456.toInt()
		val IMPORTED_COLOR = 0xffabcdef.toInt()
	}
}

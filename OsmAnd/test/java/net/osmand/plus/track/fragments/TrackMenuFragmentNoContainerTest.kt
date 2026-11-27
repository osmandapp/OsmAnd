package net.osmand.plus.track.fragments

import androidx.fragment.app.FragmentManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.shared.SharedUtil
import net.osmand.plus.track.GpxSelectionParams
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.test.common.AndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * A fragment that is fully removed and then added again has no container (Fragment.initState()
 * resets its container id), so ContextMenuFragment.onCreateView() gets a null container. The
 * track menu crashed there in processScreenHeight(); it must close instead.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TrackMenuFragmentNoContainerTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private lateinit var gpxFile: GpxFile

	@Test
	fun trackMenuAddedAgainWithoutContainerIsDismissed() {
		lateinit var fragment: TrackMenuFragment
		scenarioRule.scenario.onActivity { activity ->
			gpxFile = createTrack()
			val selectedGpxFile = app.selectedGpxHelper.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams())
			val manager = activity.supportFragmentManager
			assertTrue("Track menu not shown", TrackMenuFragment.showInstance(activity, selectedGpxFile, null))
			manager.executePendingTransactions()
			fragment = manager.findFragmentByTag(TrackMenuFragment.TAG) as TrackMenuFragment

			manager.popBackStackImmediate(TrackMenuFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
			assertFalse("Track menu not removed", fragment.isAdded)

			manager.beginTransaction()
				.add(fragment, TrackMenuFragment.TAG)
				.addToBackStack(TrackMenuFragment.TAG)
				.commit()
			manager.executePendingTransactions()
			assertNull("Track menu has a view without a container", fragment.view)
		}
		scenarioRule.scenario.onActivity { activity ->
			val manager = activity.supportFragmentManager
			manager.executePendingTransactions()
			assertNull("Track menu without a view stays open", manager.findFragmentByTag(TrackMenuFragment.TAG))
		}
	}

	@After
	fun tearDown() {
		if (::gpxFile.isInitialized) {
			app.selectedGpxHelper.selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap())
			File(gpxFile.path).delete()
		}
	}

	private fun createTrack(): GpxFile {
		val segment = TrkSegment()
		for (i in 0 until 10) {
			segment.points.add(WptPt(50.45 + i * 0.001, 30.52 + i * 0.001))
		}
		val track = Track()
		track.segments.add(segment)
		val file = File(app.cacheDir, "track-menu-no-container.gpx")
		val gpx = GpxFile("test")
		gpx.tracks.add(track)
		gpx.path = file.absolutePath
		SharedUtil.writeGpxFile(file, gpx)
		return gpx
	}
}

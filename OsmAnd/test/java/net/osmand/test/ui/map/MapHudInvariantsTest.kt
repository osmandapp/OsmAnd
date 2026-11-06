package net.osmand.test.ui.map

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.importfiles.ImportHelper
import net.osmand.plus.importfiles.SaveImportedGpxListener
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenuFragment
import net.osmand.plus.mapmarkers.PlanRouteFragment
import net.osmand.plus.measurementtool.MeasurementToolFragment
import net.osmand.plus.measurementtool.SnapTrackWarningFragment
import net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter
import net.osmand.plus.track.GpxSelectionParams
import net.osmand.plus.track.fragments.TrackMenuFragment
import net.osmand.plus.track.helpers.GpxDisplayItem
import net.osmand.plus.track.helpers.GpxUiHelper
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.shared.gpx.GpxFile
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.MapHudInvariants
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import net.osmand.test.common.ResourcesImporter
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Opens each panel that is shown over the map and checks [MapHudInvariants] with it: map buttons
 * and the ruler stay clear of the panel and of the system bars, a bottom panel starts right under
 * the map buttons, and the map center stays on the visible map.
 *
 * The result depends on the orientation, the screen size and the navigation mode, so run the class
 * in portrait and in landscape, on a phone and on a tablet. Fragments in [MapActivity] get system
 * bar insets only on Android 15+ (edge-to-edge), so the inset-related failures, like the empty strip
 * above the Analyze on map chart (https://github.com/osmandapp/OsmAnd/issues/26540), only show up
 * there.
 *
 * For a landscape run the profile's screen orientation setting has to allow it: with a fixed
 * portrait orientation the app runs letterboxed in portrait on a rotated device. Keep other OsmAnd
 * builds out of the foreground during the run: with one of them resumed, a finished MapActivity of
 * the build under test may never get destroyed and the scenario's teardown times out.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapHudInvariantsTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private val addedMarkers = ArrayList<LatLon>()

	@After
	fun closePanels() {
		scenarioRule.scenario.onActivity { hideTrackDetailsMenu(it) }
		if (addedMarkers.isNotEmpty()) {
			val markersHelper = app.mapMarkersHelper
			markersHelper.removeMarkers(markersHelper.mapMarkers.filter { it.point in addedMarkers })
		}
	}

	@Test
	fun plainMap() {
		MapHudInvariants.assertHolds(openMap())
	}

	@Test
	fun analyzeOnMap() {
		val activity = openMap()
		val selectedGpxFile = importAndSelectGpx()
		var gpxItem: GpxDisplayItem? = null
		scenarioRule.scenario.onActivity {
			val analysis = selectedGpxFile.getTrackAnalysisToDisplay(app)
			gpxItem = GpxUiHelper.makeGpxDisplayItem(app, selectedGpxFile.gpxFile, ChartPointLayer.GPX, analysis)
		}
		val item = requireNotNull(gpxItem) { "Chart item was not created for $GPX_NAME" }

		scenarioRule.scenario.onActivity {
			GPXItemPagerAdapter.prepareGpxItemChartTypes(item, null, settings)
			val trackDetailsMenu = activity.trackDetailsMenu
			trackDetailsMenu.setGpxItem(item)
			trackDetailsMenu.setSelectedGpxFile(selectedGpxFile)
			trackDetailsMenu.show()
		}
		assertPanelShown(activity, TrackDetailsMenuFragment.TAG)
	}

	@Test
	fun planRoute() {
		val activity = openMap()
		scenarioRule.scenario.onActivity {
			MeasurementToolFragment.showInstance(activity.supportFragmentManager)
		}
		assertPanelShown(activity, MeasurementToolFragment.TAG)
	}

	@Test
	fun planRouteAttachToRoads() {
		val activity = openMap()
		val selectedGpxFile = importAndSelectGpx()
		scenarioRule.scenario.onActivity {
			// Plan a route hides itself and stays in bottomFragmentContainer while the snap warning is shown
			MeasurementToolFragment.showInstance(activity, selectedGpxFile.gpxFile, 0, MeasurementToolFragment.ATTACH_ROADS_MODE)
		}
		assertPanelShown(activity, SnapTrackWarningFragment.TAG)
	}

	@Test
	fun trackMenu() {
		val activity = openMap()
		val selectedGpxFile = importAndSelectGpx()
		scenarioRule.scenario.onActivity {
			TrackMenuFragment.showInstance(activity, selectedGpxFile, null)
		}
		assertPanelShown(activity, TrackMenuFragment.TAG)
	}

	@Test
	fun contextMenu() {
		val activity = openMap()
		scenarioRule.scenario.onActivity {
			val mapView = app.osmandMap.mapView
			val latLon = LatLon(mapView.latitude, mapView.longitude)
			activity.contextMenu.show(latLon, PointDescription(PointDescription.POINT_TYPE_LOCATION, ""), null)
		}
		assertPanelShown(activity, MapContextMenuFragment.TAG)
	}

	@Test
	fun markersPlanRoute() {
		val activity = openMap()
		scenarioRule.scenario.onActivity {
			val mapView = app.osmandMap.mapView
			val markersHelper = app.mapMarkersHelper
			for (i in 1..2) {
				val point = LatLon(mapView.latitude + i * 0.01, mapView.longitude + i * 0.01)
				markersHelper.addMapMarker(point, PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, "HUD test $i"), null)
				addedMarkers.add(point)
			}
			markersHelper.planRouteContext.isMarkersListOpened = false
			PlanRouteFragment.showInstance(activity, null)
		}
		assertPanelShown(activity, PlanRouteFragment.TAG)
	}

	private fun openMap(): MapActivity {
		skipAppStartDialogs(app)
		var mapActivity: MapActivity? = null
		scenarioRule.scenario.onActivity {
			hideTrackDetailsMenu(it)
			mapActivity = it
		}
		return requireNotNull(mapActivity)
	}

	/**
	 * The track details menu is static in [MapActivity], and every new [MapActivity] shows it again
	 * while it is visible. Only hide() clears that, so that the next scenario starts without it.
	 */
	private fun hideTrackDetailsMenu(activity: MapActivity) {
		if (activity.trackDetailsMenu.isVisible) {
			activity.trackDetailsMenu.hide(false)
		}
	}

	private fun assertPanelShown(activity: MapActivity, fragmentTag: String) {
		MapHudInvariants.assertHolds(activity)
		var shown = false
		scenarioRule.scenario.onActivity {
			shown = activity.supportFragmentManager.findFragmentByTag(fragmentTag)?.view?.isShown == true
		}
		assertTrue("$fragmentTag is not shown, the invariants were checked without it", shown)
	}

	private fun importAndSelectGpx(): SelectedGpxFile {
		var selectedGpxFile: SelectedGpxFile? = null
		ResourcesImporter.importGpxAssets(app, listOf(GPX_NAME), object : SaveImportedGpxListener {
			override fun onGpxSaved(error: String?, gpxFile: GpxFile) {
				if (error.isNullOrEmpty()) {
					gpxFile.path = File(ImportHelper.getGpxDestinationDir(app, true), GPX_NAME).absolutePath
					selectedGpxFile = app.selectedGpxHelper
							.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams())
				}
			}
		})
		return requireNotNull(selectedGpxFile) { "$GPX_NAME was not imported" }
	}

	companion object {
		private const val GPX_NAME = "gpx_recalc_test.gpx"
	}
}

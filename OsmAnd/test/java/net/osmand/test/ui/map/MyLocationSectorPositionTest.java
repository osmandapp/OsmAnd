package net.osmand.test.ui.map;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.simulation.SimulatedLocation;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.PointLocationLayer;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.MapUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The location icon is a map marker, while the accuracy circle and the view angle sector are drawn
 * by the renderer at its own "my location" position. Both have to point at the same place: once
 * they diverge, the view angle sector is left behind the location icon and the gap grows with every
 * meter driven (the sector ends up kilometres away from the arrow).
 * <p>
 * The divergence used to happen when the accuracy circle was hidden and the map was not following
 * my location, because in that case the renderer position was updated neither by the layer (it does
 * it only for non-animated moves) nor by the markers animator (it did it only while the accuracy
 * circle was visible).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class MyLocationSectorPositionTest extends AndroidTest {

	private static final float SPEED_KM_PER_HOUR = 120;

	private static final long DRIVE_TIME_MS = 25000;
	private static final long CHECK_INTERVAL_MS = 500;
	private static final long RENDERER_WAIT_TIME_MS = 15000;

	// One simulated step at the speed above is ~50 m, so anything above a couple of steps
	// means the renderer position stopped following the marker.
	private static final int MAX_DEVIATION_M = 150;
	private static final int MIN_TRAVELLED_DISTANCE_M = 300;

	private static final LatLon PATH_START = new LatLon(45.92051, 35.20653);
	private static final LatLon PATH_END = new LatLon(45.92051, 35.24653);
	private static final int PATH_ZOOM = 13;

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Before
	@Override
	public void setup() {
		super.setup();
		enableSimulation(SPEED_KM_PER_HOUR);
		settings.setPreferenceForAllModes(settings.ANIMATE_MY_LOCATION.getId(), true);
		settings.setPreferenceForAllModes(settings.LOCATION_RADIUS_VISIBILITY.getId(), MarkerDisplayOption.OFF);
		settings.setPreferenceForAllModes(settings.VIEW_ANGLE_VISIBILITY.getId(), MarkerDisplayOption.RESTING_NAVIGATION);
		try {
			ResourcesImporter.importObfAssets(app, Collections.singletonList("alarm_test.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void cleanUp() {
		super.cleanUp();
		app.getLocationProvider().getLocationSimulation().stop();
	}

	@Test
	public void testMyLocationCircleFollowsLocationMarker() throws Throwable {
		skipAppStartDialogs(app);

		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = awaitMapRenderer(mapView);
		Assume.assumeTrue("Applicable to the OpenGL map renderer only", mapRenderer != null);

		PointLocationLayer locationLayer = app.getOsmandMap().getMapLayers().getLocationLayer();
		MapViewTrackingUtilities trackingUtilities = app.getMapViewTrackingUtilities();

		// Keep the whole simulated path on the screen without following my location: while the map
		// follows it, the layer re-sets the renderer position on every frame anyway.
		app.runInUIThread(() -> {
			trackingUtilities.setMapLinkedToLocation(false);
			mapView.setIntZoom(PATH_ZOOM);
			mapView.setLatLon(PATH_START.getLatitude(),
					(PATH_START.getLongitude() + PATH_END.getLongitude()) / 2);
		});
		app.getLocationProvider().getLocationSimulation()
				.startSimulationThread(app, simulatedPath(), false, 1);

		LatLon firstMarkerLocation = null;
		LatLon markerLocation = null;
		long finishTime = System.currentTimeMillis() + DRIVE_TIME_MS;
		while (System.currentTimeMillis() < finishTime) {
			Thread.sleep(CHECK_INTERVAL_MS);

			markerLocation = locationLayer.getLastMarkerLocation();
			if (markerLocation == null) {
				continue;
			}
			if (firstMarkerLocation == null) {
				firstMarkerLocation = markerLocation;
			}
			if (trackingUtilities.isMapLinkedToLocation()) {
				throw new IllegalStateException("Map started following my location, "
						+ "the divergence can't be detected anymore");
			}
			LatLon circleLocation = getMyLocationCirclePosition(mapRenderer);
			double deviation = MapUtils.getDistance(markerLocation, circleLocation);
			if (deviation > MAX_DEVIATION_M) {
				throw new AssertionError("Accuracy circle and view angle sector are drawn "
						+ (int) deviation + " m away from the location marker "
						+ "(marker " + markerLocation + ", renderer " + circleLocation + ")");
			}
		}

		if (markerLocation == null) {
			throw new IllegalStateException("My location was never shown on the map");
		}
		double travelled = MapUtils.getDistance(firstMarkerLocation, markerLocation);
		if (travelled < MIN_TRAVELLED_DISTANCE_M) {
			throw new IllegalStateException("My location moved " + (int) travelled
					+ " m only, the simulation did not run as expected");
		}
	}

	@Nullable
	private MapRendererView awaitMapRenderer(@NonNull OsmandMapTileView mapView) throws InterruptedException {
		long finishTime = System.currentTimeMillis() + RENDERER_WAIT_TIME_MS;
		MapRendererView mapRenderer = mapView.getMapRenderer();
		while (mapRenderer == null && System.currentTimeMillis() < finishTime) {
			Thread.sleep(CHECK_INTERVAL_MS);
			mapRenderer = mapView.getMapRenderer();
		}
		return mapRenderer;
	}

	@NonNull
	private LatLon getMyLocationCirclePosition(@NonNull MapRendererView mapRenderer) {
		PointI position31 = mapRenderer.getState().getMyLocation31();
		return new LatLon(MapUtils.get31LatitudeY(position31.getY()),
				MapUtils.get31LongitudeX(position31.getX()));
	}

	@NonNull
	private List<SimulatedLocation> simulatedPath() {
		List<SimulatedLocation> locations = new ArrayList<>();
		locations.add(simulatedLocation(PATH_START));
		locations.add(simulatedLocation(PATH_END));
		return locations;
	}

	@NonNull
	private SimulatedLocation simulatedLocation(@NonNull LatLon latLon) {
		Location location = new Location(SIMULATED_PROVIDER, latLon.getLatitude(), latLon.getLongitude());
		return new SimulatedLocation(location, SIMULATED_PROVIDER);
	}
}

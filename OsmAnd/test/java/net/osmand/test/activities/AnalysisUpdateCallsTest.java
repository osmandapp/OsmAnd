package net.osmand.test.activities;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.core.android.MapRendererView;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AnalysisUpdateCallsTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource observeDistToFinishIdlingResource;

	private GpxDbHelper gpxDbHelper;
	private OsmandMapTileView mapView;
	private GpxSelectionHelper selectionHelper;
	private int startFrameId;

	@Before
	@Override
	public void setup() {
		super.setup();
		gpxDbHelper = app.getGpxDbHelper();
		mapView = app.getOsmandMap().getMapView();
		selectionHelper = app.getSelectedGpxHelper();

		IdlingPolicies.setIdlingResourceTimeout(360, TimeUnit.SECONDS);
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), new SaveImportedGpxListener() {
				@Override
				public void onGpxSaved(@Nullable String error, @NonNull GPXFile gpxFile) {
					if (Algorithms.isEmpty(error)) {
						GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
						selectionHelper.selectGpxFile(gpxFile, params);
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void cleanUp() {
		super.cleanUp();
		if (observeDistToFinishIdlingResource != null) {
			unregisterIdlingResources(observeDistToFinishIdlingResource);
		}
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		observeDistToFinishIdlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(observeDistToFinishIdlingResource);

		Espresso.onIdle();
	}

	private class ObserveDistToFinishIdlingResource extends BaseIdlingResource {

		private static final int CHECK_INTERVAL = 15000;

		private boolean idle = false;

		public ObserveDistToFinishIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(createTaskRunnable(), 30000);
		}

		private Runnable createTaskRunnable() {
			return () -> {
				MapRendererView rendererView = mapView.getMapRenderer();
				if (rendererView != null) {
					if (startFrameId == 0) {
						startFrameId = rendererView.getFrameId();
						Handler handler = new Handler(Looper.getMainLooper());
						handler.postDelayed(createTaskRunnable(), CHECK_INTERVAL);
					} else {
						int renderedFrames = rendererView.getFrameId() - startFrameId;
						if (renderedFrames < 25) {
//							throw new AssertionError("Map rendering too slow. rendered " + renderedFrames + " frames");
						}
						idle = true;
						notifyIdleTransition();
					}
				} else {
					throw new AssertionError("Failed to get map renderer");
				}
				app.showToastMessage("readTrackItemCount " + GpxDbHelper.readTrackItemCount);
				if (GpxDbHelper.readTrackItemCount > 2) {
//					throw new AssertionError("To many updates of analysis " + GpxDbHelper.readTrackItemCount);
				}
			};
		}

		@Override
		public boolean isIdleNow() {
			return idle;
		}
	}
}

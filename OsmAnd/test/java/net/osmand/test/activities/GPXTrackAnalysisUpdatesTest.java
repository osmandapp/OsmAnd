package net.osmand.test.activities;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.core.android.MapRendererView;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.OsmAndFormatter;
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GPXTrackAnalysisUpdatesTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource idlingResource;

	private GpxDbHelper gpxDbHelper;
	private OsmandMapTileView mapView;
	private GpxSelectionHelper selectionHelper;

	private File file;

	@Before
	@Override
	public void setup() {
		super.setup();
		gpxDbHelper = app.getGpxDbHelper();
		mapView = app.getOsmandMap().getMapView();
		selectionHelper = app.getSelectedGpxHelper();

		importAndShowGpx();
	}

	private void importAndShowGpx() {
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), new SaveImportedGpxListener() {
				@Override
				public void onGpxSaved(@Nullable String error, @NonNull GPXFile gpxFile) {
					if (Algorithms.isEmpty(error)) {
						file = new File(ImportHelper.getGpxDestinationDir(app, true), SELECTED_GPX_NAME);
						gpxFile.path = file.getAbsolutePath();
						selectionHelper.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
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
		if (idlingResource != null) {
			unregisterIdlingResources(idlingResource);
		}
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		idlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(idlingResource);

		Espresso.onIdle();
	}

	private class ObserveDistToFinishIdlingResource extends BaseIdlingResource {

		private static final int CHECKS_COUNT = 30;
		private static final int CHECK_INTERVAL_MS = 1000;
		private static final int LOW_FPS_VALUE = 15;
		private static final int LOW_FPS_COUNT = 10;

		private int checksCounter;
		private int lowFpsCounter;

		public ObserveDistToFinishIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			startHandler();
		}

		private void startHandler() {
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(() -> {
				checksCounter++;

				checkFPS();
				checkAnalysisUpdate();

				if (isIdleNow()) {
					notifyIdleTransition();
				} else {
					startHandler();
				}
			}, CHECK_INTERVAL_MS);
		}

		private void checkFPS() {
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer != null) {
				float fps = mapView.calculateRenderFps();
				if (fps < LOW_FPS_VALUE) {
					lowFpsCounter++;
					if (lowFpsCounter >= LOW_FPS_COUNT) {
						app.showToastMessage("Map rendering too slow. rendered " + OsmAndFormatter.formatFps(fps) + " frames");
//						throw new AssertionError("Map rendering too slow. rendered " + OsmAndFormatter.formatFps(fps) + " frames");
					}
				}
			} else {
				throw new AssertionError("Failed to get map renderer");
			}
		}

		private void checkAnalysisUpdate() {
			gpxDbHelper.getItem(file); // simulate multiple calls for getting GpxDataItem
			if (GpxDbHelper.readTrackItemCount > 2) {
				throw new AssertionError("To many updates of analysis " + GpxDbHelper.readTrackItemCount);
			}
		}

		@Override
		public boolean isIdleNow() {
			return checksCounter >= CHECKS_COUNT;
		}
	}
}

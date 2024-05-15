package net.osmand.test.activities;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AnalysisUpdateCallsTest extends AndroidTest {
	public static final Log LOG = PlatformUtil.getLog(AnalysisUpdateCallsTest.class);

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule =
			new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource observeDistToFinishIdlingResource;

	private OsmandMapTileView mapView;
	private int startFrameId;

	@Before
	@Override
	public void setup() {
		super.setup();
		IdlingPolicies.setIdlingResourceTimeout(360, TimeUnit.SECONDS);
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME));
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
		GpxDbHelper dbHelper = app.getGpxDbHelper();
		GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
		GpxDataItem testItem = getTestGpxItem(dbHelper);
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		gpxFile.path = testItem.getFile().getPath();
		app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);

		mActivityScenarioRule.getScenario().moveToState(State.RESUMED).onActivity(activity -> {
			mapView = activity.getMapView();

		});
		observeDistToFinishIdlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(observeDistToFinishIdlingResource);
		Espresso.onIdle();
	}

	@NonNull
	private GpxDataItem getTestGpxItem(@NonNull GpxDbHelper dbHelper) {
		GpxDataItem testItem = null;
		List<GpxDataItem> dataItems = dbHelper.getItems();
		for (GpxDataItem item : dataItems) {
			String fileName = item.getParameter(GpxParameter.FILE_NAME);
			if (Algorithms.stringsEqual(fileName, SELECTED_GPX_NAME)) {
				testItem = item;
				break;
			}
		}
		if (testItem == null) {
			throw new AssertionError("Can't find test track");
		}
		return testItem;
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
						LOG.debug("rendered " + renderedFrames + " frames");
						if (renderedFrames < 25) {
							throw new AssertionError("Map rendering to slow. rendered " + renderedFrames + " frames");
						}
						idle = true;
						notifyIdleTransition();
					}
				} else {
					throw new AssertionError("Failed to get map renderer");
				}
				LOG.debug("readTrackItemCount " + GpxDbHelper.readTrackItemCount);
				if (GpxDbHelper.readTrackItemCount > 2) {
					throw new AssertionError("To many updates of analysis " + GpxDbHelper.readTrackItemCount);
				}
			};
		}

		@Override
		public boolean isIdleNow() {
			return idle;
		}
	}
}

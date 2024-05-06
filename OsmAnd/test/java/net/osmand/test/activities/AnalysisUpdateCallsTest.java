package net.osmand.test.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.Interactions.openNavigationMenu;
import static net.osmand.test.common.Interactions.setRouteStart;
import static net.osmand.test.common.Interactions.startNavigation;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GPXDatabase;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AnalysisUpdateCallsTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";
	private Map<String, Integer> readTrackItemCalls = new HashMap<>();

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule =
			new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource observeDistToFinishIdlingResource;

	@Before
	@Override
	public void setup() {
		super.setup();
		IdlingPolicies.setIdlingResourceTimeout(40, TimeUnit.SECONDS);
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
		dbHelper.setDbTestCallback(filePath -> {
			Integer readCount = readTrackItemCalls.get(filePath);
			readTrackItemCalls.put(filePath, readCount == null ? 1 : readCount + 1);
		});
		GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
		GpxDataItem testItem = getTestGpxItem(dbHelper);
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		gpxFile.path = testItem.getFile().getPath();
		app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);

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
			Runnable checkLeftDistanceTask = () -> {
				GpxDbHelper dbHelper = app.getGpxDbHelper();
				GpxDataItem testItem = getTestGpxItem(dbHelper);
				Integer count = readTrackItemCalls.get(testItem.getFile().getPath());
				if (count != null && count > 2) {
					throw new AssertionError("To many updates of analysis");
				}
				idle = true;
				notifyIdleTransition();
			};
			handler.postDelayed(checkLeftDistanceTask, CHECK_INTERVAL);
		}

		@Override
		public boolean isIdleNow() {
			return idle;
		}
	}
}

package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.refreshMap;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.junit.Assert.assertNotEquals;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.views.layers.GPXLayer;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CleanGpxLayerResourcesTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private GpxSelectionHelper selectionHelper;

	private File file;
	private GpxFile importedGpxFile;

	private int originalHash;
	private int newHash;

	@Before
	@Override
	public void setup() {
		super.setup();
		selectionHelper = app.getSelectedGpxHelper();

		importAndShowGpx();
	}

	private void importAndShowGpx() {
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), new SaveImportedGpxListener() {
				@Override
				public void onGpxSaved(@Nullable String error, @NotNull GpxFile gpxFile) {
					if (Algorithms.isEmpty(error)) {
						file = new File(ImportHelper.getGpxDestinationDir(app, true), SELECTED_GPX_NAME);
						gpxFile.setPath(file.getAbsolutePath());
						selectionHelper.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
						importedGpxFile = gpxFile;
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		IdlingResource idlingResource = new GpxRendererChangeIdlingResource(app);
		registerIdlingResources(idlingResource);
		Espresso.onIdle();
		unregisterIdlingResources(idlingResource);

		assertNotEquals("Cached segments didn't updated", originalHash, newHash);
	}

	private boolean moveMapToGpx() {
		if (importedGpxFile == null) {
			return false;
		}

		KQuadRect rect = importedGpxFile.getRect();
		if (rect.getLeft() != 0 && rect.getRight() != 0) {
			app.getOsmandMap().getMapView().fitRectToMap(rect.getLeft(), rect.getRight(),
					rect.getTop(), rect.getBottom(), (int) rect.width(), (int) rect.height(), 0);
		}
		return true;
	}

	public class GpxRendererChangeIdlingResource extends BaseIdlingResource {

		private static final int STEP_MOVE_MAP_TO_GPX = 1;
		private static final int STEP_GET_ORIGINAL_CACHE_HASH = 2;
		private static final int STEP_REFRESH_MAP = 3;
		private static final int STEP_GET_NEW_HASH = 4;
		private int currentStep = STEP_MOVE_MAP_TO_GPX;

		private static final int CHECKS_COUNT = 30;
		private static final int CHECK_INTERVAL_MS = 1000;
		private int checksCounter;

		private final OsmandApplication app;
		private final GPXLayer gpxLayer;
		private boolean isIdle = false;

		public GpxRendererChangeIdlingResource(@NonNull OsmandApplication app) {
			super(app);

			this.app = app;
			gpxLayer = app.getOsmandMap().getMapLayers().getGpxLayer();
			startHandler();
		}

		private void startHandler(){
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(() -> {
				checksCounter++;

				if (currentStep == STEP_MOVE_MAP_TO_GPX && moveMapToGpx()) {
					currentStep = STEP_GET_ORIGINAL_CACHE_HASH;
				} else if (currentStep == STEP_GET_ORIGINAL_CACHE_HASH && getOriginalCacheHash()) {
					currentStep = STEP_REFRESH_MAP;
				} else if (currentStep == STEP_REFRESH_MAP) {
					refreshMap(app);
					currentStep = STEP_GET_NEW_HASH;
				} else if (currentStep == STEP_GET_NEW_HASH && getNewCacheHash()) {
					isIdle = true;
				}

				if (isIdleNow()) {
					notifyIdleTransition();
				} else {
					startHandler();
				}
			}, CHECK_INTERVAL_MS);
		}

		private boolean getNewCacheHash() {
			newHash = gpxLayer.getSegmentsCacheHash();
			return newHash != 0;
		}

		private boolean getOriginalCacheHash() {
			GPXLayer gpxLayer = app.getOsmandMap().getMapLayers().getGpxLayer();
			originalHash = gpxLayer.getSegmentsCacheHash();
			if (originalHash == 0) {
				return false;
			}

			scenarioRule.getScenario().onActivity(activity -> gpxLayer.setMapRendererChanged(true));
			return true;
		}

		@Override
		public boolean isIdleNow() {
			if (isIdle) {
				return true;
			}

			if (checksCounter >= CHECKS_COUNT) {
				String stepName = switch (currentStep) {
					case STEP_MOVE_MAP_TO_GPX -> "STEP_MOVE_MAP_TO_GPX";
					case STEP_GET_ORIGINAL_CACHE_HASH -> "STEP_GET_ORIGINAL_CACHE_HASH";
					case STEP_REFRESH_MAP -> "STEP_REFRESH_MAP";
					case STEP_GET_NEW_HASH -> "STEP_GET_NEW_HASH";
					default -> "UNKNOWN_STEP";
				};

				throw new AssertionError("GpxRendererChangeIdlingResource timeout at step: " + stepName);
			}

			return false;
		}
	}
}

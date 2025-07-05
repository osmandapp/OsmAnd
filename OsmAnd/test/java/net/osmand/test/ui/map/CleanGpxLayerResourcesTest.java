package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.refreshMap;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

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
						moveMapToGpx(gpxFile);
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
		moveMapToGpx(importedGpxFile);

		IdlingResource idlingResource = new GpxRendererChangeIdlingResource(app);
		registerIdlingResources(idlingResource);
		Espresso.onIdle();
		unregisterIdlingResources(idlingResource);
	}

	private void moveMapToGpx(@Nullable GpxFile gpx) {
		if (gpx == null) {
			return;
		}

		KQuadRect rect = gpx.getRect();
		if (rect.getLeft() != 0 && rect.getRight() != 0) {
			app.getOsmandMap().getMapView().fitRectToMap(rect.getLeft(), rect.getRight(),
					rect.getTop(), rect.getBottom(), (int) rect.width(), (int) rect.height(), 0);
		}
	}

	public class GpxRendererChangeIdlingResource implements IdlingResource {

		private final OsmandApplication app;
		private ResourceCallback callback;
		private boolean isIdle = false;

		public GpxRendererChangeIdlingResource(@NonNull OsmandApplication app) {
			this.app = app;
			startSequence();
		}

		private void startSequence() {
			Handler handler = new Handler(Looper.getMainLooper());

			handler.postDelayed(() -> {
				GPXLayer gpxLayer = app.getOsmandMap().getMapLayers().getGpxLayer();
				int originalHash = gpxLayer.getSegmentsCacheHash();
				scenarioRule.getScenario().onActivity(activity -> gpxLayer.setMapRendererChanged(true));
				refreshMap(app);

				handler.postDelayed(() -> {
					int newHash = gpxLayer.getSegmentsCacheHash();

					assert originalHash != newHash : "Cached segments didn't updated";

					isIdle = true;
					if (callback != null) {
						callback.onTransitionToIdle();
					}

				}, 1000);

			}, 1000);
		}

		@Override
		public String getName() {
			return getClass().getSimpleName();
		}

		@Override
		public boolean isIdleNow() {
			return isIdle;
		}

		@Override
		public void registerIdleTransitionCallback(ResourceCallback callback) {
			this.callback = callback;
		}
	}
}

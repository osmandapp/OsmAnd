package net.osmand.plus.track;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class GpxBlockStatisticsBuilderTest extends AndroidTest {

	private static final String GPX_NAME = "gpx_recalc_test.gpx";

	/**
	 * The track Overview shows the blocks from a summary analysis without point data, but a tapped block opens
	 * Analyze on map, whose chart is drawn from the point data (https://github.com/osmandapp/OsmAnd/issues/26734).
	 */
	@Test
	public void testBlockOpensAnalyzeOnMapWithChartData() throws IOException {
		SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
		selectedGpxFile.setGpxFile(importGpx(), app);
		GpxTrackAnalysis summary = selectedGpxFile.getTrackSummaryAnalysisToDisplay(app);
		assertFalse("The Overview blocks are expected to show an analysis without point data",
				summary.getCollectPointData());

		GpxDisplayItem[] openedItem = new GpxDisplayItem[1];
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
			RecyclerView blocksView = new RecyclerView(app);
			GpxBlockStatisticsBuilder builder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, false);
			builder.setBlocksView(blocksView, false);
			builder.initStatBlocks(new AnalyzeOnMapListener(openedItem), Color.BLUE, summary);
			blocksView.measure(makeMeasureSpec(1000, EXACTLY), makeMeasureSpec(200, EXACTLY));
			blocksView.layout(0, 0, 1000, 200);
			blocksView.getChildAt(0).performClick();
		});

		assertNotNull("Tapping the Distance block did not open Analyze on map", openedItem[0]);
		assertTrue("Analyze on map was opened without the point data of its chart",
				openedItem[0].analysis.getCollectPointData() && !openedItem[0].analysis.getPointAttributes().isEmpty());
	}

	@NonNull
	private GpxFile importGpx() throws IOException {
		GpxFile[] gpxFile = new GpxFile[1];
		ResourcesImporter.importGpxAssets(app, Collections.singletonList(GPX_NAME), new SaveImportedGpxListener() {
			@Override
			public void onGpxSaved(@Nullable String error, @NonNull GpxFile file) {
				gpxFile[0] = file;
			}
		});
		assertNotNull(gpxFile[0]);
		return gpxFile[0];
	}

	private static class AnalyzeOnMapListener implements SegmentActionsListener {

		private final GpxDisplayItem[] openedItem;

		private AnalyzeOnMapListener(@NonNull GpxDisplayItem[] openedItem) {
			this.openedItem = openedItem;
		}

		@Override
		public void openAnalyzeOnMap(@NonNull GpxDisplayItem gpxItem) {
			openedItem[0] = gpxItem;
		}

		@Override
		public void updateContent() {
		}

		@Override
		public void onChartTouch() {
		}

		@Override
		public void scrollBy(int px) {
		}

		@Override
		public void onPointSelected(TrkSegment segment, double lat, double lon) {
		}

		@Override
		public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment) {
		}

		@Override
		public void showOptionsPopupMenu(View view, TrkSegment trkSegment, boolean confirmDeletion, GpxDisplayItem gpxItem) {
		}

		@Override
		public void openGetAltitudeBottomSheet(@NonNull GpxDisplayItem gpxItem) {
		}
	}
}

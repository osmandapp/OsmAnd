package net.osmand.plus.track;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.R;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GpxBlockStatisticsBuilderTest extends AndroidTest {

	private static final String GPX_NAME = "gpx_recalc_test.gpx";
	private static final int BLOCKS_WIDTH = 5000;

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

		GpxDisplayItem openedItem = tapDistanceBlock(selectedGpxFile, summary, new ArrayList<>());
		assertNotNull("Tapping the Distance block did not open Analyze on map", openedItem);
		assertTrue("Analyze on map was opened without the point data of its chart",
				openedItem.analysis.getCollectPointData() && !openedItem.analysis.getPointAttributes().isEmpty());
	}

	/**
	 * After a restart the blocks show the summary read from the GPX database, which has no data flags: the Duration
	 * block and the Speed chart of the Distance block have to follow its average speed.
	 */
	@Test
	public void testDatabaseSummaryShowsDurationAndSpeedChart() {
		SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
		selectedGpxFile.setGpxFile(createTrack(true), app);
		GpxTrackAnalysis summary = getDatabaseSummary(selectedGpxFile);

		List<String> blockTitles = new ArrayList<>();
		GpxDisplayItem openedItem = tapDistanceBlock(selectedGpxFile, summary, blockTitles);
		assertTrue("No Duration block among " + blockTitles, blockTitles.contains(app.getString(R.string.duration)));
		assertNotNull("Tapping the Distance block did not open Analyze on map", openedItem);
		assertArrayEquals(new GPXDataSetType[] {GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED}, openedItem.chartTypes);
	}

	/**
	 * The summary read from the GPX database has no elevation flag either, so a track with elevation but without
	 * time has to take its chart types from the point data.
	 */
	@Test
	public void testBlockOfTrackWithoutTimeOpensAltitudeChart() {
		SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
		selectedGpxFile.setGpxFile(createTrack(false), app);
		GpxTrackAnalysis summary = getDatabaseSummary(selectedGpxFile);

		GpxDisplayItem openedItem = tapDistanceBlock(selectedGpxFile, summary, new ArrayList<>());
		assertNotNull("Tapping the Distance block did not open Analyze on map", openedItem);
		assertArrayEquals(new GPXDataSetType[] {GPXDataSetType.ALTITUDE}, openedItem.chartTypes);
	}

	@NonNull
	private GpxTrackAnalysis getDatabaseSummary(@NonNull SelectedGpxFile selectedGpxFile) {
		// GpxDatabase reads the stored values only, without the data flags of the points
		GpxTrackAnalysis summary = selectedGpxFile.getTrackSummaryAnalysisToDisplay(app);
		summary.getAvailableAttributes().clear();
		return summary;
	}

	@Nullable
	private GpxDisplayItem tapDistanceBlock(@NonNull SelectedGpxFile selectedGpxFile, @NonNull GpxTrackAnalysis summary,
	                                        @NonNull List<String> blockTitles) {
		GpxDisplayItem[] openedItem = new GpxDisplayItem[1];
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
			RecyclerView blocksView = new RecyclerView(app);
			GpxBlockStatisticsBuilder builder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, false);
			builder.setBlocksView(blocksView, false);
			builder.initStatBlocks(new AnalyzeOnMapListener(openedItem), Color.BLUE, summary);
			blocksView.measure(makeMeasureSpec(BLOCKS_WIDTH, EXACTLY), makeMeasureSpec(200, EXACTLY));
			blocksView.layout(0, 0, BLOCKS_WIDTH, 200);
			for (int i = 0; i < blocksView.getChildCount(); i++) {
				TextView title = blocksView.getChildAt(i).findViewById(R.id.title);
				blockTitles.add(title.getText().toString());
			}
			blocksView.getChildAt(0).performClick();
		});
		return openedItem[0];
	}

	@NonNull
	private GpxFile createTrack(boolean withTime) {
		TrkSegment segment = new TrkSegment();
		segment.getPoints().add(createPoint(50.0, 30.0, 100.0, withTime ? 1_000L : 0));
		segment.getPoints().add(createPoint(50.001, 30.001, 120.0, withTime ? 61_000L : 0));
		segment.getPoints().add(createPoint(50.002, 30.002, 110.0, withTime ? 121_000L : 0));
		Track track = new Track();
		track.getSegments().add(segment);
		GpxFile gpxFile = new GpxFile(null);
		gpxFile.getTracks().add(track);
		return gpxFile;
	}

	@NonNull
	private WptPt createPoint(double lat, double lon, double elevation, long time) {
		WptPt point = new WptPt();
		point.setLat(lat);
		point.setLon(lon);
		point.setEle(elevation);
		point.setTime(time);
		return point;
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

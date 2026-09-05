package net.osmand.plus.myplaces.tracks.dialogs;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.plus.track.helpers.GpxDisplayItem;

public interface SegmentActionsListener {

	void updateContent();

	void onChartTouch();

	void scrollBy(int px);

	void onPointSelected(TrkSegment segment, double lat, double lon);

	void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment);

	void showOptionsPopupMenu(View view, TrkSegment trkSegment, boolean confirmDeletion, GpxDisplayItem gpxItem);

	void openAnalyzeOnMap(@NonNull GpxDisplayItem gpxItem);

	void openGetAltitudeBottomSheet(@NonNull GpxDisplayItem gpxItem);
}

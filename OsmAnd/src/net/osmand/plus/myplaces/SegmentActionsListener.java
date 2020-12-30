package net.osmand.plus.myplaces;

import android.view.View;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;

import java.util.List;

public interface SegmentActionsListener {

	void updateContent();

	void onChartTouch();

	void scrollBy(int px);

	void onPointSelected(double lat, double lon);

	void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment);

	void showOptionsPopupMenu(View view, TrkSegment trkSegment, boolean confirmDeletion);

	void openAnalyzeOnMap(GpxDisplayItem gpxItem, List<ILineDataSet> dataSets, GPXTabItemType tabType);
}

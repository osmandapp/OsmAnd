package net.osmand.plus.configmap.tracks;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.util.MapUtils;

import java.io.File;

public enum TracksSortMode {

	NEAREST(R.string.shared_string_nearest, R.drawable.ic_action_nearby),
	NAME_ASCENDING(R.string.sort_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	NAME_DESCENDING(R.string.sort_name_descending, R.drawable.ic_action_sort_by_name_descending),
	DATE_ASCENDING(R.string.sort_date_ascending, R.drawable.ic_action_sort_date_1),
	DATE_DESCENDING(R.string.sort_date_descending, R.drawable.ic_action_sort_date_31),
	DISTANCE_DESCENDING(R.string.sort_distance_descending, R.drawable.ic_action_sort_long_to_short),
	DISTANCE_ASCENDING(R.string.sort_distance_ascending, R.drawable.ic_action_sort_short_to_long),
	DURATION_DESCENDING(R.string.sort_duration_descending, R.drawable.ic_action_sort_duration_long_to_short),
	DURATION_ASCENDING(R.string.sort_duration_ascending, R.drawable.ic_action_sort_duration_short_to_long);

	private final int iconId;
	private final int nameId;

	TracksSortMode(@StringRes int nameId, @DrawableRes int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	// -1 - means 1st is less (higher) than 2nd
	public int compare(@NonNull GPXInfo info1, @NonNull GPXInfo info2, @NonNull TracksComparator comparator) {
		switch (this) {
			case NEAREST:
				if (info1.getGpxFile() == null || info2.getGpxFile() == null) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				double minDistance1 = getMinDistance(info1, comparator);
				double minDistance2 = getMinDistance(info2, comparator);
				return Double.compare(minDistance1, minDistance2);
			case NAME_ASCENDING:
				return comparator.collator.compare(info1.getName(), info2.getName());
			case NAME_DESCENDING:
				return -comparator.collator.compare(info1.getName(), info2.getName());
			case DATE_ASCENDING:
				File file1 = info1.getFile();
				File file2 = info2.getFile();
				if (file1 == null || file2 == null || file1.lastModified() == file2.lastModified()) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return -Long.compare(file1.lastModified(), file2.lastModified());
			case DATE_DESCENDING:
				file1 = info1.getFile();
				file2 = info2.getFile();
				if (file1 == null || file2 == null || file1.lastModified() == file2.lastModified()) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return Long.compare(file1.lastModified(), file2.lastModified());
			case DISTANCE_DESCENDING:
				GpxDataItem dataItem1 = info1.getDataItem();
				GpxDataItem dataItem2 = info2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				GPXTrackAnalysis analysis1 = dataItem1.getAnalysis();
				GPXTrackAnalysis analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.totalDistance == analysis2.totalDistance) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return -Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DISTANCE_ASCENDING:
				dataItem1 = info1.getDataItem();
				dataItem2 = info2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.totalDistance == analysis2.totalDistance) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DURATION_DESCENDING:
				dataItem1 = info1.getDataItem();
				dataItem2 = info2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.timeSpan == analysis2.timeSpan) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return -Long.compare(analysis1.timeSpan, analysis2.timeSpan);
			case DURATION_ASCENDING:
				dataItem1 = info1.getDataItem();
				dataItem2 = info2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.timeSpan == analysis2.timeSpan) {
					return comparator.collator.compare(info1.getName(), info2.getName());
				}
				return Long.compare(analysis1.timeSpan, analysis2.timeSpan);
		}
		return 0;
	}

	private double getMinDistance(@NonNull GPXInfo gpxInfo, @NonNull TracksComparator comparator) {
		double minDistance = Double.MAX_VALUE;
		for (WptPt wptPt : gpxInfo.getGpxFile().getAllSegmentsPoints()) {
			double distance = MapUtils.getDistance(comparator.latLon, wptPt.lat, wptPt.lon);
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
		return minDistance;
	}
}
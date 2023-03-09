package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.Comparator;

public class TracksComparator implements Comparator<Object> {

	public final LatLon latLon;
	public final TrackTab trackTab;
	public final TracksSortMode sortMode;
	public final Collator collator = OsmAndCollator.primaryCollator();

	public TracksComparator(@NonNull TrackTab trackTab, @NonNull LatLon latLon) {
		this.trackTab = trackTab;
		this.sortMode = trackTab.getSortMode();
		this.latLon = latLon;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Integer && o2 instanceof Integer) {
			return Integer.compare((Integer) o1, (Integer) o2);
		}
		if (o1 instanceof Integer) {
			return -1;
		}
		if (o2 instanceof Integer) {
			return 1;
		}
		if (o1 instanceof TrackItem && o2 instanceof TrackItem) {
			return compareTrackItems((TrackItem) o1, (TrackItem) o2);
		}
		return 0;
	}

	private int compareTrackItems(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		switch (sortMode) {
			case NEAREST:
				GpxDataItem dataItem1 = item1.getDataItem();
				GpxDataItem dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				GPXTrackAnalysis analysis1 = dataItem1.getAnalysis();
				GPXTrackAnalysis analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.latLonStart == null || analysis2.latLonStart == null) {
					return compareTrackItemNames(item1, item2);
				}
				double distance1 = MapUtils.getDistance(latLon, analysis1.latLonStart);
				double distance2 = MapUtils.getDistance(latLon, analysis2.latLonStart);
				return Double.compare(distance1, distance2);
			case NAME_ASCENDING:
				return compareTrackItemNames(item1, item2);
			case NAME_DESCENDING:
				return -compareTrackItemNames(item1, item2);
			case DATE_ASCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.startTime == analysis2.startTime) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(analysis1.startTime, analysis2.startTime);
			case DATE_DESCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.startTime == analysis2.startTime) {
					return compareTrackItemNames(item1, item2);
				}
				return Long.compare(analysis1.startTime, analysis2.startTime);
			case LAST_MODIFIED:
				File file1 = item1.getFile();
				File file2 = item2.getFile();
				if (file1 == null || file2 == null || file1.lastModified() == file2.lastModified()) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(file1.lastModified(), file2.lastModified());
			case DISTANCE_DESCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.totalDistance == analysis2.totalDistance) {
					return compareTrackItemNames(item1, item2);
				}
				return -Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DISTANCE_ASCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.totalDistance == analysis2.totalDistance) {
					return compareTrackItemNames(item1, item2);
				}
				return Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DURATION_DESCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.timeSpan == analysis2.timeSpan) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(analysis1.timeSpan, analysis2.timeSpan);
			case DURATION_ASCENDING:
				dataItem1 = item1.getDataItem();
				dataItem2 = item2.getDataItem();
				if (dataItem1 == null || dataItem2 == null) {
					return compareTrackItemNames(item1, item2);
				}
				analysis1 = dataItem1.getAnalysis();
				analysis2 = dataItem2.getAnalysis();
				if (analysis1 == null || analysis2 == null || analysis1.timeSpan == analysis2.timeSpan) {
					return compareTrackItemNames(item1, item2);
				}
				return Long.compare(analysis1.timeSpan, analysis2.timeSpan);
		}
		return 0;
	}

	private int compareTrackItemNames(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		if (trackTab.type == TrackTabType.FOLDER) {
			return collator.compare(item1.getName(), item2.getName());
		} else {
			return collator.compare(item1.getPath(), item2.getPath());
		}
	}
}
package net.osmand.plus.configmap.tracks;

import static com.jwetherell.openmap.common.LatLonPoint.EQUIVALENT_TOLERANCE;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolderAnalysis;

import java.util.Comparator;

public class TrackTabsComparator implements Comparator<TrackTab> {

	private final OsmandApplication app;
	private final TracksSortMode sortMode;
	final SmartFolderHelper smartFolderHelper;
	final Collator collator = OsmAndCollator.primaryCollator();

	public TrackTabsComparator(@NonNull OsmandApplication app, @NonNull TracksSortMode sortMode) {
		this.app = app;
		this.sortMode = sortMode;
		this.smartFolderHelper = app.getSmartFolderHelper();
	}

	@Override
	public int compare(TrackTab o1, TrackTab o2) {
		if (o1.type == TrackTabType.ON_MAP) {
			return o2.type == TrackTabType.ON_MAP ? compareTabNames(o1, o2) : -1;
		}
		if (o2.type == TrackTabType.ON_MAP) {
			return 1;
		}
		if (o1.type == TrackTabType.ALL) {
			return o2.type == TrackTabType.ALL ? compareTabNames(o1, o2) : -1;
		}
		if (o2.type == TrackTabType.ALL) {
			return 1;
		}
		return compareTrackFolders(o1, o2);
	}

	private int compareTrackFolders(@NonNull TrackTab folder1, @NonNull TrackTab folder2) {
		TrackFolderAnalysis folderAnalysis1 = folder1.getFolderAnalysis();
		TrackFolderAnalysis folderAnalysis2 = folder2.getFolderAnalysis();

		switch (sortMode) {
			case NAME_ASCENDING:
				return compareTabNames(folder1, folder2);
			case NAME_DESCENDING:
				return -compareTabNames(folder1, folder2);
			case DATE_ASCENDING:
				return compareTabsByLastModified(folder1, folder2);
			case DATE_DESCENDING:
				return -compareTabsByLastModified(folder1, folder2);
			case LAST_MODIFIED:
				return compareTabsByLastModified(folder1, folder2);
			case DISTANCE_DESCENDING:
				if (Math.abs(folderAnalysis1.totalDistance - folderAnalysis2.totalDistance) >= EQUIVALENT_TOLERANCE) {
					return -Float.compare(folderAnalysis1.totalDistance, folderAnalysis2.totalDistance);
				}
			case DISTANCE_ASCENDING:
				if (Math.abs(folderAnalysis1.totalDistance - folderAnalysis2.totalDistance) >= EQUIVALENT_TOLERANCE) {
					return Float.compare(folderAnalysis1.totalDistance, folderAnalysis2.totalDistance);
				}
			case DURATION_DESCENDING:
				if (folderAnalysis1.timeSpan != folderAnalysis2.timeSpan) {
					return -Long.compare(folderAnalysis1.timeSpan, folderAnalysis2.timeSpan);
				}
			case DURATION_ASCENDING:
				if (folderAnalysis1.timeSpan != folderAnalysis2.timeSpan) {
					return Long.compare(folderAnalysis1.timeSpan, folderAnalysis2.timeSpan);
				}
		}
		return compareTabNames(folder1, folder2);
	}

	private int compareTabsByLastModified(@NonNull TrackTab folder1, @NonNull TrackTab folder2) {
		long lastModified1 = folder1.lastModified(app, smartFolderHelper);
		long lastModified2 = folder2.lastModified(app, smartFolderHelper);

		if (lastModified1 == lastModified2) {
			return compareTabNames(folder1, folder2);
		}
		return -Long.compare(lastModified1, lastModified2);
	}

	private int compareTabNames(@NonNull TrackTab folder1, @NonNull TrackTab folder2) {
		return collator.compare(folder1.getName(app), folder2.getName(app));
	}
}

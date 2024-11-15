package net.osmand.plus.mapmarkers;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.MapUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

public class MapMarkersComparator implements Comparator<MapMarker> {

	public static final int BY_NAME = 0;
	public static final int BY_DISTANCE_DESC = 1;
	public static final int BY_DISTANCE_ASC = 2;
	public static final int BY_DATE_ADDED_DESC = 3;
	public static final int BY_DATE_ADDED_ASC = 4;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({BY_NAME, BY_DISTANCE_DESC, BY_DISTANCE_ASC, BY_DATE_ADDED_DESC, BY_DATE_ADDED_ASC})
	public @interface MapMarkersSortByDef {
	}

	private final OsmandApplication app;

	@MapMarkersSortByDef
	private final int sortByMode;
	@Nullable
	private final LatLon location;
	private final boolean visited;

	public MapMarkersComparator(@NonNull OsmandApplication app, @MapMarkersSortByDef int sortByMode,
	                            @Nullable LatLon location, boolean visited) {
		this.app = app;
		this.location = location;
		this.sortByMode = sortByMode;
		this.visited = visited;
	}

	@Override
	public int compare(MapMarker mapMarker1, MapMarker mapMarker2) {
		if (sortByMode == BY_DATE_ADDED_DESC || sortByMode == BY_DATE_ADDED_ASC) {
			long t1 = visited ? mapMarker1.visitedDate : mapMarker1.creationDate;
			long t2 = visited ? mapMarker2.visitedDate : mapMarker2.creationDate;
			if (t1 > t2) {
				return sortByMode == BY_DATE_ADDED_DESC ? -1 : 1;
			} else if (t1 == t2) {
				return 0;
			} else {
				return sortByMode == BY_DATE_ADDED_DESC ? 1 : -1;
			}
		} else if (location != null && (sortByMode == BY_DISTANCE_DESC || sortByMode == BY_DISTANCE_ASC)) {
			int d1 = (int) MapUtils.getDistance(location, mapMarker1.getLatitude(), mapMarker1.getLongitude());
			int d2 = (int) MapUtils.getDistance(location, mapMarker2.getLatitude(), mapMarker2.getLongitude());
			if (d1 > d2) {
				return sortByMode == BY_DISTANCE_DESC ? -1 : 1;
			} else if (d1 == d2) {
				return 0;
			} else {
				return sortByMode == BY_DISTANCE_DESC ? 1 : -1;
			}
		} else {
			String n1 = mapMarker1.getName(app);
			String n2 = mapMarker2.getName(app);
			return n1.compareToIgnoreCase(n2);
		}
	}
}
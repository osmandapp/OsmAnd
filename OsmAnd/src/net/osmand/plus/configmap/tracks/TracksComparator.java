package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.plus.track.helpers.GPXInfo;

import java.util.Comparator;

public class TracksComparator implements Comparator<Object> {

	public final LatLon latLon;
	public final TracksSortMode sortMode;
	public final Collator collator = OsmAndCollator.primaryCollator();

	public TracksComparator(@NonNull TracksSortMode sortMode, @NonNull LatLon latLon) {
		this.sortMode = sortMode;
		this.latLon = latLon;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Integer && (Integer) o1 == TYPE_SORT_TRACKS) {
			return -1;
		}
		if (o2 instanceof Integer && (Integer) o2 == TYPE_SORT_TRACKS) {
			return 1;
		}
		if (o1 instanceof Integer && ((Integer) o1 == TYPE_NO_TRACKS || (Integer) o1 == TYPE_NO_VISIBLE_TRACKS)) {
			return -1;
		}
		if (o2 instanceof Integer && ((Integer) o2 == TYPE_NO_TRACKS || (Integer) o2 == TYPE_NO_VISIBLE_TRACKS)) {
			return 1;
		}

		if (o1 instanceof GPXInfo && o2 instanceof GPXInfo) {
			return sortMode.compare((GPXInfo) o1, (GPXInfo) o2, this);
		}
		return 0;
	}
}
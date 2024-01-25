package net.osmand.plus.track.helpers;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

public class TrackDisplayGroup extends GpxDisplayGroup {

	public TrackDisplayGroup(@NonNull GPXFile gpxFile) {
		this(gpxFile, -1);
	}

	public TrackDisplayGroup(@NonNull GPXFile gpxFile, int trackIndex) {
		super(gpxFile, trackIndex);
	}

	@Override
	public void applyName(@NonNull Context context, @NonNull String name) {
		setGpxName(name);
		int trackIndex = getIndex();
		GPXFile gpxFile = getGpxFile();
		String trackIndexStr = trackIndex == -1 || gpxFile.tracks.size() == 1 ? "" : String.valueOf(trackIndex + 1);
		setName(context.getString(R.string.gpx_selection_track, name, trackIndexStr));
	}

	@Override
	public GpxDisplayItemType getType() {
		return GpxDisplayItemType.TRACK_SEGMENT;
	}

	@Override
	protected GpxDisplayGroup newInstance(@NonNull GPXFile gpxFile, int index) {
		return new TrackDisplayGroup(gpxFile, index);
	}

}

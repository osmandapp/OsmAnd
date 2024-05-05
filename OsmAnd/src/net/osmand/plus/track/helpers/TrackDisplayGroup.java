package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.GpxSplitType.DISTANCE;
import static net.osmand.plus.track.GpxSplitType.NO_SPLIT;
import static net.osmand.plus.track.GpxSplitType.TIME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.plus.R;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

public class TrackDisplayGroup extends GpxDisplayGroup {

	private final Track track;
	private final boolean isGeneralTrack;

	private double splitDistance = -1;
	private int splitTime = -1;

	public TrackDisplayGroup(@NonNull GPXFile gpxFile, @NonNull Track track, boolean isGeneralTrack) {
		this(gpxFile, track, isGeneralTrack, -1);
	}

	public TrackDisplayGroup(@NonNull GPXFile gpxFile, @NonNull Track track, boolean isGeneralTrack, int trackIndex) {
		super(gpxFile, trackIndex);
		this.track = track;
		this.isGeneralTrack = isGeneralTrack;
	}

	@Nullable
	public Track getTrack() {
		return track;
	}

	public boolean isGeneralTrack() {
		return isGeneralTrack;
	}

	@Override
	public void applyName(@NonNull Context context, @NonNull String name) {
		setGpxName(name);
		int trackIndex = getIndex();
		GPXFile gpxFile = getGpxFile();
		String trackIndexStr = trackIndex == -1 || gpxFile.tracks.size() == 1 ? "" : String.valueOf(trackIndex + 1);
		setName(context.getString(R.string.gpx_selection_track, name, trackIndexStr));
	}

	public boolean isSplitDistance() {
		return splitDistance > 0;
	}

	public double getSplitDistance() {
		return splitDistance;
	}

	public boolean isSplitTime() {
		return splitTime > 0;
	}

	public int getSplitTime() {
		return splitTime;
	}

	public void updateSplit(@NonNull GpxSplitParams splitParams) {
		clearDisplayItems();
		if (splitParams.splitType == NO_SPLIT) {
			splitDistance = -1;
			splitTime = -1;
		} else if (splitParams.splitType == DISTANCE) {
			splitDistance = splitParams.splitInterval;
			splitTime = -1;
		} else if (splitParams.splitType == TIME) {
			splitDistance = -1;
			splitTime = (int) splitParams.splitInterval;
		}
	}

	@Override
	@NonNull
	public GpxDisplayItemType getType() {
		return GpxDisplayItemType.TRACK_SEGMENT;
	}

	@Override
	@NonNull
	protected GpxDisplayGroup newInstance(@NonNull GPXFile gpxFile) {
		return new TrackDisplayGroup(gpxFile, track, isGeneralTrack, getIndex());
	}

}

package net.osmand.plus.track;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.OsmandApplication;

import java.util.List;

public class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final GpxSplitType gpxSplitType;
	private final List<GpxDisplayGroup> groups;
	private final SplitTrackListener splitTrackListener;

	private final boolean joinSegments;
	private final int timeSplitInterval;
	private final double distanceSplitInterval;

	public SplitTrackAsyncTask(@NonNull OsmandApplication app,
	                           @NonNull GpxSplitType gpxSplitType,
	                           @NonNull List<GpxDisplayGroup> groups,
	                           @Nullable SplitTrackListener splitTrackListener,
	                           boolean joinSegments,
	                           int timeSplitInterval,
	                           double distanceSplitInterval) {
		this.app = app;
		this.groups = groups;
		this.gpxSplitType = gpxSplitType;
		this.splitTrackListener = splitTrackListener;
		this.joinSegments = joinSegments;
		this.timeSplitInterval = timeSplitInterval;
		this.distanceSplitInterval = distanceSplitInterval;
	}

	@Override
	protected void onPreExecute() {
		if (splitTrackListener != null) {
			splitTrackListener.trackSplittingStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		for (GpxDisplayGroup model : groups) {
			if (gpxSplitType == GpxSplitType.NO_SPLIT) {
				model.noSplit(app);
			} else if (gpxSplitType == GpxSplitType.DISTANCE && distanceSplitInterval > 0) {
				model.splitByDistance(app, distanceSplitInterval, joinSegments);
			} else if (gpxSplitType == GpxSplitType.TIME && timeSplitInterval > 0) {
				model.splitByTime(app, timeSplitInterval, joinSegments);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (splitTrackListener != null) {
			splitTrackListener.trackSplittingFinished();
		}
	}

	public interface SplitTrackListener {

		void trackSplittingStarted();

		void trackSplittingFinished();
	}
}
package net.osmand.plus.track;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GpxDisplayGroup;

import java.util.List;

public class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final List<GpxDisplayGroup> groups;
	private final SplitTrackListener listener;

	private GpxSplitType splitType = GpxSplitType.NO_SPLIT;
	private boolean joinSegments;
	private double splitInterval;

	public SplitTrackAsyncTask(@NonNull OsmandApplication app,
	                           @NonNull List<GpxDisplayGroup> groups,
	                           @Nullable SplitTrackListener listener) {
		this.app = app;
		this.groups = groups;
		this.listener = listener;
	}

	public void setSplitType(@NonNull GpxSplitType splitType) {
		this.splitType = splitType;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
	}

	public void setSplitInterval(double splitInterval) {
		this.splitInterval = splitInterval;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.trackSplittingStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		for (GpxDisplayGroup model : groups) {
			if (splitType == GpxSplitType.NO_SPLIT) {
				model.noSplit(app);
			} else if (splitType == GpxSplitType.DISTANCE) {
				model.splitByDistance(app, splitInterval, joinSegments);
			} else if (splitType == GpxSplitType.TIME) {
				model.splitByTime(app, (int) splitInterval, joinSegments);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.trackSplittingFinished();
		}
	}

	public interface SplitTrackListener {

		default void trackSplittingStarted() {
		}

		default void trackSplittingFinished() {
		}
	}
}
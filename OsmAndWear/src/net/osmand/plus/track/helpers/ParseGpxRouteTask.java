package net.osmand.plus.track.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.track.CachedTrackParams;
import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParseGpxRouteTask extends AsyncTask<Void, Void, List<RouteSegmentResult>> {

	private final GpxFile gpxFile;
	private final CachedTrackParams trackParams;
	private final ParseGpxRouteListener listener;
	private final int selectedSegment;

	public ParseGpxRouteTask(@NonNull GpxFile gpxFile, @NonNull CachedTrackParams trackParams,
	                         int selectedSegment, @Nullable ParseGpxRouteListener listener) {
		this.gpxFile = gpxFile;
		this.trackParams = trackParams;
		this.listener = listener;
		this.selectedSegment = selectedSegment;
	}

	@NonNull
	public CachedTrackParams getCachedTrackParams() {
		return trackParams;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.parseGpxRouteStarted();
		}
	}

	@Override
	protected List<RouteSegmentResult> doInBackground(Void... params) {
		return RouteProvider.parseOsmAndGPXRoute(new ArrayList<>(), gpxFile, new ArrayList<>(), selectedSegment);
	}

	@Override
	protected void onCancelled() {
		if (listener != null) {
			listener.parseGpxRouteFinished(Collections.emptyList(), false);
		}
	}

	@Override
	protected void onPostExecute(@NonNull List<RouteSegmentResult> routeSegments) {
		if (listener != null) {
			listener.parseGpxRouteFinished(routeSegments, true);
		}
	}

	public interface ParseGpxRouteListener {

		default void parseGpxRouteStarted() {

		}

		void parseGpxRouteFinished(@NonNull List<RouteSegmentResult> routeSegments, boolean success);
	}
}
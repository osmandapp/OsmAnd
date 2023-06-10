package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HeightsResolverTask extends AsyncTask<Void, Void, float[]> {

	private final List<LatLon> points;
	private final File gpxFile;
	private final HeightsResolverCallback callback;
	private final GpxHeightsResolverCallback gpxCallback;

	private GPXFile gpx;

	public interface HeightsResolverCallback {
		void onHeightsResolveDone(@Nullable float[] heights);
	}

	public interface GpxHeightsResolverCallback {
		void onHeightsResolveDone(@Nullable GPXFile gpx);
	}

	public HeightsResolverTask(@NonNull List<LatLon> points, @NonNull HeightsResolverCallback callback) {
		this.points = points;
		this.gpxFile = null;
		this.callback = callback;
		this.gpxCallback = null;
	}

	public HeightsResolverTask(@NonNull File gpxFile, @NonNull GpxHeightsResolverCallback callback) {
		this.points = null;
		this.gpxFile = gpxFile;
		this.callback = null;
		this.gpxCallback = callback;
	}

	@Override
	protected float[] doInBackground(Void... voids) {
		List<LatLon> points = this.points;
		GPXFile gpx = null;
		if (gpxFile != null && points == null) {
			gpx = GPXUtilities.loadGPXFile(gpxFile);
			if (gpx.error == null) {
				points = new ArrayList<>();
				List<WptPt> segmentsPoints = gpx.getAllSegmentsPoints();
				for (WptPt point : segmentsPoints) {
					points.add(new LatLon(point.getLatitude(), point.getLongitude()));
				}
			}
		}
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		float[] heights = mapRendererContext != null && points != null ? mapRendererContext.calculateHeights(points) : null;
		if (heights != null && gpx != null && heights.length == points.size()) {
			List<WptPt> segmentsPoints = gpx.getAllSegmentsPoints();
			int i = 0;
			for (WptPt point : segmentsPoints) {
				point.ele = heights[i++];
			}
			this.gpx = gpx;
		}
		return heights;
	}

	@Override
	protected void onPostExecute(float[] heights) {
		if (callback != null) {
			callback.onHeightsResolveDone(heights);
		} else if (gpxCallback != null) {
			gpxCallback.onHeightsResolveDone(gpx);
		}
	}
}

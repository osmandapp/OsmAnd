package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.util.ArrayList;
import java.util.List;

public class HeightsResolverTask extends AsyncTask<Void, Void, float[]> {

	private final List<LatLon> points;
	private final GPXFile gpxFile;
	private final HeightsResolverCallback callback;
	private final GpxHeightsResolverCallback gpxCallback;


	public interface HeightsResolverCallback {
		void onHeightsResolveDone(@Nullable float[] heights);
	}

	public interface GpxHeightsResolverCallback {
		void onHeightsResolveDone(@Nullable GPXFile gpxFile);
	}

	public HeightsResolverTask(@NonNull List<LatLon> points, @NonNull HeightsResolverCallback callback) {
		this.points = points;
		this.gpxFile = null;
		this.callback = callback;
		this.gpxCallback = null;
	}

	public HeightsResolverTask(@NonNull GPXFile gpxFile, @NonNull GpxHeightsResolverCallback callback) {
		this.points = null;
		this.gpxFile = gpxFile;
		this.callback = null;
		this.gpxCallback = callback;
	}

	@Override
	protected float[] doInBackground(Void... voids) {
		List<LatLon> points = this.points;
		if (gpxFile != null && points == null) {
			points = new ArrayList<>();
			List<WptPt> segmentsPoints = gpxFile.getAllSegmentsPoints();
			for (WptPt point : segmentsPoints) {
				points.add(new LatLon(point.getLatitude(), point.getLongitude()));
			}
		}
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		float[] heights = mapRendererContext != null && points != null ? mapRendererContext.calculateHeights(points) : null;
		if (heights != null && gpxFile != null && heights.length == points.size()) {
			List<WptPt> segmentsPoints = gpxFile.getAllSegmentsPoints();
			int i = 0;
			for (WptPt point : segmentsPoints) {
				point.ele = heights[i++];
			}
		}
		return heights;
	}

	@Override
	protected void onPostExecute(float[] heights) {
		if (callback != null) {
			callback.onHeightsResolveDone(heights);
		} else if (gpxCallback != null) {
			gpxCallback.onHeightsResolveDone(gpxFile);
		}
	}
}
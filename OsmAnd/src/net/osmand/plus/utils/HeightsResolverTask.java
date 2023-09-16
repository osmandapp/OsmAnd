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

	private GPXFile gpxFile;
	private final File file;
	private final List<LatLon> points;
	private final HeightsResolverCallback callback;
	private final GpxHeightsResolverCallback gpxCallback;


	public interface HeightsResolverCallback {
		void onHeightsResolveDone(@Nullable float[] heights);
	}

	public interface GpxHeightsResolverCallback {
		void onHeightsResolveDone(@Nullable GPXFile gpxFile);
	}

	public HeightsResolverTask(@NonNull List<LatLon> points, @NonNull HeightsResolverCallback callback) {
		this.file = null;
		this.gpxFile = null;
		this.points = points;
		this.callback = callback;
		this.gpxCallback = null;
	}

	public HeightsResolverTask(@NonNull GPXFile gpxFile, @NonNull GpxHeightsResolverCallback gpxCallback) {
		this.file = null;
		this.gpxFile = gpxFile;
		this.points = null;
		this.callback = null;
		this.gpxCallback = gpxCallback;
	}

	public HeightsResolverTask(@NonNull File file, @NonNull GpxHeightsResolverCallback gpxCallback) {
		this.file = file;
		this.gpxFile = null;
		this.points = null;
		this.callback = null;
		this.gpxCallback = gpxCallback;
	}

	@Override
	protected float[] doInBackground(Void... voids) {
		GPXFile gpx = gpxFile;
		List<LatLon> points = this.points;
		if (points == null) {
			if (gpx != null) {
				points = getGpxPoints(gpx);
			} else if (file != null && file.exists()) {
				gpx = GPXUtilities.loadGPXFile(file);
				if (gpx.error == null) {
					points = getGpxPoints(gpx);
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
			this.gpxFile = gpx;
		}
		return heights;
	}

	@NonNull
	private List<LatLon> getGpxPoints(@NonNull GPXFile gpx) {
		List<LatLon> list = new ArrayList<>();
		for (WptPt point : gpx.getAllSegmentsPoints()) {
			list.add(new LatLon(point.getLatitude(), point.getLongitude()));
		}
		return list;
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
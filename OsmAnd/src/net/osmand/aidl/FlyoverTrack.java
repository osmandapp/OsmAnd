package net.osmand.aidl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.PointAttributes;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One track of a flyover: the GPX with the spec's style applied, and where along it the video is at any time.
 * Video time maps to the track by the track's own times (or by distance when it has none), see {@link #at}.
 */
class FlyoverTrack {

	final int number;
	final File file;
	final GpxFile gpx;
	final List<WptPt> points;
	final double[] dist;
	final long[] time;
	final boolean timed;
	final double fromS;
	final double toS;
	final double fadeS;

	final String draw;
	final String remaining;
	final double tailKm;
	final double tailS;
	final String head;
	/** The track as OsmAnd shows it; its display segments are replaced every frame while it grows or fades. */
	@Nullable
	SelectedGpxFile selected;
	@Nullable
	private List<TrkSegment> original;

	private final double startS;
	private final double durationS;
	private final double speedX;

	/** A place on the track: between points {@code index} and {@code index + 1}, at {@code fraction}. */
	static class Position {
		final int index;
		final double fraction;
		final double lat;
		final double lon;
		final double distance;
		final boolean started;
		final boolean finished;

		Position(int index, double fraction, double lat, double lon, double distance, boolean started, boolean finished) {
			this.index = index;
			this.fraction = fraction;
			this.lat = lat;
			this.lon = lon;
			this.distance = distance;
			this.started = started;
			this.finished = finished;
		}
	}

	FlyoverTrack(@NonNull OsmandApplication app, int number, @NonNull JSONObject spec, double videoS) {
		this.number = number;
		String name = spec.optString("file");
		file = findFile(app, name);
		if (file == null) {
			throw new IllegalArgumentException("No track '" + name + "' on the phone; see osmand_list_tracks");
		}
		gpx = SharedUtil.loadGpxFile(file);
		if (gpx.getError() != null) {
			throw new IllegalArgumentException("Cannot read '" + name + "': " + gpx.getError().getMessage());
		}
		points = gpx.getAllSegmentsPoints();
		if (points.size() < 2) {
			throw new IllegalArgumentException("Track '" + name + "' has fewer than 2 points");
		}
		applyStyle(spec);

		dist = new double[points.size()];
		time = new long[points.size()];
		boolean timed = true;
		for (int i = 0; i < points.size(); i++) {
			WptPt p = points.get(i);
			time[i] = p.getTime();
			timed &= time[i] > 0 && (i == 0 || time[i] >= time[i - 1]);
			if (i > 0) {
				WptPt prev = points.get(i - 1);
				dist[i] = dist[i - 1] + MapUtils.getDistance(prev.getLat(), prev.getLon(), p.getLat(), p.getLon());
			}
		}
		this.timed = timed && time[time.length - 1] > time[0];

		fromS = spec.optDouble("from_s", 0);
		toS = spec.optDouble("to_s", videoS);
		fadeS = spec.optDouble("fade_s", 0);
		draw = spec.optString("draw", "full");
		remaining = spec.optString("remaining", "hidden");
		tailKm = spec.optDouble("tail_km", 0);
		tailS = spec.optDouble("tail_s", 0);
		head = spec.optString("head", "progressive".equals(draw) ? "dot" : "none");
		JSONObject timing = spec.optJSONObject("time");
		String mode = timing != null ? timing.optString("mode", "duration") : "duration";
		startS = fromS + (timing != null ? timing.optDouble("offset_s", 0) : 0);
		if ("track".equals(mode) && this.timed) {
			speedX = timing.optDouble("speed_x", 1);
			durationS = (time[time.length - 1] - time[0]) / 1000.0 / speedX;
		} else {
			speedX = 0;
			durationS = timing != null ? timing.optDouble("duration_s", toS - startS) : toS - startS;
		}
	}

	@Nullable
	private static File findFile(@NonNull OsmandApplication app, @NonNull String name) {
		if (Algorithms.isEmpty(name)) {
			return null;
		}
		File[] candidates = {
				app.getAppPath(IndexConstants.GPX_INDEX_DIR + name),
				app.getAppPath(IndexConstants.GPX_IMPORT_DIR + name),
				app.getAppPath(IndexConstants.GPX_RECORDED_INDEX_DIR + name)
		};
		for (File f : candidates) {
			if (f.isFile()) {
				return f;
			}
		}
		return null;
	}

	/** Style from the spec over the file's own; the file on the phone is not changed. */
	private void applyStyle(@NonNull JSONObject spec) {
		if (spec.has("color")) gpx.setColor(spec.optString("color"));
		if (spec.has("width")) gpx.setWidth(spec.optString("width"));
		if (spec.has("coloring_type")) gpx.setColoringType(spec.optString("coloring_type"));
		if (spec.has("color_palette")) gpx.setGradientColorPalette(spec.optString("color_palette"));
		if (spec.has("show_arrows")) gpx.setShowArrows(spec.optBoolean("show_arrows"));
		if (spec.has("show_start_finish")) gpx.setShowStartFinish(spec.optBoolean("show_start_finish"));
		if (spec.has("split_type")) gpx.setSplitType(spec.optString("split_type"));
		if (spec.has("split_interval")) gpx.setSplitInterval(spec.optDouble("split_interval"));
		if (spec.has("visualization_3d")) gpx.set3DVisualizationType(spec.optString("visualization_3d"));
		if (spec.has("wall_coloring")) gpx.set3DWallColoringType(spec.optString("wall_coloring"));
		if (spec.has("line_position_3d")) gpx.set3DLinePositionType(spec.optString("line_position_3d"));
		if (spec.has("vertical_exaggeration")) gpx.setAdditionalExaggeration((float) spec.optDouble("vertical_exaggeration"));
		if (spec.has("elevation_meters")) gpx.setElevationMeters((float) spec.optDouble("elevation_meters"));
		if (spec.has("show_wpts") && !spec.optBoolean("show_wpts")) gpx.getPointsList().clear();
	}

	double getLength() {
		return dist[dist.length - 1];
	}

	boolean isVisible(double t) {
		return t >= fromS && t <= toS;
	}

	/** 0..1, how much of the track is shown at video time t (for a fade). */
	float getOpacity(double t) {
		if (!isVisible(t)) return 0;
		if (fadeS <= 0) return 1;
		return (float) Math.min(1, Math.min((t - fromS) / fadeS, (toS - t) / fadeS));
	}

	/** Where the track is at video time t; before its start it stays at the first point, after its end at the last. */
	@NonNull
	Position at(double t) {
		double progress = durationS > 0 ? (t - startS) / durationS : 1;
		boolean started = progress >= 0;
		boolean finished = progress >= 1;
		progress = Math.max(0, Math.min(1, progress));
		int i;
		double fraction;
		if (timed) {
			double ms = time[0] + progress * (time[time.length - 1] - time[0]);
			i = search(time, ms);
			long span = time[i + 1] - time[i];
			fraction = span > 0 ? (ms - time[i]) / span : 0;
		} else {
			double d = progress * getLength();
			i = search(dist, d);
			double span = dist[i + 1] - dist[i];
			fraction = span > 0 ? (d - dist[i]) / span : 0;
		}
		WptPt a = points.get(i);
		WptPt b = points.get(i + 1);
		return new Position(i, fraction, a.getLat() + (b.getLat() - a.getLat()) * fraction,
				a.getLon() + (b.getLon() - a.getLon()) * fraction, dist[i] + (dist[i + 1] - dist[i]) * fraction,
				started, finished);
	}

	/** Bearing of the track around video time t, from the point at {@code t - back} to the one at {@code t + ahead}. */
	double bearing(double t, double back, double ahead) {
		Position from = at(t - back);
		Position to = at(t + ahead);
		if (MapUtils.getDistance(from.lat, from.lon, to.lat, to.lon) < 1) {
			to = at(t + ahead * 4 + 1);
		}
		return bearing(from.lat, from.lon, to.lat, to.lon);
	}

	/** Initial great-circle bearing, degrees clockwise from north. */
	static double bearing(double lat1, double lon1, double lat2, double lon2) {
		double f1 = Math.toRadians(lat1), f2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
		double y = Math.sin(dl) * Math.cos(f2);
		double x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl);
		return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
	}

	/**
	 * What the map shows of the track at video time t: everything, nothing outside from_s..to_s, faded in and out,
	 * or with draw: progressive the part passed so far (or its last tail_km / tail_s) and the rest hidden, faded or
	 * thin. Runs on the UI thread before the map layers draw.
	 */
	void updateDisplay(double t, double videoS) {
		if (selected == null) {
			return;
		}
		List<TrkSegment> display = selected.getPointsToDisplay();
		if (original == null) {
			original = new ArrayList<>(display);
		}
		float opacity = getOpacity(Math.min(t, videoS));
		if (!"progressive".equals(draw) && fromS <= 0 && fadeS <= 0 && t <= toS) {
			if (display.size() != original.size() || (!display.isEmpty() && display.get(0) != original.get(0))) {
				display.clear();
				display.addAll(original);
			}
			return;
		}
		display.clear();
		if (opacity <= 0 || original.isEmpty()) {
			return;
		}
		TrkSegment style = original.get(0);
		if (!"progressive".equals(draw)) {
			for (TrkSegment s : original) {
				display.add(copy(s, s.getPoints(), opacity));
			}
			return;
		}
		Position p = at(t);
		double from = 0;
		if (tailKm > 0) from = Math.max(from, p.distance - tailKm * 1000);
		if (tailS > 0) from = Math.max(from, at(t - tailS).distance);
		List<WptPt> passed = new ArrayList<>();
		if (from > 0) {
			passed.add(pointAt(from));
		}
		for (int i = 0; i <= p.index; i++) {
			if (dist[i] > from) passed.add(points.get(i));
		}
		WptPt headPoint = pointAt(p.distance);
		passed.add(headPoint);
		if (passed.size() >= 2) {
			display.add(copy(style, passed, opacity));
		}
		if (!"hidden".equals(remaining) && p.index + 1 < points.size()) {
			List<WptPt> rest = new ArrayList<>();
			rest.add(headPoint);
			rest.addAll(points.subList(p.index + 1, points.size()));
			TrkSegment r = copy(style, rest, "faded".equals(remaining) ? opacity * 0.35f : opacity);
			if ("thin".equals(remaining)) {
				r.setWidth("thin");
			}
			display.add(r);
		}
	}

	/** The head marker's place and a point a little ahead, for its direction; null when there is no head. */
	@Nullable
	WptPt[] headPoints(double t) {
		if ("none".equals(head) || !isVisible(t)) {
			return null;
		}
		Position p = at(t);
		return new WptPt[] {pointAt(p.distance), pointAt(Math.min(getLength(), p.distance + 15))};
	}

	@Nullable
	Integer getColor() {
		List<TrkSegment> segments = original != null ? original : null;
		return segments != null && !segments.isEmpty() ? segments.get(0).getColor(null) : null;
	}

	/** A point at a distance along the track, with interpolated elevation (the 3D line stays on the relief). */
	@NonNull
	private WptPt pointAt(double distance) {
		int i = search(dist, distance);
		double span = dist[i + 1] - dist[i];
		double f = span > 0 ? Math.max(0, Math.min(1, (distance - dist[i]) / span)) : 0;
		WptPt a = points.get(i), b = points.get(i + 1);
		WptPt pt = new WptPt(a.getLat() + (b.getLat() - a.getLat()) * f, a.getLon() + (b.getLon() - a.getLon()) * f);
		if (!Double.isNaN(a.getEle()) && !Double.isNaN(b.getEle())) {
			pt.setEle(a.getEle() + (b.getEle() - a.getEle()) * f);
		}
		return pt;
	}

	/** A new segment object, so the map layer builds its line again; colour and width as the original's. */
	@NonNull
	private static TrkSegment copy(@NonNull TrkSegment style, @NonNull List<WptPt> points, float opacity) {
		TrkSegment s = new TrkSegment();
		s.getPoints().addAll(points);
		Integer color = style.getColor(null);
		if (color != null) {
			int alpha = Math.round(((color >>> 24) & 0xFF) * opacity);
			s.setColor((alpha << 24) | (color & 0xFFFFFF));
		}
		s.setWidth(style.getWidth(null));
		return s;
	}

	/** Value of a point attribute (sensor or any extension tag) at a position, interpolated; NaN when absent. */
	double attribute(@NonNull Position p, @NonNull String tag) {
		double a = attribute(points.get(p.index), tag);
		double b = attribute(points.get(p.index + 1), tag);
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		return a + (b - a) * p.fraction;
	}

	static double attribute(@NonNull WptPt point, @NonNull String tag) {
		Map<String, String> ext = point.getExtensionsToRead();
		String value = ext.get(tag);
		if (value == null) value = ext.get("osmand:" + tag);
		if (value == null) return Double.NaN;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	static final String TAG_HR = PointAttributes.SENSOR_TAG_HEART_RATE;
	static final String TAG_CADENCE = PointAttributes.SENSOR_TAG_CADENCE;
	static final String TAG_POWER = PointAttributes.SENSOR_TAG_BIKE_POWER;
	static final String TAG_SENSOR_SPEED = PointAttributes.SENSOR_TAG_SPEED;
	static final String TAG_TEMP_AIR = PointAttributes.SENSOR_TAG_TEMPERATURE_A;
	static final String TAG_TEMP_WATER = PointAttributes.SENSOR_TAG_TEMPERATURE_W;
	static final String TAG_BIKE_DISTANCE = PointAttributes.SENSOR_TAG_DISTANCE;

	private static int search(@NonNull double[] values, double v) {
		int lo = 0, hi = values.length - 2;
		while (lo < hi) {
			int mid = (lo + hi + 1) >>> 1;
			if (values[mid] <= v) lo = mid; else hi = mid - 1;
		}
		return lo;
	}

	private static int search(@NonNull long[] values, double v) {
		int lo = 0, hi = values.length - 2;
		while (lo < hi) {
			int mid = (lo + hi + 1) >>> 1;
			if (values[mid] <= v) lo = mid; else hi = mid - 1;
		}
		return lo;
	}
}

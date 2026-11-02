package net.osmand.plus.flyover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
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

	// OsmAnd's default track colour, for tracks that have none and still have to fade
	private static final int DEFAULT_COLOR = 0xFFE8442E;

	final int number;
	final File file;
	/** A GPX passed in the call (gpx_data): written next to the videos for the recording and deleted after it. */
	final boolean temporary;
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
	/** OsmAnd location icon for the head (LocationIcon name or a 3D model name). */
	final String headIcon;
	/** The track as OsmAnd shows it; its display segments are replaced every frame while it grows or fades. */
	@Nullable
	SelectedGpxFile selected;
	@Nullable
	private List<TrkSegment> original;

	private final double startS;
	private final double easeInS;
	private final double easeOutS;
	private final double durationS;
	private static final double HEAD_ROAD_M = 20;
	private static final double HEAD_ROAD_PX = 30;
	/** The track's own profile (request or file), null when it uses the current one. */
	@Nullable
	final String profileKey;
	@Nullable
	private Integer styleColor;
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

	/** The track's settings: osmand:flyover_* extensions of the file, overridden by the request's JSON. */
	final JSONObject spec;

	FlyoverTrack(@NonNull OsmandApplication app, int number, @NonNull JSONObject request, double videoS) {
		this.number = number;
		String name = request.optString("file");
		String data = request.optString("gpx_data");
		temporary = !Algorithms.isEmpty(data);
		if (temporary) {
			// a GPX passed in the call: kept with the flyover's files, not among the user's tracks
			name = "gpx_data " + number;
			file = new File(app.getAppPath("flyover/gpx"), "track_" + number + ".gpx");
			file.getParentFile().mkdirs();
			try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
				out.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			} catch (java.io.IOException e) {
				throw new IllegalArgumentException("Cannot store gpx_data: " + e.getMessage());
			}
		} else {
			file = findFile(app, name);
		}
		if (file == null) {
			throw new IllegalArgumentException("No track '" + name + "' on the phone; see osmand_list_tracks, or pass gpx_data");
		}
		gpx = SharedUtil.loadGpxFile(file);
		if (gpx.getError() != null) {
			throw new IllegalArgumentException("Cannot read '" + name + "': " + gpx.getError().getMessage());
		}
		JSONObject spec = settingsFromFile(gpx);
		for (java.util.Iterator<String> it = request.keys(); it.hasNext(); ) {
			String key = it.next();
			try {
				spec.put(key, request.get(key));
			} catch (org.json.JSONException ignored) {
			}
		}
		this.spec = spec;
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
		head = spec.optString("head", "progressive".equals(draw) ? "navigation" : "none");
		// the profile of the track (in the request, in the file, or the current one) gives its map icons
		String profileKey = spec.optString("profile", gpx.getExtensionsToRead().get("profile"));
		ApplicationMode profile = Algorithms.isEmpty(profileKey) ? null : ApplicationMode.valueOfStringKey(profileKey, null);
		this.profileKey = profile != null ? profile.getStringKey() : null;
		if (profile == null) {
			profile = app.getSettings().getApplicationMode();
		}
		headIcon = spec.optString("head_icon", "location".equals(head) ? profile.getLocationIcon() : profile.getNavigationIcon());
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
		// spread over a duration, a track would start and stop at full speed: it speeds up and slows down instead
		// (a track played by its own times has its real accelerations)
		double ease = speedX > 0 ? 0 : Math.min(1, durationS / 3);
		easeInS = timing != null ? timing.optDouble("ease_in_s", ease) : ease;
		easeOutS = timing != null ? timing.optDouble("ease_out_s", ease) : ease;
	}

	/** Flyover settings stored in the file: osmand:flyover_<key> on the file or its first track. */
	@NonNull
	private static JSONObject settingsFromFile(@NonNull GpxFile gpx) {
		JSONObject spec = new JSONObject();
		List<Map<String, String>> sources = new ArrayList<>();
		sources.add(gpx.getExtensionsToRead());
		if (!gpx.getTracks().isEmpty()) {
			sources.add(gpx.getTracks().get(0).getExtensionsToRead());
		}
		for (Map<String, String> ext : sources) {
			for (Map.Entry<String, String> e : ext.entrySet()) {
				String key = e.getKey().replaceFirst("^osmand:", "");
				if (key.startsWith("flyover_")) {
					try {
						spec.put(key.substring("flyover_".length()), e.getValue());
					} catch (org.json.JSONException ignored) {
					}
				}
			}
		}
		return spec;
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
		if (spec.has("color")) {
			try {
				// put on the segments in updateDisplay: a colour on the file would win over their fading
				styleColor = Algorithms.parseColor(spec.optString("color"));
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (spec.has("width")) gpx.setWidth(spec.optString("width"));
		if (spec.has("coloring_type")) gpx.setColoringType(spec.optString("coloring_type"));
		if (spec.has("color_palette")) gpx.setGradientColorPalette(spec.optString("color_palette"));
		// as a route in OsmAnd: direction arrows and start / finish, unless the file or the request says otherwise
		// (a saved track keeps the look the user gave it in OsmAnd)
		if (spec.has("show_arrows") || !gpx.isShowArrowsSet()) gpx.setShowArrows(spec.optBoolean("show_arrows", true));
		if (spec.has("show_start_finish") || !gpx.isShowStartFinishSet()) gpx.setShowStartFinish(spec.optBoolean("show_start_finish", true));
		if (spec.has("split_type")) gpx.setSplitType(spec.optString("split_type"));
		if (spec.has("split_interval")) gpx.setSplitInterval(spec.optDouble("split_interval"));
		if (spec.has("visualization_3d")) gpx.set3DVisualizationType(spec.optString("visualization_3d"));
		if (spec.has("wall_coloring")) gpx.set3DWallColoringType(spec.optString("wall_coloring"));
		if (spec.has("line_position_3d")) gpx.set3DLinePositionType(spec.optString("line_position_3d"));
		if (spec.has("vertical_exaggeration")) gpx.setAdditionalExaggeration((float) spec.optDouble("vertical_exaggeration"));
		if (spec.has("elevation_meters")) gpx.setElevationMeters((float) spec.optDouble("elevation_meters"));
		if (spec.has("show_wpts") && !spec.optBoolean("show_wpts")) gpx.getPointsList().clear();
	}

	/** Video time when the track starts moving. */
	double getStartS() {
		return startS;
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
		if (durationS > 0 && progress > 0 && progress < 1 && (easeInS > 0 || easeOutS > 0)) {
			progress = MapFlyoverRecorder.easeTime(t - startS, durationS, easeInS, easeOutS) / durationS;
		}
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

	@Nullable
	private TrkSegment live;
	@Nullable
	private TrkSegment rest;
	@Nullable
	private List<TrkSegment> faded;

	/**
	 * What the map shows of the track at video time t: everything, nothing outside from_s..to_s, faded in and out,
	 * or with draw: progressive the part passed so far (or its last tail_km / tail_s) and the rest hidden, faded or
	 * thin. The same segment objects are kept and only their points and colours change, so the map layer updates
	 * its lines in place (a new segment would be a new symbols provider, loaded too late for the frame).
	 * Runs on the UI thread before the map layers draw; returns true if the layer has to rebuild the track.
	 */
	boolean updateDisplay(double t, double videoS) {
		if (selected == null) {
			return false;
		}
		List<TrkSegment> display = selected.getPointsToDisplay();
		if (original == null) {
			original = new ArrayList<>(display);
			// a colour on the file would win over the segments' colours, which carry the fading
			Integer fileColor = styleColor != null ? styleColor : gpx.getColor(null);
			if (fileColor != null) {
				for (TrkSegment s : original) {
					s.setColor(fileColor);
				}
				gpx.setColor((Integer) null);
			}
		}
		boolean progressive = "progressive".equals(draw);
		if (progressive) {
			// drawn in pieces, but start and finish are the whole track's
			selected.setStartFinishSegments(original);
		}
		if (!progressive && fromS <= 0 && fadeS <= 0 && toS >= videoS) {
			return false;
		}
		float opacity = t < fromS || t > toS ? 0 : getOpacity(t);
		display.clear();
		if (original.isEmpty()) {
			return true;
		}
		// a hidden track keeps its segments, fully transparent: with nothing to show, the layer would decide the
		// track is out of view and not come back to it
		TrkSegment style = original.get(0);
		if (!progressive) {
			if (faded == null) {
				faded = new ArrayList<>();
				for (TrkSegment s : original) {
					faded.add(copy(s, s.getPoints(), 1));
				}
			}
			for (int k = 0; k < faded.size(); k++) {
				setOpacity(faded.get(k), original.get(k), opacity);
				display.add(faded.get(k));
			}
			return true;
		}
		if (live == null) {
			live = copy(style, new ArrayList<>(), 1);
			rest = copy(style, new ArrayList<>(), 1);
			if ("thin".equals(remaining)) {
				rest.setWidth("thin");
			}
		}
		Position p = at(t);
		double from = 0;
		if (tailKm > 0) from = Math.max(from, p.distance - tailKm * 1000);
		if (tailS > 0) from = Math.max(from, at(t - tailS).distance);
		List<WptPt> passed = live.getPoints();
		passed.clear();
		if (from > 0) {
			passed.add(pointAt(from));
		}
		for (int i = 0; i <= p.index; i++) {
			// from the track's first point when nothing is cut: the rest starts there too, and the arrows of both
			// lines, laid out from their start, must fall on the same places
			if (dist[i] > from || (from <= 0 && i == 0)) passed.add(points.get(i));
		}
		WptPt headPoint = pointAt(p.distance);
		passed.add(headPoint);
		setOpacity(live, style, opacity);
		if (passed.size() >= 2) {
			display.add(live);
		}
		// the rest is always there, transparent when hidden: with the passed part alone the layer did not draw it
		List<WptPt> after = rest.getPoints();
		if (after.isEmpty()) {
			// the whole track, under the passed part: the line's arrows are laid out from its start, so a rest that
			// started near the head made its arrows jump with every track point the head passed; with the same start
			// as the passed part they stay at the same places and the passed part covers its own
			after.addAll(points);
		}
		setOpacity(rest, style, "hidden".equals(remaining) ? 0 : "faded".equals(remaining) ? opacity * 0.45f : opacity);
		display.add(rest);
		return true;
	}

	private static void setOpacity(@NonNull TrkSegment target, @NonNull TrkSegment style, float opacity) {
		Integer color = style.getColor(null);
		int c = color != null ? color : DEFAULT_COLOR;
		int alpha = Math.round(((c >>> 24) & 0xFF) * opacity);
		target.setColor((alpha << 24) | (c & 0xFFFFFF));
	}

	/**
	 * The head marker's place, then two points along its direction (from, to). The direction is the road's here,
	 * over about 30 px of the screen each way (at least 20 m), averaged over 1 s of video: the arrow follows the
	 * road, turns smoothly in bends, and seen from far it follows a zigzag's trend instead of spinning with each
	 * hairpin. Null when there is no head.
	 */
	@Nullable
	WptPt[] headPoints(double t, double metersPerPixel) {
		double road = Math.max(HEAD_ROAD_M, HEAD_ROAD_PX * metersPerPixel);
		if ("none".equals(head) || !isVisible(t)) {
			return null;
		}
		Position p = at(t);
		double sin = 0, cos = 0;
		for (int k = -5; k <= 5; k++) {
			double d = at(t + k * 0.1).distance;
			WptPt a = pointAt(Math.max(0, d - road)), b = pointAt(Math.min(getLength(), d + road));
			double w = 6 - Math.abs(k);
			double bearing = Math.toRadians(bearing(a.getLat(), a.getLon(), b.getLat(), b.getLon()));
			sin += w * Math.sin(bearing);
			cos += w * Math.cos(bearing);
		}
		WptPt here = pointAt(p.distance);
		LatLon ahead = MapUtils.rhumbDestinationPoint(here.getLat(), here.getLon(), road,
				Math.toDegrees(Math.atan2(sin, cos)));
		WptPt to = new WptPt(ahead.getLatitude(), ahead.getLongitude());
		to.setEle(here.getEle());
		return new WptPt[] {here, here, to};
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

	/** A segment with the original's colour and width. */
	@NonNull
	private static TrkSegment copy(@NonNull TrkSegment style, @NonNull List<WptPt> points, float opacity) {
		TrkSegment s = new TrkSegment();
		s.getPoints().addAll(points);
		setOpacity(s, style, opacity);
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

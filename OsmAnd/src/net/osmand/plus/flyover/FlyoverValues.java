package net.osmand.plus.flyover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.settings.enums.AltitudeMetrics;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Values a flyover panel shows: {@code gpx[i].<name>} of a track at the current frame or over the whole track,
 * {@code video.*}, {@code camera.*} and {@code data.<name>} passed in the request. Formatted with OsmAnd's own
 * formatters in the profile's units unless a unit is given.
 */
class FlyoverValues {

	enum Kind {DISTANCE, SPEED, PACE, ALTITUDE, DURATION, CLOCK, DATE, PERCENT, NUMBER, TEXT}

	/** A resolved value: the number (NaN when the track does not have it) or text, and how to format it. */
	static class Value {
		final double number;
		final String text;
		final Kind kind;
		final String unit;

		Value(double number, @NonNull Kind kind, @Nullable String unit) {
			this.number = number;
			this.text = null;
			this.kind = kind;
			this.unit = unit;
		}

		Value(@Nullable String text) {
			this.number = Double.NaN;
			this.text = text;
			this.kind = Kind.TEXT;
			this.unit = null;
		}

		boolean isMissing() {
			return kind == Kind.TEXT ? Algorithms.isEmpty(text) : Double.isNaN(number);
		}
	}

	/** State of the frame being drawn. */
	static class Frame {
		final double t;
		final double videoS;
		final double[] camera;

		Frame(double t, double videoS, @NonNull double[] camera) {
			this.t = t;
			this.videoS = videoS;
			this.camera = camera;
		}
	}

	private static final Pattern GPX = Pattern.compile("gpx\\[(\\d+)]\\.(.+)");
	private static final Pattern TEMPLATE = Pattern.compile("\\{([^}]+)\\}");
	private static final String MISSING = "—";
	private static final double MOVING_SPEED = 0.5;

	private final OsmandApplication app;
	private final List<FlyoverTrack> tracks;
	@Nullable
	private final JSONObject data;
	private final Map<String, double[]> series = new HashMap<>();
	// point values (speed, heart rate...) are averaged over this much video time, so digits do not jump
	private double smoothS = 1.5;
	// numbers in widgets change at most this often, as on a bike computer; charts and the camera stay continuous
	private double refreshS = 0.25;

	FlyoverValues(@NonNull OsmandApplication app, @NonNull List<FlyoverTrack> tracks, @Nullable JSONObject data) {
		this.app = app;
		this.tracks = tracks;
		this.data = data;
	}

	void setTiming(double smoothS, double refreshS) {
		this.smoothS = Math.max(0, smoothS);
		this.refreshS = Math.max(0, refreshS);
	}

	/** The frame whose time is held for refresh_s, for the numbers in widgets. */
	@NonNull
	Frame textFrame(@NonNull Frame frame) {
		return refreshS > 0 ? new Frame(Math.floor(frame.t / refreshS) * refreshS, frame.videoS, frame.camera) : frame;
	}

	/** The expression at the current frame. */
	@NonNull
	Value resolve(@NonNull String expr, @NonNull Frame frame) {
		expr = expr.trim();
		Matcher m = GPX.matcher(expr);
		if (m.matches()) {
			FlyoverTrack track = getTrack(Integer.parseInt(m.group(1)));
			return track != null ? trackValue(track, m.group(2), frame) : new Value(null);
		}
		if (expr.startsWith("video.")) {
			String name = expr.substring(6);
			if (name.equals("time")) return new Value(frame.t, Kind.DURATION, null);
			if (name.equals("progress")) return new Value(100 * frame.t / frame.videoS, Kind.PERCENT, null);
		} else if (expr.startsWith("camera.")) {
			String name = expr.substring(7);
			double[] c = frame.camera;
			switch (name) {
				case "lat": return new Value(c[0], Kind.NUMBER, null);
				case "lon": return new Value(c[1], Kind.NUMBER, null);
				case "zoom": return new Value(c[2], Kind.NUMBER, null);
				case "heading": return new Value((360 - c[3] % 360) % 360, Kind.NUMBER, "°");
				case "tilt": return new Value(c[4], Kind.NUMBER, "°");
			}
		} else if (expr.startsWith("data.")) {
			return dataValue(expr.substring(5), frame);
		}
		return new Value(null);
	}

	/** Values of a point expression at every point of its track, for a chart; null if the expression has none. */
	@Nullable
	double[] seriesOf(@NonNull String expr) {
		Matcher m = GPX.matcher(expr.trim());
		if (!m.matches()) {
			return null;
		}
		FlyoverTrack track = getTrack(Integer.parseInt(m.group(1)));
		if (track == null) {
			return null;
		}
		String name = m.group(2);
		double[] values = new double[track.points.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = pointValue(track, name, i);
		}
		return values;
	}

	@Nullable
	FlyoverTrack trackOf(@NonNull String expr) {
		Matcher m = GPX.matcher(expr.trim());
		return m.matches() ? getTrack(Integer.parseInt(m.group(1))) : null;
	}

	@Nullable
	private FlyoverTrack getTrack(int number) {
		return number >= 1 && number <= tracks.size() ? tracks.get(number - 1) : null;
	}

	@NonNull
	private Value trackValue(@NonNull FlyoverTrack track, @NonNull String name, @NonNull Frame frame) {
		FlyoverTrack.Position p = track.at(frame.t);
		double here = isSmoothed(name) ? smoothedAt(track, name, frame.t) : interpolate(track, name, p);
		if (!Double.isNaN(here) || isPointValue(name)) {
			return new Value(here, kindOf(name), unitOf(name));
		}
		switch (name) {
			case "progress_dist": return new Value(p.distance, Kind.DISTANCE, null);
			case "remaining_dist": return new Value(track.getLength() - p.distance, Kind.DISTANCE, null);
			case "progress": return new Value(100 * p.distance / track.getLength(), Kind.PERCENT, null);
			case "elapsed": return new Value(elapsed(track, p), Kind.DURATION, null);
			case "moving_elapsed": {
				double[] moving = cumulative(track, "moving_time");
				return new Value(moving != null && track.timed
						? moving[p.index] + (moving[p.index + 1] - moving[p.index]) * p.fraction : Double.NaN, Kind.DURATION, null);
			}
			case "city": {
				// the nearest city OsmAnd's track list shows, from its track database
				GpxDataItem item = app.getGpxDbHelper().getItem(SharedUtil.kFile(track.file));
				Object city = item != null ? item.getParameter(GpxParameter.NEAREST_CITY_NAME) : null;
				return new Value(city != null ? city.toString() : null);
			}
			case "remaining_time": return new Value(track.timed ? (track.time[track.time.length - 1] - timeAt(track, p)) / 1000.0
					: Double.NaN, Kind.DURATION, null);
			case "next_wpt.name": {
				WptPt w = nextWaypoint(track, p.distance);
				return new Value(w != null ? w.getName() : null);
			}
			case "next_wpt.dist": {
				WptPt w = nextWaypoint(track, p.distance);
				return new Value(w != null ? waypointDistance(track, w) - p.distance : Double.NaN, Kind.DISTANCE, null);
			}
			case "dist": return new Value(track.getLength(), Kind.DISTANCE, null);
			case "duration": return new Value(track.timed ? (track.time[track.time.length - 1] - track.time[0]) / 1000.0
					: Double.NaN, Kind.DURATION, null);
			case "duration_moving": return new Value(last(cumulative(track, "moving_time")), Kind.DURATION, null);
			case "dist_moving": return new Value(last(cumulative(track, "moving_dist")), Kind.DISTANCE, null);
			case "start_time": return new Value(track.timed ? track.time[0] : Double.NaN, Kind.CLOCK, null);
			case "end_time": return new Value(track.timed ? track.time[track.time.length - 1] : Double.NaN, Kind.CLOCK, null);
			case "date": return new Value(track.timed ? track.time[0] : Double.NaN, Kind.DATE, null);
			case "climb": return new Value(last(cumulative(track, "climb")), Kind.ALTITUDE, null);
			case "descent": return new Value(last(cumulative(track, "descent")), Kind.ALTITUDE, null);
			case "points": return new Value(track.points.size(), Kind.NUMBER, null);
			case "wpt_count": return new Value(track.gpx.getPointsList().size(), Kind.NUMBER, null);
			case "segments": return new Value(track.gpx.getTracks().size(), Kind.NUMBER, null);
			case "name": return new Value(trackName(track));
			case "desc": return new Value(track.gpx.getMetadata().getDesc());
			case "author": return new Value(track.gpx.getMetadata().getAuthor() != null ? track.gpx.getMetadata().getAuthor().getName() : null);
			case "activity": return new Value(track.gpx.getMetadata().getExtensionsToRead().get("activity"));
			case "file": return new Value(track.file.getName());
		}
		if (name.endsWith("_so_far")) {
			String base = name.substring(0, name.length() - "_so_far".length());
			double[] cum = cumulative(track, base);
			if (cum != null) {
				double v = cum[p.index] + (cum[p.index + 1] - cum[p.index]) * p.fraction;
				return new Value(v, base.contains("speed") ? Kind.SPEED : Kind.ALTITUDE, null);
			}
		}
		String[] stat = name.split("_");
		if (stat.length == 2 && (stat[1].equals("min") || stat[1].equals("max") || stat[1].equals("avg"))) {
			return new Value(statistic(track, stat[0], stat[1]), kindOf(stat[0]), unitOf(stat[0]));
		}
		if (name.startsWith("meta.")) {
			return new Value(track.gpx.getExtensionsToRead().get(name.substring(5)));
		}
		return new Value(null);
	}

	private static boolean isPointValue(@NonNull String name) {
		switch (name) {
			case "speed": case "pace": case "ele": case "slope": case "hr": case "cadence": case "power":
			case "sensor_speed": case "temp_air": case "temp_water": case "bike_distance": case "time":
			case "lat": case "lon":
				return true;
		}
		return name.startsWith("ext.") || name.startsWith("obd.");
	}

	private static boolean isMotion(@NonNull String name) {
		return name.equals("speed") || name.equals("sensor_speed") || name.equals("cadence") || name.equals("power");
	}

	private static boolean isSmoothed(@NonNull String name) {
		return isPointValue(name) && !name.equals("lat") && !name.equals("lon") && !name.equals("time");
	}

	/** A point value averaged over smooth_s of video time around t, with triangular weights. */
	private double smoothedAt(@NonNull FlyoverTrack track, @NonNull String name, double t) {
		if (smoothS <= 0) {
			FlyoverTrack.Position p = track.at(t);
			return isMotion(name) && (!p.started || p.finished) ? 0 : interpolate(track, name, p);
		}
		double sum = 0, weights = 0;
		for (int k = -4; k <= 4; k++) {
			FlyoverTrack.Position p = track.at(t + k * smoothS / 8);
			// waiting at the start or arrived: not moving
			double v = isMotion(name) && (!p.started || p.finished) ? 0 : interpolate(track, name, p);
			if (!Double.isNaN(v)) {
				double w = 5 - Math.abs(k);
				sum += v * w;
				weights += w;
			}
		}
		return weights > 0 ? sum / weights : Double.NaN;
	}

	private double interpolate(@NonNull FlyoverTrack track, @NonNull String name, @NonNull FlyoverTrack.Position p) {
		if (!isPointValue(name)) {
			return Double.NaN;
		}
		if (name.equals("lat")) return p.lat;
		if (name.equals("lon")) return p.lon;
		double a = pointValue(track, name, p.index);
		double b = pointValue(track, name, p.index + 1);
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		return a + (b - a) * p.fraction;
	}

	/** A point value at point i: sensors from the point's tags, speed and slope from its neighbours. */
	private double pointValue(@NonNull FlyoverTrack track, @NonNull String name, int i) {
		WptPt pt = track.points.get(i);
		switch (name) {
			case "ele": return Double.isNaN(pt.getEle()) ? Double.NaN : pt.getEle();
			case "lat": return pt.getLat();
			case "lon": return pt.getLon();
			case "time": return pt.getTime() > 0 ? pt.getTime() : Double.NaN;
			case "speed": return smoothed(track, "speed")[i];
			case "pace": {
				double s = smoothed(track, "speed")[i];
				return s > MOVING_SPEED ? 1000 / s : Double.NaN;
			}
			case "slope": return smoothed(track, "slope")[i];
			case "hr": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_HR);
			case "cadence": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_CADENCE);
			case "power": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_POWER);
			case "sensor_speed": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_SENSOR_SPEED);
			case "temp_air": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_TEMP_AIR);
			case "temp_water": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_TEMP_WATER);
			case "bike_distance": return FlyoverTrack.attribute(pt, FlyoverTrack.TAG_BIKE_DISTANCE);
		}
		if (name.startsWith("ext.")) return FlyoverTrack.attribute(pt, name.substring(4));
		if (name.startsWith("obd.")) return FlyoverTrack.attribute(pt, name.substring(4));
		return Double.NaN;
	}

	/** Speed (m/s) and slope (%) per point, from the point's own speed or over about 50 m around it. */
	@NonNull
	private double[] smoothed(@NonNull FlyoverTrack track, @NonNull String name) {
		String key = track.number + ":" + name;
		double[] cached = series.get(key);
		if (cached != null) {
			return cached;
		}
		int n = track.points.size();
		double[] out = new double[n];
		for (int i = 0; i < n; i++) {
			int a = i, b = i;
			while (a > 0 && track.dist[i] - track.dist[a] < 25) a--;
			while (b < n - 1 && track.dist[b] - track.dist[i] < 25) b++;
			double d = track.dist[b] - track.dist[a];
			if (name.equals("speed")) {
				float own = track.points.get(i).getSpeed();
				double dt = (track.time[b] - track.time[a]) / 1000.0;
				out[i] = own > 0 ? own : track.timed && dt > 0 ? d / dt : Double.NaN;
			} else {
				double ea = track.points.get(a).getEle(), eb = track.points.get(b).getEle();
				out[i] = d > 5 && !Double.isNaN(ea) && !Double.isNaN(eb) ? 100 * (eb - ea) / d : Double.NaN;
			}
		}
		series.put(key, out);
		return out;
	}

	/** Running totals per point: climb, descent, moving_time, moving_dist, max_speed, avg_speed. */
	@Nullable
	private double[] cumulative(@NonNull FlyoverTrack track, @NonNull String name) {
		String key = track.number + ":cum:" + name;
		double[] cached = series.get(key);
		if (cached != null) {
			return cached;
		}
		int n = track.points.size();
		double[] out = new double[n];
		double[] speed = smoothed(track, "speed");
		double movingTime = 0, movingDist = 0;
		for (int i = 1; i < n; i++) {
			double prev = out[i - 1];
			double de = track.points.get(i).getEle() - track.points.get(i - 1).getEle();
			double dt = track.timed ? (track.time[i] - track.time[i - 1]) / 1000.0 : 0;
			double dd = track.dist[i] - track.dist[i - 1];
			boolean moving = dt > 0 && dd / dt > MOVING_SPEED;
			if (moving) {
				movingTime += dt;
				movingDist += dd;
			}
			switch (name) {
				case "climb": out[i] = prev + (de > 0 ? de : 0); break;
				case "descent": out[i] = prev + (de < 0 ? -de : 0); break;
				case "moving_time": out[i] = movingTime; break;
				case "moving_dist": out[i] = movingDist; break;
				case "max_speed": out[i] = Double.isNaN(speed[i]) ? prev : Math.max(prev, speed[i]); break;
				case "avg_speed": out[i] = movingTime > 0 ? movingDist / movingTime : Double.NaN; break;
				default: return null;
			}
		}
		series.put(key, out);
		return out;
	}

	private double statistic(@NonNull FlyoverTrack track, @NonNull String name, @NonNull String stat) {
		if (name.equals("speed") && stat.equals("avg")) {
			return last(cumulative(track, "avg_speed"));
		}
		double sum = 0, min = Double.NaN, max = Double.NaN;
		int count = 0;
		for (int i = 0; i < track.points.size(); i++) {
			double v = pointValue(track, name, i);
			if (Double.isNaN(v)) continue;
			sum += v;
			count++;
			min = Double.isNaN(min) ? v : Math.min(min, v);
			max = Double.isNaN(max) ? v : Math.max(max, v);
		}
		switch (stat) {
			case "min": return min;
			case "max": return max;
			default: return count > 0 ? sum / count : Double.NaN;
		}
	}

	private static double last(@Nullable double[] values) {
		return values != null && values.length > 0 ? values[values.length - 1] : Double.NaN;
	}

	private static double timeAt(@NonNull FlyoverTrack track, @NonNull FlyoverTrack.Position p) {
		return track.time[p.index] + (track.time[p.index + 1] - track.time[p.index]) * p.fraction;
	}

	private static double elapsed(@NonNull FlyoverTrack track, @NonNull FlyoverTrack.Position p) {
		return track.timed ? (timeAt(track, p) - track.time[0]) / 1000.0 : Double.NaN;
	}

	@Nullable
	private WptPt nextWaypoint(@NonNull FlyoverTrack track, double distance) {
		WptPt next = null;
		double best = Double.MAX_VALUE;
		for (WptPt w : track.gpx.getPointsList()) {
			double d = waypointDistance(track, w);
			if (d >= distance && d < best) {
				best = d;
				next = w;
			}
		}
		return next;
	}

	/** Distance along the track to the track point nearest to the waypoint. */
	private double waypointDistance(@NonNull FlyoverTrack track, @NonNull WptPt w) {
		int nearest = 0;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < track.points.size(); i++) {
			WptPt p = track.points.get(i);
			double d = MapUtils.getDistance(p.getLat(), p.getLon(), w.getLat(), w.getLon());
			if (d < best) {
				best = d;
				nearest = i;
			}
		}
		return track.dist[nearest];
	}

	@NonNull
	private static String trackName(@NonNull FlyoverTrack track) {
		String name = track.gpx.getMetadata().getName();
		if (Algorithms.isEmpty(name) && !track.gpx.getTracks().isEmpty()) {
			name = track.gpx.getTracks().get(0).getName();
		}
		return Algorithms.isEmpty(name) ? track.file.getName().replaceFirst("\\.gpx$", "") : name;
	}

	@NonNull
	private Value dataValue(@NonNull String name, @NonNull Frame frame) {
		Object entry = data != null ? data.opt(name) : null;
		if (entry instanceof Number) {
			return new Value(((Number) entry).doubleValue(), Kind.NUMBER, null);
		}
		if (entry instanceof String) {
			return new Value((String) entry);
		}
		if (!(entry instanceof JSONObject)) {
			return new Value(null);
		}
		JSONObject d = (JSONObject) entry;
		JSONArray points = d.optJSONArray("points");
		if (points == null || points.length() == 0) {
			return new Value(null);
		}
		double x = frame.t;
		if ("distance".equals(d.optString("by"))) {
			FlyoverTrack track = getTrack(d.optInt("gpx", 1));
			x = track != null ? track.at(frame.t).distance : Double.NaN;
		}
		double prevX = Double.NaN, prevV = Double.NaN;
		for (int i = 0; i < points.length(); i++) {
			JSONArray xy = points.optJSONArray(i);
			if (xy == null || xy.length() < 2) continue;
			double px = xy.optDouble(0), pv = xy.optDouble(1);
			if (px >= x) {
				double v = Double.isNaN(prevX) || px == prevX ? pv : prevV + (pv - prevV) * (x - prevX) / (px - prevX);
				return new Value(v, Kind.NUMBER, d.optString("unit", null));
			}
			prevX = px;
			prevV = pv;
		}
		return new Value(prevV, Kind.NUMBER, d.optString("unit", null));
	}

	@NonNull
	private static Kind kindOf(@NonNull String name) {
		switch (name) {
			case "speed": case "sensor_speed": return Kind.SPEED;
			case "pace": return Kind.PACE;
			case "ele": return Kind.ALTITUDE;
			case "bike_distance": return Kind.DISTANCE;
			case "time": return Kind.CLOCK;
			default: return Kind.NUMBER;
		}
	}

	@Nullable
	private static String unitOf(@NonNull String name) {
		switch (name) {
			case "hr": return "bpm";
			case "cadence": return "rpm";
			case "power": return "W";
			case "slope": return "%";
			case "temp_air": case "temp_water": case "temp": return "°C";
			default: return null;
		}
	}

	// formatting

	/** Value and unit as OsmAnd widgets show them; {@code unit} "auto" or empty uses the profile's units. */
	@NonNull
	String[] format(@NonNull Value v, @Nullable String unit, int digits) {
		if (v.isMissing()) {
			return new String[] {MISSING, ""};
		}
		if (v.kind == Kind.TEXT) {
			return new String[] {v.text, ""};
		}
		boolean auto = Algorithms.isEmpty(unit) || "auto".equals(unit);
		double x = v.number;
		if (!auto) {
			return formatIn(x, v.kind, unit, digits);
		}
		switch (v.kind) {
			case DISTANCE: {
				FormattedValue f = OsmAndFormatter.getFormattedDistanceValue((float) x, app);
				return new String[] {f.value, f.unit};
			}
			case SPEED: {
				FormattedValue f = OsmAndFormatter.getFormattedSpeedValue((float) x, app);
				return new String[] {f.value, f.unit};
			}
			case ALTITUDE: {
				AltitudeMetrics metrics = app.getSettings().ALTITUDE_METRIC.get();
				FormattedValue f = OsmAndFormatter.getFormattedAltitudeValue(x, app, metrics);
				return new String[] {f.value, f.unit};
			}
			case PACE: return formatIn(x, Kind.PACE, "min/km", digits);
			case DURATION: return new String[] {duration((long) x), ""};
			case CLOCK: return new String[] {new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date((long) x)), ""};
			case DATE: return new String[] {new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date((long) x)), ""};
			case PERCENT: return new String[] {number(x, digits < 0 ? 0 : digits), "%"};
			default: {
				// counts per minute and watts are whole numbers in OsmAnd's widgets
				boolean whole = "bpm".equals(v.unit) || "rpm".equals(v.unit) || "W".equals(v.unit);
				return new String[] {number(x, digits < 0 && whole ? 0 : digits), v.unit != null ? v.unit : ""};
			}
		}
	}

	@NonNull
	private static String[] formatIn(double x, @NonNull Kind kind, @NonNull String unit, int digits) {
		switch (unit) {
			case "km": return new String[] {number(x / 1000, digits < 0 ? 1 : digits), "km"};
			case "mi": return new String[] {number(x / 1609.344, digits < 0 ? 1 : digits), "mi"};
			case "m": return new String[] {number(x, digits < 0 ? 0 : digits), "m"};
			case "ft": return new String[] {number(x * 3.28084, digits < 0 ? 0 : digits), "ft"};
			case "km/h": return new String[] {number(x * 3.6, digits < 0 ? 0 : digits), "km/h"};
			case "mph": return new String[] {number(x * 2.236936, digits < 0 ? 0 : digits), "mph"};
			case "min/km": case "min/mi": {
				double secPerKm = kind == Kind.PACE ? x : x > MOVING_SPEED ? 1000 / x : Double.NaN;
				double sec = unit.equals("min/mi") ? secPerKm * 1.609344 : secPerKm;
				return new String[] {Double.isNaN(sec) ? MISSING : (long) sec / 60 + ":" + String.format(Locale.US, "%02d", (long) sec % 60), unit};
			}
			case "h:mm:ss": return new String[] {duration((long) x), ""};
			default: return new String[] {number(x, digits), unit};
		}
	}

	@NonNull
	private static String number(double x, int digits) {
		int d = digits < 0 ? (Math.abs(x) >= 100 ? 0 : 1) : digits;
		return String.format(Locale.getDefault(), "%." + d + "f", x);
	}

	@NonNull
	private static String duration(long seconds) {
		long h = seconds / 3600, m = seconds % 3600 / 60, s = seconds % 60;
		return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%d:%02d", m, s);
	}

	/** A text template: every {expr} replaced with its value and unit. */
	@NonNull
	String template(@NonNull String template, @Nullable String unit, int digits, @NonNull Frame frame) {
		Matcher m = TEMPLATE.matcher(template);
		StringBuffer out = new StringBuffer();
		while (m.find()) {
			String[] f = format(resolve(m.group(1), frame), unit, digits);
			m.appendReplacement(out, Matcher.quoteReplacement(f[1].isEmpty() ? f[0] : f[0] + " " + f[1]));
		}
		m.appendTail(out);
		return out.toString();
	}
}

package net.osmand.plus.flyover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.shared.gpx.primitives.WptPt;

import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Camera of a drone: a track or key frame poses give where the drone is (lat, lon, height above the sea) and where
 * its camera looks (yaw, pitch). The engine's camera turns around a point at a distance set by the zoom; the recorder
 * has the engine take that point at sea level, not on the relief, so the drone's view becomes the point where the
 * middle of the frame meets the sea level. The engine turns a zoom into a distance by whole levels, so the recorder
 * keeps a level and gets the camera to the drone's height with the scale of the frame, checked after each frame.
 */
class FlyoverDroneCamera {

	private static final String[] YAW_TAGS = {"yaw", "camera_yaw", "gimbal_yaw", "heading"};
	private static final String[] PITCH_TAGS = {"pitch", "camera_pitch", "gimbal_pitch"};
	private static final String[] FOV_TAGS = {"fov", "camera_fov"};
	private static final double MIN_TILT = 10;
	private static final double MAX_TILT = 90;
	private static final double METERS_PER_DEGREE = 111_320;
	/** Default field of view of the engine: 16.5 degrees from the middle of the frame to its top. */
	static final float ENGINE_FOV = 33f;
	// D [m] * 2^zoom / cos(lat) for the 1280x720 frame and the engine's field of view; measured on the first frame
	private static final double ZOOM_CONSTANT_GUESS = 3.5e8;
	// heights are kept on a grid of about 11 m: the view ray of the next frame asks for nearly the same places
	private static final double HEIGHT_GRID = 1e-4;
	private static final double LEARN_GAIN = 0.4;
	/** The scale of the frame stays within 2^-band..2^band; beyond it the zoom level changes. */
	static final double SCALE_BAND = 0.75;

	/** The drone's track, or null when the poses are given as key frames. */
	@Nullable
	final FlyoverTrack track;
	// key frame poses: time, lat, lon, height, heading (unwrapped), pitch, fov, height above the ground (0/1)
	private double[] poseT, poseLat, poseLon, poseAlt, poseYaw, posePitch, poseFov;
	private boolean[] poseAgl;
	private final String altitude;
	private final double minAgl;
	private final double defaultPitch;
	private final double defaultFov;
	private final double smoothS;
	private final Map<Long, Float> heights = new HashMap<>();
	private boolean noTerrain;
	private double takeoffGround = Double.NaN;

	private double zoomConstant = Double.NaN;
	/** The constant as measured at each zoom level: the engine converts the zoom by whole levels. */
	private final Map<Double, Double> levelConstants = new HashMap<>();
	private double fovForConstant = Double.NaN;

	// what the last camera was made of, to check the engine's camera against it
	private double expectedAlt = Double.NaN;
	private double lastTilt;
	private double lastLat;
	// what the recorder rendered: the zoom level, the map magnifier and the scale of the frame
	private double lastLevel = Double.NaN;
	private double lastDensity = 1;
	private double lastScale = 1;
	private double[] lastCamera;
	private int corrected;
	private double predictedScale;
	private double scaleMiss;
	private double scaleMissLevel = Double.NaN;
	// the previous try of this frame: time, scale and the height it gave
	private double correctionT = Double.NaN;
	private double correctionScale = Double.NaN;
	private double correctionAlt;
	private double lastT = Double.NaN;
	// the best try of this frame
	private double bestT = Double.NaN;
	private double bestError;
	private double[] bestCamera;

	private int frames;
	private int tiltClamped;
	private int lowered;
	private double maxHeightError;
	private double maxPitchShift;

	FlyoverDroneCamera(@Nullable FlyoverTrack track, @NonNull JSONObject camera) {
		this.track = track;
		altitude = camera.optString("altitude", "msl");
		minAgl = camera.optDouble("min_agl_m", 5);
		defaultPitch = camera.optDouble("pitch", -30);
		defaultFov = camera.optDouble("fov", ENGINE_FOV);
		// a track is a GPS log and is averaged; key frame poses are a smooth curve already
		smoothS = camera.optDouble("smooth_s", track != null ? 1 : 0);
		if (track == null) {
			readPoses(camera.optJSONArray("poses"), camera);
		}
	}

	/** Where the drone is and where its camera looks. */
	static class Pose {
		double lat;
		double lon;
		double alt;
		double yaw;
		double pitch;
		double fov;
	}

	/** The drone's pose at video time t, averaged over smooth_s with triangular weights. */
	@NonNull
	Pose pose(double t) {
		int half = smoothS > 0 ? Math.max(3, (int) Math.round(smoothS * 15)) : 0;
		Pose p = new Pose();
		double sin = 0, cos = 0, weights = 0;
		for (int k = -half; k <= half; k++) {
			double tk = half > 0 ? t + k * smoothS / (2 * half) : t;
			double w = half + 1 - Math.abs(k);
			Pose s = sample(tk);
			p.lat += w * s.lat;
			p.lon += w * s.lon;
			p.alt += w * s.alt;
			p.pitch += w * s.pitch;
			p.fov += w * s.fov;
			sin += w * Math.sin(Math.toRadians(s.yaw));
			cos += w * Math.cos(Math.toRadians(s.yaw));
			weights += w;
		}
		p.lat /= weights;
		p.lon /= weights;
		p.alt /= weights;
		p.pitch /= weights;
		p.fov /= weights;
		p.yaw = Math.toDegrees(Math.atan2(sin, cos));
		// never under the relief: it is a ~30 m grid, and a drone flying low would dive into a slope
		double ground = ground(p.lat, p.lon);
		if (p.alt < ground + minAgl) {
			p.alt = ground + minAgl;
			lowered++;
		}
		return p;
	}

	/** "poses": [{"time_s", "lat", "lon", "alt_m", "alt_ref": "sea" | "ground", "heading", "pitch", "fov"}]. */
	private void readPoses(@Nullable JSONArray poses, @NonNull JSONObject camera) {
		int n = poses != null ? poses.length() : 0;
		if (n == 0) {
			throw new IllegalArgumentException("camera.poses needs at least one pose");
		}
		poseT = new double[n];
		poseLat = new double[n];
		poseLon = new double[n];
		poseAlt = new double[n];
		poseYaw = new double[n];
		posePitch = new double[n];
		poseFov = new double[n];
		poseAgl = new boolean[n];
		for (int k = 0; k < n; k++) {
			JSONObject p = poses.optJSONObject(k);
			if (p == null || !p.has("lat") || !p.has("lon") || !p.has("alt_m")) {
				throw new IllegalArgumentException("camera.poses[" + k + "] needs lat, lon and alt_m");
			}
			poseT[k] = p.optDouble("time_s", k);
			if (k > 0 && poseT[k] <= poseT[k - 1]) {
				throw new IllegalArgumentException("camera.poses times must go up");
			}
			poseLat[k] = p.optDouble("lat");
			poseLon[k] = p.optDouble("lon");
			poseAlt[k] = p.optDouble("alt_m");
			String ref = p.optString("alt_ref", camera.optString("alt_ref", "sea"));
			poseAgl[k] = "ground".equals(ref);
			double heading = p.optDouble("heading", k > 0 ? poseYaw[k - 1] : 0);
			// unwrapped, so that 170 -> -170 turns 20 degrees, not 340
			poseYaw[k] = k == 0 ? heading : poseYaw[k - 1] + MapUtils.unifyRotationDiff((float) poseYaw[k - 1], (float) heading);
			posePitch[k] = p.optDouble("pitch", k > 0 ? posePitch[k - 1] : defaultPitch);
			poseFov[k] = p.optDouble("fov", k > 0 ? poseFov[k - 1] : defaultFov);
		}
	}

	@NonNull
	private Pose sample(double t) {
		if (track == null) {
			Pose s = new Pose();
			s.lat = MapFlyoverRecorder.hermite(poseT, poseLat, t);
			s.lon = MapFlyoverRecorder.hermite(poseT, poseLon, t);
			s.alt = MapFlyoverRecorder.hermite(poseT, poseAlt, t);
			int i = 0;
			while (i < poseT.length - 1 && t > poseT[i + 1]) {
				i++;
			}
			if (poseAgl[i]) {
				s.alt += ground(s.lat, s.lon);
			}
			s.yaw = MapFlyoverRecorder.hermite(poseT, poseYaw, t);
			s.pitch = MapFlyoverRecorder.hermite(poseT, posePitch, t);
			s.fov = MapFlyoverRecorder.hermite(poseT, poseFov, t);
			return s;
		}
		FlyoverTrack.Position at = track.at(t);
		WptPt a = track.points.get(at.index);
		WptPt b = track.points.get(at.index + 1);
		Pose s = new Pose();
		s.lat = at.lat;
		s.lon = at.lon;
		double ea = a.getEle(), eb = b.getEle();
		double ele = Double.isNaN(ea) ? eb : Double.isNaN(eb) ? ea : ea + (eb - ea) * at.fraction;
		if (Double.isNaN(ele)) {
			ele = 0;
		}
		if ("takeoff".equals(altitude)) {
			if (Double.isNaN(takeoffGround)) {
				WptPt first = track.points.get(0);
				takeoffGround = ground(first.getLat(), first.getLon());
			}
			ele += takeoffGround;
		} else if ("agl".equals(altitude)) {
			ele += ground(s.lat, s.lon);
		}
		s.alt = ele;
		double yaw = attribute(at, YAW_TAGS);
		s.yaw = Double.isNaN(yaw) ? track.bearing(t, 1, 3) : yaw;
		double pitch = attribute(at, PITCH_TAGS);
		s.pitch = Double.isNaN(pitch) ? defaultPitch : pitch;
		double fov = attribute(at, FOV_TAGS);
		s.fov = Double.isNaN(fov) ? defaultFov : fov;
		return s;
	}

	private double attribute(@NonNull FlyoverTrack.Position at, @NonNull String[] tags) {
		for (String tag : tags) {
			double v = track.attribute(at, tag);
			if (!Double.isNaN(v)) {
				return v;
			}
		}
		return Double.NaN;
	}

	/** Camera for video time t, as {lat, lon, zoom, rotation, tilt, target x, target y, scale of the frame, fov}. */
	@NonNull
	double[] camera(double t) {
		lastT = t;
		frames++;
		return cameraLookingAt(pose(t), true);
	}

	/** Camera for time t, not checked against the engine: for estimates over the whole video. */
	@NonNull
	double[] roughCamera(double t) {
		return cameraLookingAt(pose(t), false);
	}

	private static double tilt(@NonNull Pose c) {
		return Math.max(MIN_TILT, Math.min(MAX_TILT, -c.pitch));
	}

	@NonNull
	private double[] cameraLookingAt(@NonNull Pose c, boolean record) {
		double tilt = tilt(c);
		// the middle of the frame down to the sea level
		double distance = Math.max(1, c.alt) / Math.sin(Math.toRadians(tilt));
		double horizontal = distance * Math.cos(Math.toRadians(tilt)), yaw = Math.toRadians(c.yaw);
		double gLat = c.lat + horizontal * Math.cos(yaw) / METERS_PER_DEGREE;
		double gLon = c.lon + horizontal * Math.sin(yaw) / (METERS_PER_DEGREE * Math.cos(Math.toRadians(c.lat)));
		if (fovForConstant != c.fov) {
			// a wider view puts the camera closer for the same zoom
			zoomConstant = Double.isNaN(zoomConstant) ? ZOOM_CONSTANT_GUESS * Math.tan(Math.toRadians(ENGINE_FOV / 2))
					/ Math.tan(Math.toRadians(c.fov / 2)) : zoomConstant * Math.tan(Math.toRadians(fovForConstant / 2))
					/ Math.tan(Math.toRadians(c.fov / 2));
			fovForConstant = c.fov;
		}
		double zoom = Math.log(zoomConstant * Math.cos(Math.toRadians(gLat)) / distance) / Math.log(2);
		// OsmAnd turns the map by minus the heading; the recorder sets the level and the scale of the frame (fit)
		double[] camera = {gLat, gLon, zoom, -c.yaw, tilt, 0.5, 0.5, 1, c.fov};
		if (record) {
			lastCamera = camera;
			if (tilt != -c.pitch) {
				tiltClamped++;
			}
			maxPitchShift = Math.max(maxPitchShift, Math.abs(tilt + c.pitch));
			expectedAlt = c.alt;
			lastTilt = tilt;
			lastLat = gLat;
		}
		return camera;
	}

	/**
	 * Puts the camera at the drone's height for a zoom level chosen by the recorder: the engine takes the distance
	 * of the level and divides it by the magnifier and the scale of the frame, so the scale is what makes the height
	 * right: height = distance(level) * sin(tilt) / (magnifier * scale).
	 * {@code predict}: a new frame; the formula misses the engine by a share that changes slowly, so the last frame's
	 * miss is taken into the prediction.
	 */
	void fit(@NonNull double[] camera, double level, double density, boolean predict) {
		double sin = Math.sin(Math.toRadians(camera[4]));
		Double known = levelConstants.get(level);
		double levelDistance = (known != null ? known : zoomConstant) * Math.cos(Math.toRadians(camera[0])) / Math.pow(2, level);
		double scale = levelDistance * sin / (density * expectedAlt);
		if (predict) {
			predictedScale = scale;
			// the miss of another level says nothing about this one
			if (scaleMissLevel == level) {
				scale *= Math.exp(scaleMiss);
			}
		}
		camera[2] = level;
		camera[7] = scale;
		lastLevel = level;
		lastDensity = density;
		lastScale = scale;
		lastCamera = camera;
	}

	/**
	 * After a frame: when the engine's camera is not at the drone's height, the same frame again with another scale,
	 * or null.
	 * {@code last}: no more tries, only the best one of this frame again when the last one was worse.
	 */
	@Nullable
	double[] correction(@NonNull MapRendererView renderer, boolean last) {
		if (lastCamera == null || Double.isNaN(lastLevel)) {
			return null;
		}
		double cameraAlt = renderer.getCameraHeightInMeters();
		double error = Math.abs(cameraAlt - expectedAlt);
		if (bestT != lastT) {
			bestT = lastT;
			bestError = Double.MAX_VALUE;
		}
		if (error < bestError) {
			bestError = error;
			bestCamera = lastCamera.clone();
			bestCamera[7] = lastScale;
		}
		// a thousandth of the height over the ground is under a pixel; each try is a whole render
		if (error <= Math.max(1.5, expectedAlt * 0.001)) {
			correctionScale = Double.NaN;
			return null;
		}
		if (last) {
			if (bestCamera[7] == lastScale) {
				return null;
			}
			lastCamera = bestCamera.clone();
			lastScale = bestCamera[7];
			return bestCamera.clone();
		}
		double[] camera = lastCamera.clone();
		double triedScale = lastScale;
		if (Double.isNaN(correctionScale) || correctionT != lastT || correctionScale == triedScale) {
			// first try: the formula without the last frame's miss
			fit(camera, lastLevel, lastDensity, false);
		} else {
			// then the secant through the last two tries: the height goes with 1 / scale
			double u1 = 1 / correctionScale, u2 = 1 / triedScale;
			double slope = (cameraAlt - correctionAlt) / (u2 - u1);
			double u = slope != 0 ? u2 + (expectedAlt - cameraAlt) / slope : u2;
			double scale = u > 0 ? 1 / u : triedScale;
			camera[7] = scale;
			lastCamera = camera;
		}
		// the level stays within a frame: tries at two levels would not line up for the secant; far scales are unsteady
		camera[7] = Math.max(0.25, Math.min(4, camera[7]));
		correctionT = lastT;
		correctionScale = triedScale;
		correctionAlt = cameraAlt;
		lastScale = camera[7];
		corrected++;
		return camera;
	}

	/**
	 * Checks the engine's camera after a frame: its height above the sea against the drone's, and learns from it how
	 * far the camera is at a zoom (it depends on the frame size, the magnifier and the view). The engine's answer can
	 * lag a frame behind, so each frame moves the constant only part of the way.
	 */
	void measure(@NonNull MapRendererView renderer, boolean learn) {
		if (Double.isNaN(expectedAlt) || Double.isNaN(lastLevel)) {
			return;
		}
		double cameraAlt = renderer.getCameraHeightInMeters();
		double sin = Math.sin(Math.toRadians(lastTilt));
		if (!learn) {
			maxHeightError = Math.max(maxHeightError, Math.abs(cameraAlt - expectedAlt));
			if (predictedScale > 0 && Math.abs(cameraAlt - expectedAlt) < expectedAlt * 0.01) {
				scaleMiss = Math.log(lastScale / predictedScale);
				scaleMissLevel = lastLevel;
			}
		}
		double levelDistance = cameraAlt * lastDensity * lastScale / sin;
		if (levelDistance > 0 && sin > 0.05) {
			double measured = levelDistance * Math.pow(2, lastLevel) / Math.cos(Math.toRadians(lastLat));
			Double known = levelConstants.get(lastLevel);
			// a new level takes its first measurement as it is, a known one moves only part of the way
			double value = known == null ? measured : Math.exp(Math.log(known) + (Math.log(measured) - Math.log(known)) * LEARN_GAIN);
			levelConstants.put(lastLevel, value);
			zoomConstant = value;
		}
	}

	private double ground(double lat, double lon) {
		List<LatLon> one = new ArrayList<>(1);
		one.add(new LatLon(lat, lon));
		double h = grounds(one)[0];
		return Double.isNaN(h) ? 0 : h;
	}

	/** Heights of the relief from OsmAnd's terrain files (NaN where they have none), one call for all places not asked before. */
	@NonNull
	private double[] grounds(@NonNull List<LatLon> points) {
		double[] result = new double[points.size()];
		List<LatLon> missing = new ArrayList<>();
		List<Long> missingKeys = new ArrayList<>();
		for (LatLon p : points) {
			long key = key(p);
			if (!heights.containsKey(key) && !missingKeys.contains(key)) {
				missingKeys.add(key);
				missing.add(new LatLon(Math.round(p.getLatitude() / HEIGHT_GRID) * HEIGHT_GRID,
						Math.round(p.getLongitude() / HEIGHT_GRID) * HEIGHT_GRID));
			}
		}
		if (!missing.isEmpty()) {
			MapRendererContext context = NativeCoreContext.getMapRendererContext();
			float[] found = context != null ? context.calculateHeights(missing) : null;
			for (int i = 0; i < missing.size(); i++) {
				float h = found != null && i < found.length ? found[i] : Float.NaN;
				if (Float.isNaN(h) || h < -1000 || h > 9000) {
					noTerrain = true;
					h = Float.NaN;
				}
				heights.put(missingKeys.get(i), h);
			}
		}
		for (int i = 0; i < points.size(); i++) {
			Float h = heights.get(key(points.get(i)));
			result[i] = h != null ? h : Double.NaN;
		}
		return result;
	}

	private static long key(@NonNull LatLon p) {
		long x = Math.round(p.getLongitude() / HEIGHT_GRID);
		long y = Math.round(p.getLatitude() / HEIGHT_GRID);
		return (y << 32) ^ (x & 0xffffffffL);
	}

	/** What the recorder says about the drone camera in its warnings, or null when there is nothing to say. */
	@Nullable
	String report() {
		if (frames == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder(String.format(Locale.US,
				"drone camera: height off by up to %.1f m, looks %.1f° off the drone's pitch at most",
				maxHeightError, maxPitchShift));
		if (corrected > 0) {
			sb.append(String.format(Locale.US, "; %d frames rendered again to put the camera at the drone's height", corrected));
		}
		if (tiltClamped > 0) {
			sb.append(String.format(Locale.US, "; %d frames looked flatter than %.0f° down and were tilted to it", tiltClamped, MIN_TILT));
		}
		if (lowered > 0) {
			sb.append(String.format(Locale.US, "; poses under %.0f m above the relief were lifted", minAgl));
		}
		if (noTerrain) {
			sb.append("; no terrain data for some places (download the Terrain maps): taken as sea level");
		}
		return sb.toString();
	}
}

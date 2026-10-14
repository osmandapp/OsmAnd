package net.osmand.gpx.flyover;

import net.osmand.data.LatLon;

/**
 * Camera keyframes of a track flyover: an overview of the whole track, a flight along it,
 * a return to the overview and a final hold for the summary card.
 * Rotation follows OsmAnd map rotation: {@code rotation = -heading}, so the direction of travel points up.
 */
public class FlyoverCameraPath {

	public enum Phase {
		INTRO, FLIGHT, OUTRO, SUMMARY
	}

	public static final float TOP_DOWN_TILT = 90f;
	public static final float DEFAULT_FLIGHT_TILT = 35f;

	public static final double INTRO_SECONDS = 2.0;
	public static final double OUTRO_SECONDS = 2.0;
	public static final double SUMMARY_SECONDS = 3.5;
	public static final double MIN_FLIGHT_SECONDS = 12;
	/** The whole video, intro and summary included, stays within 40 seconds. */
	public static final double MAX_VIDEO_SECONDS = 40;
	public static final double MAX_FLIGHT_SECONDS = MAX_VIDEO_SECONDS - INTRO_SECONDS - OUTRO_SECONDS - SUMMARY_SECONDS;

	public static final float MIN_FLIGHT_ZOOM = 7f;
	public static final float MAX_FLIGHT_ZOOM = 17f;
	/**
	 * Long tracks are flown in steps of zoom by length: the flight takes the same forty seconds whatever the
	 * length, so a longer track is flown from higher up, where the map still reads at the speed. Each row is
	 * the length up to which the zoom applies; beyond the last row {@link #MIN_FLIGHT_ZOOM}.
	 */
	public static final double[] LONG_TRACK_METERS = {50_000, 150_000, 500_000, 1_000_000};
	public static final float[] LONG_TRACK_ZOOMS = {Float.NaN, 10f, 9f, 8f};
	/** The flight is always this much closer than the overview, so compact loops are still flown, not panned. */
	public static final float MIN_ZOOM_ABOVE_OVERVIEW = 2f;

	/** A held heading is the direction of the track over this much of the flight ahead. */
	private static final double HEADING_WINDOW_SECONDS = 5;
	private static final double MIN_HEADING_WINDOW_METERS = 300;
	/** The direction of travel is measured over this much of the flight. */
	private static final double LOCAL_WINDOW_SECONDS = 1;
	private static final double MIN_LOCAL_WINDOW_METERS = 100;
	/** The camera keeps its heading until the direction of travel deviates by more than this, degrees ... */
	public static final double HEADING_CHANGE_THRESHOLD = 50;
	/** ... for at least this long, so that a short zigzag does not turn the camera. */
	public static final double HEADING_CHANGE_CONFIRM_SECONDS = 0.5;
	/** A change of heading is animated over this long. */
	public static final double HEADING_TURN_SECONDS = 1.5;
	/**
	 * The camera follows the track averaged over this much of the flight around the current point,
	 * so it glides along the line of the track while the current point wanders about the middle of the
	 * frame, instead of the map being shifted sideways on every bend to keep the point centred.
	 */
	public static final double CAMERA_WINDOW_SECONDS = 4;
	private static final double MIN_CAMERA_WINDOW_METERS = 200;
	private static final int CAMERA_WINDOW_SAMPLES = 8;

	public static class Frame {
		public final double lat;
		public final double lon;
		public final float zoom;
		public final float rotation;
		public final float tilt;
		public final Phase phase;
		/** 0..1 within the current phase. */
		public final double phaseProgress;
		/** 0..1 along the track; 0 before the flight, 1 after it. */
		public final double flightProgress;

		Frame(double lat, double lon, float zoom, float rotation, float tilt, Phase phase,
		      double phaseProgress, double flightProgress) {
			this.lat = lat;
			this.lon = lon;
			this.zoom = zoom;
			this.rotation = rotation;
			this.tilt = tilt;
			this.phase = phase;
			this.phaseProgress = phaseProgress;
			this.flightProgress = flightProgress;
		}
	}

	private final FlyoverTrackGeometry geometry;
	private final int fps;
	private final double overviewLat;
	private final double overviewLon;
	private final float overviewZoom;
	private final float flightTilt;
	private final float flightZoom;
	private final double flightSeconds;
	private final int introFrames;
	private final int flightFrames;
	private final int outroFrames;
	private final int summaryFrames;
	private final float[] headings;
	private final double[] cameraLats;
	private final double[] cameraLons;

	public FlyoverCameraPath(FlyoverTrackGeometry geometry, int fps, double overviewLat, double overviewLon,
	                         float overviewZoom, float flightTilt) {
		this.geometry = geometry;
		this.fps = fps;
		this.overviewLat = overviewLat;
		this.overviewLon = overviewLon;
		this.overviewZoom = overviewZoom;
		this.flightTilt = flightTilt;
		this.flightSeconds = flightSeconds(geometry.getLength());
		this.flightZoom = flightZoom(geometry.getLength(), overviewZoom);
		this.introFrames = (int) Math.round(INTRO_SECONDS * fps);
		this.flightFrames = Math.max(2, (int) Math.round(flightSeconds * fps));
		this.outroFrames = (int) Math.round(OUTRO_SECONDS * fps);
		this.summaryFrames = (int) Math.round(SUMMARY_SECONDS * fps);
		this.headings = computeHeadings();
		this.cameraLats = new double[flightFrames];
		this.cameraLons = new double[flightFrames];
		computeCameraPositions();
	}

	public static double flightSeconds(double lengthMeters) {
		double seconds = 8 + lengthMeters / 1000 * 1.2;
		return Math.max(MIN_FLIGHT_SECONDS, Math.min(MAX_FLIGHT_SECONDS, seconds));
	}

	/**
	 * Zoom of the flight: closer for short tracks, in steps further up for long ones (see
	 * {@link #LONG_TRACK_METERS}), and always closer than the overview.
	 */
	public static float flightZoom(double lengthMeters, float overviewZoom) {
		double zoom = MIN_FLIGHT_ZOOM;
		for (int i = 0; i < LONG_TRACK_METERS.length; i++) {
			if (lengthMeters <= LONG_TRACK_METERS[i]) {
				zoom = i == 0 ? 15.5 - Math.log(1 + lengthMeters / 1500) / Math.log(2) : LONG_TRACK_ZOOMS[i];
				break;
			}
		}
		zoom = Math.max(zoom, overviewZoom + MIN_ZOOM_ABOVE_OVERVIEW);
		return (float) Math.max(MIN_FLIGHT_ZOOM, Math.min(MAX_FLIGHT_ZOOM, zoom));
	}

	public int getFps() {
		return fps;
	}

	public int getTotalFrames() {
		return introFrames + flightFrames + outroFrames + summaryFrames;
	}

	public int getIntroFrames() {
		return introFrames;
	}

	public int getFlightFrames() {
		return flightFrames;
	}

	public double getFlightSeconds() {
		return flightSeconds;
	}

	public float getFlightZoom() {
		return flightZoom;
	}

	/** Ground speed of the camera during the flight, meters per second. */
	public double getFlightSpeed() {
		return geometry.getLength() / flightSeconds;
	}

	/** Seconds since the flight started; negative during the intro, beyond the flight length afterwards. */
	public double flightTimeSeconds(int frameIndex) {
		return (frameIndex - introFrames) / (double) fps;
	}

	public Frame frameAt(int index) {
		int total = getTotalFrames();
		index = Math.max(0, Math.min(total - 1, index));
		if (index < introFrames) {
			double t = index / (double) introFrames;
			return interpolate(overviewFrame(Phase.INTRO, t, 0), flightFrame(0), smooth(t), Phase.INTRO, t, 0);
		}
		int j = index - introFrames;
		if (j < flightFrames) {
			return flightFrame(j);
		}
		int k = j - flightFrames;
		if (k < outroFrames) {
			double t = (k + 1) / (double) outroFrames;
			return interpolate(flightFrame(flightFrames - 1), overviewFrame(Phase.OUTRO, t, 1), smooth(t),
					Phase.OUTRO, t, 1);
		}
		int m = k - outroFrames;
		return overviewFrame(Phase.SUMMARY, m / (double) summaryFrames, 1);
	}

	private Frame overviewFrame(Phase phase, double phaseProgress, double flightProgress) {
		return new Frame(overviewLat, overviewLon, overviewZoom, 0f, TOP_DOWN_TILT, phase, phaseProgress, flightProgress);
	}

	private Frame flightFrame(int j) {
		double progress = flightFrames <= 1 ? 1 : j / (double) (flightFrames - 1);
		return new Frame(cameraLats[j], cameraLons[j], flightZoom,
				rotationForHeading(headings[j]), flightTilt, Phase.FLIGHT, progress, progress);
	}

	/**
	 * Camera position per flight frame: the mean of the track positions over a window centred on the
	 * current point. The window shrinks towards the ends of the track so that the camera starts and
	 * finishes exactly on it.
	 */
	private void computeCameraPositions() {
		double length = geometry.getLength();
		double halfWindow = Math.max(MIN_CAMERA_WINDOW_METERS, getFlightSpeed() * CAMERA_WINDOW_SECONDS) / 2;
		for (int j = 0; j < flightFrames; j++) {
			double progress = flightFrames <= 1 ? 1 : j / (double) (flightFrames - 1);
			double distance = length * progress;
			double half = Math.min(halfWindow, Math.min(distance, length - distance));
			double lat = 0;
			double lon = 0;
			int samples = 2 * CAMERA_WINDOW_SAMPLES + 1;
			for (int k = -CAMERA_WINDOW_SAMPLES; k <= CAMERA_WINDOW_SAMPLES; k++) {
				LatLon position = geometry.positionAt(distance + half * k / CAMERA_WINDOW_SAMPLES);
				lat += position.getLatitude();
				lon += position.getLongitude();
			}
			cameraLats[j] = lat / samples;
			cameraLons[j] = lon / samples;
		}
	}

	/**
	 * Heading per flight frame. The camera holds one heading, the direction of the track over the
	 * next few seconds, and turns to a new one only once the direction of travel has deviated from
	 * it by more than {@link #HEADING_CHANGE_THRESHOLD} for a while; so the map does not rotate,
	 * and redraw, on every bend.
	 */
	private float[] computeHeadings() {
		float[] result = new float[flightFrames];
		double speed = getFlightSpeed();
		double window = Math.max(MIN_HEADING_WINDOW_METERS, speed * HEADING_WINDOW_SECONDS);
		double localWindow = Math.max(MIN_LOCAL_WINDOW_METERS, speed * LOCAL_WINDOW_SECONDS);
		int turnFrames = Math.max(1, (int) Math.round(HEADING_TURN_SECONDS * fps));
		int confirmFrames = Math.max(1, (int) Math.round(HEADING_CHANGE_CONFIRM_SECONDS * fps));
		double held = geometry.bearingAt(0, window);
		double turnFrom = held;
		double turnTo = held;
		int turnFrame = turnFrames;
		int deviatingFrames = 0;
		for (int j = 0; j < flightFrames; j++) {
			double progress = flightFrames <= 1 ? 1 : j / (double) (flightFrames - 1);
			double distance = geometry.getLength() * progress;
			if (turnFrame < turnFrames) {
				turnFrame++;
				double t = smooth(turnFrame / (double) turnFrames);
				held = turnFrom + shortestDiff(turnFrom, turnTo) * t;
			} else {
				double travel = geometry.bearingAt(distance, localWindow);
				deviatingFrames = Math.abs(shortestDiff(held, travel)) > HEADING_CHANGE_THRESHOLD ? deviatingFrames + 1 : 0;
				if (deviatingFrames >= confirmFrames) {
					turnFrom = held;
					turnTo = geometry.bearingAt(distance, window);
					turnFrame = 0;
					deviatingFrames = 0;
				}
			}
			result[j] = normalize360(held);
		}
		return result;
	}

	private static float normalize360(double degrees) {
		double d = degrees % 360;
		return (float) (d < 0 ? d + 360 : d);
	}

	private static Frame interpolate(Frame a, Frame b, double t, Phase phase, double phaseProgress,
	                                 double flightProgress) {
		double lat = a.lat + (b.lat - a.lat) * t;
		double lon = a.lon + (b.lon - a.lon) * t;
		float zoom = (float) (a.zoom + (b.zoom - a.zoom) * t);
		float tilt = (float) (a.tilt + (b.tilt - a.tilt) * t);
		float rotation = normalize180(a.rotation + shortestDiff(a.rotation, b.rotation) * t);
		return new Frame(lat, lon, zoom, rotation, tilt, phase, phaseProgress, flightProgress);
	}

	/**
	 * Zoom at which a bounding box fits into a part of a frame, in Web Mercator with square tiles of
	 * {@code tilePx} pixels: the largest zoom at which the box is no wider than {@code availableWidthPx}
	 * and no higher than {@code availableHeightPx}.
	 */
	public static float overviewZoom(double minLat, double maxLat, double minLon, double maxLon,
	                                 double availableWidthPx, double availableHeightPx, double tilePx) {
		double lonSpan = Math.max(1e-6, maxLon - minLon) / 360;
		double latSpan = Math.max(1e-9, mercatorY(minLat) - mercatorY(maxLat));
		double byWidth = availableWidthPx / (tilePx * lonSpan);
		double byHeight = availableHeightPx / (tilePx * latSpan);
		double zoom = Math.log(Math.min(byWidth, byHeight)) / Math.log(2);
		return (float) Math.max(1, Math.min(22, zoom));
	}

	/** Web Mercator y of a latitude, 0 at the north edge of the world and 1 at the south edge. */
	static double mercatorY(double lat) {
		double clamped = Math.max(-85.05, Math.min(85.05, lat));
		double rad = Math.toRadians(clamped);
		return (1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2;
	}

	public static float rotationForHeading(double heading) {
		return normalize180(-heading);
	}

	/** Smoothstep easing. */
	public static double smooth(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/** Normalizes an angle to [-180, 180). */
	public static float normalize180(double degrees) {
		double d = degrees % 360;
		if (d < -180) {
			d += 360;
		} else if (d >= 180) {
			d -= 360;
		}
		return (float) d;
	}

	/** Signed shortest rotation from one angle to another, in (-180, 180]. */
	public static float shortestDiff(double from, double to) {
		return normalize180(to - from);
	}
}

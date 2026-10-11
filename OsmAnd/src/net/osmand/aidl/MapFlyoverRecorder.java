package net.osmand.aidl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.aidlapi.map.MapFlyoverParams;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.views.MapViewWithLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Renders a camera flight off the screen, frame by frame, into an MP4 video. It takes the OpenGL renderer over
 * the way Android Auto does and gives it back to the phone's map at the end. Every frame waits until the
 * renderer is idle (map data and symbols loaded), and frames get exact timestamps, so the video is smooth
 * however long a frame takes to render.
 */
class MapFlyoverRecorder {

	private static final Log LOG = PlatformUtil.getLog(MapFlyoverRecorder.class);

	private static final String FOLDER = "flyover";
	private static final long APP_INIT_TIMEOUT_MS = 60_000;
	private static final long FIRST_FRAME_TIMEOUT_MS = 30_000;
	private static final long FRAME_TIMEOUT_MS = 5_000;
	private static final int BIT_RATE_PER_PIXEL = 8;
	private static final int SUPERSAMPLING = 2;
	private static final long SYMBOLS_UPDATE_MS = 2500;
	private static final float SYMBOLS_FADE_MS = 400;
	private static final int MAX_SIZE = 3840;

	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private int renderWidth;
	private int renderHeight;
	// map layers (tracks, markers, favourites) build their OpenGL objects while they draw on a canvas, as on
	// Android Auto; nothing visible is drawn there, so a 1 px canvas does
	private final Canvas layersCanvas = new Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));

	MapFlyoverRecorder(@NonNull OsmandApplication app) {
		this.app = app;
		this.mapView = app.getOsmandMap().getMapView();
	}

	boolean record(@NonNull MapFlyoverParams params) {
		// a call can start OsmAnd's process; OpenGL is ready only when the app has started
		long initUntil = SystemClock.elapsedRealtime() + APP_INIT_TIMEOUT_MS;
		while (app.isApplicationInitializing() && SystemClock.elapsedRealtime() < initUntil) {
			SystemClock.sleep(200);
		}
		JSONObject spec;
		String error;
		try {
			spec = parseSpec(params.getSpec());
			error = validate(params, spec);
		} catch (IllegalArgumentException e) {
			spec = null;
			error = e.getMessage();
		}
		if (error != null) {
			params.setError(error);
			return false;
		}
		long start = SystemClock.elapsedRealtime();
		File file = getFile(params.getFileName());
		AtlasMapRendererView view = null;
		VideoEncoder encoder = null;
		List<FlyoverTrack> tracks = new ArrayList<>();
		Map<String, SelectedGpxFile> shownBefore = new HashMap<>();
		boolean[] linkedToLocation = new boolean[1];
		int notIdleFrames = 0;
		try {
			// a map linked to my location goes back to it at every fix and draws my marker at its center
			runInUiThread(() -> {
				linkedToLocation[0] = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
				app.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			});
			double videoS = getVideoDuration(params, spec);
			JSONArray gpxSpecs = spec.optJSONArray("gpx");
			for (int k = 0; gpxSpecs != null && k < gpxSpecs.length(); k++) {
				tracks.add(new FlyoverTrack(app, k + 1, gpxSpecs.getJSONObject(k), videoS));
			}
			JSONObject follow = spec.optJSONObject("camera");
			// camera.follow: a track number, or several (ghost mode: all of them kept in the frame)
			List<FlyoverTrack> followed = null;
			if (follow != null && follow.has("follow")) {
				followed = new ArrayList<>();
				JSONArray numbers = follow.optJSONArray("follow");
				int count = numbers != null ? numbers.length() : 1;
				for (int k = 0; k < count; k++) {
					int number = numbers != null ? numbers.optInt(k) : follow.optInt("follow");
					if (number < 1 || number > tracks.size()) {
						throw new IllegalArgumentException("camera.follow takes numbers of tracks in gpx[], 1-" + tracks.size());
					}
					followed.add(tracks.get(number - 1));
				}
			}
			int frameMin = Math.min(params.getWidth(), params.getHeight());
			float tilePx = 256 * Math.max(1f, Math.max(params.getWidth(), params.getHeight()) / 640f);
			showTracks(tracks, shownBefore);
			// rendered at twice the size and scaled down: thin lines and text edges stop shimmering in motion
			int scale = params.getWidth() * SUPERSAMPLING <= MAX_SIZE && params.getHeight() * SUPERSAMPLING <= MAX_SIZE ? SUPERSAMPLING : 1;
			renderWidth = params.getWidth() * scale;
			renderHeight = params.getHeight() * scale;
			view = takeRendererOver(renderWidth, renderHeight, "screen".equals(spec.optString("scale")));
			encoder = new VideoEncoder(file, params.getWidth(), params.getHeight(), params.getFps());
			FlyoverOverlay overlay = new FlyoverOverlay(app, spec, params.getWidth(), params.getHeight());
			FlyoverValues values = new FlyoverValues(app, tracks, spec.optJSONObject("data"));
			FlyoverPanels panels = new FlyoverPanels(app, values, spec, params.getWidth(), params.getHeight());
			FlyoverCallouts callouts = new FlyoverCallouts(app, tracks, spec, params.getWidth(), params.getHeight(),
					videoS, params.getFps());
			int frames = (int) Math.round(videoS * params.getFps()) + 1;
			// the map moves across the screen at most this fast: above ~250 px/s sharp lines strobe
			float pxPerTile = 256 * Math.max(1f, Math.max(params.getWidth(), params.getHeight()) / 640f);
			double maxSpeed = 0;
			double[] prev = null;
			// Labels and icons are laid out again only every SYMBOLS_UPDATE_MS of video time and move with the map
			// in between, as on the moving map. The engine would also lay them out after 1 s of real time, which
			// is a few frames here, so that timer is switched off while the symbols are suspended.
			view.setSymbolsUpdateInterval(Integer.MAX_VALUE);
			int symbolsEvery = Math.max(1, (int) (SYMBOLS_UPDATE_MS * params.getFps() / 1000));
			// the renderer still shows the phone's camera: draw the start once, unrecorded, so frame 0 is right
			applyCamera(followed != null ? followCamera(followed, follow, 0, frameMin, tilePx) : keyframeCamera(params, spec, 0),
					() -> updateTracks(tracks, 0, videoS));
			// load_timeout_s: how long a frame may wait for map data (relief on a slow device loads for long)
			double loadTimeoutS = spec.optDouble("load_timeout_s", Double.NaN);
			long firstTimeout = Double.isNaN(loadTimeoutS) ? FIRST_FRAME_TIMEOUT_MS : (long) (loadTimeoutS * 1000);
			long frameTimeout = Double.isNaN(loadTimeoutS) ? FRAME_TIMEOUT_MS : (long) (loadTimeoutS * 1000);
			String notIdleReason = null;
			waitForIdleFrame(view, firstTimeout);
			for (int i = 0; i < frames; i++) {
				long t = i * 1000L / params.getFps();
				boolean layoutFrame = i % symbolsEvery == 0;
				if (layoutFrame && view.isSymbolsUpdateSuspended()) {
					view.resumeSymbolsUpdate();
				} else if (!layoutFrame && !view.isSymbolsUpdateSuspended()) {
					view.suspendSymbolsUpdate();
				}
				// symbols that come and go at a layout fade in and out on the video's clock
				view.setSymbolsFade(SYMBOLS_FADE_MS, t);
				double[] camera = followed != null ? followCamera(followed, follow, t / 1000.0, frameMin, tilePx) : keyframeCamera(params, spec, t);
				applyCamera(camera, () -> updateTracks(tracks, t / 1000.0, videoS));
				if (prev != null) {
					maxSpeed = Math.max(maxSpeed, screenDistance(prev, camera, pxPerTile) * params.getFps());
				}
				prev = camera;
				if (!waitForIdleFrame(view, frameTimeout)) {
					notIdleFrames++;
					String reason = view.getNotIdleReason();
					if (notIdleReason == null) {
						notIdleReason = reason;
					}
					LOG.warn("Flyover frame " + i + " is not idle: " + reason);
				}
				Bitmap bitmap = view.getBitmap();
				if (bitmap == null) {
					throw new IllegalStateException("The renderer gave no picture for frame " + i);
				}
				// the overlay is drawn at the video's size, so its text stays sharp
				Bitmap frame = Bitmap.createScaledBitmap(bitmap, params.getWidth(), params.getHeight(), true);
				if (frame == bitmap) {
					frame = bitmap.copy(Bitmap.Config.ARGB_8888, true);
				}
				Canvas frameCanvas = new Canvas(frame);
				overlay.draw(frameCanvas, camera[0], (float) camera[2], (float) camera[3]);
				for (FlyoverTrack track : tracks) {
					WptPt[] head = track.headPoints(t / 1000.0);
					if (head != null) {
						Map<WptPt, PointF> pts = project(Arrays.asList(head), scale, params.getWidth(), params.getHeight());
						PointF at = pts.get(head[0]);
						if (at != null) {
							overlay.drawHead(frameCanvas, at, pts.get(head[1]), track.head, track.getColor(), track.headIcon);
						}
					}
				}
				if (!callouts.isEmpty()) {
					Map<WptPt, PointF> onScreen = project(callouts.visibleAt(t / 1000.0), scale,
							params.getWidth(), params.getHeight());
					callouts.draw(frameCanvas, t / 1000.0, onScreen::get);
				}
				panels.draw(frameCanvas, new FlyoverValues.Frame(t / 1000.0, videoS, camera));
				encoder.encode(frame, i * 1_000_000L / params.getFps());
				frame.recycle();
			}
			encoder.finish();
			encoder = null;
			params.setFilePath(file.getAbsolutePath());
			params.setFrames(frames);
			String warnings = unknownKeys(spec);
			if (notIdleFrames > 0) {
				warnings += (warnings.isEmpty() ? "" : "; ") + notIdleFrames + " frames were recorded before the map " +
						"finished loading (" + notIdleReason + "); load_timeout_s gives frames more time";
			}
			params.setWarnings(warnings);
			params.setMaxScreenSpeed((float) maxSpeed);
			params.setRenderTimeMs(SystemClock.elapsedRealtime() - start);
			LOG.info("Flyover " + file + ": " + frames + " frames in " + params.getRenderTimeMs()
					+ " ms, not idle " + notIdleFrames);
			return true;
		} catch (Exception | LinkageError e) {
			// a broken request or a bug must not take OsmAnd down with it: report it, give the renderer back
			LOG.error("Flyover failed", e);
			params.setError(e.getMessage() != null ? e.getMessage() : e.toString());
			return false;
		} finally {
			hideTracks(tracks, shownBefore);
			if (linkedToLocation[0]) {
				try {
					runInUiThread(() -> app.getMapViewTrackingUtilities().setMapLinkedToLocation(true));
				} catch (InterruptedException e) {
					LOG.error(e);
				}
			}
			if (encoder != null) {
				encoder.release();
				file.delete();
			}
			if (view != null) {
				giveRendererBack(view);
			}
		}
	}

	@Nullable
	private String validate(@NonNull MapFlyoverParams p, @NonNull JSONObject spec) {
		int n = p.getLatitudes().length;
		JSONObject camera = spec.optJSONObject("camera");
		if (n == 0 && camera != null && camera.has("follow")) {
			double d = spec.optDouble("duration_s", 0);
			return d > 0 && d <= 600 ? checkVideo(p) : "A video that follows a track needs duration_s, 0-600";
		}
		if (n == 0 || p.getLongitudes().length != n || p.getZooms().length != n || p.getAzimuths().length != n
				|| p.getElevationAngles().length != n || p.getTimesMs().length != n) {
			return "Key frames need the same number of latitudes, longitudes, zooms, azimuths, elevation angles and times";
		}
		long[] times = p.getTimesMs();
		for (int i = 1; i < n; i++) {
			if (times[i] <= times[i - 1]) {
				return "Key frame times must go up";
			}
		}
		if (times[n - 1] <= 0 || times[n - 1] > 10 * 60_000) {
			return "The flight lasts from 1 ms to 10 minutes";
		}
		return checkVideo(p);
	}

	@Nullable
	private String checkVideo(@NonNull MapFlyoverParams p) {
		if (p.getWidth() < 16 || p.getHeight() < 16 || p.getWidth() > 3840 || p.getHeight() > 3840
				|| p.getWidth() % 2 != 0 || p.getHeight() % 2 != 0) {
			return "Width and height are even numbers from 16 to 3840";
		}
		if (p.getFps() < 1 || p.getFps() > 60) {
			return "fps is 1-60";
		}
		if (!app.useOpenGlRenderer()) {
			return "The flyover needs the OpenGL renderer (Settings > Development > OpenGL)";
		}
		return null;
	}

	@NonNull
	private File getFile(@Nullable String name) {
		File dir = app.getAppPath(FOLDER);
		dir.mkdirs();
		if (Algorithms.isEmpty(name)) {
			name = "flyover_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
		}
		name = name.replaceAll("[/\\\\]", "_");
		return new File(dir, name.endsWith(".mp4") ? name : name + ".mp4");
	}

	/**
	 * Same steps as SurfaceRenderer.setupOffscreenRenderer(): the new offscreen view takes the renderer over
	 * from the view that has it now, and the map layers are attached to it.
	 */
	@NonNull
	private AtlasMapRendererView takeRendererOver(int width, int height, boolean phoneScale) throws InterruptedException {
		AtlasMapRendererView[] result = new AtlasMapRendererView[1];
		runInUiThread(() -> {
			MapRendererContext context = NativeCoreContext.getMapRendererContext();
			if (context == null) {
				return;
			}
			MapRendererView current = null;
			if (mapView.getMapRenderer() != null) {
				mapView.detachMapRenderer();
			}
			if (context.getMapRendererView() != null) {
				current = context.getMapRendererView();
				context.setMapRendererView(null);
			}
			// by default the symbols are sized for a frame about 640 dp wide; "scale": "screen" keeps the phone's
			// map scale, so the video matches screenshots of the phone (as wide as its short side)
			DisplayMetrics dm = app.getResources().getDisplayMetrics();
			float density = phoneScale ? dm.density * width / Math.min(dm.widthPixels, dm.heightPixels)
					: Math.max(1f, Math.max(width, height) / 640f);
			NativeCoreContext.setMapRendererContext(app, density);
			context = NativeCoreContext.getMapRendererContext();
			if (context == null) {
				return;
			}
			AtlasMapRendererView view = new AtlasMapRendererView(app);
			context.presetMapRendererOptions(view, app.getSettings().ENABLE_MSAA.get());
			view.setupRenderer(app, width, height, current);
			view.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.getMinZoom()));
			view.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.getMaxZoom()));
			view.setAzimuth(0);
			view.setFlatEarth(!app.getSettings().SPHERICAL_MAP.get());
			view.removeAllSymbolsProviders();
			view.resumeSymbolsUpdate();
			context.setMapRendererView(view);
			mapView.setMapRenderer(view, false);
			app.getOsmandMap().getMapLayers().updateMapSource(mapView, null);
			PluginsHelper.refreshLayers(app, null);
			result[0] = view;
		});
		if (result[0] == null) {
			throw new IllegalStateException("The OpenGL renderer is not ready");
		}
		return result[0];
	}

	private void giveRendererBack(@NonNull AtlasMapRendererView view) {
		// the suspension and the interval live in the renderer, which goes back to the phone's map
		while (view.isSymbolsUpdateSuspended()) {
			view.resumeSymbolsUpdate();
		}
		view.setSymbolsUpdateInterval(MapViewWithLayers.SYMBOLS_UPDATE_INTERVAL);
		view.setSymbolsFade(0, -1);
		try {
			runInUiThread(() -> {
				if (mapView.getMapRenderer() == view) {
					mapView.detachMapRenderer();
				}
				MapRendererContext context = NativeCoreContext.getMapRendererContext();
				if (context != null) {
					context.suspendMapRendererView(view);
				}
				// the phone's map takes the renderer over from the paused view, as after Android Auto
				app.getOsmandMap().setupRenderingView();
			});
		} catch (InterruptedException e) {
			LOG.error(e);
		}
	}

	@NonNull
	private static JSONObject parseSpec(@Nullable String spec) {
		try {
			return Algorithms.isEmpty(spec) ? new JSONObject() : new JSONObject(spec);
		} catch (JSONException e) {
			throw new IllegalArgumentException("The flyover request is not valid JSON: " + e.getMessage());
		}
	}

	// what this OsmAnd understands in a flyover request; anything else is reported back, not silently dropped
	private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("keyframes", "width", "height", "fps",
			"name", "camera", "duration_s", "gpx", "osmand_widgets", "attribution", "profile", "scale",
			"ease_in_s", "ease_out_s", "panels", "data", "ui_scale", "load_timeout_s"));

	@NonNull
	private static String unknownKeys(@NonNull JSONObject spec) {
		List<String> unknown = new ArrayList<>();
		for (Iterator<String> it = spec.keys(); it.hasNext(); ) {
			String key = it.next();
			if (!KNOWN_KEYS.contains(key)) {
				unknown.add(key);
			}
		}
		return unknown.isEmpty() ? "" : "This OsmAnd does not support yet and ignored: " + String.join(", ", unknown);
	}

	private static double getVideoDuration(@NonNull MapFlyoverParams p, @NonNull JSONObject spec) {
		long[] times = p.getTimesMs();
		return times.length > 0 ? times[times.length - 1] / 1000.0 : spec.optDouble("duration_s");
	}

	/**
	 * Camera that follows a track: the center is the track's point averaged over a short window (GPS noise does
	 * not shake the video), heading from the track a little behind to a little ahead, also averaged.
	 * Returns {lat, lon, zoom, rotation, elevation angle}.
	 */
	@NonNull
	private static double[] followCamera(@NonNull List<FlyoverTrack> tracks, @NonNull JSONObject camera, double t,
	                                     int frameMin, float tilePx) {
		double ahead = camera.optDouble("look_ahead_s", 3);
		double lat = 0, lon = 0, sin = 0, cos = 0, spread = 0;
		int samples = 5;
		for (int k = 0; k < samples; k++) {
			double tk = t + (k - samples / 2) * 0.15;
			double kLat = 0, kLon = 0;
			List<FlyoverTrack.Position> here = new ArrayList<>();
			for (FlyoverTrack track : tracks) {
				FlyoverTrack.Position p = track.at(tk);
				here.add(p);
				kLat += p.lat;
				kLon += p.lon;
				double b = Math.toRadians(track.bearing(tk, 1, ahead));
				sin += Math.sin(b);
				cos += Math.cos(b);
			}
			kLat /= tracks.size();
			kLon /= tracks.size();
			for (FlyoverTrack.Position p : here) {
				spread = Math.max(spread, MapUtils.getDistance(kLat, kLon, p.lat, p.lon));
			}
			lat += kLat;
			lon += kLon;
		}
		lat /= samples;
		lon /= samples;
		double heading = Math.toDegrees(Math.atan2(sin, cos));
		double zoom = camera.optDouble("zoom", 15);
		if (tracks.size() > 1 && spread > 1) {
			// every followed track stays within about a third of the frame from its center
			double fit = Math.log(0.35 * frameMin * 40075016 * Math.cos(Math.toRadians(lat)) / (tilePx * spread)) / Math.log(2);
			zoom = Math.max(camera.optDouble("min_zoom", 10), Math.min(zoom, fit));
		}
		// OsmAnd turns the map by minus the heading so that the way ahead is up
		return new double[] {lat, lon, zoom, -heading, camera.optDouble("tilt", 50)};
	}

	/**
	 * Camera at time t from the key frames; returns {lat, lon, zoom, rotation, elevation angle}. Every channel follows
	 * a curve through the key frames with tangents in time units, so the camera passes key frames without stopping;
	 * it only speeds up after the start and slows down before the end (ease_in_s / ease_out_s).
	 */
	@NonNull
	private double[] keyframeCamera(@NonNull MapFlyoverParams p, @NonNull JSONObject spec, long tMs) {
		long[] times = p.getTimesMs();
		int n = times.length;
		double total = times[n - 1] / 1000.0;
		double t = easeTime(tMs / 1000.0, total, spec.optDouble("ease_in_s", Math.min(1, total / 3)),
				spec.optDouble("ease_out_s", Math.min(1, total / 3)));
		double[] ts = new double[n];
		double[] rotation = new double[n];
		for (int k = 0; k < n; k++) {
			ts[k] = times[k] / 1000.0;
			// unwrapped, so that 170 -> -170 turns 20 degrees, not 340
			rotation[k] = k == 0 ? p.getAzimuths()[0]
					: rotation[k - 1] + MapUtils.unifyRotationDiff((float) rotation[k - 1], p.getAzimuths()[k]);
		}
		double[] zoom = new double[n], tilt = new double[n], lat = new double[n], lon = new double[n];
		for (int k = 0; k < n; k++) {
			lat[k] = p.getLatitudes()[k];
			lon[k] = p.getLongitudes()[k];
			zoom[k] = p.getZooms()[k];
			tilt[k] = p.getElevationAngles()[k];
		}
		return new double[] {hermite(ts, lat, t), hermite(ts, lon, t),
				Math.max(1, Math.min(22, hermite(ts, zoom, t))), hermite(ts, rotation, t),
				Math.max(10, Math.min(90, hermite(ts, tilt, t)))};
	}

	/** Cubic Hermite through (ts, values) with Catmull-Rom tangents per second: smooth speed across key frames. */
	private static double hermite(@NonNull double[] ts, @NonNull double[] values, double t) {
		int n = ts.length;
		if (n == 1) {
			return values[0];
		}
		int i = 0;
		while (i < n - 2 && t > ts[i + 1]) {
			i++;
		}
		double h = ts[i + 1] - ts[i];
		double s = Math.max(0, Math.min(1, (t - ts[i]) / h));
		double m0 = tangent(ts, values, i);
		double m1 = tangent(ts, values, i + 1);
		double s2 = s * s, s3 = s2 * s;
		return (2 * s3 - 3 * s2 + 1) * values[i] + (s3 - 2 * s2 + s) * h * m0
				+ (-2 * s3 + 3 * s2) * values[i + 1] + (s3 - s2) * h * m1;
	}

	private static double tangent(@NonNull double[] ts, @NonNull double[] values, int k) {
		int a = Math.max(0, k - 1);
		int b = Math.min(ts.length - 1, k + 1);
		return (values[b] - values[a]) / (ts[b] - ts[a]);
	}

	/** Time warped so that the camera speeds up over easeIn seconds and slows down over easeOut, same total. */
	private static double easeTime(double t, double total, double easeIn, double easeOut) {
		double a = Math.max(0, easeIn), b = Math.max(0, easeOut);
		if (a + b > total) {
			a = b = total / 2;
		}
		if (a + b <= 0) {
			return t;
		}
		double v = total / (total - (a + b) / 2);
		if (t < a) {
			return v * t * t / (2 * a);
		} else if (t <= total - b) {
			return v * (t - a / 2);
		}
		double rest = Math.max(0, total - t);
		return total - v * rest * rest / (2 * Math.max(b, 1e-9));
	}

	/** Sets the camera and lets the map layers build what changed; {@code beforeLayers} runs just before them. */
	private void applyCamera(@NonNull double[] camera, @Nullable Runnable beforeLayers) throws InterruptedException {
		double fLat = camera[0], fLon = camera[1];
		float fZoom = (float) camera[2], fAzimuth = (float) camera[3], fElevation = (float) camera[4];
		runInUiThread(() -> {
			// immediate setters: the animated ones would run on their own clock, not the video's
			mapView.getAnimatedDraggingThread().stopAnimating();
			mapView.rotateToAnimate(fAzimuth);
			mapView.setElevationAngle(fElevation);
			mapView.setLatLon(fLat, fLon);
			int base = (int) Math.floor(fZoom);
			mapView.setZoomWithFloatPart(base, fZoom - base);
			// the map view puts the target at its ratio of the phone's view; the rendered frame has another size
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer != null) {
				// the map magnifier (map_density) as on the phone's map
				renderer.setVisualZoomShift(app.getOsmandMap().getMapDensity() - 1.0f);
			}
			if (renderer != null && renderWidth > 0) {
				PointF ratio = app.getMapViewTrackingUtilities().getMapDisplayPositionManager().getMapRatio();
				renderer.setMapTarget(new PointI(Math.round(ratio.x * renderWidth), Math.round(ratio.y * renderHeight)),
						NativeUtilities.calculateTarget31(renderer, fLat, fLon, false));
			}
			if (beforeLayers != null) {
				beforeLayers.run();
			}
			boolean night = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
			mapView.drawOverMap(layersCanvas, mapView.getRotatedTileBox(), new DrawSettings(night, false));
		});
	}

	/** Tracks that grow or fade change their lines; the GPX layer then rebuilds them in place for this frame. */
	private void updateTracks(@NonNull List<FlyoverTrack> tracks, double t, double videoS) {
		boolean changed = false;
		for (FlyoverTrack track : tracks) {
			changed |= track.updateDisplay(t, videoS);
		}
		if (changed) {
			// the layer resets the flag itself after it has drawn
			app.getOsmandMap().getMapLayers().getGpxLayer().setInvalidated(true);
		}
	}

	/** Where waypoints are on the video frame, with relief and tilt; those off the frame are left out. */
	@NonNull
	private Map<WptPt, PointF> project(@NonNull List<WptPt> points, int supersampling, int width, int height)
			throws InterruptedException {
		Map<WptPt, PointF> result = new HashMap<>();
		if (points.isEmpty()) {
			return result;
		}
		runInUiThread(() -> {
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer == null) {
				return;
			}
			for (WptPt w : points) {
				PointI out = new PointI();
				PointI location = new PointI(MapUtils.get31TileNumberX(w.getLon()), MapUtils.get31TileNumberY(w.getLat()));
				if (renderer.getElevatedPointFromLocation(location, out, true)) {
					float x = out.getX() / (float) supersampling, y = out.getY() / (float) supersampling;
					if (x >= 0 && y >= 0 && x <= width && y <= height) {
						result.put(w, new PointF(x, y));
					}
				}
			}
		});
		return result;
	}

	/** The tracks are shown for the recording only: the user's selection is put back afterwards. */
	private void showTracks(@NonNull List<FlyoverTrack> tracks, @NonNull Map<String, SelectedGpxFile> shownBefore)
			throws InterruptedException {
		if (tracks.isEmpty()) {
			return;
		}
		runInUiThread(() -> {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			for (FlyoverTrack track : tracks) {
				SelectedGpxFile before = helper.getSelectedFileByPath(track.file.getAbsolutePath());
				if (before != null) {
					shownBefore.put(track.file.getAbsolutePath(), before);
				}
				track.selected = helper.selectGpxFile(track.gpx, GpxSelectionParams.newInstance().showOnMap());
			}
		});
	}

	private void hideTracks(@NonNull List<FlyoverTrack> tracks, @NonNull Map<String, SelectedGpxFile> shownBefore) {
		if (tracks.isEmpty()) {
			return;
		}
		try {
			runInUiThread(() -> {
				GpxSelectionHelper helper = app.getSelectedGpxHelper();
				for (FlyoverTrack track : tracks) {
					SelectedGpxFile before = shownBefore.get(track.file.getAbsolutePath());
					if (before != null) {
						helper.selectGpxFile(before.getGpxFile(), GpxSelectionParams.newInstance().showOnMap());
					} else {
						helper.selectGpxFile(track.gpx, GpxSelectionParams.newInstance().hideFromMap());
					}
				}
			});
		} catch (InterruptedException e) {
			LOG.error(e);
		}
	}

	/** Pixels the map moves on the screen between two cameras, measured at the higher zoom. */
	private static double screenDistance(@NonNull double[] a, @NonNull double[] b, float pxPerTile) {
		double zoom = Math.max(a[2], b[2]);
		double dx = MapUtils.getTileNumberX(0, b[1]) - MapUtils.getTileNumberX(0, a[1]);
		double dy = MapUtils.getTileNumberY(0, b[0]) - MapUtils.getTileNumberY(0, a[0]);
		return Math.sqrt(dx * dx + dy * dy) * Math.pow(2, zoom) * pxPerTile;
	}

	/**
	 * Waits for a frame rendered after the renderer became idle, i.e. with all map data and symbols in place.
	 */
	private boolean waitForIdleFrame(@NonNull MapRendererView view, long timeoutMs) throws InterruptedException {
		long until = SystemClock.elapsedRealtime() + timeoutMs;
		int startFrame = view.getFrameId();
		int idleFrame = -1;
		while (SystemClock.elapsedRealtime() < until) {
			view.requestRender();
			Thread.sleep(10);
			int frame = view.getFrameId();
			if (frame <= startFrame) {
				continue;
			}
			if (view.isIdle()) {
				if (idleFrame < 0) {
					idleFrame = frame;
				} else if (frame > idleFrame) {
					return true;
				}
			} else {
				idleFrame = -1;
			}
		}
		return false;
	}

	private void runInUiThread(@NonNull Runnable run) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		RuntimeException[] error = new RuntimeException[1];
		app.runInUIThread(() -> {
			try {
				run.run();
			} catch (RuntimeException e) {
				error[0] = e;
			} finally {
				latch.countDown();
			}
		});
		if (!latch.await(30, TimeUnit.SECONDS)) {
			throw new IllegalStateException("The UI thread did not answer in 30 s");
		}
		if (error[0] != null) {
			throw error[0];
		}
	}




	/** H.264 in MP4 from bitmaps; frames are fed as YUV with exact presentation times. */
	private static class VideoEncoder {

		private static final long TIMEOUT_US = 10_000;

		private final MediaCodec codec;
		private final MediaMuxer muxer;
		private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		private final int width;
		private final int height;
		private final int[] argb;
		private int track = -1;
		private boolean muxerStarted;
		private long lastPtsUs;

		VideoEncoder(@NonNull File file, int width, int height, int fps) throws Exception {
			this.width = width;
			this.height = height;
			argb = new int[width * height];
			MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
			format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * BIT_RATE_PER_PIXEL);
			format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
			codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			codec.start();
			muxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		}

		void encode(@NonNull Bitmap bitmap, long ptsUs) {
			int index;
			while ((index = codec.dequeueInputBuffer(TIMEOUT_US)) < 0) {
				drain(false);
			}
			Image image = codec.getInputImage(index);
			if (image == null) {
				throw new IllegalStateException("The video encoder gave no input image");
			}
			Bitmap frame = bitmap.getWidth() == width && bitmap.getHeight() == height ? bitmap
					: Bitmap.createScaledBitmap(bitmap, width, height, true);
			frame.getPixels(argb, 0, width, 0, 0, width, height);
			if (frame != bitmap) {
				frame.recycle();
			}
			toYuv(image);
			codec.queueInputBuffer(index, 0, width * height * 3 / 2, ptsUs, 0);
			lastPtsUs = ptsUs;
			drain(false);
		}

		void finish() {
			int index;
			while ((index = codec.dequeueInputBuffer(TIMEOUT_US)) < 0) {
				drain(false);
			}
			codec.queueInputBuffer(index, 0, 0, lastPtsUs + 1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			drain(true);
			release();
		}

		void release() {
			try {
				codec.stop();
			} catch (Exception ignored) {
			}
			codec.release();
			try {
				if (muxerStarted) {
					muxer.stop();
				}
			} catch (Exception e) {
				LOG.error(e);
			}
			muxer.release();
		}

		private void drain(boolean endOfStream) {
			while (true) {
				int index = codec.dequeueOutputBuffer(info, TIMEOUT_US);
				if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
					if (!endOfStream) {
						return;
					}
				} else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					track = muxer.addTrack(codec.getOutputFormat());
					muxer.start();
					muxerStarted = true;
				} else if (index >= 0) {
					ByteBuffer data = codec.getOutputBuffer(index);
					if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
						info.size = 0;
					}
					if (info.size > 0 && data != null && muxerStarted) {
						data.position(info.offset);
						data.limit(info.offset + info.size);
						muxer.writeSampleData(track, data, info);
					}
					codec.releaseOutputBuffer(index, false);
					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						return;
					}
				}
			}
		}

		/** BT.601 limited range, the planes laid out as the codec asks. */
		private void toYuv(@NonNull Image image) {
			Image.Plane[] planes = image.getPlanes();
			ByteBuffer yBuf = planes[0].getBuffer();
			int yRow = planes[0].getRowStride();
			int yPix = planes[0].getPixelStride();
			ByteBuffer uBuf = planes[1].getBuffer();
			ByteBuffer vBuf = planes[2].getBuffer();
			int uvRow = planes[1].getRowStride();
			int uvPix = planes[1].getPixelStride();
			for (int y = 0; y < height; y++) {
				int row = y * width;
				for (int x = 0; x < width; x++) {
					int c = argb[row + x];
					int r = (c >> 16) & 0xff;
					int g = (c >> 8) & 0xff;
					int b = c & 0xff;
					yBuf.put(y * yRow + x * yPix, (byte) (((66 * r + 129 * g + 25 * b + 128) >> 8) + 16));
					if ((y & 1) == 0 && (x & 1) == 0) {
						int uv = (y >> 1) * uvRow + (x >> 1) * uvPix;
						uBuf.put(uv, (byte) (((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128));
						vBuf.put(uv, (byte) (((112 * r - 94 * g - 18 * b + 128) >> 8) + 128));
					}
				}
			}
		}
	}
}

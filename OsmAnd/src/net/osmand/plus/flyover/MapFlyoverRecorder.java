package net.osmand.plus.flyover;

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
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.shared.SharedUtil;
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
import java.util.concurrent.CancellationException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders a camera flight off the screen, frame by frame, into an MP4 video. It takes the OpenGL renderer over
 * the way Android Auto does and gives it back to the phone's map at the end. Every frame waits until the
 * renderer is idle (map data and symbols loaded), and frames get exact timestamps, so the video is smooth
 * however long a frame takes to render.
 */
public class MapFlyoverRecorder {

	/** Set by the banner's cancel button; the recording stops before its next frame. */
	private volatile boolean cancelled;
	@Nullable
	private volatile double[] appliedCamera;
	/** Screen offset the relief gives each projected point (a track's head, a waypoint), from its last good frame. */
	private final Map<Object, PointF> reliefLift = new HashMap<>();
	/** camera.drone / camera.poses: the camera is where a drone is and looks where its camera looks. */
	@Nullable
	private FlyoverDroneCamera drone;
	private float elevationScaleBefore = Float.NaN;
	private double droneLevel = Double.NaN;

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
	/** One recording at a time: a second call (an MCP client retrying a long call) would take the renderer over. */
	private static final AtomicBoolean RECORDING = new AtomicBoolean();

	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private int renderWidth;
	private int renderHeight;
	/** The offscreen view of this recording; replaced when the phone's map took the renderer back meanwhile. */
	@Nullable
	private AtlasMapRendererView view;
	private boolean phoneScale;
	private float detail;
	@Nullable
	private String detailNote;
	// frames the renderer has read out into its bitmap buffer; a frame is taken only after a new one
	private final AtomicInteger readyFrames = new AtomicInteger();
	private final MapRendererView.MapRendererViewListener frameListener = new MapRendererView.MapRendererViewListener() {
		@Override
		public void onUpdateFrame(MapRendererView mapRenderer) {
		}

		@Override
		public void onFrameReady(MapRendererView mapRenderer) {
			readyFrames.incrementAndGet();
		}
	};
	// map layers (tracks, markers, favourites) build their OpenGL objects while they draw on a canvas, as on
	// Android Auto; nothing visible is drawn there, so a 1 px canvas does
	private final Canvas layersCanvas = new Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));

	public MapFlyoverRecorder(@NonNull OsmandApplication app) {
		this.app = app;
		this.mapView = app.getOsmandMap().getMapView();
	}

	public boolean record(@NonNull MapFlyoverParams params) {
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
		if (!RECORDING.compareAndSet(false, true)) {
			params.setError("A flyover is already being recorded; wait for it to finish");
			return false;
		}
		long start = SystemClock.elapsedRealtime();
		drone = null;
		droneLevel = Double.NaN;
		elevationScaleBefore = Float.NaN;
		File file = getFile(params.getFileName());
		VideoEncoder encoder = null;
		List<FlyoverTrack> tracks = new ArrayList<>();
		Map<String, SelectedGpxFile> shownBefore = new HashMap<>();
		boolean[] linkedToLocation = new boolean[1];
		Float[] magnifierBefore = new Float[1];
		FlyoverSession[] session = new FlyoverSession[1];
		FlyoverBanner banner = new FlyoverBanner(app, () -> cancelled = true);
		int notIdleFrames = 0;
		try {
			// a map linked to my location goes back to it at every fix and draws my marker at its center;
			// navigation, compass and auto-follow would also move, zoom and tilt it between frames
			runInUiThread(() -> {
				// kept on disk until the end: a crash meanwhile is undone at the next start
				session[0] = new FlyoverSession(app, mapView);
				mapView.setTouchesBlocked(true);
				mapView.setArrowsFixedOnPath(true);
				banner.show(0);
				MapViewTrackingUtilities tracking = app.getMapViewTrackingUtilities();
				tracking.setTrackingPaused(true);
				linkedToLocation[0] = tracking.isMapLinkedToLocation();
				tracking.setMapLinkedToLocation(false);
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
			if (follow != null && (follow.has("drone") || follow.has("poses"))) {
				FlyoverTrack droneTrack = null;
				if (follow.has("drone")) {
					int number = follow.optInt("drone");
					if (number < 1 || number > tracks.size()) {
						throw new IllegalArgumentException("camera.drone takes a number of a track in gpx[], 1-" + tracks.size());
					}
					droneTrack = tracks.get(number - 1);
				}
				drone = new FlyoverDroneCamera(droneTrack, follow);
			}
			int frameMin = Math.min(params.getWidth(), params.getHeight());
			float tilePx = 256 * Math.max(1f, Math.max(params.getWidth(), params.getHeight()) / 640f);
			showTracks(tracks, shownBefore);
			// rendered at twice the size and scaled down: thin lines and text edges stop shimmering in motion
			int scale = params.getWidth() * SUPERSAMPLING <= MAX_SIZE && params.getHeight() * SUPERSAMPLING <= MAX_SIZE ? SUPERSAMPLING : 1;
			renderWidth = params.getWidth() * scale;
			renderHeight = params.getHeight() * scale;
			phoneScale = "screen".equals(spec.optString("scale"));
			DetailChoice choice = chooseDetail(params, spec, tracks, followed, follow, frameMin, tilePx);
			detailNote = choice.note;
			detail = choice.detailedDistance;
			takeRendererOver();
			if (choice.magnifier != null) {
				// the map magnifier for this video only, as Configure map > Map magnifier: below 100% the relief
				// shading and the details get finer; the user's value is put back afterwards
				float magnifier = choice.magnifier;
				runInUiThread(() -> {
					magnifierBefore[0] = app.getSettings().MAP_DENSITY.get();
					app.getSettings().MAP_DENSITY.set(magnifier);
					mapView.applyDisplayScaleSettings();
				});
			}
			encoder = new VideoEncoder(file, params.getWidth(), params.getHeight(), params.getFps());
			if (!spec.has("profile") && !tracks.isEmpty() && tracks.get(0).profileKey != null) {
				// the profile icon of the first track's profile, not the phone's current one
				spec.put("profile", tracks.get(0).profileKey);
			}
			FlyoverOverlay overlay = new FlyoverOverlay(app, spec, params.getWidth(), params.getHeight());
			FlyoverValues values = new FlyoverValues(app, tracks, spec.optJSONObject("data"));
			values.setTiming(spec.optDouble("smooth_s", 1.5), spec.optDouble("refresh_s", 0.25));
			FlyoverPanels panels = new FlyoverPanels(app, values, spec, params.getWidth(), params.getHeight());
			panels.prepare(videoS);
			String layoutWarning = panels.overlaps(overlay.takenAreas(params.getWidth(), params.getHeight()));
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
			applyCamera(cameraAt(params, spec, followed, follow, 0, frameMin, tilePx),
					() -> updateTracks(tracks, 0, videoS));
			// load_timeout_s: how long a frame may wait for map data (relief on a slow device loads for long)
			double loadTimeoutS = spec.optDouble("load_timeout_s", Double.NaN);
			long firstTimeout = Double.isNaN(loadTimeoutS) ? FIRST_FRAME_TIMEOUT_MS : (long) (loadTimeoutS * 1000);
			long frameTimeout = Double.isNaN(loadTimeoutS) ? FRAME_TIMEOUT_MS : (long) (loadTimeoutS * 1000);
			String notIdleReason = null;
			waitForIdleFrame(view, firstTimeout);
			measureDrone(true);
			// once more: settings changed for the video (map magnifier, relief) reach the renderer with a delay,
			// and the first recorded frame showed the map at the old scale
			applyCamera(cameraAt(params, spec, followed, follow, 0, frameMin, tilePx),
					() -> updateTracks(tracks, 0, videoS));
			waitForIdleFrame(view, firstTimeout);
			measureDrone(true);
			for (int k = 0; drone != null && k < DRONE_WARMUP_FRAMES; k++) {
				// the drone's camera learns how far the engine's camera is at a zoom: frame 0 again until it knows
				applyCamera(cameraAt(params, spec, followed, follow, 0, frameMin, tilePx),
						() -> updateTracks(tracks, 0, videoS));
				// only the camera's height is read: the map of a flat view may load for minutes
				waitForFrames(view, 2, firstTimeout);
				measureDrone(true);
			}
			for (int i = 0; i < frames; i++) {
				if (cancelled) {
					throw new CancellationException("Cancelled on the phone");
				}
				// a recreated MapActivity (rotation, theme change) takes the renderer for its own map: take it back
				boolean retaken = ensureRenderer();
				if (i % params.getFps() == 0) {
					int percent = 100 * i / frames;
					app.runInUIThread(() -> banner.show(percent));
				}
				long t = i * 1000L / params.getFps();
				boolean layoutFrame = i % symbolsEvery == 0;
				if (layoutFrame && view.isSymbolsUpdateSuspended()) {
					view.resumeSymbolsUpdate();
				} else if (!layoutFrame && !view.isSymbolsUpdateSuspended()) {
					view.suspendSymbolsUpdate();
				}
				// symbols that come and go at a layout fade in and out on the video's clock
				view.setSymbolsFade(SYMBOLS_FADE_MS, t);
				double[] camera = cameraAt(params, spec, followed, follow, t, frameMin, tilePx);
				applyCamera(camera, () -> updateTracks(tracks, t / 1000.0, videoS));
				if (prev != null) {
					maxSpeed = Math.max(maxSpeed, screenDistance(prev, camera, pxPerTile) * params.getFps());
				}
				prev = camera;
				boolean idle = waitForIdleFrame(view, retaken ? firstTimeout : frameTimeout);
				for (int retry = 0; retry < 3 && cameraMoved(); retry++) {
					// the app moved the map meanwhile: this frame's camera again, then its picture
					applyCamera(camera, () -> updateTracks(tracks, t / 1000.0, videoS));
					idle = waitForIdleFrame(view, frameTimeout);
				}
				boolean corrected = false;
				for (int retry = 0; retry <= DRONE_CORRECTIONS; retry++) {
					// the drone's camera at the height the engine has under its target: this frame once more
					double[] fixed = droneCorrection(retry == DRONE_CORRECTIONS);
					if (fixed == null) {
						break;
					}
					applyCamera(fixed, () -> updateTracks(tracks, t / 1000.0, videoS));
					// the height is the camera's, not the map's: the tiles are waited for only by the last try
					waitForFrames(view, 2, frameTimeout);
					corrected = true;
				}
				if (corrected) {
					idle = waitForIdleFrame(view, frameTimeout);
				}
				measureDrone(false);
				if (!idle) {
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
					double metersPerPixel = 40075016 * Math.cos(Math.toRadians(camera[0]))
							/ (tilePx * Math.pow(2, camera[2]));
					WptPt[] head = track.headPoints(t / 1000.0, metersPerPixel);
					if (head != null) {
						Map<WptPt, PointF> pts = project(Arrays.asList(head),
								Arrays.asList(track.number + ":head", track.number + ":from", track.number + ":to"),
								scale, params.getWidth(), params.getHeight());
						PointF at = pts.get(head[0]);
						if (at != null) {
							PointF behind = pts.get(head[1]), ahead = pts.get(head[2]);
							PointF direction = behind != null && ahead != null
									? new PointF(at.x + ahead.x - behind.x, at.y + ahead.y - behind.y) : ahead;
							overlay.drawHead(frameCanvas, at, direction, track.head, track.getColor(), track.headIcon);
						}
					}
				}
				if (!callouts.isEmpty()) {
					Map<WptPt, PointF> onScreen = project(callouts.visibleAt(t / 1000.0), null, scale,
							params.getWidth(), params.getHeight());
					callouts.draw(frameCanvas, t / 1000.0, onScreen::get);
				}
				panels.draw(frameCanvas, new FlyoverValues.Frame(t / 1000.0, videoS, camera));
				encoder.encode(frame, i * 1_000_000L / params.getFps());
				if (i % Math.max(1, params.getFps() / PREVIEW_PER_S) == 0) {
					// the map area of the app shows what is recorded, a few times a second
					Bitmap preview = Bitmap.createScaledBitmap(frame, frame.getWidth() / 2, frame.getHeight() / 2, true);
					app.runInUIThread(() -> banner.showFrame(preview));
				}
				frame.recycle();
			}
			encoder.finish();
			encoder = null;
			params.setFilePath(file.getAbsolutePath());
			params.setFrames(frames);
			String warnings = unknownKeys(spec);
			if (detailNote != null) {
				warnings += (warnings.isEmpty() ? "" : "; ") + detailNote;
			}
			String droneNote = drone != null ? drone.report() : null;
			if (droneNote != null) {
				warnings += (warnings.isEmpty() ? "" : "; ") + droneNote;
			}
			if (!layoutWarning.isEmpty()) {
				warnings += (warnings.isEmpty() ? "" : "; ") + layoutWarning;
			}
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
			// the request (with gpx_data) is not sent back over Binder: the reply has its own size limit
			params.setSpec("");
			hideTracks(tracks, shownBefore);
			try {
				runInUiThread(() -> {
					MapViewTrackingUtilities tracking = app.getMapViewTrackingUtilities();
					tracking.setTrackingPaused(false);
					if (linkedToLocation[0]) {
						tracking.setMapLinkedToLocation(true);
					}
				});
			} catch (InterruptedException e) {
				LOG.error(e);
			}
			if (encoder != null) {
				encoder.release();
				file.delete();
			}
			if (view != null) {
				giveRendererBack(view);
			}
			if (magnifierBefore[0] != null) {
				try {
					runInUiThread(() -> {
						app.getSettings().MAP_DENSITY.set(magnifierBefore[0]);
						mapView.applyDisplayScaleSettings();
					});
				} catch (InterruptedException e) {
					LOG.error(e);
				}
			}
			try {
				runInUiThread(() -> {
					mapView.setTouchesBlocked(false);
					mapView.setArrowsFixedOnPath(false);
					banner.hide();
					if (session[0] != null) {
						session[0].finish(app, mapView);
					}
				});
			} catch (InterruptedException e) {
				LOG.error(e);
			}
			RECORDING.set(false);
		}
	}

	private static class DetailChoice {
		final float detailedDistance;
		@Nullable
		final Float magnifier;
		@Nullable
		final String note;

		DetailChoice(float detailedDistance, @Nullable Float magnifier, @Nullable String note) {
			this.detailedDistance = detailedDistance;
			this.magnifier = magnifier;
			this.note = note;
		}
	}

	/**
	 * {@code detail}: "low", "normal" (default) or "high". The recorder picks the engine's detailed distance and the
	 * map magnifier from the camera's zoom over the video, and the relief of the tracks: mountains
	 * seen from far get a finer magnifier (relief shading), streets keep 100% (smaller ones would show the next
	 * zoom's data). A number as {@code detail} and {@code map_magnifier} still set
	 * them by hand.
	 */
	@NonNull
	private DetailChoice chooseDetail(@NonNull MapFlyoverParams params, @NonNull JSONObject spec,
	                                  @NonNull List<FlyoverTrack> tracks, @Nullable List<FlyoverTrack> followed,
	                                  @Nullable JSONObject follow, int frameMin, float tilePx) {
		Float manualMagnifier = spec.has("map_magnifier")
				? (float) Math.max(0.25, Math.min(4, spec.optDouble("map_magnifier"))) : null;
		Object value = spec.opt("detail");
		if (value instanceof Number) {
			return new DetailChoice(((Number) value).floatValue(), manualMagnifier, null);
		}
		String level = value instanceof String ? (String) value : "normal";
		int step = "low".equals(level) ? 0 : "high".equals(level) ? 2 : 1;
		// the camera over the video, sampled
		double zoom = 0, tilt = 0;
		int samples = 20;
		double videoS = getVideoDuration(params, spec);
		for (int k = 0; k < samples; k++) {
			double t = videoS * (k + 0.5) / samples;
			double[] camera = drone != null ? drone.roughCamera(t)
					: followed != null && follow != null ? followCamera(followed, follow, t, frameMin, tilePx)
					: keyframeCamera(params, spec, (long) (t * 1000));
			zoom += camera[2] / samples;
			tilt += camera[4] / samples;
		}
		Boolean mountains = null;
		List<FlyoverTrack> heightTracks = drone != null
				? (drone.track != null ? Arrays.asList(drone.track) : new ArrayList<>()) : followed != null ? followed : tracks;
		for (FlyoverTrack track : heightTracks) {
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (WptPt p : track.points) {
				if (!Double.isNaN(p.getEle())) {
					min = Math.min(min, p.getEle());
					max = Math.max(max, p.getEle());
				}
			}
			if (max >= min) {
				mountains = (mountains != null && mountains) || max - min > MOUNTAIN_RISE_M;
			}
		}
		float detailedDistance = step == 0 ? 1 : step == 1 ? DEFAULT_DETAIL : DEFAULT_DETAIL + 1;
		// finer to coarser
		float[] steps = {0.6f, 0.75f, 0.85f, 1f};
		int m;
		if (step == 0 || zoom >= STREET_ZOOM) {
			m = 3;
		} else if (zoom <= OVERVIEW_ZOOM && (mountains == null || mountains)) {
			// seen from far: relief unless the tracks say it is flat; tracks without heights count as relief
			m = mountains != null ? (step == 1 ? 1 : 0) : (step == 1 ? 2 : 1);
		} else {
			m = step == 1 ? 2 : 1;
		}
		float magnifier = manualMagnifier != null ? manualMagnifier : steps[m];
		String note = String.format(Locale.US, "detail %s: map magnifier %d%%, detailed distance %.0f (camera zoom %.1f, tilt %.0f, %s)",
				level, Math.round(magnifier * 100), detailedDistance, zoom, tilt,
				mountains == null ? "no heights on the tracks" : mountains ? "mountains" : "flat");
		return new DetailChoice(detailedDistance, magnifier != 1f ? magnifier : null, note);
	}

	@Nullable
	private String validate(@NonNull MapFlyoverParams p, @NonNull JSONObject spec) {
		int n = p.getLatitudes().length;
		JSONObject camera = spec.optJSONObject("camera");
		if (n == 0 && camera != null && (camera.has("follow") || camera.has("drone") || camera.has("poses"))) {
			double d = getVideoDuration(p, spec);
			return d > 0 && d <= 600 ? checkVideo(p)
					: "A video that follows a track or a drone needs duration_s (or the last pose's time_s), 0-600";
		}
		if (n == 0) {
			return "Give key frames, camera.follow or camera.drone with duration_s, or camera.poses";
		}
		if (p.getLongitudes().length != n || p.getZooms().length != n || p.getAzimuths().length != n
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

	/** Where the videos are kept, under OsmAnd's own folder. */
	@NonNull
	public static File getFolder(@NonNull OsmandApplication app) {
		return app.getAppPath(FOLDER);
	}

	/** The recorded videos on the phone, newest first. */
	@NonNull
	public static List<File> listVideos(@NonNull OsmandApplication app) {
		File[] files = getFolder(app).listFiles((dir, name) -> name.toLowerCase(Locale.US).endsWith(".mp4"));
		List<File> videos = new ArrayList<>();
		if (files != null) {
			videos.addAll(Arrays.asList(files));
			videos.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
		}
		return videos;
	}

	@NonNull
	private File getFile(@Nullable String name) {
		File dir = getFolder(app);
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
	private void takeRendererOver() throws InterruptedException {
		int width = renderWidth, height = renderHeight;
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
			// how far from the camera a tilted map keeps tiles of full detail; the phone's 0.5 blurs the middle
			// of a 3D video, where the eye looks
			view.setDetailedDistance(detail);
			view.setFlatEarth(!app.getSettings().SPHERICAL_MAP.get());
			view.removeAllSymbolsProviders();
			view.resumeSymbolsUpdate();
			context.setMapRendererView(view);
			mapView.setMapRenderer(view, false);
			app.getOsmandMap().getMapLayers().updateMapSource(mapView, null);
			PluginsHelper.refreshLayers(app, null);
			view.addListener(frameListener);
			result[0] = view;
		});
		if (result[0] == null) {
			throw new IllegalStateException("The OpenGL renderer is not ready");
		}
		view = result[0];
	}

	/**
	 * A MapActivity created during the recording (the phone turned, the theme changed) takes the renderer for its
	 * map, as it does after Android Auto; our view then renders nothing. The recording takes it back, with the
	 * video's size and density, and keeps its symbol settings; the phone's map gets it again at the end.
	 */
	private boolean ensureRenderer() throws InterruptedException {
		AtlasMapRendererView current = view;
		if (current == null || mapView.getMapRenderer() == current) {
			return false;
		}
		LOG.info("Flyover: the app took the renderer over, taking it back");
		current.removeListener(frameListener);
		takeRendererOver();
		AtlasMapRendererView next = view;
		if (next != null) {
			next.setSymbolsUpdateInterval(Integer.MAX_VALUE);
		}
		appliedCamera = null;
		return true;
	}

	private void giveRendererBack(@NonNull AtlasMapRendererView view) {
		view.removeListener(frameListener);
		// the suspension and the interval live in the renderer, which goes back to the phone's map
		while (view.isSymbolsUpdateSuspended()) {
			view.resumeSymbolsUpdate();
		}
		view.setSymbolsUpdateInterval(MapViewWithLayers.SYMBOLS_UPDATE_INTERVAL);
		view.setSymbolsFade(0, -1);
		if (drone != null) {
			view.setFieldOfView(FlyoverDroneCamera.ENGINE_FOV / 2);
			view.setIgnoreTargetElevation(false);
			if (!Float.isNaN(elevationScaleBefore)) {
				view.setElevationScaleFactor(elevationScaleBefore);
			}
		}
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
	private static final float DEFAULT_DETAIL = 3f;
	// the camera's zoom over the video: up to this the relief fills the frame, from the next one streets do
	private static final double OVERVIEW_ZOOM = 14.5;
	private static final double STREET_ZOOM = 15.5;
	// tracks climbing or descending more than this are in the mountains
	private static final double MOUNTAIN_RISE_M = 300;
	private static final int PREVIEW_PER_S = 5;
	private static final int DRONE_WARMUP_FRAMES = 8;
	private static final int DRONE_CORRECTIONS = 6;
	private static final long FRAMES_TIMEOUT_MS = 2000;
	private static final float RELIEF_LIFT_SMOOTHING = 0.35f;

	private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("keyframes", "width", "height", "fps",
			"name", "camera", "duration_s", "gpx", "osmand_widgets", "attribution", "profile", "scale",
			"ease_in_s", "ease_out_s", "panels", "data", "ui_scale", "load_timeout_s", "smooth_s", "refresh_s", "detail", "map_magnifier"));

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
		if (times.length > 0) {
			return times[times.length - 1] / 1000.0;
		}
		JSONObject camera = spec.optJSONObject("camera");
		JSONArray poses = camera != null ? camera.optJSONArray("poses") : null;
		if (!spec.has("duration_s") && poses != null && poses.length() > 0 && poses.optJSONObject(poses.length() - 1) != null) {
			return poses.optJSONObject(poses.length() - 1).optDouble("time_s", poses.length() - 1);
		}
		return spec.optDouble("duration_s");
	}

	/** The camera of the frame at video time tMs: drone, track following or key frames. */
	@NonNull
	private double[] cameraAt(@NonNull MapFlyoverParams params, @NonNull JSONObject spec,
	                          @Nullable List<FlyoverTrack> followed, @Nullable JSONObject follow, long tMs,
	                          int frameMin, float tilePx) {
		if (drone != null) {
			return droneZoomBand(drone.camera(tMs / 1000.0));
		}
		return followed != null && follow != null ? followCamera(followed, follow, tMs / 1000.0, frameMin, tilePx)
				: keyframeCamera(params, spec, tMs);
	}

	@Nullable
	private double[] droneCorrection(boolean last) throws InterruptedException {
		FlyoverDroneCamera drone = this.drone;
		if (drone == null) {
			return null;
		}
		double[][] result = new double[1][];
		runInUiThread(() -> {
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer != null) {
				result[0] = drone.correction(renderer, last);
			}
		});
		return result[0];
	}

	/**
	 * The engine changes the map's detail, and moves a camera over the relief by a step, where the zoom passes a whole
	 * level. A drone camera keeps a level (its middle, n.5) and gets the rest of its distance from the scale of the
	 * frame; the level changes only when that scale leaves 2^-band..2^band.
	 */
	@NonNull
	private double[] droneZoomBand(@NonNull double[] camera) {
		FlyoverDroneCamera drone = this.drone;
		if (drone == null) {
			return camera;
		}
		double zoom = camera[2];
		if (Double.isNaN(droneLevel)) {
			droneLevel = Math.floor(zoom) + 0.5;
		}
		float density = app.getOsmandMap().getMapDensity();
		double[] fitted = camera.clone();
		for (int k = 0; k < 4; k++) {
			double level = Math.min(droneLevel, mapView.getMaxZoom());
			fitted = camera.clone();
			drone.fit(fitted, level, density, true);
			// far from 1 the engine answers the scale unevenly: then the next level, which needs about twice less
			double steps = Math.log(fitted[7]) / Math.log(2);
			if (Math.abs(steps) <= FlyoverDroneCamera.SCALE_BAND || (steps < 0 && level >= mapView.getMaxZoom())) {
				break;
			}
			droneLevel += Math.round(steps);
		}
		return fitted;
	}

	/** After a drone frame: how high the engine's camera really is; the first frames teach the zoom to distance. */
	private void measureDrone(boolean learn) throws InterruptedException {
		FlyoverDroneCamera drone = this.drone;
		if (drone == null) {
			return;
		}
		runInUiThread(() -> {
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer != null) {
				drone.measure(renderer, learn);
			}
		});
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
		// averaged over smooth_s of video time with triangular weights: corners of the track and GPS noise
		// do not jerk the center, the heading or the zoom
		double window = camera.optDouble("smooth_s", 2);
		double baseZoom = camera.optDouble("zoom", 15);
		double minZoom = camera.optDouble("min_zoom", 10);
		double lat = 0, lon = 0, sin = 0, cos = 0, zoom = 0, weights = 0;
		// still until the first followed track starts moving: averaging over a window would otherwise start the
		// camera before anything is there to follow
		double firstStart = Double.MAX_VALUE;
		for (FlyoverTrack track : tracks) {
			firstStart = Math.min(firstStart, track.getStartS());
		}
		t = Math.max(t, firstStart);
		// samples every 1/60 s of video: sparse ones beat with a fast-forwarded zigzag and shook the camera
		int half = Math.max(6, (int) Math.round(window * 30));
		for (int k = -half; k <= half; k++) {
			double tk = t + k * window / (2 * half);
			double w = half + 1 - Math.abs(k);
			double kLat = 0, kLon = 0;
			List<FlyoverTrack.Position> here = new ArrayList<>();
			for (FlyoverTrack track : tracks) {
				FlyoverTrack.Position p = track.at(tk);
				here.add(p);
				kLat += p.lat;
				kLon += p.lon;
				double b = Math.toRadians(track.bearing(tk, 1, ahead));
				sin += w * Math.sin(b);
				cos += w * Math.cos(b);
			}
			kLat /= tracks.size();
			kLon /= tracks.size();
			double spread = 0;
			for (FlyoverTrack.Position p : here) {
				spread = Math.max(spread, MapUtils.getDistance(kLat, kLon, p.lat, p.lon));
			}
			double kZoom = baseZoom;
			if (tracks.size() > 1 && spread > 1) {
				// every followed track stays within about a third of the frame from its center
				double fit = Math.log(0.35 * frameMin * 40075016 * Math.cos(Math.toRadians(kLat)) / (tilePx * spread)) / Math.log(2);
				kZoom = Math.max(minZoom, Math.min(baseZoom, fit));
			}
			lat += w * kLat;
			lon += w * kLon;
			zoom += w * kZoom;
			weights += w;
		}
		lat /= weights;
		lon /= weights;
		zoom /= weights;
		double heading = Math.toDegrees(Math.atan2(sin, cos));
		// OsmAnd turns the map by minus the heading so that the way ahead is up
		JSONArray target = camera.optJSONArray("target");
		return new double[] {lat, lon, zoom, -heading, camera.optDouble("tilt", 50),
				target != null ? target.optDouble(0) : Double.NaN, target != null ? target.optDouble(1) : Double.NaN};
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
		// "target": [x, y], where the camera's point is on the frame (fractions); NaN = OsmAnd's own map ratio
		JSONArray frames = spec.optJSONArray("keyframes");
		double[] targetX = new double[n], targetY = new double[n];
		boolean hasTarget = false;
		for (int k = 0; k < n; k++) {
			JSONArray target = frames != null && frames.optJSONObject(k) != null
					? frames.optJSONObject(k).optJSONArray("target") : null;
			if (target != null && target.length() == 2) {
				targetX[k] = target.optDouble(0);
				targetY[k] = target.optDouble(1);
				hasTarget = true;
			} else {
				targetX[k] = k > 0 ? targetX[k - 1] : Double.NaN;
				targetY[k] = k > 0 ? targetY[k - 1] : Double.NaN;
			}
		}
		if (hasTarget) {
			// a key frame before the first target uses that target
			for (int k = n - 2; k >= 0; k--) {
				if (Double.isNaN(targetX[k])) {
					targetX[k] = targetX[k + 1];
					targetY[k] = targetY[k + 1];
				}
			}
		}
		return new double[] {hermite(ts, lat, t), hermite(ts, lon, t),
				Math.max(1, Math.min(22, hermite(ts, zoom, t))), hermite(ts, rotation, t),
				Math.max(10, Math.min(90, hermite(ts, tilt, t))),
				hasTarget ? hermite(ts, targetX, t) : Double.NaN, hasTarget ? hermite(ts, targetY, t) : Double.NaN};
	}

	/** Cubic Hermite through (ts, values) with Catmull-Rom tangents per second: smooth speed across key frames. */
	static double hermite(@NonNull double[] ts, @NonNull double[] values, double t) {
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
	static double easeTime(double t, double total, double easeIn, double easeOut) {
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
		double targetX = camera.length > 6 ? camera[5] : Double.NaN, targetY = camera.length > 6 ? camera[6] : Double.NaN;
		float fZoom = (float) camera[2], fAzimuth = (float) camera[3], fElevation = (float) camera[4];
		// a drone camera: the scale of the frame, its field of view
		float extraScale = camera.length > 7 ? (float) camera[7] : 1f;
		float fov = camera.length > 8 ? (float) camera[8] : Float.NaN;
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
				renderer.setVisualZoomShift(app.getOsmandMap().getMapDensity() * extraScale - 1.0f);
				// the map view keeps whole degrees of tilt: the renderer gets the exact one
				renderer.setElevationAngle(fElevation);
				if (!Float.isNaN(fov)) {
					renderer.setFieldOfView(fov / 2);
					renderer.setIgnoreTargetElevation(true);
					// heights to scale: an exaggerated relief would rise into the drone's way
					if (Float.isNaN(elevationScaleBefore)) {
						elevationScaleBefore = renderer.getElevationScaleFactor();
					}
					renderer.setElevationScaleFactor(1f);
				}
			}
			if (renderer != null && renderWidth > 0) {
				PointF ratio = !Double.isNaN(targetX) && !Double.isNaN(targetY) ? new PointF((float) targetX, (float) targetY)
						: app.getMapViewTrackingUtilities().getMapDisplayPositionManager().getMapRatio();
				renderer.setMapTarget(new PointI(Math.round(ratio.x * renderWidth), Math.round(ratio.y * renderHeight)),
						NativeUtilities.calculateTarget31(renderer, fLat, fLon, false));
			}
			if (beforeLayers != null) {
				beforeLayers.run();
			}
			boolean night = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
			mapView.drawOverMap(layersCanvas, mapView.getRotatedTileBox(), new DrawSettings(night, false));
			appliedCamera = cameraState();
		});
	}

	/** The map view's camera as it is now: center, zoom, rotation, tilt. On the UI thread. */
	@NonNull
	private double[] cameraState() {
		return new double[] {mapView.getLatitude(), mapView.getLongitude(), mapView.getZoom() + mapView.getZoomFloatPart(),
				mapView.getRotate(), mapView.getElevationAngle()};
	}

	/**
	 * Whether something else moved the map since applyCamera, e.g. a button or a menu of the app (the user may use the
	 * app while a video is recorded, only the map itself takes no touches).
	 */
	private boolean cameraMoved() throws InterruptedException {
		boolean[] moved = new boolean[1];
		runInUiThread(() -> {
			double[] now = cameraState();
			double[] was = appliedCamera;
			moved[0] = was != null && (Math.abs(now[0] - was[0]) > 1e-7 || Math.abs(now[1] - was[1]) > 1e-7
					|| Math.abs(now[2] - was[2]) > 1e-4 || Math.abs(MapUtils.unifyRotationDiff((float) now[3], (float) was[3])) > 0.01
					|| Math.abs(now[4] - was[4]) > 0.01);
		});
		return moved[0];
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
	private Map<WptPt, PointF> project(@NonNull List<WptPt> points, @Nullable List<?> keys, int supersampling,
	                                   int width, int height) throws InterruptedException {
		Map<WptPt, PointF> result = new HashMap<>();
		if (points.isEmpty()) {
			return result;
		}
		runInUiThread(() -> {
			MapRendererView renderer = mapView.getMapRenderer();
			if (renderer == null) {
				return;
			}
			for (int k = 0; k < points.size(); k++) {
				WptPt w = points.get(k);
				Object key = keys != null ? keys.get(k) : w;
				PointI out = new PointI();
				PointI flat = new PointI();
				PointI location = new PointI(MapUtils.get31TileNumberX(w.getLon()), MapUtils.get31TileNumberY(w.getLat()));
				if (renderer.getElevatedPointFromLocation(location, out, true)
						&& renderer.getScreenPointFromLocation(location, flat, true)) {
					// the relief under a point is sometimes missing for a frame (its tile is being replaced), and the
					// point falls to sea level: then the offset the relief gave it in the last frames is kept
					PointF lift = new PointF(out.getX() - flat.getX(), out.getY() - flat.getY());
					PointF last = reliefLift.get(key);
					if (Math.abs(lift.x) < 0.5f && Math.abs(lift.y) < 0.5f && last != null
							&& (Math.abs(last.x) > 2 || Math.abs(last.y) > 2)) {
						lift = last;
					} else if (last != null) {
						// relief of another detail level from frame to frame: averaged, so the point does not shake
						lift = new PointF(last.x + (lift.x - last.x) * RELIEF_LIFT_SMOOTHING,
								last.y + (lift.y - last.y) * RELIEF_LIFT_SMOOTHING);
					}
					reliefLift.put(key, lift);
					float x = (flat.getX() + lift.x) / supersampling, y = (flat.getY() + lift.y) / supersampling;
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
			// the user's selection first: a file given twice would otherwise record our own selection as his
			for (FlyoverTrack track : tracks) {
				SelectedGpxFile before = helper.getSelectedFileByPath(track.file.getAbsolutePath());
				if (before != null) {
					shownBefore.put(track.file.getAbsolutePath(), before);
				}
			}
			for (FlyoverTrack track : tracks) {
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
					if (track.selected != null) {
						track.selected.setStartFinishSegments(null);
					}
					SelectedGpxFile before = shownBefore.get(track.file.getAbsolutePath());
					if (before != null) {
						helper.selectGpxFile(before.getGpxFile(), GpxSelectionParams.newInstance().showOnMap());
					} else {
						helper.selectGpxFile(track.gpx, GpxSelectionParams.newInstance().hideFromMap());
					}
					if (track.temporary) {
						// a GPX passed in the call: not kept on the phone, nor in the track database
						app.getGpxDbHelper().remove(SharedUtil.kFile(track.file));
						track.file.delete();
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

	/** Frames of the camera just set, the map loaded or not: only the camera is read from them; at most 2 s. */
	private void waitForFrames(@NonNull MapRendererView view, int count, long timeoutMs) throws InterruptedException {
		long until = SystemClock.elapsedRealtime() + Math.min(timeoutMs, FRAMES_TIMEOUT_MS);
		int startFrame = view.getFrameId();
		int startReady = readyFrames.get();
		while (SystemClock.elapsedRealtime() < until) {
			if (cancelled) {
				throw new CancellationException("Cancelled on the phone");
			}
			view.requestRender();
			Thread.sleep(10);
			if (view.getFrameId() >= startFrame + count && readyFrames.get() >= startReady + count) {
				return;
			}
		}
	}

	/**
	 * Waits for a frame rendered after the renderer became idle, i.e. with all map data and symbols in place.
	 */
	private boolean waitForIdleFrame(@NonNull MapRendererView view, long timeoutMs) throws InterruptedException {
		long until = SystemClock.elapsedRealtime() + timeoutMs;
		int startFrame = view.getFrameId();
		int idleFrame = -1;
		int idleReady = 0;
		while (SystemClock.elapsedRealtime() < until) {
			if (cancelled) {
				throw new CancellationException("Cancelled on the phone");
			}
			view.requestRender();
			Thread.sleep(10);
			int frame = view.getFrameId();
			if (frame <= startFrame) {
				continue;
			}
			if (view.isIdle()) {
				if (idleFrame < 0) {
					idleFrame = frame;
					idleReady = readyFrames.get();
				} else if (frame > idleFrame && readyFrames.get() > idleReady) {
					// a frame rendered while idle has been read out: getBitmap gives this camera, not the last one
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

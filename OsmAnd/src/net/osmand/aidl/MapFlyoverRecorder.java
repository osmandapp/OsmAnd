package net.osmand.aidl;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.aidlapi.map.MapFlyoverParams;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.MapViewWithLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
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
		String error = validate(params);
		if (error != null) {
			params.setError(error);
			return false;
		}
		long start = SystemClock.elapsedRealtime();
		File file = getFile(params.getFileName());
		AtlasMapRendererView view = null;
		VideoEncoder encoder = null;
		int notIdleFrames = 0;
		try {
			// rendered at twice the size and scaled down: thin lines and text edges stop shimmering in motion
			int scale = params.getWidth() * SUPERSAMPLING <= MAX_SIZE && params.getHeight() * SUPERSAMPLING <= MAX_SIZE ? SUPERSAMPLING : 1;
			view = takeRendererOver(params.getWidth() * scale, params.getHeight() * scale);
			encoder = new VideoEncoder(file, params.getWidth(), params.getHeight(), params.getFps());
			long[] times = params.getTimesMs();
			long durationMs = times[times.length - 1];
			int frames = (int) (durationMs * params.getFps() / 1000) + 1;
			// the map moves across the screen at most this fast: above ~250 px/s sharp lines strobe
			float pxPerTile = 256 * Math.max(1f, Math.max(params.getWidth(), params.getHeight()) / 640f);
			double maxSpeed = 0;
			double[] prev = null;
			// Labels and icons are laid out again only every SYMBOLS_UPDATE_MS of video time and move with the map
			// in between, as on the moving map. The engine would also lay them out after 1 s of real time, which
			// is a few frames here, so that timer is switched off while the symbols are suspended.
			view.setSymbolsUpdateInterval(Integer.MAX_VALUE);
			int symbolsEvery = Math.max(1, (int) (SYMBOLS_UPDATE_MS * params.getFps() / 1000));
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
				double[] camera = setCamera(params, t);
				if (prev != null) {
					maxSpeed = Math.max(maxSpeed, screenDistance(prev, camera, pxPerTile) * params.getFps());
				}
				prev = camera;
				if (!waitForIdleFrame(view, i == 0 ? FIRST_FRAME_TIMEOUT_MS : FRAME_TIMEOUT_MS)) {
					notIdleFrames++;
					LOG.warn("Flyover frame " + i + " is not idle: " + view.getNotIdleReason());
				}
				Bitmap bitmap = view.getBitmap();
				if (bitmap == null) {
					throw new IllegalStateException("The renderer gave no picture for frame " + i);
				}
				encoder.encode(bitmap, i * 1_000_000L / params.getFps());
			}
			encoder.finish();
			encoder = null;
			params.setFilePath(file.getAbsolutePath());
			params.setFrames(frames);
			params.setMaxScreenSpeed((float) maxSpeed);
			params.setRenderTimeMs(SystemClock.elapsedRealtime() - start);
			LOG.info("Flyover " + file + ": " + frames + " frames in " + params.getRenderTimeMs()
					+ " ms, not idle " + notIdleFrames);
			return true;
		} catch (Exception e) {
			LOG.error("Flyover failed", e);
			params.setError(e.getMessage() != null ? e.getMessage() : e.toString());
			return false;
		} finally {
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
	private String validate(@NonNull MapFlyoverParams p) {
		int n = p.getLatitudes().length;
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
	private AtlasMapRendererView takeRendererOver(int width, int height) throws InterruptedException {
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
			// the symbols are sized for a frame about 640 dp wide
			NativeCoreContext.setMapRendererContext(app, Math.max(1f, Math.max(width, height) / 640f));
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

	/** Camera at time t, set on the map; returns {lat, lon, zoom}. */
	@NonNull
	private double[] setCamera(@NonNull MapFlyoverParams p, long t) throws InterruptedException {
		long[] times = p.getTimesMs();
		int n = times.length;
		int i = 0;
		while (i < n - 2 && t > times[i + 1]) {
			i++;
		}
		double lat, lon;
		float zoom, azimuth, elevation;
		if (n == 1) {
			lat = p.getLatitudes()[0];
			lon = p.getLongitudes()[0];
			zoom = p.getZooms()[0];
			azimuth = p.getAzimuths()[0];
			elevation = p.getElevationAngles()[0];
		} else {
			float s = Math.max(0f, Math.min(1f, (float) (t - times[i]) / (times[i + 1] - times[i])));
			int i0 = Math.max(i - 1, 0);
			int i3 = Math.min(i + 2, n - 1);
			// the position follows a Catmull-Rom curve through the key frames, so it does not turn sharply at them
			lat = catmullRom(p.getLatitudes()[i0], p.getLatitudes()[i], p.getLatitudes()[i + 1], p.getLatitudes()[i3], s);
			lon = catmullRom(p.getLongitudes()[i0], p.getLongitudes()[i], p.getLongitudes()[i + 1], p.getLongitudes()[i3], s);
			float e = smoothStep(s);
			zoom = lerp(p.getZooms()[i], p.getZooms()[i + 1], e);
			float from = p.getAzimuths()[i];
			azimuth = from + (float) MapUtils.unifyRotationDiff(from, p.getAzimuths()[i + 1]) * e;
			elevation = lerp(p.getElevationAngles()[i], p.getElevationAngles()[i + 1], e);
		}
		double fLat = lat, fLon = lon;
		float fZoom = zoom, fAzimuth = azimuth, fElevation = elevation;
		runInUiThread(() -> {
			// immediate setters: the animated ones would run on their own clock, not the video's
			mapView.getAnimatedDraggingThread().stopAnimating();
			mapView.rotateToAnimate(fAzimuth);
			mapView.setElevationAngle(fElevation);
			mapView.setLatLon(fLat, fLon);
			int base = (int) Math.floor(fZoom);
			mapView.setZoomWithFloatPart(base, fZoom - base);
		});
		return new double[] {lat, lon, zoom};
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

	private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
		double t2 = t * t;
		double t3 = t2 * t;
		return 0.5 * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (3 * p1 - p0 - 3 * p2 + p3) * t3);
	}

	private static float smoothStep(float t) {
		return t * t * (3 - 2 * t);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
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

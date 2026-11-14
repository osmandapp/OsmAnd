package net.osmand.plus.plugins.topoprint.render;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.topoprint.MapProjection;
import net.osmand.plus.plugins.topoprint.PrintParameters;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

/**
 * Renders the map off-screen through the software rasterizer
 * ({@link MapRenderRepositories}) — the same rendering core and style the app
 * uses, at print scale.
 *
 * Mirrors the EyePiece CLI parameterization
 * (topo_map_generator.py:1116-1141): the style stays laid out for a fixed
 * 300 dpi reference view, so roads/labels/icons keep their physical size;
 * a higher target dpi is pure supersampling via an enlarged map density.
 * The fractional zoom of the formula is decomposed into an integer tileBox
 * zoom plus the remaining factor in {@code mapDensity}.
 *
 * The rendered bitmap is north-up and aligned to the UTM axes; its geographic
 * coverage is described by the returned {@link MapGeoref} (true lat/lon of the
 * four raster corners), which replaces the EyePiece pipeline's
 * {@code .georef.json} in the SVG composition step.
 */
public class OffscreenMapRenderer {

	/** Web-mercator world circumference in meters (WGS84 equator). */
	private static final double WORLD_METERS = 40075016.686;
	private static final int BASE_TILE_SIZE = 256;
	/**
	 * Reference dpi the rendering style is designed for — the anchor that keeps
	 * physical sizes of roads/labels/icons stable across dpi choices.
	 */
	public static final double STYLE_DPI = 300.0;
	/**
	 * Offscreen raster resolution cap. Print DPI above this only sharpens the
	 * vector grid/labels/text; the embedded basemap raster is rendered at this
	 * cap because full-resolution offscreen bitmaps exceed device memory
	 * (1200 dpi A4 ARGB alone is ~556 MB). The SVG stays resolution-
	 * independent, so a PC can rasterize the print at the full DPI.
	 */
	public static final double MAX_RENDER_DPI = 600.0;

	private final OsmandApplication app;
	/** Set by {@link #cancel()}; polled by the render pipeline at stage boundaries. */
	private volatile boolean cancelled;

	public OffscreenMapRenderer(@NonNull OsmandApplication app) {
		this.app = app;
	}

	/**
	 * Requests cancellation: the render aborts at the next stage boundary and
	 * delivers a null result — the caller turns that into onCancelled. Safe
	 * from any thread, idempotent.
	 */
	public void cancel() {
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/** Geographic coverage of the rendered raster: true lat/lon of its corners. */
	public static class MapGeoref {

		public final LatLon topLeft;
		public final LatLon topRight;
		public final LatLon bottomRight;
		public final LatLon bottomLeft;
		public final int widthPx;
		public final int heightPx;

		MapGeoref(@NonNull LatLon topLeft, @NonNull LatLon topRight,
		          @NonNull LatLon bottomRight, @NonNull LatLon bottomLeft,
		          int widthPx, int heightPx) {
			this.topLeft = topLeft;
			this.topRight = topRight;
			this.bottomRight = bottomRight;
			this.bottomLeft = bottomLeft;
			this.widthPx = widthPx;
			this.heightPx = heightPx;
		}
	}

	public static class RenderResult {

		public final Bitmap bitmap;
		public final MapGeoref georef;

		RenderResult(@NonNull Bitmap bitmap, @NonNull MapGeoref georef) {
			this.bitmap = bitmap;
			this.georef = georef;
		}
	}

	public interface RenderListener {
		/**
		 * @param result null when the render was interrupted or the requested
		 *               raster size exceeds the device's memory budget
		 */
		void onRendered(@Nullable RenderResult result);
	}

	/** Optional stage reporter; implementations run on the UI thread. */
	public interface ProgressListener {
		void onProgress(@NonNull String message);
	}

	/**
	 * Renders a north-up, UTM-aligned raster covering
	 * {@code widthMeters x heightMeters} centered at {@code center}.
	 *
	 * @param center       center of the coverage (map center)
	 * @param widthMeters  east-west extent in ground meters (UTM aligned)
	 * @param heightMeters north-south extent in ground meters
	 * @param progress     optional stage reporter (initialization, frame
	 *                     settling, capture), called on the UI thread
	 * @param listener     called on the UI thread when rendering finishes
	 */
	public void render(@NonNull PrintParameters params, @NonNull LatLon center,
	                   double widthMeters, double heightMeters,
	                   @Nullable ProgressListener progress, @NonNull RenderListener listener) {
		boolean spherical = params.getProjection() == MapProjection.SPHERICAL;
		if (cancelled) {
			app.runInUIThread(() -> listener.onRendered(null));
			return;
		}
		if (app.useOpenGlRenderer() && NativeCoreContext.getMapRendererContext() != null) {
			// The whole GL lifecycle runs on the dedicated TopoPrintGL thread.
			GLOffscreenRenderer glRenderer = new GLOffscreenRenderer(app, params, center,
					widthMeters, heightMeters, spherical, progress, listener, this::isCancelled);
			glRenderer.start();
			return;
		}
		if (spherical) {
			// The software rasterizer cannot render the spherical projection.
			app.runInUIThread(() -> listener.onRendered(null));
			return;
		}
		renderSoft(params, center, widthMeters, heightMeters, listener);
	}

	private void renderSoft(@NonNull PrintParameters params, @NonNull LatLon center,
	                        double widthMeters, double heightMeters, @NonNull RenderListener listener) {
		double latitude = center.getLatitude();
		int widthPx = (int) Math.ceil(widthMeters / groundMetersPerPixel(params));
		int heightPx = (int) Math.ceil(heightMeters / groundMetersPerPixel(params));
		if (!isBitmapSizeAllowed(app, widthPx, heightPx)) {
			app.runInUIThread(() -> listener.onRendered(null));
			return;
		}

		double zoomFractional = fractionalZoom(params, latitude);
		int zoom = (int) Math.floor(zoomFractional);
		double supersampling = renderDpi(params) / STYLE_DPI;
		double mapDensity = Math.pow(2, zoomFractional - zoom) * supersampling;
		// The software renderer divides the text scale by mapDensity; boost the
		// display density by the same factor so labels stay at their anchored
		// physical size (MapRenderRepositories: textScale = density·scale/mapDensity).
		double density = mapDensity * supersampling;

		RotatedTileBox tileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(latitude, center.getLongitude())
				.setZoom(zoom)
				.density((float) density)
				.setMapDensity(mapDensity)
				.setPixelDimensions(widthPx, heightPx, 0.5f, 0.5f)
				.build();

		// Maps are always printed with the day style, like the CLI pipeline.
		DrawSettings drawSettings = new DrawSettings(false, true);
		ResourceManager resourceManager = app.getResourceManager();
		MapRenderRepositories renderer = resourceManager.getRenderer();
		resourceManager.updateRendererMap(tileBox, interrupted -> {
			if (interrupted) {
				// The bitmap, if any, no longer matches this tile box — its
				// georeference would be wrong. Report failure instead.
				app.runInUIThread(() -> listener.onRendered(null));
				return;
			}
			Bitmap bitmap = renderer.getBitmap();
			if (bitmap == null) {
				app.runInUIThread(() -> listener.onRendered(null));
				return;
			}
			MapGeoref georef = createGeoref(tileBox, widthPx, heightPx);
			app.runInUIThread(() -> listener.onRendered(new RenderResult(bitmap, georef)));
		}, true);
	}

	@NonNull
	private static MapGeoref createGeoref(@NonNull RotatedTileBox tileBox, int widthPx, int heightPx) {
		return new MapGeoref(
				tileBox.getLatLonFromPixel(0, 0),
				tileBox.getLatLonFromPixel(widthPx, 0),
				tileBox.getLatLonFromPixel(widthPx, heightPx),
				tileBox.getLatLonFromPixel(0, heightPx),
				widthPx, heightPx);
	}

	/**
	 * Ground meters per raster pixel at the selected scale and DPI:
	 * (N/1000 ground meters per paper mm) / (dpi/25.4 paper pixels per mm).
	 */
	/** Render DPI for the offscreen basemap, capped at [MAX_RENDER_DPI]. */
	public static double renderDpi(@NonNull PrintParameters params) {
		return Math.min(params.getDpi(), MAX_RENDER_DPI);
	}

	public static double groundMetersPerPixel(@NonNull PrintParameters params) {
		return 25.4 * params.getScaleDenominator() / (1000.0 * renderDpi(params));
	}

	/**
	 * Fractional web-mercator zoom that renders the style's reference 300 dpi
	 * view, identical to the EyePiece CLI formula:
	 * {@code log2(W·cosφ / (256 · style_m_per_px))} with
	 * {@code style_m_per_px = 25.4 · N / (1000 · styleDpi)}.
	 */
	public static double fractionalZoom(@NonNull PrintParameters params, double latitudeDegrees) {
		double styleMetersPerPixel = 25.4 * params.getScaleDenominator() / (1000.0 * STYLE_DPI);
		return Math.log(WORLD_METERS * Math.cos(Math.toRadians(latitudeDegrees))
				/ (BASE_TILE_SIZE * styleMetersPerPixel)) / Math.log(2);
	}

	/**
	 * Refuse early with a hint to lower the DPI instead of OOMing mid-render.
	 * <p>
	 * The budget is checked against really available RAM, not the Java
	 * {@code memoryClass}: on Android 8+ bitmap pixels are allocated in the
	 * native heap (like the GL framebuffer and the frame-reader copy), so the
	 * Java heap limit does not bound them — containers and largeHeap devices
	 * were rejected far too eagerly. Three bitmaps coexist at capture time
	 * (EGL surface + frame-reader copy + delivered bitmap), so the peak is
	 * budgeted at 3× one bitmap, capped at half of the available memory.
	 */
	public static boolean isBitmapSizeAllowed(@NonNull OsmandApplication app, int widthPx, int heightPx) {
		ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
		long bytes = (long) widthPx * heightPx * 4;
		long peakBytes = bytes * 3;
		long maxBytes = 512L * 1024 * 1024;
		if (am != null) {
			ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(info);
			long availBudget = (long) (info.availMem * 0.5);
			maxBytes = Math.max(maxBytes, Math.min(availBudget, 4L * 1024 * 1024 * 1024));
			// The frame read-back buffer (one bitmap) is a Java direct buffer
			// bounded by the heap growth limit — keep 55% of the heap for the
			// rest of the app.
			long heapBudget = (long) am.getLargeMemoryClass() * 1024 * 1024 * 45 / 100;
			if (bytes > heapBudget) {
				return false;
			}
		}
		return peakBytes <= maxBytes;
	}
}

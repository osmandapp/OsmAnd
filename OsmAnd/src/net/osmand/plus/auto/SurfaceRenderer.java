package net.osmand.plus.auto;

import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.HostException;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.Location;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.MapRendererView.MapRendererViewListener;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.views.CarSurfaceView;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapTileView.ElevationListener;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

/**
 * A very simple implementation of a renderer for the app's background surface.
 */
public final class SurfaceRenderer implements DefaultLifecycleObserver, MapRendererViewListener, ElevationListener {
	private static final String TAG = "SurfaceRenderer";

	public static final float MIN_ALLOWED_ELEVATION_ANGLE_AA = 30;

	private static final double VISIBLE_AREA_MIN_DETECTION_SIZE = 1.025;
	private static final int MAP_RENDER_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 7;

	private final CarContext carContext;
	private final CarSurfaceView surfaceView;
	private OsmandMapTileView mapView;
	private final Handler handler;

	@Nullable
	private AtlasMapRendererView offscreenMapRendererView;
	@Nullable
	private Surface surface;
	@Nullable
	private SurfaceContainer surfaceContainer;

	@Nullable
	private Rect visibleArea;
	@Nullable
	private Rect stableArea;

	private float cachedRatioX = 0f;
	private float cachedRatioY = 0f;
	private float cachedDefaultRatioY = 0f;


	private boolean darkMode;

	private SurfaceRendererCallback callback;

	public void setCallback(@Nullable SurfaceRendererCallback callback) {
		this.callback = callback;
	}

	public interface SurfaceRendererCallback {
		void onFrameRendered(@NonNull Canvas canvas, @NonNull Rect visibleArea, @NonNull Rect stableArea);
		void onElevationChanging(float angle);
	}

	public final SurfaceCallback mSurfaceCallback = new SurfaceCallback() {
		@Override
		public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Surface available " + surfaceContainer);
				if (surface != null) {
					surface.release();
				}
				SurfaceRenderer.this.surfaceContainer = surfaceContainer;
				surface = surfaceContainer.getSurface();
				surfaceView.setSurfaceParams(surfaceContainer.getWidth(), surfaceContainer.getHeight(), surfaceContainer.getDpi());
				darkMode = carContext.isDarkMode();
				OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
				if (mapView != null) {
					mapView.setupRenderingView();
				}
				renderFrame();
			}
		}

		@Override
		public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Visible area changed " + surface + ". stableArea: "
						+ stableArea + " visibleArea:" + visibleArea);
				SurfaceRenderer.this.visibleArea = visibleArea;
				OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
				if (!visibleArea.isEmpty() && mapView != null) {
					MapDisplayPositionManager displayPositionManager = getDisplayPositionManager();

					int visibleAreaWidth = visibleArea.width();
					int visibleAreaHeight = visibleArea.height();
					int containerWidth = surfaceContainer.getWidth();
					int containerHeight = surfaceContainer.getHeight();

					float ratioX = cachedRatioX;
					if ((float) containerWidth / visibleAreaWidth > VISIBLE_AREA_MIN_DETECTION_SIZE) {
						int centerX = visibleArea.centerX();
						ratioX = (float) centerX / containerWidth;
						cachedRatioX = ratioX;
					}
					float ratioY = cachedRatioY;
					float defaultRatioY = displayPositionManager.getNavigationMapPosition().getRatioY();
					if (defaultRatioY != cachedDefaultRatioY || (float) containerHeight / visibleAreaHeight > VISIBLE_AREA_MIN_DETECTION_SIZE) {
						float centerY = (visibleAreaHeight * defaultRatioY) + visibleArea.top;
						ratioY = centerY / containerHeight;
						cachedRatioY = ratioY;
						cachedDefaultRatioY = defaultRatioY;
					}
					displayPositionManager.setCustomMapRatio(ratioX, ratioY);
				}
				renderFrame();
			}
		}

		@Override
		public void onStableAreaChanged(@NonNull Rect stableArea) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Stable area changed " + surface + ". stableArea: "
						+ stableArea + " visibleArea:" + visibleArea);
				SurfaceRenderer.this.stableArea = stableArea;
				renderFrame();
			}
		}

		@Override
		public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Surface destroyed");
				if (surface != null) {
					surface.release();
					surface = null;
				}
				OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
				if (mapView != null) {
					getDisplayPositionManager().restoreMapRatio();
					mapView.setupRenderingView();
				}
			}
		}

		@Override
		public void onScroll(float distanceX, float distanceY) {
			synchronized (SurfaceRenderer.this) {
				OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
				if (mapView != null) {
					mapView.scrollMap(distanceX, distanceY);
				}
			}
		}

		@Override
		public void onFling(float velocityX, float velocityY) {
			OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
			if (mapView != null) {
				mapView.flingMap(0, 0, velocityX, velocityY);
			}
		}

		@Override
		public void onScale(float focusX, float focusY, float scaleFactor) {
			//TODO handleScale(focusX, focusY, scaleFactor);
		}
	};

	public SurfaceRenderer(@NonNull CarContext carContext, @NonNull Lifecycle lifecycle) {
		this.handler = new Handler();
		this.carContext = carContext;
		this.surfaceView = new CarSurfaceView(carContext, this);
		lifecycle.addObserver(this);
	}

	private void sendRenderFrameMsg() {
		if (!handler.hasMessages(MAP_RENDER_MESSAGE)) {
			Message msg = Message.obtain(handler, () -> {
				handler.removeMessages(MAP_RENDER_MESSAGE);
				renderFrame();
			});
			msg.what = MAP_RENDER_MESSAGE;
			handler.sendMessage(msg);
		}
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		Log.i(TAG, "SurfaceRenderer created");
		try {
			carContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
		} catch (SecurityException | HostException e) {
			Log.e(TAG, "setSurfaceCallback failed ", e);
		}
	}

	/**
	 * Callback called when the car configuration changes.
	 */
	public void onCarConfigurationChanged() {
		renderFrame();
	}

	@Override
	public void onUpdateFrame(MapRendererView mapRendererView) {
	}

	/**
	 * Callback called when OpenGL rendering result is ready and needs to be drawn on output canvas.
	 */
	@Override
	public void onFrameReady(MapRendererView mapRendererView) {
		//renderFrame();
		sendRenderFrameMsg();
	}

	/**
	 * Handles the map zoom-in and zoom-out events.
	 */
	public void handleScale(float focusX, float focusY, float scaleFactor) {
		synchronized (this) {
			float x = focusX;
			float y = focusY;
			Rect visibleArea = this.visibleArea;
			if (visibleArea != null) {
				// If a focal point value is negative, use the center point of the visible area.
				if (x < 0) {
					x = visibleArea.centerX();
				}
				if (y < 0) {
					y = visibleArea.centerY();
				}
			}
			OsmandMapTileView mapView = this.mapView;
			if (mapView != null) {
				if (scaleFactor > 1) {
					mapView.zoomInAndAdjustTiltAngle();
				} else if (scaleFactor < 1) {
					mapView.zoomOutAndAdjustTiltAngle();
				}
			}
		}
	}

	/**
	 * Handles the map 2D/3D button press events.
	 */
	public void handleTilt() {
		synchronized (this) {
			if (mapView != null && mapView.getAnimatedDraggingThread() != null && offscreenMapRendererView != null) {
				int adjustedTiltAngle = mapView.getAdjustedTiltAngle(mapView.getZoom(), true);
				mapView.getAnimatedDraggingThread().startTilting(
						mapView.getElevationAngle() < DEFAULT_ELEVATION_ANGLE ? DEFAULT_ELEVATION_ANGLE : adjustedTiltAngle, 0.0f);
			}
		}
	}

	/**
	 * Handles the map re-centering events.
	 */
	public void handleRecenter() {
		OsmandMapTileView mapView = this.mapView;
		if (mapView != null) {
			mapView.backToLocation();
		}
	}

	/**
	 * Updates the location coordinate string drawn on the surface.
	 */
	public void updateLocation(@Nullable Location location) {
		//renderFrame();
	}

	@NonNull
	private MapDisplayPositionManager getDisplayPositionManager() {
		return getApp().getMapViewTrackingUtilities().getMapDisplayPositionManager();
	}

	@NonNull
	private OsmandApplication getApp() {
		return (OsmandApplication) carContext.getApplicationContext();
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public void setMapView(OsmandMapTileView mapView) {
		if (mapView == null) {
			stopOffscreenRenderer();
			return;
		}
		this.mapView = mapView;
		if (surface != null) {
			mapView.setView(surfaceView);
		}
		if (getApp().isApplicationInitializing()) {
			getApp().getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					setupOffscreenRenderer();
				}
			});
		} else
			setupOffscreenRenderer();
	}

	public void setupOffscreenRenderer() {
		Log.i(TAG, "setupOffscreenRenderer");
		if (getApp().useOpenGlRenderer()) {
			if (surface != null && surface.isValid()) {
				if (offscreenMapRendererView != null) {
					MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
					if (mapRendererContext != null) {
						if (mapRendererContext.getMapRendererView() == offscreenMapRendererView)
							return;
						offscreenMapRendererView = null;
					}
				}
				if (offscreenMapRendererView == null) {
					MapRendererView mapRendererView = null;
					MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
					if (mapRendererContext != null) {
						if (mapView != null && mapView.getMapRenderer() != null)
							mapView.setMapRenderer(null);
						if (mapRendererContext.getMapRendererView() != null) {
							mapRendererView = mapRendererContext.getMapRendererView();
							mapRendererContext.setMapRendererView(null);
						}
						offscreenMapRendererView = new AtlasMapRendererView(carContext);
						offscreenMapRendererView.setupRenderer(carContext, getWidth(), getHeight(), mapRendererView);
						offscreenMapRendererView.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.getMinZoom()));
						offscreenMapRendererView.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.getMaxZoom()));
						offscreenMapRendererView.setAzimuth(0);
						mapView.setMinAllowedElevationAngle(MIN_ALLOWED_ELEVATION_ANGLE_AA);
						float elevationAngle = mapView.normalizeElevationAngle(getApp().getSettings().getLastKnownMapElevation());
						NativeCoreContext.setMapRendererContext(getApp(), surfaceView.getDensity());
						mapRendererContext = NativeCoreContext.getMapRendererContext();
						if (mapRendererContext != null) {
							mapRendererContext.setMapRendererView(offscreenMapRendererView);
							mapView.setMapRenderer(offscreenMapRendererView);
							mapView.setElevationAngle(elevationAngle);
							mapView.addElevationListener(this);
							getApp().getOsmandMap().getMapLayers().updateMapSource(mapView, null);
							PluginsHelper.refreshLayers(getApp(), null);
							offscreenMapRendererView.addListener(this);
							mapView.getAnimatedDraggingThread().toggleAnimations();
						}
						mapView.applyBatterySavingModeSetting(offscreenMapRendererView);
					}
				}
			}
		}
	}

	public void stopOffscreenRenderer() {
		Log.i(TAG, "stopOffscreenRenderer");
		if (offscreenMapRendererView != null) {
			AtlasMapRendererView offscreenMapRendererView = this.offscreenMapRendererView;
			this.offscreenMapRendererView = null;
			if (mapView != null) {
				mapView.removeElevationListener(this);
				mapView.getAnimatedDraggingThread().toggleAnimations();
				if (mapView.getMapRenderer() == offscreenMapRendererView) {
					mapView.setMapRenderer(null);
				}
			}
			MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
			if (mapRendererContext != null && mapRendererContext.getMapRendererView() == offscreenMapRendererView)
				offscreenMapRendererView.stopRenderer();
		}
	}

	public int getWidth() {
		return surfaceView.getWidth();
	}

	public int getHeight() {
		return surfaceView.getHeight();
	}

	public int getDpi() {
		return surfaceView.getDpi();
	}

	public float getDensity() {
		return surfaceView.getDensity();
	}

	public boolean hasSurface() {
		return surface != null && surface.isValid();
	}

	public boolean hasOffscreenRenderer() {
		return offscreenMapRendererView != null;
	}

	public void renderFrame() {
		if (mapView == null || surface == null || !surface.isValid()) {
			// Surface is not available, or has been destroyed, skip this frame.
			return;
		}
		DrawSettings drawSettings = new DrawSettings(carContext.isDarkMode(), false);
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox().copy();
		try {
			renderFrame(tileBox, drawSettings);
		} catch (Exception ignored) {
			// Ignored
		}
	}

	public void renderFrame(RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (mapView == null || surface == null || !surface.isValid()) {
			// Surface is not available, or has been destroyed, skip this frame.
			return;
		}
		Canvas canvas = surface.lockCanvas(null);
		try {
			boolean newDarkMode = carContext.isDarkMode();
			boolean updateVectorRendering = drawSettings.isUpdateVectorRendering() || darkMode != newDarkMode;
			darkMode = newDarkMode;
			drawSettings = new DrawSettings(newDarkMode, updateVectorRendering);
			if (offscreenMapRendererView != null)
				canvas.drawBitmap(offscreenMapRendererView.getBitmap(), 0, 0, null);
			mapView.drawOverMap(canvas, tileBox, drawSettings);
			SurfaceRendererCallback callback = this.callback;
			if (callback != null) {
				Rect visibleArea = this.visibleArea;
				Rect stableArea = this.stableArea;
				if (visibleArea != null && stableArea != null) {
					callback.onFrameRendered(canvas, visibleArea, stableArea);
				}
			}
		} finally {
			surface.unlockCanvasAndPost(canvas);
		}
	}

	public double getVisibleAreaWidth() {
		return visibleArea != null ? visibleArea.width() : 0f;
	}

	@Override
	public void onElevationChanging(float angle) {
		SurfaceRendererCallback callback = this.callback;
		if (callback != null) {
			callback.onElevationChanging(angle);
		}
	}

	@Override
	public void onStopChangingElevation(float angle) {
	}
}

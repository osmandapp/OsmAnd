package net.osmand.plus.auto;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

/**
 * A very simple implementation of a renderer for the app's background surface.
 */
public final class SurfaceRenderer implements DefaultLifecycleObserver {
	private static final String TAG = "SurfaceRenderer";

	private static final double VISIBLE_AREA_MIN_DETECTION_SIZE = 1.25;

	private final CarContext carContext;
	private final CarSurfaceView surfaceView;
	private OsmandMapTileView mapView;

	@Nullable
	private Surface surface;
	@Nullable
	private SurfaceContainer surfaceContainer;

	@Nullable
	private Rect visibleArea;
	@Nullable
	private Rect stableArea;

	private boolean darkMode;

	private SurfaceRendererCallback callback;

	public void setCallback(@Nullable SurfaceRendererCallback callback) {
		this.callback = callback;
	}

	interface SurfaceRendererCallback {
		void onFrameRendered(@NonNull Canvas canvas, @NonNull Rect visibleArea, @NonNull Rect stableArea);
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
					int visibleAreaWidth = visibleArea.width();
					int visibleAreaHeight = visibleArea.height();
					int containerWidth = surfaceContainer.getWidth();
					int containerHeight = surfaceContainer.getHeight();

					float ratioX = 0;
					if ((float) containerWidth / visibleAreaWidth > VISIBLE_AREA_MIN_DETECTION_SIZE) {
						int centerX = visibleArea.centerX();
						ratioX = (float) centerX / containerWidth;
					}
					float ratioY = 0;
					if ((float) containerHeight / visibleAreaHeight > VISIBLE_AREA_MIN_DETECTION_SIZE) {
						float defaultRatioY = mapView.getDefaultRatioY();
						float centerY = (visibleAreaHeight * defaultRatioY) + visibleArea.top;
						ratioY = centerY / containerHeight;
					}
					mapView.setCustomMapRatio(ratioX, ratioY);
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
					mapView.restoreMapRatio();
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
		this.carContext = carContext;
		this.surfaceView = new CarSurfaceView(carContext, this);
		lifecycle.addObserver(this);
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		Log.i(TAG, "SurfaceRenderer created");
		carContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
	}

	/**
	 * Callback called when the car configuration changes.
	 */
	public void onCarConfigurationChanged() {
		renderFrame();
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
					mapView.zoomIn();
				} else if (scaleFactor < 1) {
					mapView.zoomOut();
				}
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
	private OsmandApplication getApp() {
		return (OsmandApplication) carContext.getApplicationContext();
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		if (surface != null) {
			mapView.setView(surfaceView);
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
}

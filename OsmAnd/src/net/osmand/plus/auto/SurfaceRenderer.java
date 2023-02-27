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

	private final CarSurfaceView surfaceView;
	private OsmandMapTileView mapView;

	@Nullable
	Surface mSurface;
	@Nullable
	Rect mVisibleArea;
	@Nullable
	Rect mStableArea;

	private boolean darkMode;
	private final CarContext mCarContext;

	SurfaceRendererCallback callback;

	interface SurfaceRendererCallback {
		void onFrameRendered(@NonNull Canvas canvas, @NonNull Rect visibleArea, @NonNull Rect stableArea);
	}

	public final SurfaceCallback mSurfaceCallback = new SurfaceCallback() {
		@Override
		public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Surface available " + surfaceContainer);
				if (mSurface != null) {
					mSurface.release();
				}
				mSurface = surfaceContainer.getSurface();
				surfaceView.setSurfaceParams(surfaceContainer.getWidth(), surfaceContainer.getHeight(), surfaceContainer.getDpi());
				darkMode = getApp().getDaynightHelper().isNightMode();
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
				Log.i(TAG, "Visible area changed " + mSurface + ". stableArea: "
						+ mStableArea + " visibleArea:" + visibleArea);
				mVisibleArea = visibleArea;
				renderFrame();
			}
		}

		@Override
		public void onStableAreaChanged(@NonNull Rect stableArea) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Stable area changed " + mSurface + ". stableArea: "
						+ mStableArea + " visibleArea:" + mVisibleArea);
				mStableArea = stableArea;
				renderFrame();
			}
		}

		@Override
		public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
			synchronized (SurfaceRenderer.this) {
				Log.i(TAG, "Surface destroyed");
				if (mSurface != null) {
					mSurface.release();
					mSurface = null;
				}
				OsmandMapTileView mapView = SurfaceRenderer.this.mapView;
				if (mapView != null) {
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
		mCarContext = carContext;
		surfaceView = new CarSurfaceView(mCarContext, this);
		lifecycle.addObserver(this);
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		Log.i(TAG, "SurfaceRenderer created");
		mCarContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
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
			Rect visibleArea = mVisibleArea;
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
		return (OsmandApplication) mCarContext.getApplicationContext();
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		if (mSurface != null) {
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
		return mSurface != null && mSurface.isValid();
	}

	public void renderFrame() {
		if (mapView == null || mSurface == null || !mSurface.isValid()) {
			// Surface is not available, or has been destroyed, skip this frame.
			return;
		}
		boolean nightMode = getApp().getDaynightHelper().isNightMode();
		DrawSettings drawSettings = new DrawSettings(nightMode, false);
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox().copy();
		try {
			renderFrame(tileBox, drawSettings);
		} catch (Exception ignored) {
			// Ignored
		}
	}

	public void renderFrame(RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (mapView == null || mSurface == null || !mSurface.isValid()) {
			// Surface is not available, or has been destroyed, skip this frame.
			return;
		}
		Canvas canvas = mSurface.lockCanvas(null);
		try {
			boolean newDarkMode = getApp().getDaynightHelper().isNightMode();
			boolean updateVectorRendering = drawSettings.isUpdateVectorRendering() || darkMode != newDarkMode;
			darkMode = newDarkMode;
			drawSettings = new DrawSettings(newDarkMode, updateVectorRendering);
			mapView.drawOverMap(canvas, tileBox, drawSettings);
			SurfaceRendererCallback callback = this.callback;
			if (callback != null) {
				Rect visibleArea = this.mVisibleArea;
				Rect stableArea = this.mStableArea;
				if (visibleArea != null && stableArea != null) {
					callback.onFrameRendered(canvas, visibleArea, stableArea);
				}
			}
		} finally {
			mSurface.unlockCanvasAndPost(canvas);
		}
	}
}

package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandApplication.NavigationSessionListener;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class OsmandMap implements NavigationSessionListener {

	private final OsmandApplication app;

	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final OsmandMapTileView mapView;
	private final MapLayers mapLayers;
	private final MapActions mapActions;
	private final IMapDownloaderCallback downloaderCallback;

	private List<OsmandMapListener> listeners = new ArrayList<>();

	public interface OsmandMapListener {
		void onChangeZoom(int stp);

		void onSetMapElevation(float angle);

		void onSetupRenderingView();
	}

	public void addListener(@NonNull OsmandMapListener listener) {
		if (!listeners.contains(listener)) {
			listeners = Algorithms.addToList(listeners, listener);
		}
	}

	public void removeListener(@NonNull OsmandMapListener listener) {
		listeners = Algorithms.removeFromList(listeners, listener);
	}

	public OsmandMap(@NonNull OsmandApplication app) {
		this.app = app;
		mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		mapActions = new MapActions(app);

		int width;
		int height;
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null) {
			WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point screenDimensions = new Point(0, 0);
			display.getSize(screenDimensions);
			width = screenDimensions.x;
			height = screenDimensions.y - AndroidUtils.getStatusBarHeight(app);
		} else {
			SurfaceRenderer surface = carNavigationSession.getNavigationCarSurface();
			width = surface != null ? surface.getWidth() : 100;
			height = surface != null ? surface.getHeight() : 100;
		}
		mapView = new OsmandMapTileView(app, width, height);
		mapLayers = new MapLayers(app);

		// to not let it gc
		downloaderCallback = request -> {
			if (request != null && !request.error && request.fileToSave != null) {
				ResourceManager mgr = app.getResourceManager();
				mgr.tileDownloaded(request);
			}
			if (request == null || !request.error) {
				mapView.tileDownloaded(request);
			}
		};
		app.getResourceManager().getMapTileDownloader().addDownloaderCallback(downloaderCallback);

		app.setNavigationSessionListener(this);
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public OsmandMapTileView getMapView() {
		return mapView;
	}

	@NonNull
	public MapLayers getMapLayers() {
		return mapLayers;
	}

	@NonNull
	public MapActions getMapActions() {
		return mapActions;
	}

	public void refreshMap() {
		mapView.refreshMap();
	}

	public void refreshMap(boolean updateVectorRendering) {
		mapView.refreshMap(updateVectorRendering);
	}

	public void changeZoom(int stp, long time) {
		mapViewTrackingUtilities.setZoomTime(time);
		changeZoom(stp);
	}

	public void changeZoom(int stp) {
		// delta = Math.round(delta * OsmandMapTileView.ZOOM_DELTA) * OsmandMapTileView.ZOOM_DELTA_1;
		boolean changeLocation = false;
		// if (settings.AUTO_ZOOM_MAP.get() == AutoZoomMap.NONE) {
		// changeLocation = false;
		// }

		// double curZoom = mapView.getZoom() + mapView.getZoomFractionalPart() + stp * 0.3;
		// int newZoom = (int) Math.round(curZoom);
		// double zoomFrac = curZoom - newZoom;
		AnimateDraggingMapThread animateDraggingThread = mapView.getAnimatedDraggingThread();
		if (animateDraggingThread.isAnimatingMapZoom()) {
			animateDraggingThread.stopAnimatingSync();
		}

		Zoom zoom = new Zoom(mapView.getZoom(), mapView.getZoomFloatPart(), mapView.getMinZoom(), mapView.getMaxZoom());

		if (stp > 0 && !zoom.isZoomInAllowed()) {
			Toast.makeText(app, R.string.edit_tilesource_maxzoom, Toast.LENGTH_SHORT).show();
			return;
		} else if (stp < 0 && !zoom.isZoomOutAllowed()) {
			Toast.makeText(app, R.string.edit_tilesource_minzoom, Toast.LENGTH_SHORT).show();
			return;
		}

		zoom.changeZoom(stp);
		animateDraggingThread.startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), null, changeLocation);
		if (app.accessibilityEnabled()) {
			Toast.makeText(app, app.getString(R.string.zoomIs) + " " + zoom.getBaseZoom(), Toast.LENGTH_SHORT).show();
		}
		for (OsmandMapListener listener : listeners) {
			listener.onChangeZoom(stp);
		}
	}

	public void setMapLocation(double lat, double lon) {
		mapView.setLatLon(lat, lon);
		mapViewTrackingUtilities.locationChanged(lat, lon, this);
	}

	public void setMapElevation(float angle) {
		for (OsmandMapListener listener : listeners) {
			listener.onSetMapElevation(angle);
		}
	}

	public void setupRenderingView() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapListener listener : listeners) {
			listener.onSetupRenderingView();
		}
		NavigationSession navigationSession = app.getCarNavigationSession();
		if (navigationSession != null) {
			navigationSession.setMapView(mapView);
			app.getMapViewTrackingUtilities().setMapView(mapView);
		} else if (mapView.getMapActivity() == null) {
			app.getMapViewTrackingUtilities().setMapView(null);
		}
	}

	@Override
	public void onNavigationSessionChanged(@Nullable NavigationSession navigationSession) {
		if (navigationSession != null) {
			navigationSession.setMapView(mapView);
			app.getMapViewTrackingUtilities().setMapView(mapView);
		} else if (mapView.getMapActivity() == null) {
			app.getMapViewTrackingUtilities().setMapView(null);
		}
	}

	public float getTextScale() {
		float scale = app.getSettings().TEXT_SCALE.get();
		return scale * getCarDensityScaleCoef();
	}

	public float getOriginalTextScale() {
		return app.getSettings().TEXT_SCALE.get();
	}

	public float getMapDensity() {
		float scale = app.getSettings().MAP_DENSITY.get();
		return scale * getCarDensityScaleCoef();
	}

	public float getCarDensityScaleCoef() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (mapView.isCarView()) {
			float carViewDensity = mapView.getCarViewDensity();
			float density = mapView.getDensity();
			return carViewDensity / density;
		}
		return 1f;
	}

	public void fitCurrentRouteToMap(boolean portrait, int leftBottomPaddingPx) {
		RoutingHelper rh = app.getRoutingHelper();
		Location lt = rh.getLastProjection();
		if (lt == null) {
			lt = app.getTargetPointsHelper().getPointToStartLocation();
		}
		if (lt != null) {
			double left = lt.getLongitude(), right = lt.getLongitude();
			double top = lt.getLatitude(), bottom = lt.getLatitude();
			List<Location> list = rh.getCurrentCalculatedRoute();
			for (Location l : list) {
				left = Math.min(left, l.getLongitude());
				right = Math.max(right, l.getLongitude());
				top = Math.max(top, l.getLatitude());
				bottom = Math.min(bottom, l.getLatitude());
			}
			List<TargetPointsHelper.TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
			if (rh.getRoute().hasMissingMaps()) {
				TargetPointsHelper.TargetPoint pointToStart = app.getTargetPointsHelper().getPointToStart();
				if (pointToStart != null) {
					targetPoints.add(pointToStart);
				}
			}
			for (TargetPointsHelper.TargetPoint l : targetPoints) {
				left = Math.min(left, l.getLongitude());
				right = Math.max(right, l.getLongitude());
				top = Math.max(top, l.getLatitude());
				bottom = Math.min(bottom, l.getLatitude());
			}
			RotatedTileBox tb = getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			if (!portrait) {
				tileBoxWidthPx = tb.getPixWidth() - leftBottomPaddingPx;
			} else {
				tileBoxHeightPx = tb.getPixHeight() - leftBottomPaddingPx;
			}
			getMapView().fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
		}
	}
}

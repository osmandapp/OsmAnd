package net.osmand.plus.views;

import android.graphics.Point;
import android.view.Display;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class OsmandMap {

	private final OsmandApplication app;

	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final OsmandMapTileView mapView;
	private final MapLayers mapLayers;
	private final MapActions mapActions;
	private final IMapDownloaderCallback downloaderCallback;

	private List<RenderingViewSetupListener> renderingViewSetupListeners = new ArrayList<>();

	public interface RenderingViewSetupListener {

		void onSetupRenderingView();
	}

	public void addRenderingViewSetupListener(@NonNull RenderingViewSetupListener listener) {
		if (!renderingViewSetupListeners.contains(listener)) {
			renderingViewSetupListeners = CollectionUtils.addToList(renderingViewSetupListeners, listener);
		}
	}

	public void removeRenderingViewSetupListener(@NonNull RenderingViewSetupListener listener) {
		renderingViewSetupListeners = CollectionUtils.removeFromList(renderingViewSetupListeners, listener);
	}

	public OsmandMap(@NonNull OsmandApplication app) {
		this.app = app;
		mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		mapActions = new MapActions(app);

		int width;
		int height;
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null) {
			Display display = AndroidUtils.getDisplay(app);
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

	public void setMapLocation(double lat, double lon) {
		mapView.setLatLon(lat, lon);
		mapViewTrackingUtilities.locationChanged(lat, lon, this);
	}

	public void setupRenderingView() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (RenderingViewSetupListener listener : renderingViewSetupListeners) {
			listener.onSetupRenderingView();
		}
		NavigationSession navigationSession = app.getCarNavigationSession();
		if (navigationSession != null) {
			if (navigationSession.hasStarted()) {
				navigationSession.setMapView(mapView);
				app.getMapViewTrackingUtilities().setMapView(mapView);
			} else {
				navigationSession.setMapView(null);
				if (mapView.getMapActivity() == null) {
					app.getMapViewTrackingUtilities().setMapView(null);
				}
			}
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

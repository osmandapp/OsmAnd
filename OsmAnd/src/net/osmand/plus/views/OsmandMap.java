package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandApplication.NavigationSessionListener;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OsmandMap implements NavigationSessionListener {

	private final OsmandApplication app;

	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final OsmandMapTileView mapView;
	private final MapLayers mapLayers;
	private final IMapDownloaderCallback downloaderCallback;

	private final List<OsmandMapListener> listeners = Collections.synchronizedList(new ArrayList<>());

	public interface OsmandMapListener {
		void onChangeZoom(int stp);
		void onSetMapElevation(float angle);
		void onSetupOpenGLView(boolean init);
	}

	public void addListener(@NonNull OsmandMapListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(@NonNull OsmandMapListener listener) {
		listeners.remove(listener);
	}

	public OsmandMap(@NonNull OsmandApplication app) {
		this.app = app;
		mapViewTrackingUtilities = app.getMapViewTrackingUtilities();

		int w;
		int h;
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null) {
			WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point screenDimensions = new Point(0, 0);
			display.getSize(screenDimensions);
			w = screenDimensions.x;
			h = screenDimensions.y - AndroidUtils.getStatusBarHeight(app);
		} else {
			SurfaceRenderer surface = carNavigationSession.getNavigationCarSurface();
			w = surface != null ? surface.getWidth() : 100;
			h = surface != null ? surface.getHeight() : 100;
		}
		mapView = new OsmandMapTileView(app, w, h);
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

	public void refreshMap() {
		mapView.refreshMap();
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

		final int newZoom = mapView.getZoom() + stp;
		final double zoomFrac = mapView.getZoomFractionalPart();
		if (newZoom > mapView.getMaxZoom()) {
			Toast.makeText(app, R.string.edit_tilesource_maxzoom, Toast.LENGTH_SHORT).show();
			return;
		}
		if (newZoom < mapView.getMinZoom()) {
			Toast.makeText(app, R.string.edit_tilesource_minzoom, Toast.LENGTH_SHORT).show();
			return;
		}
		mapView.getAnimatedDraggingThread().startZooming(newZoom, zoomFrac, changeLocation);
		if (app.accessibilityEnabled()) {
			Toast.makeText(app, app.getString(R.string.zoomIs) + " " + newZoom, Toast.LENGTH_SHORT).show();
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

	public void setupOpenGLView(boolean init) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapListener listener : listeners) {
			listener.onSetupOpenGLView(init);
		}
		NavigationSession navigationSession = app.getCarNavigationSession();
		if (navigationSession != null && navigationSession.hasSurface()) {
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
}

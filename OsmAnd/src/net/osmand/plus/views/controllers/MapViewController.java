package net.osmand.plus.views.controllers;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Натали on 29.09.2014.
 */
public class MapViewController {
	private GLSurfaceView glSurfaceView;
	private OsmandMapTileView mapTileView;
	private OsmandSettings settings;
	private MapActivity mapActivity;
	private boolean isNative = false;

	public void setAccessibilityActions(MapAccessibilityActions accessibilityActions) {
		if (isNative){

		} else {
			mapTileView.setAccessibilityActions(accessibilityActions);
		}
	}

	public void refreshMap(boolean b) {
		if (isNative){

		} else {
			mapTileView.refreshMap(b);
		}
	}

	public void createLayers(MapActivityLayers mapLayers) {
		if (isNative) {

		} else {
			mapLayers.createLayers(mapTileView);
		}
	}

	public void setLatLon(double latitude, double longitude) {
		if (isNative){

		} else {
			mapTileView.setLatLon(latitude, longitude);
		}
	}

	public void setIntZoom(int i) {
		if (isNative) {

		} else {
			mapTileView.setIntZoom(i);
		}
	}

	public void addView(FrameLayout view) {
		if (isNative) {

		} else {
			((FrameLayout)mapTileView.getParent()).addView(view);
		}
	}

	public void setTrackingUtilities(MapViewTrackingUtilities mapViewTrackingUtilities) {
		if (isNative) {

		} else {
			mapViewTrackingUtilities.setMapView(mapTileView);
		}
	}

	public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
		if (isNative) {

		} else {
			mapTileView.tileDownloaded(request);
		}
	}

	public ViewParent getParentView() {
		if (isNative) {
			return glSurfaceView.getParent();
		} else {
			return mapTileView.getParent();
		}
	}

	public List<OsmandMapLayer> getLayers() {
		if (isNative) {

		} else {
			return mapTileView.getLayers();
		}
		return null;
	}

	public double getLatitude() {
		if (isNative){

		} else {
			return mapTileView.getLatitude();
		}
		return 0;
	}

	public double getLongitude() {
		if (isNative){

		} else {
			return mapTileView.getLongitude();
		}
		return 0;
	}

	public void startMoving(double latitude, double longitude, int mapZoomToShow, boolean b) {
		if (isNative){

		} else {
			mapTileView.getAnimatedDraggingThread().startMoving(latitude, longitude,
					settings.getMapZoomToShow(), true);
		}

	}

	public int getZoom() {
		if (isNative) {

		} else {
			return mapTileView.getZoom();
		}
		return 0;
	}

	public void startZooming(int newZoom, boolean changeLocation) {
		if (isNative){

		} else {
			mapTileView.getAnimatedDraggingThread().startZooming(newZoom, changeLocation);
		}
	}

	public boolean isZooming() {
		if (isNative){

		} else {
			return mapTileView.isZooming();
		}
		return false;
	}

	public RotatedTileBox getCurrentRotatedTileBox() {
		if (isNative) {

		} else {
			return mapTileView.getCurrentRotatedTileBox();
		}
		return null;
	}


	public interface OnTrackBallListener {
		public boolean onTrackBallEvent(MotionEvent e);
	}

	public MapViewController(GLSurfaceView surfaceView, MapActivity activity){
		this.glSurfaceView = surfaceView;
		this.settings = activity.getMyApplication().getSettings();
		this.mapActivity = activity;
		isNative = true;

	}

	public MapViewController(OsmandMapTileView mapTileView, MapActivity activity){
		this.mapTileView = mapTileView;
		this.settings = activity.getMyApplication().getSettings();
		this.mapActivity = activity;
		isNative = false;
	}

	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		if (isNative){

		} else {
			mapTileView.setTrackBallDelegate(trackBallDelegate);
		}
	}

}

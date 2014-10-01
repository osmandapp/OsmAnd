package net.osmand.plus.views.controllers;

import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.FrameLayout;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Denis on 01.10.2014.
 */
public abstract class MapViewBaseController {

	public void  setAccessibilityActions(MapAccessibilityActions accessibilityActions) {
	}

	public void refreshMap(boolean b) {
	}

	public void createLayers(MapActivityLayers mapLayers) {
	}

	public void setLatLon(double latitude, double longitude) {
	}

	public void setIntZoom(int i) {
	}

	public void addView(FrameLayout view) {
	}

	public void setTrackingUtilities(MapViewTrackingUtilities mapViewTrackingUtilities) {
	}

	public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
	}

	public ViewParent getParentView() {
		return null;
	}

	public List<OsmandMapLayer> getLayers() {
		return null;
	}

	public double getLatitude() {
		return 0;
	}

	public double getLongitude() {
		return 0;
	}

	public void startMoving(double latitude, double longitude, int mapZoomToShow, boolean b) {
	}

	public int getZoom() {
		return 0;
	}

	public void startZooming(int newZoom, boolean changeLocation) {
	}

	public boolean isZooming() {
		return false;
	}

	public RotatedTileBox getCurrentRotatedTileBox() {
		return null;
	}

	public void onPause() {
	}

	public void updateLayers(MapActivityLayers mapLayers) {
	}

	public void setComplexZoom() {
	}

	public void showAndHideMapPosition() {
	}

	public OsmandMapTileView getMapTileView() {
		return null;
	}

	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
	}

	public interface OnTrackBallListener {
		public boolean onTrackBallEvent(MotionEvent e);
	}
}

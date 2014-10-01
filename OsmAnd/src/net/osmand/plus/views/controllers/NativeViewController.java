package net.osmand.plus.views.controllers;

import android.opengl.GLSurfaceView;
import android.view.ViewParent;
import android.widget.FrameLayout;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeViewController extends MapViewBaseController {
	private GLSurfaceView glSurfaceView;
	private OsmandSettings settings;
	private MapActivity mapActivity;


	public NativeViewController(GLSurfaceView surfaceView, MapActivity activity) {
		this.glSurfaceView = surfaceView;
		this.settings = activity.getMyApplication().getSettings();
		this.mapActivity = activity;
	}

	@Override
	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		super.setTrackBallDelegate(trackBallDelegate);
	}

	@Override
	public void setAccessibilityActions(MapAccessibilityActions accessibilityActions) {
		super.setAccessibilityActions(accessibilityActions);
	}

	@Override
	public void refreshMap(boolean b) {
		super.refreshMap(b);
	}

	@Override
	public void createLayers(MapActivityLayers mapLayers) {
		super.createLayers(mapLayers);
	}

	@Override
	public void setLatLon(double latitude, double longitude) {
		super.setLatLon(latitude, longitude);
	}

	@Override
	public void setIntZoom(int i) {
		super.setIntZoom(i);
	}

	@Override
	public void addView(FrameLayout view) {
		super.addView(view);
	}

	@Override
	public void setTrackingUtilities(MapViewTrackingUtilities mapViewTrackingUtilities) {
		super.setTrackingUtilities(mapViewTrackingUtilities);
	}

	@Override
	public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
		super.tileDownloaded(request);
	}

	@Override
	public ViewParent getParentView() {
		return super.getParentView();
	}

	@Override
	public List<OsmandMapLayer> getLayers() {
		return super.getLayers();
	}

	@Override
	public double getLatitude() {
		return super.getLatitude();
	}

	@Override
	public double getLongitude() {
		return super.getLongitude();
	}

	@Override
	public void startMoving(double latitude, double longitude, int mapZoomToShow, boolean b) {
		super.startMoving(latitude, longitude, mapZoomToShow, b);
	}

	@Override
	public int getZoom() {
		return super.getZoom();
	}

	@Override
	public void startZooming(int newZoom, boolean changeLocation) {
		super.startZooming(newZoom, changeLocation);
	}

	@Override
	public boolean isZooming() {
		return super.isZooming();
	}

	@Override
	public RotatedTileBox getCurrentRotatedTileBox() {
		return super.getCurrentRotatedTileBox();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void updateLayers(MapActivityLayers mapLayers) {
		super.updateLayers(mapLayers);
	}

	@Override
	public void setComplexZoom() {
		super.setComplexZoom();
	}

	@Override
	public void showAndHideMapPosition() {
		super.showAndHideMapPosition();
	}

	@Override
	public OsmandMapTileView getMapTileView() {
		return super.getMapTileView();
	}
}

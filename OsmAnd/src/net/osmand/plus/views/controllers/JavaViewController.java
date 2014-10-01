package net.osmand.plus.views.controllers;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Натали on 29.09.2014.
 */
public class JavaViewController extends MapViewBaseController {
	private GLSurfaceView glSurfaceView;
	private OsmandMapTileView mapTileView;
	private OsmandSettings settings;
	private MapActivity mapActivity;

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;

	@Override
	public void refreshMap(boolean b) {
		mapTileView.refreshMap(b);
	}

	@Override
	public void setAccessibilityActions(MapAccessibilityActions accessibilityActions) {
		mapTileView.setAccessibilityActions(accessibilityActions);
	}

	@Override
	public void createLayers(MapActivityLayers mapLayers) {
		mapLayers.createLayers(mapTileView);
	}

	public void setLatLon(double latitude, double longitude) {
		mapTileView.setLatLon(latitude, longitude);
	}

	@Override
	public void setIntZoom(int i) {
		mapTileView.setIntZoom(i);
	}

	@Override
	public void addView(FrameLayout view) {
		((FrameLayout) mapTileView.getParent()).addView(view);
	}

	@Override
	public void setTrackingUtilities(MapViewTrackingUtilities mapViewTrackingUtilities) {
		mapViewTrackingUtilities.setMapView(mapTileView);

	}

	@Override
	public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
		mapTileView.tileDownloaded(request);
	}

	@Override
	public ViewParent getParentView() {
		return mapTileView.getParent();
	}

	@Override
	public List<OsmandMapLayer> getLayers() {
		return mapTileView.getLayers();
	}

	@Override
	public double getLatitude() {
		return mapTileView.getLatitude();
	}

	@Override
	public double getLongitude() {
		return mapTileView.getLongitude();

	}

	@Override
	public void startMoving(double latitude, double longitude, int mapZoomToShow, boolean b) {
		mapTileView.getAnimatedDraggingThread().startMoving(latitude, longitude,
				settings.getMapZoomToShow(), true);
	}

	@Override
	public int getZoom() {
		return mapTileView.getZoom();
	}

	@Override
	public void startZooming(int newZoom, boolean changeLocation) {
		mapTileView.getAnimatedDraggingThread().startZooming(newZoom, changeLocation);
	}

	@Override
	public boolean isZooming() {
		return mapTileView.isZooming();
	}

	@Override
	public RotatedTileBox getCurrentRotatedTileBox() {
		return mapTileView.getCurrentRotatedTileBox();

	}

	@Override
	public void onPause() {
		AnimateDraggingMapThread animatedThread = mapTileView.getAnimatedDraggingThread();
		if (animatedThread.isAnimating() && animatedThread.getTargetIntZoom() != 0) {
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(),
					animatedThread.getTargetIntZoom());
		}
	}

	@Override
	public void updateLayers(MapActivityLayers mapLayers) {
		mapLayers.updateLayers(mapTileView);
	}

	@Override
	public void setComplexZoom() {
		mapTileView.setComplexZoom(mapTileView.getZoom(), mapTileView.getSettingsZoomScale());
	}

	@Override
	public void showAndHideMapPosition() {
		mapTileView.setShowMapPosition(true);
		mapActivity.getMyApplication().runMessageInUIThreadAndCancelPrevious(SHOW_POSITION_MSG_ID, new Runnable() {
			@Override
			public void run() {
				if (mapTileView.isShowMapPosition()) {
					mapTileView.setShowMapPosition(false);
					mapTileView.refreshMap();
				}
			}
		}, 2500);
	}

	@Override
	public OsmandMapTileView getMapTileView() {
		return mapTileView;
	}

	public JavaViewController(OsmandMapTileView mapTileView, MapActivity activity) {
		this.mapTileView = mapTileView;
		this.settings = activity.getMyApplication().getSettings();
		this.mapActivity = activity;
	}

	@Override
	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		mapTileView.setTrackBallDelegate(trackBallDelegate);
	}

}

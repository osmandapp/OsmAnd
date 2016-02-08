package net.osmand.plus;

import android.app.Activity;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.data.LocationPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginsActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.views.OsmandMapTileView;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OsmAndAppCustomization {
	
	protected OsmandApplication app;
	protected OsmandSettings osmandSettings;

	public void setup(OsmandApplication app) {
		this.app = app;
		this.osmandSettings = new OsmandSettings(app, new net.osmand.plus.api.SettingsAPIImpl(app));
	}

	public OsmandSettings getOsmandSettings(){ return osmandSettings;}
	// Main menu
	public boolean checkExceptionsOnStart() {
		return true;
	}

	public boolean showFirstTimeRunAndTips(boolean firstTime, boolean appVersionChanged) {
		return true;
	}

	public boolean checkBasemapDownloadedOnStart() {
		return true;
	}

	
	// Activities
	public Class<? extends Activity> getSettingsActivity(){
		return SettingsActivity.class;
	}
	
	public Class<MapActivity> getMapActivity(){
		return MapActivity.class;
	}
	
	public Class<SearchActivity> getSearchActivity(){
		return SearchActivity.class;
	}
	
	public Class<TrackActivity> getTrackActivity(){
		return TrackActivity.class;
	}
	
	public Class<FavoritesActivity> getFavoritesActivity(){
		return FavoritesActivity.class;
	}

	public Class<? extends Activity> getDownloadIndexActivity() {
		return DownloadActivity.class;
	}
	
	public Class<? extends Activity> getPluginsActivity() {
		return PluginsActivity.class;
	}
	
	public Class<? extends Activity> getDownloadActivity() {
		return DownloadActivity.class;
	}

	// Download screen
	public void getDownloadTypes(List<DownloadActivityType> items) {
		
	}


	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		return Collections.emptyList();
	}

	public void prepareLayerContextMenu(MapActivity activity, ContextMenuAdapter adapter) {
	}

	public void prepareOptionsMenu(MapActivity mapActivity, ContextMenuAdapter optionsMenuHelper) {
	}


	public String getIndexesUrl() {
		return "http://"+IndexConstants.INDEX_DOWNLOAD_DOMAIN+"/get_indexes?gzip&" + Version.getVersionAsURLParam(app); //$NON-NLS-1$;
	}

	public boolean showDownloadExtraActions() {
		return true;
	}


	public File getTracksDir() {
		return app.getAppPath(IndexConstants.GPX_RECORDED_INDEX_DIR);
	}

	public void createLayers(OsmandMapTileView mapView, MapActivity activity) {
		
	}
	
	public List<? extends LocationPoint> getWaypoints() {
		return Collections.emptyList();
	}

	public boolean isWaypointGroupVisible(int waypointType, RouteCalculationResult route) {
		if(waypointType == WaypointHelper.ALARMS) {
			return route != null && !route.getAlarmInfo().isEmpty();
		} else if(waypointType == WaypointHelper.WAYPOINTS) {
			return route != null && !route.getLocationPoints().isEmpty();
		}
		return true;
	}

	public void showLocationPoint(MapActivity ctx, LocationPoint locationPoint) {
	}
	
	public boolean onDestinationReached() {
		return true;
	}


	public boolean onlyTourDownload() {
		return false;
	}

	public <T> void resumeActivity(Class<T> class1, T d) {
	}
	
	public <T> void pauseActivity(Class<T> class1) {
	}

}

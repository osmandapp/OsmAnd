package net.osmand.plus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LocationPoint;
import net.osmand.plus.activities.*;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.view.Window;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;

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

	public void customizeMainMenu(Window window, Activity activity) {
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
	
	public Class<FavouritesActivity> getFavoritesActivity(){
		return FavouritesActivity.class;
	}

	public Class<MainMenuActivity> getMainMenuActivity() {
		return MainMenuActivity.class;
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

	public void updatedLoadedFiles(Map<String, String> indexFileNames, Map<String, String> indexActivatedFileNames) {
	}

	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		return Collections.emptyList();
	}

	public void prepareLayerContextMenu(MapActivity activity, ContextMenuAdapter adapter) {
	}

	public void prepareOptionsMenu(MapActivity mapActivity, ContextMenuAdapter optionsMenuHelper) {
	}

	public void prepareLocationMenu(MapActivity mapActivity, ContextMenuAdapter adapter) {
	}

	public String getIndexesUrl() {
		return "http://"+IndexConstants.INDEX_DOWNLOAD_DOMAIN+"/get_indexes?gzip&" + Version.getVersionAsURLParam(app); //$NON-NLS-1$;
	}

	public boolean showDownloadExtraActions() {
		return true;
	}

	public boolean saveGPXPoint(Location location) {
		return false;
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

	public int getWaypointSearchRadius(int searchDeviationRadius, int type) {
		return searchDeviationRadius;
	}

	public File getExternalStorageDir() {
		return osmandSettings.getExternalStorageDirectory();
	}

	public boolean showNavigationControls() { return true;}
}

package net.osmand.plus;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceScreen;
import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.download.LocalIndexesFragment.LoadLocalIndexTask;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.distancecalculator.DistanceCalculatorPlugin;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmo.OsMoPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routepointsnavigation.RoutePointsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.OsmandMapTileView;
import org.apache.commons.logging.Log;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> installedPlugins = new ArrayList<OsmandPlugin>();  
	private static List<OsmandPlugin> activePlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);
	
	private static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin"; //$NON-NLS-1$
	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid"; //$NON-NLS-1$
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin"; //$NON-NLS-1$

	private static final String OSMODROID_PLUGIN_COMPONENT = "com.OsMoDroid"; //$NON-NLS-1$
	
	public abstract String getId();
	
	public abstract String getDescription();
	
	public abstract String getName();
	
	public String getVersion() {
		return "";
	}
	
	/**
	 * Initialize plugin runs just after creation
	 */
	public abstract boolean init(OsmandApplication app);
	
	public void disable(OsmandApplication app) {};
	
	
	public static void initPlugins(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		OsmandRasterMapsPlugin rasterMapsPlugin = new OsmandRasterMapsPlugin(app);
		installedPlugins.add(rasterMapsPlugin);
		installedPlugins.add(new OsmandMonitoringPlugin(app));
		installedPlugins.add(new OsMoPlugin(app));
		installedPlugins.add(new AudioVideoNotesPlugin(app));
		installedPlugins.add(new DistanceCalculatorPlugin(app));
		installedPlugins.add(new AccessibilityPlugin(app));
		if(!installPlugin(SRTM_PLUGIN_COMPONENT_PAID, SRTMPlugin.ID, app,
				new SRTMPlugin(app, true))) {
			installPlugin(SRTM_PLUGIN_COMPONENT, SRTMPlugin.FREE_ID, app,
					new SRTMPlugin(app, false));
		}
		final ParkingPositionPlugin parking = new ParkingPositionPlugin(app);
		boolean f = installPlugin(PARKING_PLUGIN_COMPONENT, ParkingPositionPlugin.ID, app, parking);
		if(!f && Version.isParkingPluginInlined(app)) {
			installedPlugins.add(parking);
		}
		Set<String> enabledPlugins = settings.getEnabledPlugins();
		if(Version.isRouteNavPluginInlined(app)) {
			RoutePointsPlugin routePointsPlugin = new RoutePointsPlugin(app);
			installedPlugins.add(routePointsPlugin);
			enabledPlugins.add(routePointsPlugin.getId());
		}

		// osmodroid disabled
//		installPlugin(OSMODROID_PLUGIN_COMPONENT, OsMoDroidPlugin.ID, app, new OsMoDroidPlugin(app));
		installedPlugins.add(new OsmEditingPlugin(app));
		installedPlugins.add(new OsmandDevelopmentPlugin(app));
		
		for (OsmandPlugin plugin : installedPlugins) {
			if (enabledPlugins.contains(plugin.getId())) {
				try {
					if (plugin.init(app)) {
						activePlugins.add(plugin);
					}
				} catch (Exception e) {
					LOG.error("Plugin initialization failed " + plugin.getId(), e);
				}
			}
		}
	}
	
	public static boolean enablePlugin(OsmandApplication app, OsmandPlugin plugin, boolean enable) {
		if (enable) {
			if (!plugin.init(app)) {
				return false;
			}
			activePlugins.add(plugin);
		} else {
			plugin.disable(app);
			activePlugins.remove(plugin);
		}
		app.getSettings().enablePlugin(plugin.getId(), enable);
		return true;
	}
	
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {};
	
	/**
	 * Register layers calls when activity is created and before @mapActivityCreate
	 * @param activity
	 */
	public void registerLayers(MapActivity activity)  { }

	public void mapActivityCreate(MapActivity activity) { }
	
	public void mapActivityResume(MapActivity activity) { }
	
	public void mapActivityPause(MapActivity activity) { }
	
	public void mapActivityDestroy(MapActivity activity) { }
	
	public boolean destinationReached() { return true;	}
	
	public void settingsActivityCreate(SettingsActivity activity, PreferenceScreen screen) {}
	
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {}
	
	public void registerMapContextMenuActions(MapActivity mapActivity, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {}
	
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {}
	
	public void loadLocalIndexes(List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {}
	
	public void updateLocation(Location location) {}
	
	public void contextMenuLocalIndexes(Activity activity, SherlockFragment fragment, Object info, ContextMenuAdapter adapter) {};
	
	public void updateLocalIndexDescription(LocalIndexInfo info) {}
	
	public void optionsMenuLocalIndexes(Activity activity, SherlockFragment fragment, ContextMenuAdapter optionsMenuAdapter) {};
	
	public List<String> indexingFiles(IProgress progress) {	return null;}
	
	public boolean mapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		return false;
	}
	
	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
	}
	
	public static void refreshLayers(OsmandMapTileView mapView, MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.updateLayers(mapView, activity);
		}
	}
	
	public static List<OsmandPlugin> getAvailablePlugins(){
		return installedPlugins;
	}
	
	public static List<OsmandPlugin> getEnabledPlugins(){
		return activePlugins;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getEnabledPlugin(Class<T> clz) {
		for(OsmandPlugin lr : activePlugins) {
			if(clz.isInstance(lr)){
				return (T) lr;
			}
		}
		return null;
	}
	
	public static List<String> onIndexingFiles(IProgress progress) {
		List<String> l = new ArrayList<String>(); 
		for (OsmandPlugin plugin : activePlugins) {
			List<String> ls = plugin.indexingFiles(progress);
			if(ls != null && ls.size() > 0) {
				l.addAll(ls);
			}
		}
		return l;
	}
	
	

	public static void onMapActivityCreate(MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.mapActivityCreate(activity);
		}
	}
	
	public static void onMapActivityResume(MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.mapActivityResume(activity);
		}
	}
	
	public static void onMapActivityPause(MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.mapActivityPause(activity);
		}
	}
	
	public static void onMapActivityDestroy(MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.mapActivityDestroy(activity);
		}
	}
	
	public static void onMapActivityResult(int requestCode, int resultCode, Intent data) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.onMapActivityExternalResult(requestCode, resultCode, data);
		}
	}
	

	public static void onSettingsActivityCreate(SettingsActivity activity, PreferenceScreen screen) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.settingsActivityCreate(activity, screen);
		}
	}
	
	public static boolean onDestinationReached() {
		boolean b = true;
		for (OsmandPlugin plugin : activePlugins) {
			if(!plugin.destinationReached()){
				b = false;
			}
		}		
		return b;
	}
	

	public static void createLayers(OsmandMapTileView mapView, MapActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.registerLayers(activity);
		}
	}
	
	public static void registerMapContextMenu(MapActivity map, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.registerMapContextMenuActions(map, latitude, longitude, adapter, selectedObj);
		}
	}

	public static void registerLayerContextMenu(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.registerLayerContextMenuActions(mapView, adapter, mapActivity);
		}
	}
	
	public static void registerOptionsMenu(MapActivity map, ContextMenuAdapter helper) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.registerOptionsMenuItems(map, helper);
		}
	}
	public static void onUpdateLocalIndexDescription(LocalIndexInfo info) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.updateLocalIndexDescription(info);
		}
	}
	
	public static void onLoadLocalIndexes(List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.loadLocalIndexes(result, loadTask);
		}		
	}
	
	public static void onContextMenuActivity(Activity activity, SherlockFragment fragment, Object info, ContextMenuAdapter adapter) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.contextMenuLocalIndexes(activity, fragment, info, adapter);
		}
	}
	
	
	public static void onOptionsMenuActivity(Activity activity, SherlockFragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.optionsMenuLocalIndexes(activity, fragment, optionsMenuAdapter);
		}
		
	}


	private static boolean installPlugin(String packageInfo, 
			String pluginId, OsmandApplication app, OsmandPlugin plugin) {
		boolean installed = false;
		try{
			installed = app.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch ( NameNotFoundException e){
		}
		
		
		if(installed) {
			installedPlugins.add(plugin);
			if(!app.getSettings().getPlugins().contains("-"+pluginId)) {
				app.getSettings().enablePlugin(pluginId, true);
			}
			return true;
		} else {
			if(app.getSettings().getPlugins().contains(pluginId)) {
				app.getSettings().enablePlugin(pluginId, false);
			}
			return false;
		}
	}

	public static boolean onMapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		for(OsmandPlugin p : installedPlugins){
			if(p.mapActivityKeyUp(mapActivity, keyCode))
				return true;
		}
		return false;
	}

	public static void updateLocationPlugins(net.osmand.Location location) {
		for(OsmandPlugin p : installedPlugins){
			p.updateLocation(location);
		}		
	}

	public static boolean isDevelopment() {
		return getEnabledPlugin(OsmandDevelopmentPlugin.class) != null;
	}

	


}

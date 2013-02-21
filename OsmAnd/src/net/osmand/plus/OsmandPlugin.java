package net.osmand.plus;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.LocalIndexesActivity;
import net.osmand.plus.activities.LocalIndexesActivity.LoadLocalIndexTask;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.background.OsmandBackgroundServicePlugin;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.distancecalculator.DistanceCalculatorPlugin;
import net.osmand.plus.extrasettings.OsmandExtraSettings;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmodroid.OsMoDroidPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.planning.PlanningPlugin;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceScreen;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> installedPlugins = new ArrayList<OsmandPlugin>();  
	private static List<OsmandPlugin> activePlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);
	
	private static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin"; //$NON-NLS-1$
	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid"; //$NON-NLS-1$
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin"; //$NON-NLS-1$
	
	private static final String OSMODROID_PLUGIN_COMPONENT = "com.OsMoDroid"; //$NON-NLS-1$
	private static final String PLANNING_PLUGIN_COMPONENT = "net.osmand.planningPlugin";
        

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
		installedPlugins.add(new OsmandBackgroundServicePlugin(app));
		installedPlugins.add(new OsmandExtraSettings(app));
		installedPlugins.add(new AccessibilityPlugin(app));
		if(!installPlugin(SRTM_PLUGIN_COMPONENT_PAID, SRTMPlugin.ID, app,
				new SRTMPlugin(app, true))) {
			installPlugin(SRTM_PLUGIN_COMPONENT, SRTMPlugin.ID, app,
					new SRTMPlugin(app, false));
		}
		installPlugin(PARKING_PLUGIN_COMPONENT, ParkingPositionPlugin.ID, app, new ParkingPositionPlugin(app));
		installPlugin(PLANNING_PLUGIN_COMPONENT, PlanningPlugin.ID, app, new PlanningPlugin(app));
		installPlugin(OSMODROID_PLUGIN_COMPONENT, OsMoDroidPlugin.ID, app, new OsMoDroidPlugin(app));
		installedPlugins.add(new DistanceCalculatorPlugin(app));
		installedPlugins.add(new AudioVideoNotesPlugin(app));
		installedPlugins.add(new OsmEditingPlugin(app));
		installedPlugins.add(new OsmandDevelopmentPlugin(app));
		
		
		Set<String> enabledPlugins = settings.getEnabledPlugins();
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
	public abstract void registerLayers(MapActivity activity);

	public void mapActivityCreate(MapActivity activity) { }
	
	public void mapActivityResume(MapActivity activity) { }
	
	public void mapActivityPause(MapActivity activity) { }
	
	public void mapActivityDestroy(MapActivity activity) { }
	
	public void settingsActivityCreate(SettingsActivity activity, PreferenceScreen screen) {}
	
	public void settingsActivityDestroy(final SettingsActivity activity){}
	
	public void settingsActivityUpdate(final SettingsActivity activity){}
	
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {}
	
	public void registerMapContextMenuActions(MapActivity mapActivity, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {}
	
	public void registerOptionsMenuItems(MapActivity mapActivity, OptionsMenuHelper helper) {}
	
	public void loadLocalIndexes(List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {};
	
	public void contextMenuLocalIndexes(LocalIndexesActivity la, LocalIndexInfo info, ContextMenuAdapter adapter) {};
	
	public void updateLocalIndexDescription(LocalIndexInfo info) {}
	
	public void optionsMenuLocalIndexes(LocalIndexesActivity localIndexesActivity, ContextMenuAdapter optionsMenuAdapter) {};
	
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
	
	public static void onSettingsActivityDestroy(SettingsActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.settingsActivityDestroy(activity);
		}
	}
	
	public static void onSettingsActivityUpdate(SettingsActivity activity) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.settingsActivityUpdate(activity);
		}
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
	
	public static void registerOptionsMenu(MapActivity map, OptionsMenuHelper helper) {
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
	
	public static void onContextMenuLocalIndexes(LocalIndexesActivity la, LocalIndexInfo info, ContextMenuAdapter adapter) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.contextMenuLocalIndexes(la, info, adapter);
		}
	}
	public static void onOptionsMenuLocalIndexes(LocalIndexesActivity localIndexesActivity, ContextMenuAdapter optionsMenuAdapter) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.optionsMenuLocalIndexes(localIndexesActivity, optionsMenuAdapter);
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
			app.getSettings().enablePlugin(plugin.getId(), true);
			return true;
		} else {
			app.getSettings().enablePlugin(pluginId, false);
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


}

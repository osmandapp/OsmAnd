package net.osmand.plus;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.osmand.LogUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
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
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceScreen;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> installedPlugins = new ArrayList<OsmandPlugin>();  
	private static List<OsmandPlugin> activePlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = LogUtil.getLog(OsmandPlugin.class);
	
	private static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin"; //$NON-NLS-1$
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
		installedPlugins.add(new OsmandBackgroundServicePlugin(app));
		installedPlugins.add(new OsmandExtraSettings(app));
		installedPlugins.add(new AccessibilityPlugin(app));
		installPlugin(SRTM_PLUGIN_COMPONENT, SRTMPlugin.ID, app,
				new SRTMPlugin(app));
		installPlugin(PARKING_PLUGIN_COMPONENT, ParkingPositionPlugin.ID, app, new ParkingPositionPlugin(app));
		installPlugin(OSMODROID_PLUGIN_COMPONENT, OsMoDroidPlugin.ID, app, new OsMoDroidPlugin(app));
		installedPlugins.add(new OsmEditingPlugin(app));
		installedPlugins.add(new OsmandDevelopmentPlugin(app));
		installedPlugins.add(new DistanceCalculatorPlugin(app));
		
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

	private static void installPlugin(String packageInfo, 
			String pluginId, OsmandApplication app, OsmandPlugin plugin) {
		boolean installed = false;
		try{
			installed = app.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch ( NameNotFoundException e){
		}
		
		if(installed) {
			installedPlugins.add(plugin);
			app.getSettings().enablePlugin(plugin.getId(), true);
		} else {
			app.getSettings().enablePlugin(pluginId, false);
		}
	}
	
}

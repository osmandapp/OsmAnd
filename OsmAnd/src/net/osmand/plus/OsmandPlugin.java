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
import net.osmand.plus.extrasettings.OsmandExtraSettings;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceScreen;
import android.view.Menu;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> installedPlugins = new ArrayList<OsmandPlugin>();  
	private static List<OsmandPlugin> activePlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = LogUtil.getLog(OsmandPlugin.class);
	
	private static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin"; //$NON-NLS-1$
	private static final String PARKING_PLUGIN_ACTIVITY = "net.osmand.parkingPlugin.ParkingPluginActivity"; //$NON-NLS-1$
	
	
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
		installedPlugins.add(new OsmEditingPlugin(app));
		installedPlugins.add(new OsmandDevelopmentPlugin(app));
//		installedPlugins.add(parkingPlugin);
		installParkingPlugin(app);
		
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
	
	public void prepareOptionsMenuItems(MapActivity mapActivity, Menu menu) {}

	public boolean onOptionsItemSelected(MapActivity mapActivity, int itemId) { return false; }
	
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

	public static void registerOnPrepareOptionsMenu(MapActivity mapActivity, Menu menu) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.prepareOptionsMenuItems(mapActivity, menu);
		}
	}

	public static boolean registerOnOptionsMenuItemSelected(MapActivity mapActivity, int itemId) {
		for (OsmandPlugin plugin : activePlugins) {
			if (plugin.onOptionsItemSelected(mapActivity, itemId)) {
				return true;
			}
		}
		return false;
	}

	private static void installParkingPlugin(OsmandApplication app) {
		Intent parkingIntent = new Intent();
		parkingIntent.setComponent(new ComponentName(PARKING_PLUGIN_COMPONENT, PARKING_PLUGIN_ACTIVITY));
		parkingIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		ResolveInfo resolved = app.getPackageManager().resolveActivity(parkingIntent, PackageManager.MATCH_DEFAULT_ONLY);
		ParkingPositionPlugin parkingPlugin = new ParkingPositionPlugin(app);
		if(resolved != null) {
			installedPlugins.add(parkingPlugin);
			app.getSettings().enablePlugin(parkingPlugin.getId(), true);
		} else {
			app.getSettings().enablePlugin(parkingPlugin.getId(), false);
		}
	}
}

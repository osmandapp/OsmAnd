package net.osmand.plus;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.views.OsmandMapTileView;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> installedPlugins = new ArrayList<OsmandPlugin>();  
	private static List<OsmandPlugin> activePlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = LogUtil.getLog(OsmandPlugin.class);
	
	
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
	
	
	public static void initPlugins(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		installedPlugins.add(new OsmEditingPlugin(app));
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
	
	/**
	 * ????
	 */
	public abstract void updateLayers(OsmandMapTileView mapView);
	
	public static void refreshLayers(OsmandMapTileView mapView) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.updateLayers(mapView);
		}
	}

	
	public abstract void registerLayers(MapActivity activity);

	public void mapActivityCreate(MapActivity activity) { }
	
	public void mapActivityResume(MapActivity activity) { }
	
	public void mapActivityPause(MapActivity activity) { }
	
	public void mapActivityDestroy(MapActivity activity) { }
	
	
	public abstract void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter);
	
	public abstract void registerMapContextMenuActions(MapActivity mapActivity, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj);
	
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

	public static void registerLayerContextMenu(OsmandMapTileView mapView, ContextMenuAdapter adapter) {
		for (OsmandPlugin plugin : activePlugins) {
			plugin.registerLayerContextMenuActions(mapView, adapter);
		}
	}
}

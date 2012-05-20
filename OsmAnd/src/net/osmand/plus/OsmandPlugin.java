package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.views.OsmandMapTileView;

public abstract class OsmandPlugin {
	
	private static List<OsmandPlugin> plugins = new ArrayList<OsmandPlugin>();  
	static {
		plugins.add(new OsmEditingPlugin());
	}
	
	public static List<OsmandPlugin> getAvailablePlugins(){
		return plugins;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getEnabledPlugin(Class<T> clz) {
		for(OsmandPlugin lr : plugins) {
			if(clz.isInstance(lr)){
				return (T) lr;
			}
		}
		return null;
	}
	
	
	public abstract String getId();
	
	/**
	 * Initialize plugin runs just after creation
	 */
	public abstract boolean init(OsmandApplication app);
	
	
	public static void initPlugins(OsmandApplication app) {
		for (OsmandPlugin plugin : plugins) {
			plugin.init(app);
		}
	}
	
	/**
	 * ????
	 */
	public abstract void updateLayers(OsmandMapTileView mapView);
	
	public static void refreshLayers(OsmandMapTileView mapView) {
		for (OsmandPlugin plugin : plugins) {
			plugin.updateLayers(mapView);
		}
	}

	
	public abstract void registerLayers(MapActivity activity);

	public static void createLayers(OsmandMapTileView mapView, MapActivity activity) {
		for (OsmandPlugin plugin : plugins) {
			plugin.registerLayers(activity);
		}
	}
	
	public abstract void mapActivityCreate(MapActivity activity);

	public static void onMapActivityCreate(MapActivity activity) {
		for (OsmandPlugin plugin : plugins) {
			plugin.mapActivityCreate(activity);
		}
	}
	
	public abstract boolean handleContextMenuAction(int resId, double latitude, double longitude);
	
	public abstract void registerContextMenuActions(double latitude, double longitude, TIntArrayList list);
	
	public static void registerContextMenu(double latitude, double longitude, TIntArrayList list) {
		for (OsmandPlugin plugin : plugins) {
			plugin.registerContextMenuActions(latitude, longitude, list);
		}
	}
	
	public static boolean contextMenuAction(int resId, double latitude, double longitude) {
		for (OsmandPlugin plugin : plugins) {
			if(plugin.handleContextMenuAction(resId, latitude, longitude)){
				return true;
			}
		}
		return false;
	}
}

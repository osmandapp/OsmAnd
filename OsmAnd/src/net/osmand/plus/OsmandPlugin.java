package net.osmand.plus;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.distancecalculator.DistanceCalculatorPlugin;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.skimapsplugin.SkiMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class OsmandPlugin {
	private static List<OsmandPlugin> allPlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);

	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid"; //$NON-NLS-1$
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin";
	private boolean active;
	private String installURL = null;

	public abstract String getId();

	public abstract String getDescription();

	public abstract String getName();

	public abstract int getAssetResourceName();

	@DrawableRes
	public int getLogoResourceId() {
		return R.drawable.ic_extension_dark;
	}

	public abstract Class<? extends Activity> getSettingsActivity();

	public String getVersion() {
		return "";
	}

	/**
	 * Initialize plugin runs just after creation
	 */
	public boolean init(OsmandApplication app, Activity activity) {
		return true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public boolean needsInstallation() {
		return installURL != null;
	}

	public void setInstallURL(String installURL) {
		this.installURL = installURL;
	}

	public String getInstallURL() {
		return installURL;
	}

	public void disable(OsmandApplication app) {
	}

	public String getHelpFileName() {
		return null;
	}

	/*
	 * Return true in case if plugin should fill the map context menu with buildContextMenuRows method.
	 */
	public boolean isMenuControllerSupported(Class<? extends MenuController> menuControllerClass) {
		return false;
	}

	/*
	 * Add menu rows to the map context menu.
	 */
	public void buildContextMenuRows(@NonNull MenuBuilder menuBuilder, @NonNull View view) {
	}

	/*
	 * Clear resources after menu was closed
	 */
	public void clearContextMenuRows() {
	}

	public static void initPlugins(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		Set<String> enabledPlugins = settings.getEnabledPlugins();
		allPlugins.add(new MapillaryPlugin(app));
		allPlugins.add(new OsmandRasterMapsPlugin(app));
		allPlugins.add(new OsmandMonitoringPlugin(app));
		// allPlugins.add(new OsMoPlugin(app));
		checkMarketPlugin(app, new SRTMPlugin(app), true, SRTM_PLUGIN_COMPONENT_PAID, SRTM_PLUGIN_COMPONENT);

		// ? questionable - definitely not market plugin 
//		checkMarketPlugin(app, new TouringViewPlugin(app), false, TouringViewPlugin.COMPONENT, null);
		checkMarketPlugin(app, new NauticalMapsPlugin(app), false, NauticalMapsPlugin.COMPONENT, null);
		checkMarketPlugin(app, new SkiMapsPlugin(app), false, SkiMapsPlugin.COMPONENT, null);

//		checkMarketPlugin(app, new RoutePointsPlugin(app), false /*FIXME*/, RoutePointsPlugin.ROUTE_POINTS_PLUGIN_COMPONENT, null);
		allPlugins.add(new AudioVideoNotesPlugin(app));
		checkMarketPlugin(app, new ParkingPositionPlugin(app), false, ParkingPositionPlugin.PARKING_PLUGIN_COMPONENT, null);
		allPlugins.add(new DistanceCalculatorPlugin(app));
		allPlugins.add(new AccessibilityPlugin(app));
		allPlugins.add(new OsmEditingPlugin(app));
		allPlugins.add(new OsmandDevelopmentPlugin(app));

		activatePlugins(app, enabledPlugins);
	}

	private static void activatePlugins(OsmandApplication app, Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : allPlugins) {
			if (enabledPlugins.contains(plugin.getId()) || plugin.isActive()) {
				try {
					if (plugin.init(app, null)) {
						plugin.setActive(true);
					}
				} catch (Exception e) {
					LOG.error("Plugin initialization failed " + plugin.getId(), e);
				}
			}
		}
	}

	private static void checkMarketPlugin(OsmandApplication app, OsmandPlugin srtm, boolean paid, String id, String id2) {
		boolean marketEnabled = Version.isMarketEnabled(app);
		boolean pckg = isPackageInstalled(id, app) ||
				isPackageInstalled(id2, app);
		if ((Version.isDeveloperVersion(app) || !Version.isProductionVersion(app)) && !paid) {
			// for test reasons
			marketEnabled = false;
		}
		if (pckg || (!marketEnabled && !paid)) {
			if (pckg && !app.getSettings().getPlugins().contains("-" + srtm.getId())) {
				srtm.setActive(true);
			}
			allPlugins.add(srtm);
		} else {
			if (marketEnabled) {
				srtm.setInstallURL(Version.marketPrefix(app) + id);
				allPlugins.add(srtm);
			}
		}
	}

	public static boolean enablePlugin(Activity activity, OsmandApplication app, OsmandPlugin plugin, boolean enable) {
		if (enable) {
			if (!plugin.init(app, activity)) {
				plugin.setActive(false);
				return false;
			} else {
				plugin.setActive(true);
			}
		} else {
			plugin.disable(app);
			plugin.setActive(false);
		}
		app.getSettings().enablePlugin(plugin.getId(), enable);
		if (activity instanceof MapActivity) {
			final MapActivity mapActivity = (MapActivity) activity;
			plugin.updateLayers(mapActivity.getMapView(), mapActivity);
			mapActivity.getDashboard().refreshDashboardFragments();
			if (!enable && plugin.getCardFragment() != null) {
				Fragment fragment = mapActivity.getSupportFragmentManager()
						.findFragmentByTag(plugin.getCardFragment().tag);
				LOG.debug("fragment=" + fragment);
				mapActivity.getSupportFragmentManager().beginTransaction()
						.remove(fragment).commit();
			}
		}
		return true;
	}

	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
	}

	;

	/**
	 * Register layers calls when activity is created and before @mapActivityCreate
	 *
	 * @param activity
	 */
	public void registerLayers(MapActivity activity) {
	}

	public void mapActivityCreate(MapActivity activity) {
	}

	public void mapActivityResume(MapActivity activity) {
	}

	public void mapActivityPause(MapActivity activity) {
	}

	public void mapActivityDestroy(MapActivity activity) {
	}

	public void mapActivityScreenOff(MapActivity activity) {
	}

	@TargetApi(Build.VERSION_CODES.M)
	public void handleRequestPermissionsResult(int requestCode, String[] permissions,
											   int[] grantResults) {
	}

	public static final void onRequestPermissionsResult(int requestCode, String[] permissions,
														int[] grantResults) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.handleRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	public boolean destinationReached() {
		return true;
	}


	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
	}

	public void registerMapContextMenuActions(MapActivity mapActivity, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {
	}

	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
	}

	public DashFragmentData getCardFragment() {
		return null;
	}

	public void updateLocation(Location location) {
	}

	public void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
	}

	public void contextMenuFragment(Activity activity, Fragment fragment, Object info, ContextMenuAdapter adapter) {
	}

	public void optionsMenuFragment(Activity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
	}

	public List<String> indexingFiles(IProgress progress) {
		return null;
	}

	public boolean mapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		return false;
	}

	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
	}

	public static void refreshLayers(OsmandMapTileView mapView, MapActivity activity) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.updateLayers(mapView, activity);
		}
	}

	public static List<OsmandPlugin> getAvailablePlugins() {
		return allPlugins;
	}

	public static List<OsmandPlugin> getEnabledPlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isActive()) {
				lst.add(p);
			}
		}
		return lst;
	}

	public static List<OsmandPlugin> getNotEnabledPlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (!p.isActive()) {
				lst.add(p);
			}
		}
		return lst;
	}

	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getEnabledPlugin(Class<T> clz) {
		for (OsmandPlugin lr : getEnabledPlugins()) {
			if (clz.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getPlugin(Class<T> clz) {
		for (OsmandPlugin lr : getAvailablePlugins()) {
			if (clz.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	public static List<String> onIndexingFiles(IProgress progress) {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			List<String> ls = plugin.indexingFiles(progress);
			if (ls != null && ls.size() > 0) {
				l.addAll(ls);
			}
		}
		return l;
	}


	public static void onMapActivityCreate(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityCreate(activity);
		}
	}

	public static void onMapActivityResume(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityResume(activity);
		}
	}

	public static void onMapActivityPause(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityPause(activity);
		}
	}

	public static void onMapActivityDestroy(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityDestroy(activity);
		}
	}

	public static void onMapActivityResult(int requestCode, int resultCode, Intent data) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.onMapActivityExternalResult(requestCode, resultCode, data);
		}
	}

	public static void onMapActivityScreenOff(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityScreenOff(activity);
		}
	}

	public static boolean onDestinationReached() {
		boolean b = true;
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (!plugin.destinationReached()) {
				b = false;
			}
		}
		return b;
	}


	public static void createLayers(OsmandMapTileView mapView, MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerLayers(activity);
		}
	}

	public static void registerMapContextMenu(MapActivity map, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin instanceof ParkingPositionPlugin) {
				plugin.registerMapContextMenuActions(map, latitude, longitude, adapter, selectedObj);
			} else if (plugin instanceof OsmandMonitoringPlugin) {
				plugin.registerMapContextMenuActions(map, latitude, longitude, adapter, selectedObj);
			}
		}
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (!(plugin instanceof ParkingPositionPlugin) && !(plugin instanceof OsmandMonitoringPlugin)) {
				int itemsCount = adapter.length();
				plugin.registerMapContextMenuActions(map, latitude, longitude, adapter, selectedObj);
				if (adapter.length() > itemsCount) {
					adapter.addItem(new ContextMenuItem.ItemBuilder()
							.setPosition(itemsCount)
							.setLayout(R.layout.context_menu_list_divider)
							.createItem());
				}
			}
		}
	}

	public static void registerLayerContextMenu(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerLayerContextMenuActions(mapView, adapter, mapActivity);
		}
	}

	public static void registerOptionsMenu(MapActivity map, ContextMenuAdapter helper) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerOptionsMenuItems(map, helper);
		}
	}

	public static void onContextMenuActivity(Activity activity, Fragment fragment, Object info, ContextMenuAdapter adapter) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.contextMenuFragment(activity, fragment, info, adapter);
		}
	}


	public static void onOptionsMenuActivity(Activity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.optionsMenuFragment(activity, fragment, optionsMenuAdapter);
		}
	}

	public static Collection<DashFragmentData> getPluginsCardsList() {
		HashSet<DashFragmentData> collection = new HashSet<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			final DashFragmentData fragmentData = plugin.getCardFragment();
			if (fragmentData != null) collection.add(fragmentData);
		}
		return collection;
	}

	public static boolean isPackageInstalled(String packageInfo, Context ctx) {
		if (packageInfo == null) {
			return false;
		}
		boolean installed = false;
		try {
			installed = ctx.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch (NameNotFoundException e) {
		}
		return installed;
	}

	public static boolean onMapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			if (p.mapActivityKeyUp(mapActivity, keyCode))
				return true;
		}
		return false;
	}

	public static void updateLocationPlugins(net.osmand.Location location) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.updateLocation(location);
		}
	}

	public static boolean isDevelopment() {
		return getEnabledPlugin(OsmandDevelopmentPlugin.class) != null;
	}

	public static void addMyPlacesTabPlugins(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.addMyPlacesTab(favoritesActivity, mTabs, intent);
		}
	}
}

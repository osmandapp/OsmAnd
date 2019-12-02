package net.osmand.plus;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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
import net.osmand.plus.dialogs.PluginInstalledBottomSheetDialog;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class OsmandPlugin {

	public static final String PLUGIN_ID_KEY = "plugin_id";

	private static List<OsmandPlugin> allPlugins = new ArrayList<OsmandPlugin>();
	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);

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
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		return true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return true;
	}

	public boolean isMarketPlugin() {
		return false;
	}

	public boolean isPaid() {
		return false;
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

	public String getComponentId1() {
		return null;
	}

	public String getComponentId2() {
		return null;
	}

	public List<ApplicationMode> getAddedAppModes() {
		return Collections.emptyList();
	}

	public List<IndexItem> getSuggestedMaps() {
		return Collections.emptyList();
	}

	/**
	 * Plugin was installed
	 */
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			if (fragmentManager != null) {
				PluginInstalledBottomSheetDialog.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
			}
		}
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

	public static void initPlugins(@NonNull OsmandApplication app) {
		Set<String> enabledPlugins = app.getSettings().getEnabledPlugins();

		allPlugins.clear();
		allPlugins.add(new MapillaryPlugin(app));

		if (!enabledPlugins.contains(MapillaryPlugin.ID) && !app.getSettings().getPlugins().contains("-" + MapillaryPlugin.ID)) {
			enabledPlugins.add(MapillaryPlugin.ID);
			app.getSettings().enablePlugin(MapillaryPlugin.ID, true);
		}

		allPlugins.add(new OsmandRasterMapsPlugin(app));
		allPlugins.add(new OsmandMonitoringPlugin(app));
		checkMarketPlugin(app, enabledPlugins, new SRTMPlugin(app));

		// ? questionable - definitely not market plugin
//		checkMarketPlugin(app, enabledPlugins, new TouringViewPlugin(app), false, TouringViewPlugin.COMPONENT, null);
		checkMarketPlugin(app, enabledPlugins, new NauticalMapsPlugin(app));
		checkMarketPlugin(app, enabledPlugins, new SkiMapsPlugin(app));

		allPlugins.add(new AudioVideoNotesPlugin(app));
		checkMarketPlugin(app, enabledPlugins, new ParkingPositionPlugin(app));
		allPlugins.add(new AccessibilityPlugin(app));
		allPlugins.add(new OsmEditingPlugin(app));
		allPlugins.add(new OsmandDevelopmentPlugin(app));

		activatePlugins(app, enabledPlugins);
	}

	private static void activatePlugins(OsmandApplication app, Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : allPlugins) {
			if (enabledPlugins.contains(plugin.getId()) || plugin.isActive()) {
				initPlugin(app, plugin);
			}
		}
	}

	private static void initPlugin(OsmandApplication app, OsmandPlugin plugin) {
		try {
			if (plugin.init(app, null)) {
				plugin.setActive(true);
			}
		} catch (Exception e) {
			LOG.error("Plugin initialization failed " + plugin.getId(), e);
		}
	}

	private static void checkMarketPlugin(@NonNull OsmandApplication app, @NonNull Set<String> enabledPlugins, @NonNull OsmandPlugin plugin) {
		if (updateMarketPlugin(app, enabledPlugins, plugin)) {
			allPlugins.add(plugin);
		}
	}

	private static boolean updateMarketPlugin(@NonNull OsmandApplication app, @NonNull Set<String> enabledPlugins, @NonNull OsmandPlugin plugin) {
		boolean marketEnabled = Version.isMarketEnabled(app);
		boolean pckg = checkPluginPackage(app, plugin);
		boolean paid = plugin.isPaid();
		if ((Version.isDeveloperVersion(app) || !Version.isProductionVersion(app)) && !paid) {
			// for test reasons
			marketEnabled = false;
		}
		if (pckg || (!marketEnabled && !paid)) {
			if (pckg && !app.getSettings().getPlugins().contains("-" + plugin.getId())) {
				enabledPlugins.add(plugin.getId());
				plugin.setActive(true);
			}
			plugin.setInstallURL(null);
			return true;
		} else {
			if (marketEnabled) {
				plugin.setActive(false);
				if (!app.getSettings().getPlugins().contains("-" + plugin.getId())) {
					enabledPlugins.remove(plugin.getId());
				}
				plugin.setInstallURL(Version.getUrlWithUtmRef(app, plugin.getComponentId1()));
				return true;
			}
		}
		return false;
	}

	public static void checkInstalledMarketPlugins(@NonNull OsmandApplication app, @Nullable Activity activity) {
		Set<String> enabledPlugins = app.getSettings().getEnabledPlugins();
		for (OsmandPlugin plugin : OsmandPlugin.getMarketPlugins()) {
			if (plugin.getInstallURL() != null && checkPluginPackage(app, plugin) && updateMarketPlugin(app, enabledPlugins, plugin)) {
				plugin.onInstall(app, activity);
				initPlugin(app, plugin);
			}
		}
	}

	private static boolean checkPluginPackage(OsmandApplication app, OsmandPlugin plugin) {
		return isPackageInstalled(plugin.getComponentId1(), app) || isPackageInstalled(plugin.getComponentId2(), app)
				|| InAppPurchaseHelper.isSubscribedToLiveUpdates(app);
	}

	public static boolean enablePlugin(@Nullable Activity activity, OsmandApplication app, OsmandPlugin plugin, boolean enable) {
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
		if (activity != null && activity instanceof MapActivity) {
			final MapActivity mapActivity = (MapActivity) activity;
			plugin.updateLayers(mapActivity.getMapView(), mapActivity);
			mapActivity.getDashboard().refreshDashboardFragments();

			DashFragmentData fragmentData = plugin.getCardFragment();
			if (!enable && fragmentData != null) {
				FragmentManager fm = mapActivity.getSupportFragmentManager();
				Fragment fragment = fm.findFragmentByTag(fragmentData.tag);
				if (fragment != null) {
					fm.beginTransaction().remove(fragment).commit();
				}
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

	public static List<OsmandPlugin> getVisiblePlugins() {
		List<OsmandPlugin> list = new ArrayList<>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isVisible()) {
				list.add(p);
			}
		}
		return list;
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

	public static List<OsmandPlugin> getEnabledVisiblePlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isActive() && p.isVisible()) {
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

	public static List<OsmandPlugin> getNotEnabledVisiblePlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (!p.isActive() && p.isVisible()) {
				lst.add(p);
			}
		}
		return lst;
	}

	public static List<OsmandPlugin> getMarketPlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isMarketPlugin()) {
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

	public static OsmandPlugin getPlugin(String id) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			if (plugin.getId().equals(id)) {
				return plugin;
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
			plugin.registerMapContextMenuActions(map, latitude, longitude, adapter, selectedObj);
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

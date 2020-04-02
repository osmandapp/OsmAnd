package net.osmand.plus;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.dialogs.PluginDisabledBottomSheet;
import net.osmand.plus.dialogs.PluginInstalledBottomSheetDialog;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.skimapsplugin.SkiMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class OsmandPlugin {

	public static final String PLUGIN_ID_KEY = "plugin_id";

	private static final String PLUGINS_PREFERENCES_NAME = "net.osmand.plugins";
	private static final String CUSTOM_PLUGINS_KEY = "custom_plugins";

	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);

	private static List<OsmandPlugin> allPlugins = new ArrayList<OsmandPlugin>();

	protected OsmandApplication app;

	protected List<OsmandSettings.OsmandPreference> pluginPreferences = new ArrayList<>();

	private boolean active;
	private String installURL = null;

	public OsmandPlugin(OsmandApplication app) {
		this.app = app;
	}

	public abstract String getId();

	public abstract String getName();

	public abstract String getDescription();

	@Nullable
	public Drawable getAssetResourceImage() {
		return null;
	}

	@DrawableRes
	public int getLogoResourceId() {
		return R.drawable.ic_extension_dark;
	}

	@NonNull
	public Drawable getLogoResource() {
		return app.getUIUtilities().getIcon(getLogoResourceId());
	}

	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

	public Class<? extends BaseSettingsFragment> getSettingsFragment() {
		return null;
	}

	public List<OsmandSettings.OsmandPreference> getPreferences() {
		return pluginPreferences;
	}

	public String getPrefsDescription() {
		return null;
	}

	public String getVersion() {
		return "";
	}

	/**
	 * Initialize plugin runs just after creation
	 */
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (activity != null) {
			// called from UI
			for (ApplicationMode appMode: getAddedAppModes()) {
				ApplicationMode.changeProfileAvailability(appMode, true, app);
			}
		}
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

	public List<String> getRendererNames() {
		return Collections.emptyList();
	}

	public List<String> getRouterNames() {
		return Collections.emptyList();
	}

	/**
	 * Plugin was installed
	 */
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, true, app);
		}
		showInstallDialog(activity);
	}

	public void showInstallDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginInstalledBottomSheetDialog.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void showDisableDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginDisabledBottomSheet.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void disable(OsmandApplication app) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, false, app);
		}
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

		loadCustomPlugins(app);
		activatePlugins(app, enabledPlugins);
	}

	public static void addCustomPlugin(@NonNull OsmandApplication app, @NonNull CustomOsmandPlugin plugin) {
		OsmandPlugin oldPlugin = OsmandPlugin.getPlugin(plugin.getId());
		if (oldPlugin != null) {
			allPlugins.remove(oldPlugin);
		}
		allPlugins.add(plugin);
		enablePlugin(null, app, plugin, true);
		saveCustomPlugins(app);
	}

	public static void removeCustomPlugin(@NonNull OsmandApplication app, @NonNull final CustomOsmandPlugin plugin) {
		allPlugins.remove(plugin);
		if (plugin.isActive()) {
			plugin.removePluginItems(new CustomOsmandPlugin.PluginItemsListener() {
				@Override
				public void onItemsRemoved() {
					Algorithms.removeAllFiles(plugin.getPluginDir());
				}
			});
		} else {
			Algorithms.removeAllFiles(plugin.getPluginDir());
		}
		saveCustomPlugins(app);
	}

	private static void loadCustomPlugins(@NonNull OsmandApplication app) {
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object pluginPrefs = settingsAPI.getPreferenceObject(PLUGINS_PREFERENCES_NAME);
		String customPluginsJson = settingsAPI.getString(pluginPrefs, CUSTOM_PLUGINS_KEY, "");
		if (!Algorithms.isEmpty(customPluginsJson)) {
			try {
				JSONArray jArray = new JSONArray(customPluginsJson);
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json = jArray.getJSONObject(i);
					CustomOsmandPlugin plugin = new CustomOsmandPlugin(app, json);
					allPlugins.add(plugin);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private static void saveCustomPlugins(OsmandApplication app) {
		List<CustomOsmandPlugin> customOsmandPlugins = getCustomPlugins();
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object pluginPrefs = settingsAPI.getPreferenceObject(PLUGINS_PREFERENCES_NAME);
		JSONArray itemsJson = new JSONArray();
		for (CustomOsmandPlugin plugin : customOsmandPlugins) {
			try {
				JSONObject json = new JSONObject();
				json.put("pluginId", plugin.getId());
				plugin.writeAdditionalDataToJson(json);
				plugin.writeDependentFilesJson(json);
				itemsJson.put(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		String jsonStr = itemsJson.toString();
		if (!jsonStr.equals(settingsAPI.getString(pluginPrefs, CUSTOM_PLUGINS_KEY, ""))) {
			settingsAPI.edit(pluginPrefs).putString(CUSTOM_PLUGINS_KEY, jsonStr).commit();
		}
	}

	private static void activatePlugins(OsmandApplication app, Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : allPlugins) {
			if (enabledPlugins.contains(plugin.getId()) || plugin.isActive()) {
				initPlugin(app, plugin);
			}
		}
		app.getQuickActionRegistry().updateActionTypes();
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
		boolean pckg = plugin.pluginAvailable(app);
		boolean paid = plugin.isPaid();
		if ((Version.isDeveloperVersion(app) || !Version.isProductionVersion(app)) && !paid) {
			// for test reasons
			// marketEnabled = false;
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
			if (plugin.getInstallURL() != null && plugin.pluginAvailable(app)) {
				plugin.onInstall(app, activity);
				initPlugin(app, plugin);
			}
			updateMarketPlugin(app, enabledPlugins, plugin);
		}
		app.getQuickActionRegistry().updateActionTypes();
	}

	protected boolean pluginAvailable(OsmandApplication app) {
		return checkPluginPackage(app, this) || !isPaid();
	}

	public static boolean checkPluginPackage(@NonNull OsmandApplication app, @NonNull OsmandPlugin plugin) {
		return isPackageInstalled(plugin.getComponentId1(), app) || isPackageInstalled(plugin.getComponentId2(), app);
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
		app.getQuickActionRegistry().updateActionTypes();
		if (activity != null) {
			if (activity instanceof MapActivity) {
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

			if (plugin.isMarketPlugin()) {
				if (plugin.isActive()) {
					plugin.showInstallDialog(activity);
				} else if (OsmandPlugin.checkPluginPackage(app, plugin)) {
					plugin.showDisableDialog(activity);
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

	protected void registerQuickActionTypes(List<QuickActionType> quickActionTypes) {
	}

	protected void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
	}

	protected void registerMapContextMenuActions(MapActivity mapActivity, double latitude, double longitude, ContextMenuAdapter adapter, Object selectedObj) {
	}

	protected void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
	}

	public DashFragmentData getCardFragment() {
		return null;
	}

	public void updateLocation(Location location) {
	}

	protected void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
	}

	protected void contextMenuFragment(Activity activity, Fragment fragment, Object info, ContextMenuAdapter adapter) {
	}

	protected void optionsMenuFragment(Activity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
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

	public static List<CustomOsmandPlugin> getCustomPlugins() {
		ArrayList<CustomOsmandPlugin> lst = new ArrayList<CustomOsmandPlugin>(allPlugins.size());
		for (OsmandPlugin plugin : allPlugins) {
			if (plugin instanceof CustomOsmandPlugin) {
				lst.add((CustomOsmandPlugin) plugin);
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

	public static List<String> getDisabledRendererNames() {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getNotEnabledPlugins()) {
			l.addAll(plugin.getRendererNames());
		}
		return l;
	}

	public static List<String> getDisabledRouterNames() {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getNotEnabledPlugins()) {
			l.addAll(plugin.getRouterNames());
		}
		return l;
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

	public static void registerQuickActionTypesPlugins(List<QuickActionType> quickActionTypes) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.registerQuickActionTypes(quickActionTypes);
		}
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

	protected OsmandSettings.CommonPreference<Boolean> registerBooleanPreference(OsmandApplication app, String prefId, boolean defValue) {
		OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().registerBooleanPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	private OsmandSettings.CommonPreference<Boolean> registerBooleanAccessibilityPreference(OsmandApplication app, String prefId, boolean defValue) {
		OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().registerBooleanAccessibilityPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	protected OsmandSettings.CommonPreference<String> registerStringPreference(OsmandApplication app, String prefId, String defValue) {
		OsmandSettings.CommonPreference<String> preference = app.getSettings().registerStringPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	protected OsmandSettings.CommonPreference<Integer> registerIntPreference(OsmandApplication app, String prefId, int defValue) {
		OsmandSettings.CommonPreference<Integer> preference = app.getSettings().registerIntPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	protected OsmandSettings.CommonPreference<Long> registerLongPreference(OsmandApplication app, String prefId, long defValue) {
		OsmandSettings.CommonPreference<Long> preference = app.getSettings().registerLongPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	protected OsmandSettings.CommonPreference<Float> registerFloatPreference(OsmandApplication app, String prefId, float defValue) {
		OsmandSettings.CommonPreference<Float> preference = app.getSettings().registerFloatPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
	}

	protected <T extends Enum> OsmandSettings.CommonPreference<T> registerEnumIntPreference(OsmandApplication app, String prefId, Enum defaultValue, Enum[] values, Class<T> clz) {
		OsmandSettings.CommonPreference<T> preference = app.getSettings().registerEnumIntPreference(prefId, defaultValue, values, clz);
		pluginPreferences.add(preference);
		return preference;
	}
}

package net.osmand.plus.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

import com.github.mikephil.charting.charts.LineChart;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.custom.CustomRegion;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.skimaps.SkiMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class PluginsHelper {

	private static final Log log = PlatformUtil.getLog(PluginsHelper.class);

	private static final String CUSTOM_PLUGINS_KEY = "custom_plugins";
	private static final String PLUGINS_PREFERENCES_NAME = "net.osmand.plugins";

	private static final List<OsmandPlugin> allPlugins = new ArrayList<>();

	public static void initPlugins(@NonNull OsmandApplication app) {
		Set<String> enabledPlugins = app.getSettings().getEnabledPlugins();
		allPlugins.clear();

		allPlugins.add(new WikipediaPlugin(app));
		allPlugins.add(new OsmandRasterMapsPlugin(app));
		allPlugins.add(new OsmandMonitoringPlugin(app));
		checkMarketPlugin(app, new SRTMPlugin(app));
		allPlugins.add(new WeatherPlugin(app));
		checkMarketPlugin(app, new NauticalMapsPlugin(app));
		checkMarketPlugin(app, new SkiMapsPlugin(app));
		allPlugins.add(new AudioVideoNotesPlugin(app));
		checkMarketPlugin(app, new ParkingPositionPlugin(app));
		allPlugins.add(new OsmEditingPlugin(app));
		// OpenPlaceReviews has been discontinued in 15 June 2023 (schedule to delete the code).
		// allPlugins.add(new OpenPlaceReviewsPlugin(app));
		allPlugins.add(new MapillaryPlugin(app));
		allPlugins.add(new ExternalSensorsPlugin(app));
		allPlugins.add(new AccessibilityPlugin(app));
		allPlugins.add(new OsmandDevelopmentPlugin(app));

		loadCustomPlugins(app);
		registerAppInitializingDependedProperties(app);
		enablePluginsByDefault(app, enabledPlugins);
		activatePlugins(app, enabledPlugins);
	}

	public static void addCustomPlugin(@NonNull OsmandApplication app, @NonNull CustomOsmandPlugin plugin) {
		OsmandPlugin oldPlugin = getPlugin(plugin.getId());
		if (oldPlugin != null) {
			allPlugins.remove(oldPlugin);
		}
		allPlugins.add(plugin);
		enablePlugin(null, app, plugin, true);
		saveCustomPlugins(app);
	}

	public static void removeCustomPlugin(@NonNull OsmandApplication app, @NonNull CustomOsmandPlugin plugin) {
		allPlugins.remove(plugin);
		if (plugin.isActive()) {
			plugin.removePluginItems(() -> Algorithms.removeAllFiles(plugin.getPluginDir()));
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
				log.error(e);
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
				json.put("version", plugin.getVersion());
				plugin.writeAdditionalDataToJson(json);
				plugin.writeDependentFilesJson(json);
				itemsJson.put(json);
			} catch (JSONException e) {
				log.error(e);
			}
		}
		String jsonStr = itemsJson.toString();
		if (!jsonStr.equals(settingsAPI.getString(pluginPrefs, CUSTOM_PLUGINS_KEY, ""))) {
			settingsAPI.edit(pluginPrefs).putString(CUSTOM_PLUGINS_KEY, jsonStr).commit();
		}
	}

	private static void enablePluginsByDefault(@NonNull OsmandApplication app, @NonNull Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			if (plugin.isEnableByDefault()
					&& !enabledPlugins.contains(plugin.getId())
					&& !isPluginDisabledManually(app, plugin)) {
				enabledPlugins.add(plugin.getId());
				app.getSettings().enablePlugin(plugin.getId(), true);
			}
		}
	}

	private static void activatePlugins(OsmandApplication app, Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			if (enabledPlugins.contains(plugin.getId()) || plugin.isEnabled()) {
				initPlugin(app, plugin);
			}
		}
		app.getMapButtonsHelper().updateActionTypes();
	}

	private static void initPlugin(OsmandApplication app, OsmandPlugin plugin) {
		try {
			if (plugin.init(app, null)) {
				plugin.setEnabled(true);
			}
		} catch (Exception e) {
			log.error("Plugin initialization failed " + plugin.getId(), e);
		}
	}

	private static void checkMarketPlugin(@NonNull OsmandApplication app,
	                                      @NonNull OsmandPlugin plugin) {
		if (updateMarketPlugin(app, plugin)) {
			allPlugins.add(plugin);
		}
	}

	private static boolean updateMarketPlugin(@NonNull OsmandApplication app,
	                                          @NonNull OsmandPlugin plugin) {
		boolean marketEnabled = Version.isMarketEnabled();
		boolean available = plugin.isAvailable(app);
		boolean paid = plugin.isPaid();
		boolean processed = false;
		// for test reasons
		//if ((Version.isDeveloperVersion(app) || !Version.isProductionVersion(app)) && !paid) {
		//	marketEnabled = false;
		//}
		if (available || (!marketEnabled && !paid)) {
			plugin.setInstallURL(null);
			processed = true;
		} else if (marketEnabled) {
			plugin.setInstallURL(Version.getUrlWithUtmRef(app, plugin.getComponentId1()));
			processed = true;
		}
		return processed;
	}

	public static void checkInstalledMarketPlugins(@NonNull OsmandApplication app, @Nullable Activity activity) {
		for (OsmandPlugin plugin : getMarketPlugins()) {
			if (plugin.getInstallURL() != null && plugin.isAvailable(app)) {
				plugin.onInstall(app, activity);
				initPlugin(app, plugin);
			}
			updateMarketPlugin(app, plugin);
		}
		app.getMapButtonsHelper().updateActionTypes();
	}


	public static boolean checkPluginPackage(@NonNull OsmandApplication app, @NonNull OsmandPlugin plugin) {
		return isPackageInstalled(plugin.getComponentId1(), app) || isPackageInstalled(plugin.getComponentId2(), app);
	}

	public static boolean enablePluginIfNeeded(@Nullable Activity activity,
	                                           @NonNull OsmandApplication app,
	                                           @Nullable OsmandPlugin plugin,
	                                           boolean enable) {
		if (plugin != null) {
			boolean stateChanged = enable != plugin.isEnabled();
			boolean canChangeState = !enable || !plugin.isLocked();
			if (stateChanged && canChangeState) {
				return enablePlugin(activity, app, plugin, enable);
			}
		}
		return false;
	}

	public static boolean enablePlugin(@Nullable Activity activity,
	                                   @NonNull OsmandApplication app,
	                                   @NonNull OsmandPlugin plugin,
	                                   boolean enable) {
		if (enable) {
			if (!plugin.init(app, activity)) {
				plugin.setEnabled(false);
				return false;
			} else {
				plugin.setEnabled(true);
			}
		} else {
			plugin.disable(app);
			plugin.setEnabled(false);
		}
		app.getSettings().enablePlugin(plugin.getId(), enable);
		app.getMapButtonsHelper().updateActionTypes();
		if (activity != null) {
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				plugin.updateLayers(mapActivity, mapActivity);
				MapLayers mapLayers = app.getOsmandMap().getMapLayers();
				mapLayers.getMapInfoLayer().recreateAllControls(mapActivity);
				mapActivity.getDashboard().refreshDashboardFragments();

				DashFragmentData fragmentData = plugin.getCardFragment();
				if (!enable && fragmentData != null) {
					FragmentManager fm = mapActivity.getSupportFragmentManager();
					Fragment fragment = fm.findFragmentByTag(fragmentData.tag);
					if (fragment != null) {
						fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
					}
				}
			}

			if (plugin.isMarketPlugin() || plugin.isPaid()) {
				if (plugin.isActive()) {
					plugin.showInstallDialog(activity);
				} else if (checkPluginPackage(app, plugin)) {
					plugin.showDisableDialog(activity);
				}
			}
		}
		return true;
	}

	private static void registerAppInitializingDependedProperties(@NonNull OsmandApplication app) {
		app.getAppInitializer().addListener(new AppInitializeListener() {

			@Override
			public void onFinish(@NonNull AppInitializer init) {
				registerRenderingPreferences(app);
			}
		});
	}


	public static void onRequestPermissionsResult(int requestCode, String[] permissions,
	                                              int[] grantResults) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.handleRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}


	public static void refreshLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.updateLayers(context, mapActivity);
		}
	}

	@NonNull
	public static List<OsmandPlugin> getAvailablePlugins() {
		return new ArrayList<>(allPlugins);
	}

	@NonNull
	public static List<OsmandPlugin> getEnabledPlugins() {
		List<OsmandPlugin> availablePlugins = getAvailablePlugins();
		List<OsmandPlugin> plugins = new ArrayList<>(availablePlugins.size());
		for (OsmandPlugin plugin : availablePlugins) {
			if (plugin.isEnabled()) {
				plugins.add(plugin);
			}
		}
		return plugins;
	}

	@NonNull
	public static List<OsmandPlugin> getActivePlugins() {
		List<OsmandPlugin> availablePlugins = getAvailablePlugins();
		List<OsmandPlugin> plugins = new ArrayList<>(availablePlugins.size());
		for (OsmandPlugin plugin : availablePlugins) {
			if (plugin.isActive()) {
				plugins.add(plugin);
			}
		}
		return plugins;
	}

	@NonNull
	public static List<OsmandPlugin> getNotActivePlugins() {
		List<OsmandPlugin> availablePlugins = getAvailablePlugins();
		List<OsmandPlugin> plugins = new ArrayList<>(availablePlugins.size());
		for (OsmandPlugin plugin : availablePlugins) {
			if (!plugin.isActive()) {
				plugins.add(plugin);
			}
		}
		return plugins;
	}

	@NonNull
	public static List<OsmandPlugin> getMarketPlugins() {
		List<OsmandPlugin> availablePlugins = getAvailablePlugins();
		List<OsmandPlugin> plugins = new ArrayList<>(availablePlugins.size());
		for (OsmandPlugin plugin : availablePlugins) {
			if (plugin.isMarketPlugin()) {
				plugins.add(plugin);
			}
		}
		return plugins;
	}

	@NonNull
	public static List<CustomOsmandPlugin> getCustomPlugins() {
		List<OsmandPlugin> availablePlugins = getAvailablePlugins();
		List<CustomOsmandPlugin> customPlugins = new ArrayList<>(availablePlugins.size());
		for (OsmandPlugin plugin : availablePlugins) {
			if (plugin instanceof CustomOsmandPlugin) {
				customPlugins.add((CustomOsmandPlugin) plugin);
			}
		}
		return customPlugins;
	}

	@NonNull
	public static List<OsmandPlugin> getEnabledSettingsScreenPlugins() {
		List<OsmandPlugin> plugins = new ArrayList<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin.getSettingsScreenType() != null) {
				plugins.add(plugin);
			}
		}
		return plugins;
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
	public static <T extends OsmandPlugin> T getActivePlugin(Class<T> clz) {
		for (OsmandPlugin lr : getActivePlugins()) {
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

	public static <T extends OsmandPlugin> boolean isEnabled(Class<T> clz) {
		return getEnabledPlugin(clz) != null;
	}

	public static <T extends OsmandPlugin> boolean isActive(Class<T> clz) {
		return getActivePlugin(clz) != null;
	}

	public static boolean isPluginDisabledManually(OsmandApplication app, OsmandPlugin plugin) {
		return app.getSettings().getPlugins().contains("-" + plugin.getId());
	}

	public static List<WorldRegion> getCustomDownloadRegions() {
		List<WorldRegion> l = new ArrayList<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			l.addAll(plugin.getDownloadMaps());
		}
		return l;
	}

	@NonNull
	public static List<IndexItem> getCustomDownloadItems() {
		List<IndexItem> items = new ArrayList<>();
		for (WorldRegion region : getCustomDownloadRegions()) {
			collectIndexItemsFromSubregion(region, items);
		}
		return items;
	}

	public static void collectIndexItemsFromSubregion(WorldRegion region, List<IndexItem> items) {
		if (region instanceof CustomRegion) {
			items.addAll(((CustomRegion) region).loadIndexItems());
		}
		for (WorldRegion subregion : region.getSubregions()) {
			collectIndexItemsFromSubregion(subregion, items);
		}
	}

	public static void attachAdditionalInfoToRecordedTrack(@NonNull Location location, @NonNull JSONObject json) {
		try {
			for (OsmandPlugin plugin : getEnabledPlugins()) {
				plugin.attachAdditionalInfoToRecordedTrack(location, json);
			}
		} catch (JSONException e) {
			log.error(e);
		}
	}

	public static List<String> getDisabledRendererNames() {
		List<String> l = new ArrayList<>();
		for (OsmandPlugin plugin : getNotActivePlugins()) {
			l.addAll(plugin.getRendererNames());
		}
		return l;
	}

	public static List<String> getDisabledRouterNames() {
		List<String> l = new ArrayList<>();
		for (OsmandPlugin plugin : getNotActivePlugins()) {
			l.addAll(plugin.getRouterNames());
		}
		return l;
	}

	public static List<String> onIndexingFiles(@Nullable IProgress progress) {
		List<String> l = new ArrayList<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			List<String> ls = plugin.indexingFiles(progress);
			if (ls != null && ls.size() > 0) {
				l.addAll(ls);
			}
		}
		return l;
	}

	public static void onMapActivityCreate(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityCreate(activity);
		}
	}

	public static void onMapActivityResume(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityResume(activity);
		}
	}

	public static void onMapActivityResumeOnTop(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityResumeOnTop(activity);
		}
	}

	public static void onMapActivityPause(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityPause(activity);
		}
	}

	public static void onMapActivityDestroy(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityDestroy(activity);
		}
	}

	public static void onMapActivityResult(int requestCode, int resultCode, Intent data) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.onMapActivityExternalResult(requestCode, resultCode, data);
		}
	}

	public static void onMapActivityScreenOff(@NonNull MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityScreenOff(activity);
		}
	}

	public static void createLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerLayers(context, mapActivity);
		}
	}

	public static void createMapWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetInfos, @NonNull ApplicationMode appMode) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.createWidgets(mapActivity, widgetInfos, appMode);
		}
	}

	@Nullable
	public static MapWidget createMapWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			MapWidget widget = plugin.createMapWidgetForParams(mapActivity, widgetType, customId, widgetsPanel);
			if (widget != null) {
				return widget;
			}
		}
		return null;
	}

	public static void registerMapContextMenu(@NonNull MapActivity mapActivity, double latitude, double longitude,
	                                          ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerMapContextMenuActions(mapActivity, latitude, longitude, adapter, selectedObj, configureMenu);
		}
	}

	public static void registerLayerContextMenu(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.registerLayerContextMenuActions(adapter, mapActivity, customRules);
		}
	}

	public static void registerConfigureMapCategory(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerConfigureMapCategoryActions(adapter, mapActivity, customRules);
		}
	}

	public static void registerRenderingPreferences(@NonNull OsmandApplication app) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer == null) return;

		List<RenderingRuleProperty> customRules = new ArrayList<>(renderer.PROPS.getCustomRules());
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			String prefix = plugin.getRenderPropertyPrefix();
			if (prefix != null) {
				Iterator<RenderingRuleProperty> iterator = customRules.iterator();
				while (iterator.hasNext()) {
					RenderingRuleProperty property = iterator.next();
					if (property.getAttrName().startsWith(prefix)) {
						iterator.remove();
						if (property.isBoolean()) {
							plugin.registerBooleanRenderingPreference(property);
						} else {
							plugin.registerRenderingPreference(property);
						}
					}
				}
			}
		}
	}

	public static void registerOptionsMenu(MapActivity map, ContextMenuAdapter helper) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerOptionsMenuItems(map, helper);
		}
	}

	public static void onOptionsMenuActivity(FragmentActivity activity, Fragment fragment, Set<TrackItem> selectedItems, List<PopUpMenuItem> items) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.optionsMenuFragment(activity, fragment, selectedItems, items);
		}
	}

	public static boolean onSearchFinished(QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		boolean processed = false;
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			processed = plugin.searchFinished(searchFragment, phrase, isResultEmpty) || processed;
		}
		return processed;
	}

	public static void onNewDownloadIndexes(Fragment fragment) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.newDownloadIndexes(fragment);
		}
	}

	public static void onPrepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilters) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.prepareExtraTopPoiFilters(poiUIFilters);
		}
	}

	public static String onGetMapObjectPreferredLang(MapObject object, String preferredMapLang, String preferredMapAppLang) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			String locale = plugin.getMapObjectPreferredLang(object, preferredMapLang);
			if (locale != null) {
				return locale;
			}
		}
		return preferredMapAppLang;
	}

	public static String onGetMapObjectsLocale(Amenity amenity, String preferredLocale) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			String locale = plugin.getMapObjectsLocale(amenity, preferredLocale);
			if (locale != null) {
				return locale;
			}
		}
		return preferredLocale;
	}

	public static void registerCustomPoiFilters(List<PoiUIFilter> poiUIFilters) {
		for (OsmandPlugin p : getAvailablePlugins()) {
			poiUIFilters.addAll(p.getCustomPoiFilters());
		}
	}

	public static Collection<DashFragmentData> getPluginsCardsList() {
		HashSet<DashFragmentData> collection = new HashSet<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			DashFragmentData fragmentData = plugin.getCardFragment();
			if (fragmentData != null) collection.add(fragmentData);
		}
		return collection;
	}

	public static void populateContextMenuImageCards(@NonNull ImageCardsHolder holder, @NonNull Map<String, String> params,
	                                                 @Nullable Map<String, String> additionalParams, @Nullable GetImageCardsListener listener) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.collectContextMenuImageCards(holder, params, additionalParams, listener);
		}
	}

	/**
	 * @param holder      an object to collect results
	 * @param imageObject json object that contains data for create an image card
	 * @return 'true' if an image card was created
	 */
	public static boolean createImageCardForJson(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin.createContextMenuImageCard(holder, imageObject)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPackageInstalled(@Nullable String packageInfo, @NonNull Context ctx) {
		if (packageInfo == null) {
			return false;
		}
		boolean installed = false;
		try {
			installed = ctx.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch (NameNotFoundException e) {
			log.info("Package not found: " + packageInfo);
		}
		return installed;
	}

	public static void addCommonKeyEventAssignments(@NonNull List<KeyAssignment> assignments) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.addCommonKeyEventAssignments(assignments);
		}
	}

	@Nullable
	public static KeyEventCommand createKeyEventCommand(@NonNull String commandId) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			KeyEventCommand command = plugin.createKeyEventCommand(commandId);
			if (command != null) {
				return command;
			}
		}
		return null;
	}

	public static boolean layerShouldBeDisabled(@NonNull OsmandMapLayer layer) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin.layerShouldBeDisabled(layer)) {
				return true;
			}
		}
		return false;
	}

	public static void registerQuickActionTypesPlugins(List<QuickActionType> allTypes,
	                                                   List<QuickActionType> enabledTypes) {
		for (OsmandPlugin p : getAvailablePlugins()) {
			List<QuickActionType> types = p.getQuickActionTypes();
			allTypes.addAll(types);
			if (p.isEnabled()) {
				enabledTypes.addAll(types);
			}
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

	public static void addMyPlacesTabPlugins(MyPlacesActivity myPlacesActivity, List<TabItem> mTabs, Intent intent) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.addMyPlacesTab(myPlacesActivity, mTabs, intent);
		}
	}

	public static void updateMapPresentationEnvironment(MapRendererContext mapRendererContext) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.updateMapPresentationEnvironment(mapRendererContext);
		}
	}

	public static TrackPointsAnalyser getTrackPointsAnalyser() {
		List<TrackPointsAnalyser> trackPointsAnalysers = new ArrayList<>();
		for (OsmandPlugin plugin : getActivePlugins()) {
			TrackPointsAnalyser analyser = plugin.getTrackPointsAnalyser();
			if (analyser != null) {
				trackPointsAnalysers.add(analyser);
			}
		}
		if(!isActive(ExternalSensorsPlugin.class)) {
			OsmandPlugin plugin = getPlugin(ExternalSensorsPlugin.class);
			if(plugin != null) {
				trackPointsAnalysers.add(plugin.getTrackPointsAnalyser());
			}
		}
		return (gpxTrackAnalysis, wptPt, pointAttributes) -> {
			for (TrackPointsAnalyser analyser : trackPointsAnalysers) {
				analyser.onAnalysePoint(gpxTrackAnalysis, wptPt, pointAttributes);
			}
		};
	}

	@Nullable
	public static OrderedLineDataSet getOrderedLineDataSet(@NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean calcWithoutGaps, boolean useRightAxis) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			OrderedLineDataSet dataSet = plugin.getOrderedLineDataSet(chart, analysis, graphType, axisType, calcWithoutGaps, useRightAxis);
			if (dataSet != null) {
				return dataSet;
			}
		}
		return null;
	}

	public static void getAvailableGPXDataSetTypes(@NonNull GPXTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.getAvailableGPXDataSetTypes(analysis, availableTypes);
		}
	}

	public static void onIndexItemDownloaded(@NonNull IndexItem item, boolean updatingFile) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.onIndexItemDownloaded(item, updatingFile);
		}
	}
}

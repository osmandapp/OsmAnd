package net.osmand.plus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.data.LocationPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginsActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.views.OsmandMapTileView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME;

public class OsmAndAppCustomization {

	private static final int MAX_NAV_DRAWER_ITEMS_PER_APP = 3;

	protected OsmandApplication app;
	protected OsmandSettings osmandSettings;

	private Bitmap navDrawerLogo;
	private ArrayList<String> navDrawerParams;

	private String navDrawerFooterIntent;
	private String navDrawerFooterAppName;
	private String navDrawerFooterPackageName;

	private Set<String> featuresEnabledIds = new HashSet<>();
	private Set<String> featuresDisabledIds = new HashSet<>();
	private Set<String> featuresEnabledPatterns = new HashSet<>();
	private Set<String> featuresDisabledPatterns = new HashSet<>();
	private Map<String, Set<ApplicationMode>> widgetsVisibilityMap = new LinkedHashMap<>();
	private Map<String, Set<ApplicationMode>> widgetsAvailabilityMap = new LinkedHashMap<>();
	private CustomOsmandSettings customOsmandSettings;

	private boolean featuresCustomized;
	private boolean widgetsCustomized;

	private List<OsmAndAppCustomizationListener> listeners = new ArrayList<>();

	public interface OsmAndAppCustomizationListener {

		void onOsmAndSettingsCustomized();
	}

	public static class CustomOsmandSettings {
		private String sharedPreferencesName;
		private OsmandSettings settings;

		CustomOsmandSettings(OsmandApplication app, String sharedPreferencesName, Bundle bundle) {
			this.sharedPreferencesName = sharedPreferencesName;
			this.settings = new OsmandSettings(app, new net.osmand.plus.api.SettingsAPIImpl(app), sharedPreferencesName);
			if (bundle != null) {
				for (String key : bundle.keySet()) {
					Object object = bundle.get(key);
					this.settings.setPreference(key, object);
				}
			}
		}

		public OsmandSettings getSettings() {
			return settings;
		}
	}

	public void setup(OsmandApplication app) {
		this.app = app;
		this.osmandSettings = new OsmandSettings(app, new net.osmand.plus.api.SettingsAPIImpl(app));
	}

	public OsmandSettings getOsmandSettings() {
		return customOsmandSettings != null ? customOsmandSettings.getSettings() : osmandSettings;
	}

	public void customizeOsmandSettings(@NonNull String sharedPreferencesName, @Nullable Bundle bundle) {
		customOsmandSettings = new CustomOsmandSettings(app, sharedPreferencesName, bundle);
		OsmandSettings newSettings = customOsmandSettings.getSettings();
		if (Build.VERSION.SDK_INT < 19) {
			if (osmandSettings.isExternalStorageDirectorySpecifiedPre19()) {
				File externalStorageDirectory = osmandSettings.getExternalStorageDirectoryPre19();
				newSettings.setExternalStorageDirectoryPre19(externalStorageDirectory.getAbsolutePath());
			}
		} else if (osmandSettings.isExternalStorageDirectoryTypeSpecifiedV19()
				&& osmandSettings.isExternalStorageDirectorySpecifiedV19()) {
			int type = osmandSettings.getExternalStorageDirectoryTypeV19();
			String directory = osmandSettings.getExternalStorageDirectoryV19();
			newSettings.setExternalStorageDirectoryV19(type, directory);
		}
		app.setOsmandSettings(newSettings);
		notifySettingsCustomized();
	}

	public void restoreOsmandSettings() {
		app.setOsmandSettings(osmandSettings);
		notifySettingsCustomized();
	}

	public boolean restoreOsmand() {
		navDrawerLogo = null;
		featuresCustomized = false;
		widgetsCustomized = false;
		customOsmandSettings = null;
		restoreOsmandSettings();

		featuresEnabledIds.clear();
		featuresDisabledIds.clear();
		featuresEnabledPatterns.clear();
		featuresDisabledPatterns.clear();
		widgetsVisibilityMap.clear();
		widgetsAvailabilityMap.clear();

		return true;
	}

	// Activities
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsActivity.class;
	}

	public Class<MapActivity> getMapActivity() {
		return MapActivity.class;
	}

	public Class<TrackActivity> getTrackActivity() {
		return TrackActivity.class;
	}

	public Class<FavoritesActivity> getFavoritesActivity() {
		return FavoritesActivity.class;
	}

	public Class<? extends Activity> getDownloadIndexActivity() {
		return DownloadActivity.class;
	}

	public Class<? extends Activity> getPluginsActivity() {
		return PluginsActivity.class;
	}

	public Class<? extends Activity> getDownloadActivity() {
		return DownloadActivity.class;
	}

	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		return Collections.emptyList();
	}

	public String getIndexesUrl() {
		return "https://" + IndexConstants.INDEX_DOWNLOAD_DOMAIN + "/get_indexes?gzip&" + Version.getVersionAsURLParam(app);
	}

	public boolean showDownloadExtraActions() {
		return true;
	}

	public File getTracksDir() {
		return app.getAppPath(IndexConstants.GPX_RECORDED_INDEX_DIR);
	}

	public void createLayers(OsmandMapTileView mapView, MapActivity activity) {

	}

	public List<? extends LocationPoint> getWaypoints() {
		return Collections.emptyList();
	}

	public boolean isWaypointGroupVisible(int waypointType, RouteCalculationResult route) {
		if (waypointType == WaypointHelper.ALARMS) {
			return route != null && !route.getAlarmInfo().isEmpty();
		} else if (waypointType == WaypointHelper.WAYPOINTS) {
			return route != null && !route.getLocationPoints().isEmpty();
		}
		return true;
	}

	public boolean onDestinationReached() {
		return true;
	}

	@Nullable
	public Bitmap getNavDrawerLogo() {
		return navDrawerLogo;
	}

	@Nullable
	public ArrayList<String> getNavDrawerLogoParams() {
		return navDrawerParams;
	}

	public boolean setNavDrawerLogo(String uri, @Nullable String packageName, @Nullable String intent) {
		if (TextUtils.isEmpty(uri)) {
			navDrawerLogo = null;
		} else {
			try {
				InputStream is = app.getContentResolver().openInputStream(Uri.parse(uri));
				if (is != null) {
					navDrawerLogo = BitmapFactory.decodeStream(is);
					is.close();

				}
			} catch (FileNotFoundException e) {
				return false;
			} catch (IOException e) {
				// ignore
			}
			if (packageName != null && intent != null) {
				navDrawerParams = new ArrayList<>();
				navDrawerParams.add(packageName);
				navDrawerParams.add(intent);
			}
		}
		return true;
	}

	public boolean setNavDrawerFooterParams(String uri, @Nullable String packageName, @Nullable String intent) {
		navDrawerFooterAppName = uri;
		navDrawerFooterIntent = intent;
		navDrawerFooterPackageName = packageName;
		return true;
	}

	public String getNavFooterAppName() {
		return navDrawerFooterAppName;
	}

	public void setFeaturesEnabledIds(@NonNull Collection<String> ids) {
		featuresEnabledIds.clear();
		featuresEnabledIds.addAll(ids);
		setFeaturesCustomized();
	}

	public void setFeaturesDisabledIds(@NonNull Collection<String> ids) {
		featuresDisabledIds.clear();
		featuresDisabledIds.addAll(ids);
		setFeaturesCustomized();
	}

	public void setFeaturesEnabledPatterns(@NonNull Collection<String> patterns) {
		featuresEnabledPatterns.clear();
		featuresEnabledPatterns.addAll(patterns);
		setFeaturesCustomized();
	}

	public void setFeaturesDisabledPatterns(@NonNull Collection<String> patterns) {
		featuresDisabledPatterns.clear();
		featuresDisabledPatterns.addAll(patterns);
		setFeaturesCustomized();
	}

	public Set<ApplicationMode> regWidgetVisibility(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		HashSet<ApplicationMode> set = getAppModesSet(appModeKeys);
		widgetsVisibilityMap.put(widgetId, set);
		setWidgetsCustomized();
		return set;
	}

	public Set<ApplicationMode> regWidgetAvailability(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		HashSet<ApplicationMode> set = getAppModesSet(appModeKeys);
		widgetsAvailabilityMap.put(widgetId, set);
		setWidgetsCustomized();
		return set;
	}

	public boolean isWidgetVisible(@NonNull String key, ApplicationMode appMode) {
		Set<ApplicationMode> set = widgetsVisibilityMap.get(key);
		if (set == null) {
			return false;
		}
		return set.contains(appMode);
	}

	public boolean isWidgetAvailable(@NonNull String key, ApplicationMode appMode) {
		Set<ApplicationMode> set = widgetsAvailabilityMap.get(key);
		if (set == null) {
			return true;
		}
		return set.contains(appMode);
	}

	public boolean setNavDrawerLogoWithParams(String imageUri, @Nullable String packageName,
	                                          @Nullable String intent) {
		return setNavDrawerLogo(imageUri, packageName, intent);
	}

	public boolean changePluginStatus(String pluginId, int newState) {
		if (newState == 0) {
			for (OsmandPlugin plugin : OsmandPlugin.getEnabledPlugins()) {
				if (plugin.getId().equals(pluginId)) {
					OsmandPlugin.enablePlugin(null, app, plugin, false);
				}
			}
			return true;
		}

		if (newState == 1) {
			for (OsmandPlugin plugin : OsmandPlugin.getAvailablePlugins()) {
				if (plugin.getId().equals(pluginId)) {
					OsmandPlugin.enablePlugin(null, app, plugin, true);
				}
			}
			return true;
		}

		return false;
	}

	public boolean setNavDrawerItems(String appPackage, List<NavDrawerItem> items) {
		if (!TextUtils.isEmpty(appPackage) && items != null) {
			clearNavDrawerItems(appPackage);
			if (items.isEmpty()) {
				return true;
			}
			List<NavDrawerItem> newItems = new ArrayList<>(MAX_NAV_DRAWER_ITEMS_PER_APP);
			boolean success = true;
			for (int i = 0; i < items.size() && i <= MAX_NAV_DRAWER_ITEMS_PER_APP; i++) {
				NavDrawerItem item = items.get(i);
				if (!TextUtils.isEmpty(item.name) && !TextUtils.isEmpty(item.uri)) {
					newItems.add(item);
				} else {
					success = false;
					break;
				}
			}
			if (success) {
				saveNavDrawerItems(appPackage, newItems);
			}
			return success;
		}
		return false;
	}

	private void clearNavDrawerItems(String appPackage) {
		try {
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			allItems.put(appPackage, new JSONArray());
			app.getSettings().API_NAV_DRAWER_ITEMS_JSON.set(allItems.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void saveNavDrawerItems(String appPackage, List<NavDrawerItem> items) {
		try {
			JSONArray jArray = new JSONArray();
			for (NavDrawerItem item : items) {
				JSONObject obj = new JSONObject();
				obj.put(NavDrawerItem.NAME_KEY, item.name);
				obj.put(NavDrawerItem.URI_KEY, item.uri);
				obj.put(NavDrawerItem.ICON_NAME_KEY, item.iconName);
				obj.put(NavDrawerItem.FLAGS_KEY, item.flags);
				jArray.put(obj);
			}
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			allItems.put(appPackage, jArray);
			app.getSettings().API_NAV_DRAWER_ITEMS_JSON.set(allItems.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void registerNavDrawerItems(final Activity activity, ContextMenuAdapter adapter) {
		PackageManager pm = activity.getPackageManager();
		for (Map.Entry<String, List<NavDrawerItem>> entry : getNavDrawerItems().entrySet()) {
			String appPackage = entry.getKey();
			for (NavDrawerItem item : entry.getValue()) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.uri));
				if (intent.resolveActivity(pm) == null) {
					intent = pm.getLaunchIntentForPackage(appPackage);
				}
				if (intent != null) {
					if (item.flags != -1) {
						intent.addFlags(item.flags);
					}
					final Intent finalIntent = intent;
					adapter.addItem(new ContextMenuItem.ItemBuilder()
							.setId(item.getId())
							.setTitle(item.name)
							.setIcon(getIconId(item.iconName))
							.setListener(new ContextMenuAdapter.ItemClickListener() {
								@Override
								public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
									activity.startActivity(finalIntent);
									return true;
								}
							})
							.createItem());
				}
			}
		}
	}

	private Map<String, List<NavDrawerItem>> getNavDrawerItems() {
		Map<String, List<NavDrawerItem>> res = new LinkedHashMap<>();
		try {
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			for (Iterator<?> it = allItems.keys(); it.hasNext(); ) {
				String appPackage = (String) it.next();
				OsmandAidlApi.ConnectedApp connectedApp = app.getAidlApi().getConnectedApp(appPackage);
				if (connectedApp != null && connectedApp.isEnabled()) {
					JSONArray jArray = allItems.getJSONArray(appPackage);
					List<NavDrawerItem> list = new ArrayList<>();
					for (int i = 0; i < jArray.length(); i++) {
						JSONObject obj = jArray.getJSONObject(i);
						list.add(new NavDrawerItem(
								obj.optString(NavDrawerItem.NAME_KEY),
								obj.optString(NavDrawerItem.URI_KEY),
								obj.optString(NavDrawerItem.ICON_NAME_KEY),
								obj.optInt(NavDrawerItem.FLAGS_KEY, -1)
						));
					}
					res.put(appPackage, list);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return res;
	}

	private int getIconId(@Nullable String iconName) {
		if (!TextUtils.isEmpty(iconName)) {
			int id = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
			return id == 0 ? -1 : id;
		}
		return -1;
	}

	@NonNull
	private HashSet<ApplicationMode> getAppModesSet(@Nullable List<String> appModeKeys) {
		HashSet<ApplicationMode> set = new HashSet<>();
		List<ApplicationMode> values = ApplicationMode.allPossibleValues();
		if (appModeKeys == null) {
			set.addAll(values);
		} else {
			for (String key : appModeKeys) {
				ApplicationMode am = ApplicationMode.valueOfStringKey(key, null);
				if (am != null) {
					set.add(am);
				}
			}
		}
		for (ApplicationMode m : values) {
			// add derived modes
			if (set.contains(m.getParent())) {
				set.add(m);
			}
		}
		return set;
	}

	public boolean isFeatureEnabled(@NonNull String id) {
		if (!featuresCustomized) {
			return true;
		}
		if (featuresEnabledIds.contains(id)) {
			return true;
		}
		if (featuresDisabledIds.contains(id)) {
			return false;
		}
		if (isMatchesPattern(id, featuresEnabledPatterns)) {
			return true;
		}
		return !isMatchesPattern(id, featuresDisabledPatterns);
	}

	public boolean isOsmandCustomized() {
		return areWidgetsCustomized() || areFeaturesCustomized() || areSettingsCustomized();
	}

	public boolean areWidgetsCustomized() {
		return widgetsCustomized;
	}

	public boolean areFeaturesCustomized() {
		return featuresCustomized;
	}

	public boolean areSettingsCustomized() {
		return customOsmandSettings != null;
	}

	public boolean areSettingsCustomizedForPreference(String sharedPreferencesName) {
		if (customOsmandSettings != null && customOsmandSettings.sharedPreferencesName.equals(sharedPreferencesName)) {
			return true;
		}
		return OsmandSettings.areSettingsCustomizedForPreference(sharedPreferencesName, app);
	}

	private void setFeaturesCustomized() {
		featuresCustomized = true;
	}

	private void setWidgetsCustomized() {
		widgetsCustomized = true;
	}

	private boolean isMatchesPattern(@NonNull String id, @NonNull Set<String> patterns) {
		for (String pattern : patterns) {
			if (id.startsWith(pattern)) {
				return true;
			}
		}
		return false;
	}

	private void notifySettingsCustomized() {
		app.uiHandler.post(new Runnable() {

			@Override
			public void run() {
				for (OsmAndAppCustomizationListener l : listeners) {
					l.onOsmAndSettingsCustomized();
				}
			}
		});
	}

	public void addListener(OsmAndAppCustomizationListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(OsmAndAppCustomizationListener listener) {
		this.listeners.remove(listener);
	}

	public static class NavDrawerItem {

		static final String NAME_KEY = "name";
		static final String URI_KEY = "uri";
		static final String ICON_NAME_KEY = "icon_name";
		static final String FLAGS_KEY = "flags";

		private String name;
		private String uri;
		private String iconName;
		private int flags;

		public NavDrawerItem(String name, String uri, String iconName, int flags) {
			this.name = name;
			this.uri = uri;
			this.iconName = iconName;
			this.flags = flags;
		}

		public String getId() {
			return DRAWER_ITEM_ID_SCHEME + name;
		}
	}
}
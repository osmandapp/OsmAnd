package net.osmand.plus;

import static net.osmand.plus.osmedit.OpenstreetmapLocalUtil.LOG;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.aidl.navdrawer.NavDrawerFooterParams;
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams;
import net.osmand.aidl.plugins.PluginParams;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsmAndAppCustomization {

	protected OsmandApplication app;
	protected OsmandSettings osmandSettings;

	private Bitmap navDrawerLogo;
	private ArrayList<String> navDrawerParams;
	private NavDrawerFooterParams navDrawerFooterParams;

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
	public ArrayList<String> getNavDrawerLogoParams() {return navDrawerParams; }

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

	public boolean setNavDrawerFooterParams(NavDrawerFooterParams params) {
		navDrawerFooterParams = params;
		return true;
	}

	public NavDrawerFooterParams getNavFooterParams() {
		return navDrawerFooterParams;
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

	public boolean changePluginStatus(PluginParams params) {
		if (params.getNewState() == 0) {
			for (OsmandPlugin plugin : OsmandPlugin.getEnabledPlugins()) {
				if (plugin.getId().equals(params.getPluginId())) {
					OsmandPlugin.enablePlugin(null, app, plugin, false);
				}
			}
			return true;
		}

		if (params.getNewState() == 1) {
			for (OsmandPlugin plugin : OsmandPlugin.getAvailablePlugins()) {
				if (plugin.getId().equals(params.getPluginId())) {
					OsmandPlugin.enablePlugin(null, app, plugin, true);
				}
			}
			return true;
		}

		return false;
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

	public boolean areWidgetsCustomized() {
		return widgetsCustomized;
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
}

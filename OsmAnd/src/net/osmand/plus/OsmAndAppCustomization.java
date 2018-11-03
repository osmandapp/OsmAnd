package net.osmand.plus;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsmAndAppCustomization {

	protected OsmandApplication app;
	protected OsmandSettings osmandSettings;

	private Bitmap navDrawerLogo;

	private Set<String> enabledIds = new HashSet<>();
	private Set<String> disabledIds = new HashSet<>();
	private Set<String> enabledPatterns = new HashSet<>();
	private Set<String> disabledPatterns = new HashSet<>();

	private boolean customizationEnabled;

	public void setup(OsmandApplication app) {
		this.app = app;
		this.osmandSettings = new OsmandSettings(app, new net.osmand.plus.api.SettingsAPIImpl(app));
	}

	public OsmandSettings getOsmandSettings() {
		return osmandSettings;
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
		return "http://" + IndexConstants.INDEX_DOWNLOAD_DOMAIN + "/get_indexes?gzip&" + Version.getVersionAsURLParam(app);
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

	public boolean setNavDrawerLogo(@Nullable String uri) {
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
		}
		return true;
	}

	public void setEnabledIds(@NonNull Collection<String> ids) {
		enabledIds.clear();
		enabledIds.addAll(ids);
		updateCustomizationEnabled();
	}

	public void setDisabledIds(@NonNull Collection<String> ids) {
		disabledIds.clear();
		disabledIds.addAll(ids);
		updateCustomizationEnabled();
	}

	public void setEnabledPatterns(@NonNull Collection<String> patterns) {
		enabledPatterns.clear();
		enabledPatterns.addAll(patterns);
		updateCustomizationEnabled();
	}

	public void setDisabledPatterns(@NonNull Collection<String> patterns) {
		disabledPatterns.clear();
		disabledPatterns.addAll(patterns);
		updateCustomizationEnabled();
	}

	public boolean isFeatureEnabled(@NonNull String id) {
		if (!customizationEnabled) {
			return true;
		}
		if (enabledIds.contains(id)) {
			return true;
		}
		if (disabledIds.contains(id)) {
			return false;
		}
		if (isMatchesPattern(id, enabledPatterns)) {
			return true;
		}
		return !isMatchesPattern(id, disabledPatterns);
	}

	private void updateCustomizationEnabled() {
		customizationEnabled = !enabledIds.isEmpty() || !disabledIds.isEmpty()
				|| !enabledPatterns.isEmpty() || !disabledPatterns.isEmpty();
	}

	private boolean isMatchesPattern(@NonNull String id, @NonNull Set<String> patterns) {
		for (String pattern : patterns) {
			if (id.startsWith(pattern)) {
				return true;
			}
		}
		return false;
	}
}

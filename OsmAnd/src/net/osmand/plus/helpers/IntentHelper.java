package net.osmand.plus.helpers;

import static net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import static net.osmand.plus.track.fragments.TrackMenuFragment.CURRENT_RECORDING;
import static net.osmand.plus.track.fragments.TrackMenuFragment.OPEN_TAB_NAME;
import static net.osmand.plus.track.fragments.TrackMenuFragment.RETURN_SCREEN_NAME;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TEMPORARY_SELECTED;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TRACK_FILE_NAME;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapcontextmenu.editors.FavouriteGroupEditorFragment;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.ui.EditFavoriteGroupDialogFragment;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.openplacereviews.OPRConstants;
import net.osmand.plus.plugins.openplacereviews.OprAuthHelper.OprAuthorizationListener;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.views.mapwidgets.configure.ConfigureScreenFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentHelper {

	private static final Log LOG = PlatformUtil.getLog(IntentHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	public IntentHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
	}

	public boolean parseLaunchIntents() {
		return parseNavigationUrlIntent()
				|| parseSetPinOnMapUrlIntent()
				|| parseMoveMapToLocationUrlIntent()
				|| parseOpenLocationMenuUrlIntent()
				|| parseRedirectUrlIntent()
				|| parseTileSourceUrlIntent()
				|| parseOpenGpxUrlIntent()
				|| parseSendIntent()
				|| parseOAuthIntent()
				|| parseOprOAuthIntent();
	}

	private boolean parseNavigationUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			boolean hasNavigationDestination = data.getQueryParameterNames().contains("end");
			if (isOsmAndMapUrl(data) && hasNavigationDestination) {
				String startLatLonParam = data.getQueryParameter("start");
				String endLatLonParam = data.getQueryParameter("end");
				String appModeKeyParam = data.getQueryParameter("mode");

				if (Algorithms.isEmpty(endLatLonParam)) {
					LOG.error("Malformed OsmAnd navigation URL: destination location is missing");
					return true;
				}

				LatLon startLatLon = startLatLonParam == null ? null : parseLatLon(startLatLonParam);
				if (startLatLonParam != null && startLatLon == null) {
					LOG.error("Malformed OsmAnd navigation URL: start location is broken");
				}

				LatLon endLatLon = parseLatLon(endLatLonParam);
				if (endLatLon == null) {
					LOG.error("Malformed OsmAnd navigation URL: destination location is broken");
					return true;
				}

				ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKeyParam, null);
				if (!Algorithms.isEmpty(appModeKeyParam) && appMode == null) {
					LOG.debug("App mode with specified key not available, using default navigation app mode");
				}

				if (app.isApplicationInitializing()) {
					app.getAppInitializer().addListener(new AppInitializeListener() {

						@Override
						public void onFinish(@NonNull AppInitializer init) {
							init.removeListener(this);
							buildRoute(startLatLon, endLatLon, appMode);
						}
					});
				} else {
					buildRoute(startLatLon, endLatLon, appMode);
				}

				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private void buildRoute(@Nullable LatLon start, @NonNull LatLon end, @Nullable ApplicationMode appMode) {
		if (appMode != null) {
			app.getRoutingHelper().setAppMode(appMode);
		}
		app.getTargetPointsHelper().navigateToPoint(end, true, -1);
		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, appMode, start,
				null, false, true, MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	private boolean parseSetPinOnMapUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndMapUrl(data) && data.getQueryParameterNames().contains("pin")) {
				String latLonParam = data.getQueryParameter("pin");
				LatLon latLon = Algorithms.isEmpty(latLonParam) ? null : parseLatLon(latLonParam);
				if (latLon != null) {
					double lat = latLon.getLatitude();
					double lon = latLon.getLongitude();
					int zoom = settings.getLastKnownMapZoom();
					settings.setMapLocationToShow(lat, lon, zoom, new PointDescription(lat, lon));
				}

				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	@Nullable
	private LatLon parseLatLon(@NonNull String latLon) {
		String[] coords = latLon.split(",");
		if (coords.length != 2) {
			return null;
		}
		try {
			double lat = Double.parseDouble(coords[0]);
			double lon = Double.parseDouble(coords[1]);
			return new LatLon(lat, lon);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private boolean parseMoveMapToLocationUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			String uri = data.toString();
			String pathPrefix = "/map#";
			int pathStartIndex = uri.indexOf(pathPrefix);
			if (isOsmAndMapUrl(data) && pathStartIndex != -1) {
				String[] params = uri.substring(pathStartIndex + pathPrefix.length()).split("/");
				if (params.length == 3) {
					try {
						int zoom = Integer.parseInt(params[0]);
						double lat = Double.parseDouble(params[1]);
						double lon = Double.parseDouble(params[2]);
						settings.setMapLocationToShow(lat, lon, zoom);
					} catch (NumberFormatException e) {
						LOG.error("Invalid map URL params", e);
					}
				}
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private boolean isOsmAndMapUrl(@NonNull Uri uri) {
		return isOsmAndSite(uri) && isPathPrefix(uri, "/map");
	}

	private boolean parseOpenLocationMenuUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndGoUrl(data)) {
				String lat = data.getQueryParameter("lat");
				String lon = data.getQueryParameter("lon");
				if (lat != null && lon != null) {
					try {
						double lt = Double.parseDouble(lat);
						double ln = Double.parseDouble(lon);
						String zoom = data.getQueryParameter("z");
						int z = settings.getLastKnownMapZoom();
						if (zoom != null) {
							z = (int) Double.parseDouble(zoom);
						}
						settings.setMapLocationToShow(lt, ln, z, new PointDescription(lt, ln));
					} catch (NumberFormatException e) {
						LOG.error("error", e);
					}
				}
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private boolean parseRedirectUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndGoUrl(data)) {
				String url = data.getQueryParameter("url");
				if (url != null) {
					url = DiscountHelper.parseUrl(app, url);
					if (DiscountHelper.validateUrl(app, url)) {
						DiscountHelper.openUrl(mapActivity, url);
					}
				}
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private boolean isOsmAndGoUrl(@NonNull Uri uri) {
		return isOsmAndSite(uri) && isPathPrefix(uri, "/go");
	}

	private boolean parseTileSourceUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndSite(data) && isPathPrefix(data, "/add-tile-source")) {
				Map<String, String> attrs = new HashMap<>();
				for (String name : data.getQueryParameterNames()) {
					String value = data.getQueryParameter(name);
					if (value != null) {
						attrs.put(name, value);
					}
				}
				if (!attrs.isEmpty()) {
					try {
						TileSourceManager.TileSourceTemplate r = TileSourceManager.createTileSourceTemplate(attrs);
						if (r != null) {
							EditMapSourceDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), r);
						}
					} catch (Exception e) {
						LOG.error("parseAddTileSourceIntent error", e);
					}
				}
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private boolean parseOpenGpxUrlIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndSite(data) && isPathPrefix(data, "/open-gpx")) {
				String url = data.getQueryParameter("url");
				if (Algorithms.isEmpty(url)) {
					return false;
				}
				String name = data.getQueryParameter("name");
				if (Algorithms.isEmpty(name)) {
					name = Algorithms.getFileWithoutDirs(url);
				}
				if (!name.endsWith(IndexConstants.GPX_FILE_EXT)) {
					name += IndexConstants.GPX_FILE_EXT;
				}
				String fileName = name;
				AndroidNetworkUtils.downloadFileAsync(url, app.getAppPath(IndexConstants.GPX_IMPORT_DIR + fileName),
						error -> {
							if (error == null) {
								String downloaded = app.getString(R.string.shared_string_download_successful);
								app.showShortToastMessage(app.getString(R.string.ltr_or_rtl_combine_via_colon, downloaded, fileName));
							} else {
								app.showShortToastMessage(app.getString(R.string.error_occurred_loading_gpx));
							}
							return true;
						});

				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	public void parseContentIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action)) {
				Uri data = intent.getData();
				if (data != null) {
					String scheme = data.getScheme();
					if ("file".equals(scheme)) {
						String path = data.getPath();
						if (path != null) {
							mapActivity.getImportHelper().handleFileImport(data, new File(path).getName(), intent.getExtras(), true);
						}
						clearIntent(intent);
					} else if ("content".equals(scheme)) {
						mapActivity.getImportHelper().handleContentImport(data, intent.getExtras(), true);
						clearIntent(intent);
					} else if ("google.navigation".equals(scheme) || "osmand.navigation".equals(scheme)) {
						parseNavigationIntent(intent);
					} else if ("osmand.api".equals(scheme)) {
						ExternalApiHelper apiHelper = new ExternalApiHelper(mapActivity);
						Intent result = apiHelper.processApiRequest(intent);
						mapActivity.setResult(apiHelper.getResultCode(), result);
						result.setAction(null);
						mapActivity.setIntent(result);
						if (apiHelper.needFinish()) {
							mapActivity.finish();
						}
					} else if (LauncherShortcutsHelper.INTENT_SCHEME.equals(scheme)) {
						app.getLauncherShortcutsHelper().parseIntent(mapActivity, intent);
						clearIntent(intent);
					}
				}
			}

			if (intent.hasExtra(MapMarkersDialogFragment.OPEN_MAP_MARKERS_GROUPS)) {
				Bundle openMapMarkersGroupsExtra = intent.getBundleExtra(MapMarkersDialogFragment.OPEN_MAP_MARKERS_GROUPS);
				if (openMapMarkersGroupsExtra != null) {
					MapMarkersDialogFragment.showInstance(mapActivity, openMapMarkersGroupsExtra.getString(MapMarkersGroup.MARKERS_SYNC_GROUP_ID));
				}
				clearIntent(intent);
			}
			if (intent.hasExtra(BaseSettingsFragment.OPEN_SETTINGS)) {
				String appMode = intent.getStringExtra(BaseSettingsFragment.APP_MODE_KEY);
				String settingsTypeName = intent.getStringExtra(BaseSettingsFragment.OPEN_SETTINGS);
				if (!Algorithms.isEmpty(settingsTypeName)) {
					try {
						SettingsScreenType screenType = SettingsScreenType.valueOf(settingsTypeName);
						BaseSettingsFragment.showInstance(mapActivity, screenType, ApplicationMode.valueOfStringKey(appMode, null));
					} catch (IllegalArgumentException e) {
						LOG.error("error", e);
					}
				}
				clearIntent(intent);
			}
			if (intent.hasExtra(PluginsFragment.OPEN_PLUGINS)) {
				boolean openPlugins = intent.getBooleanExtra(PluginsFragment.OPEN_PLUGINS, false);
				if (openPlugins) {
					PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
				clearIntent(intent);
			}
			if (intent.hasExtra(EditFavoriteGroupDialogFragment.GROUP_NAME_KEY)) {
				String groupName = intent.getStringExtra(EditFavoriteGroupDialogFragment.GROUP_NAME_KEY);
				FavoriteGroup favoriteGroup = app.getFavoritesHelper().getGroup(FavoriteGroup.convertDisplayNameToGroupIdName(app, groupName));

				PointsGroup pointsGroup = favoriteGroup != null ? favoriteGroup.toPointsGroup(app) : null;
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				FavouriteGroupEditorFragment.showInstance(manager, pointsGroup, null, true);

				clearIntent(intent);
			}
			if (intent.hasExtra(BaseSettingsFragment.OPEN_CONFIG_ON_MAP)) {
				switch (intent.getStringExtra(BaseSettingsFragment.OPEN_CONFIG_ON_MAP)) {
					case BaseSettingsFragment.MAP_CONFIG:
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, null);
						break;

					case BaseSettingsFragment.SCREEN_CONFIG:
						ConfigureScreenFragment.showInstance(mapActivity);
						break;
				}
				clearIntent(intent);
			}
			if (intent.hasExtra(TrackMenuFragment.OPEN_TRACK_MENU)) {
				String path = intent.getStringExtra(TRACK_FILE_NAME);
				String name = intent.getStringExtra(RETURN_SCREEN_NAME);
				String tabName = intent.getStringExtra(OPEN_TAB_NAME);
				boolean currentRecording = intent.getBooleanExtra(CURRENT_RECORDING, false);
				boolean temporarySelected = intent.getBooleanExtra(TEMPORARY_SELECTED, false);
				TrackMenuFragment.showInstance(mapActivity, path, currentRecording, temporarySelected, name, null, tabName);
				clearIntent(intent);
			}
			if (intent.getExtras() != null) {
				Bundle extras = intent.getExtras();
				if (extras.containsKey(ChoosePlanFragment.OPEN_CHOOSE_PLAN)) {
					String featureValue = extras.getString(ChoosePlanFragment.CHOOSE_PLAN_FEATURE);
					if (!Algorithms.isEmpty(featureValue)) {
						try {
							OsmAndFeature feature = OsmAndFeature.valueOf(featureValue);
							if (feature == OsmAndFeature.ANDROID_AUTO) {
								if (!InAppPurchaseHelper.isAndroidAutoAvailable(app)) {
									ChoosePlanFragment.showInstance(mapActivity, feature);
								}
							} else {
								ChoosePlanFragment.showInstance(mapActivity, feature);
							}
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
					}
				}
				clearIntent(intent);
			}
		}
	}

	private void parseNavigationIntent(Intent intent) {
		Uri data = intent.getData();
		if (data != null) {
			String schemeSpecificPart = data.getSchemeSpecificPart();

			Matcher matcher = Pattern.compile("(?:q|ll)=([\\-0-9.]+),([\\-0-9.]+)(?:.*)").matcher(schemeSpecificPart);
			if (matcher.matches()) {
				try {
					double lat = Double.parseDouble(matcher.group(1));
					double lon = Double.parseDouble(matcher.group(2));

					app.getTargetPointsHelper().navigateToPoint(new LatLon(lat, lon), false, -1);
					mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, false, true);
				} catch (NumberFormatException e) {
					app.showToastMessage(app.getString(R.string.navigation_intent_invalid, schemeSpecificPart));
				}
			} else {
				app.showToastMessage(app.getString(R.string.navigation_intent_invalid, schemeSpecificPart));
			}
			clearIntent(intent);
		}
	}

	private void clearIntent(@NonNull Intent intent) {
		intent.replaceExtras(new Bundle());
		intent.setAction(null);
		intent.setData(null);
		intent.setFlags(0);
	}

	private boolean parseSendIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null) {
			String action = intent.getAction();
			String type = intent.getType();
			if (Intent.ACTION_SEND.equals(action) && type != null) {
				if ("text/plain".equals(type)) {
					return handleSendText(intent);
				}
			}
		}
		return false;
	}

	private boolean parseOAuthIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			if (uri.toString().startsWith("osmand-oauth")) {
				String oauthVerifier = uri.getQueryParameter("oauth_verifier");
				if (oauthVerifier != null) {
					app.getOsmOAuthHelper().addListener(getOnAuthorizeListener());
					app.getOsmOAuthHelper().authorize(oauthVerifier);
					clearIntent(intent);
					return true;
				}
			}
		}
		return false;
	}

	private boolean parseOprOAuthIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			if (uri.toString().startsWith(OPRConstants.OPR_OAUTH_PREFIX)) {
				String token = uri.getQueryParameter("opr-token");
				String username = uri.getQueryParameter("opr-nickname");
				app.getOprAuthHelper().addListener(getOprAuthorizationListener());
				app.getOprAuthHelper().authorize(token, username);
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private OsmAuthorizationListener getOnAuthorizeListener() {
		return () -> {
			for (Fragment fragment : mapActivity.getSupportFragmentManager().getFragments()) {
				if (fragment instanceof OsmAuthorizationListener) {
					((OsmAuthorizationListener) fragment).authorizationCompleted();
				}
			}
		};
	}

	private OprAuthorizationListener getOprAuthorizationListener() {
		return () -> {
			for (Fragment fragment : mapActivity.getSupportFragmentManager().getFragments()) {
				if (fragment instanceof OprAuthorizationListener) {
					((OprAuthorizationListener) fragment).authorizationCompleted();
				}
			}
		};
	}

	private boolean handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (!Algorithms.isEmpty(sharedText)) {
			return QuickSearchDialogFragment.showInstance(
					mapActivity,
					sharedText,
					null,
					QuickSearchDialogFragment.QuickSearchType.REGULAR,
					QuickSearchDialogFragment.QuickSearchTab.CATEGORIES,
					null
			);
		}
		return false;
	}

	private boolean isUriHierarchical(@NonNull Intent intent) {
		return intent.getData() != null && intent.getData().isHierarchical();
	}

	private boolean isOsmAndSite(@NonNull Uri uri) {
		return isHttpOrHttpsScheme(uri) && isOsmAndHost(uri);
	}

	private boolean isHttpOrHttpsScheme(@NonNull Uri uri) {
		String scheme = uri.getScheme();
		return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
	}

	private boolean isOsmAndHost(@NonNull Uri uri) {
		String host = uri.getHost();
		return host != null && host.endsWith("osmand.net");
	}

	private boolean isPathPrefix(@NonNull Uri uri, @NonNull String pathPrefix) {
		String path = uri.getPath();
		return path != null && path.startsWith(pathPrefix);
	}
}
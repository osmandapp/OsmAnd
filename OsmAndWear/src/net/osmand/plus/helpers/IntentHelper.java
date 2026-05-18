package net.osmand.plus.helpers;

import static net.osmand.plus.backup.BackupListeners.OnRegisterDeviceListener;
import static net.osmand.plus.configmap.tracks.PreselectedTabParams.CALLING_FRAGMENT_TAG;
import static net.osmand.plus.configmap.tracks.PreselectedTabParams.PRESELECTED_TRACKS_TAB_NAME;
import static net.osmand.plus.configmap.tracks.PreselectedTabParams.PRESELECTED_TRACKS_TAB_TYPE;
import static net.osmand.plus.configmap.tracks.PreselectedTabParams.SELECT_ALL_ITEMS_ON_TAB;
import static net.osmand.plus.helpers.MapFragmentsHelper.CLOSE_ALL_FRAGMENTS;
import static net.osmand.plus.settings.fragments.ExportSettingsFragment.SELECTED_TYPES;
import static net.osmand.plus.track.fragments.TrackMenuFragment.CURRENT_RECORDING;
import static net.osmand.plus.track.fragments.TrackMenuFragment.OPEN_TAB_NAME;
import static net.osmand.plus.track.fragments.TrackMenuFragment.RETURN_SCREEN_NAME;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TEMPORARY_SELECTED;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TRACK_FILE_NAME;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.backup.ui.AuthorizeFragment;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.backup.ui.LoginDialogType;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.configmap.tracks.PreselectedTabParams;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.TracksTabsFragment;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.mapcontextmenu.editors.FavouriteGroupEditorFragment;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.EditFavoriteGroupDialogFragment;
import net.osmand.plus.notifications.GpxNotification;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ExportSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.GeoPointParserUtil;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentHelper {

	private static final Log LOG = PlatformUtil.getLog(IntentHelper.class);

	private static final String URL_SCHEME = "https";
	private static final String URL_AUTHORITY = "osmand.net";
	private static final String URL_PATH = "map";
	private static final String URL_PARAMETER_START = "start";
	private static final String URL_PARAMETER_END = "end";
	private static final String URL_PARAMETER_TOKEN = "token";
	private static final String URL_PARAMETER_MODE = "profile";
	private static final String URL_PARAMETER_INTERMEDIATE_POINTS = "via";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	private OnRegisterDeviceListener registerDeviceListener;

	public IntentHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();

		registerDeviceListener = getRegisterDeviceListener();
	}

	private OnRegisterDeviceListener getRegisterDeviceListener() {
		if (registerDeviceListener == null) {
			registerDeviceListener = (status, message, error) -> {
				if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
					if (status == BackupHelper.STATUS_SUCCESS) {
						BackupCloudFragment.showInstance(mapActivity.getSupportFragmentManager());
					} else if (Algorithms.isEmpty(message)) {
						LOG.error(message);
					}
				}
				app.runInUIThread(() -> app.getBackupHelper().getBackupListeners().removeRegisterDeviceListener(registerDeviceListener));
			};
		}
		return registerDeviceListener;
	}

	public boolean parseLaunchIntents() {
		return parseNavigationIntent()
				|| parseBackupAuthorizationIntent()
				|| parseSetPinOnMapIntent()
				|| parseMoveMapToLocationIntent()
				|| parseOpenLocationMenuIntent()
				|| parseRedirectIntent()
				|| parseTileSourceIntent()
				|| parseOpenGpxIntent()
				|| parseSendIntent()
				|| parseOAuthIntent();
	}

	private boolean parseNavigationIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			boolean hasNavigationDestination = data.getQueryParameterNames().contains(URL_PARAMETER_END);
			if (isOsmAndMapUrl(data) && hasNavigationDestination) {
				String startLatLonParam = data.getQueryParameter(URL_PARAMETER_START);
				String endLatLonParam = data.getQueryParameter(URL_PARAMETER_END);
				String appModeKeyParam = data.getQueryParameter(URL_PARAMETER_MODE);
				String intermediatePointsParam = data.getQueryParameter(URL_PARAMETER_INTERMEDIATE_POINTS);

				if (Algorithms.isEmpty(endLatLonParam)) {
					LOG.error("Malformed OsmAnd navigation URL: destination location is missing");
					return true;
				}

				LatLon startLatLon = startLatLonParam == null ? null : Algorithms.parseLatLon(startLatLonParam);
				if (startLatLonParam != null && startLatLon == null) {
					LOG.error("Malformed OsmAnd navigation URL: start location is broken");
				}

				LatLon endLatLon = Algorithms.parseLatLon(endLatLonParam);
				if (endLatLon == null) {
					LOG.error("Malformed OsmAnd navigation URL: destination location is broken");
					return true;
				}

				ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKeyParam, null);
				if (!Algorithms.isEmpty(appModeKeyParam) && appMode == null) {
					LOG.debug("App mode with specified key not available, using default navigation app mode");
				}

				List<LatLon> points = parseIntermediatePoints(intermediatePointsParam);

				if (app.isApplicationInitializing()) {
					app.getAppInitializer().addListener(new AppInitializeListener() {

						@Override
						public void onFinish(@NonNull AppInitializer init) {
							init.removeListener(this);
							buildRoute(startLatLon, endLatLon, appMode, points);
						}
					});
				} else {
					buildRoute(startLatLon, endLatLon, appMode, points);
				}
				clearIntent(intent);
				return true;
			} else {
				List<GeoParsedPoint> points = GeoPointParserUtil.parsePoints(data.toString());
				if (points != null && points.size() > 1) {
					GeoParsedPoint startPoint = points.get(0);
					GeoParsedPoint endPoint = points.get(points.size() - 1);

					LatLon startLatLon = startPoint != null ? new LatLon(startPoint.getLatitude(), startPoint.getLongitude()) : null;
					LatLon endLatLon = endPoint != null ? new LatLon(endPoint.getLatitude(), endPoint.getLongitude()) : null;
					if (endLatLon == null) {
						LOG.error("Malformed navigation URL: destination location is empty");
						return true;
					}
					if (app.isApplicationInitializing()) {
						app.getAppInitializer().addListener(new AppInitializeListener() {

							@Override
							public void onFinish(@NonNull AppInitializer init) {
								init.removeListener(this);
								buildRoute(startLatLon, endLatLon, null, null);
							}
						});
					} else {
						buildRoute(startLatLon, endLatLon, null, null);
					}
					clearIntent(intent);
					return true;
				}
			}
		}
		return false;
	}

	private boolean parseBackupAuthorizationIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndSite(data) && isPathPrefix(data, "/premium")) {
				String token = data.getQueryParameter(URL_PARAMETER_TOKEN);
				if (!Algorithms.isEmpty(token)) {
					registerDevice(token);
				} else {
					LOG.error("Malformed OsmAnd backup authorization URL: token is empty");
				}
				clearIntent(intent);
				return true;
			}
		}
		return false;
	}

	private void registerDevice(@NonNull String token) {
		AuthorizeFragment fragment = mapActivity.getFragmentsHelper().getFragment(AuthorizeFragment.TAG);
		if (fragment != null && fragment.getDialogType() == LoginDialogType.VERIFY_EMAIL) {
			fragment.setToken(token);
		} else if (!app.getBackupHelper().isRegistered() && !Algorithms.isEmpty(settings.BACKUP_USER_EMAIL.get())) {
			if (BackupUtils.isTokenValid(token)) {
				BackupHelper backupHelper = app.getBackupHelper();
				backupHelper.getBackupListeners().addRegisterDeviceListener(registerDeviceListener);
				backupHelper.registerDevice(token);
			} else {
				LOG.error("Malformed OsmAnd backup authorization URL: token is not valid");
			}
		}
	}

	private void buildRoute(@Nullable LatLon start, @NonNull LatLon end,
	                        @Nullable ApplicationMode appMode, @Nullable List<LatLon> points) {
		if (appMode != null) {
			app.getRoutingHelper().setAppMode(appMode);
		}
		app.getTargetPointsHelper().navigateToPoint(end, true, -1);

		boolean hasIntermediatePoints = !Algorithms.isEmpty(points);
		if (hasIntermediatePoints) {
			settings.clearIntermediatePoints();
			for (LatLon point : points) {
				settings.insertIntermediatePoint(point.getLatitude(), point.getLongitude(),
						null, settings.getIntermediatePoints().size());
			}
		}

		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, appMode, start,
				null, hasIntermediatePoints, true, MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	private boolean parseSetPinOnMapIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && isUriHierarchical(intent)) {
			Uri data = intent.getData();
			if (isOsmAndMapUrl(data) && data.getQueryParameterNames().contains("pin")) {
				String latLonParam = data.getQueryParameter("pin");
				LatLon latLon = Algorithms.isEmpty(latLonParam) ? null : Algorithms.parseLatLon(latLonParam);
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
	private List<LatLon> parseIntermediatePoints(@Nullable String parameter) {
		if (!Algorithms.isEmpty(parameter)) {
			String[] params = parameter.split("[,;]");
			List<LatLon> points = new ArrayList<>();

			if (params.length >= 2 && params.length % 2 == 0) {
				for (int i = 0; i <= params.length - 2; i += 2) {
					try {
						points.add(new LatLon(Double.parseDouble(params[i]), Double.parseDouble(params[i + 1])));
					} catch (NumberFormatException e) {
						LOG.error("Malformed OsmAnd navigation URL: corrupted intermediate point");
					}
				}
			} else {
				LOG.error("Malformed OsmAnd navigation URL: corrupted intermediate points");
			}
			return points;
		}
		return null;
	}

	private boolean parseMoveMapToLocationIntent() {
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

	private boolean parseOpenLocationMenuIntent() {
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

	private boolean parseRedirectIntent() {
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

	private boolean parseTileSourceIntent() {
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

	private boolean parseOpenGpxIntent() {
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
					mapActivity.getFragmentsHelper().closeAllFragments();
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
			if (intent.getBooleanExtra(CLOSE_ALL_FRAGMENTS, false)) {
				mapActivity.getFragmentsHelper().closeAllFragments();
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
			Bundle extras = intent.getExtras();
			if (extras != null && intent.hasExtra(PRESELECTED_TRACKS_TAB_NAME) && intent.hasExtra(PRESELECTED_TRACKS_TAB_TYPE)) {
				String name = extras.getString(PRESELECTED_TRACKS_TAB_NAME, TrackTabType.ALL.name());
				String callingFragmentTag = extras.getString(CALLING_FRAGMENT_TAG, null);
				TrackTabType type = AndroidUtils.getSerializable(extras, PRESELECTED_TRACKS_TAB_TYPE, TrackTabType.class);
				boolean selectAllItems = intent.getBooleanExtra(SELECT_ALL_ITEMS_ON_TAB, false);

				PreselectedTabParams params = new PreselectedTabParams(name, type != null ? type : TrackTabType.ALL, selectAllItems);
				TracksTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), params, callingFragmentTag);
				clearIntent(intent);
			}
			if (intent.hasExtra(ExportSettingsFragment.SELECTED_TYPES)) {
				ApplicationMode mode = settings.getApplicationMode();
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				HashMap<ExportType, List<?>> selectedTypes = (HashMap<ExportType, List<?>>) intent.getSerializableExtra(SELECTED_TYPES);
				ExportSettingsFragment.showInstance(manager, mode, selectedTypes, true);

				clearIntent(intent);
			}
			if (intent.hasExtra(BackupAuthorizationFragment.OPEN_BACKUP_AUTH)) {
				BackupAuthorizationFragment.showInstance(mapActivity.getSupportFragmentManager());
				clearIntent(intent);
			}
			if (intent.hasExtra(GpxNotification.OSMAND_START_GPX_SERVICE_ACTION)) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.startGPXMonitoring(null);
					plugin.updateWidgets();
				}
				clearIntent(intent);
			}
			if (intent.getExtras() != null) {
				if (extras != null && extras.containsKey(ChoosePlanFragment.OPEN_CHOOSE_PLAN)) {
					String featureValue = extras.getString(ChoosePlanFragment.CHOOSE_PLAN_FEATURE);
					if (!Algorithms.isEmpty(featureValue)) {
						try {
							OsmAndFeature feature = OsmAndFeature.valueOf(featureValue);
							if (feature == OsmAndFeature.ANDROID_AUTO) {
								if (!InAppPurchaseUtils.isAndroidAutoAvailable(app)) {
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
				String code = uri.getQueryParameter("code");
				if (code != null) {
					app.getOsmOAuthHelper().addListener(getOnAuthorizeListener());
					app.getOsmOAuthHelper().authorize(code);
					clearIntent(intent);
					return true;
				}
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

	public static String generateRouteUrl(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();

		LatLon startPoint = settings.getPointToStart();
		LatLon endPoint = settings.getPointToNavigate();
		List<LatLon> intermediatePoints = settings.getIntermediatePoints();

		Uri.Builder builder = new Uri.Builder();
		builder.scheme(URL_SCHEME)
				.authority(URL_AUTHORITY)
				.appendPath(URL_PATH);

		if (startPoint != null) {
			String startPointCoordinates = Algorithms.formatLatlon(startPoint);
			builder.appendQueryParameter(URL_PARAMETER_START, startPointCoordinates);
		}

		if (!Algorithms.isEmpty(intermediatePoints)) {
			StringBuilder stringBuilder = new StringBuilder();
			for (LatLon latLon : intermediatePoints) {
				stringBuilder.append(";")
						.append(getFormattedCoordinate(latLon.getLatitude()))
						.append(",")
						.append(getFormattedCoordinate(latLon.getLongitude()));
			}
			builder.appendQueryParameter(URL_PARAMETER_INTERMEDIATE_POINTS, stringBuilder.substring(1));
		}

		if (endPoint != null) {
			String endPointCoordinates = Algorithms.formatLatlon(endPoint);
			builder.appendQueryParameter(URL_PARAMETER_END, endPointCoordinates);
		}

		builder.appendQueryParameter(URL_PARAMETER_MODE, app.getRoutingHelper().getAppMode().getStringKey())
				.encodedFragment(mapTileView.getZoom() + "/" + getFormattedCoordinate(mapTileView.getLatitude()) + "/" + getFormattedCoordinate(mapTileView.getLongitude()));

		return builder.build().toString();
	}

	private static String getFormattedCoordinate(double coordinate) {
		return String.format(Locale.US, "%.6f", coordinate);
	}

	@NonNull
	public static List<Uri> getIntentUris(@NonNull Intent intent) {
		List<Uri> uris = new ArrayList<>();
		Uri data = intent.getData();
		if (data != null) {
			uris.add(data);
		}
		ClipData clipData = intent.getClipData();
		if (clipData != null) {
			for (int i = 0; i < clipData.getItemCount(); i++) {
				Uri uri = clipData.getItemAt(i).getUri();
				if (uri != null) {
					uris.add(uri);
				}
			}
		}
		return uris;
	}
}
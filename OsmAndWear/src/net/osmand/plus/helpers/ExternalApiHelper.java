package net.osmand.plus.helpers;

import static net.osmand.plus.helpers.NavigateGpxHelper.startNavigation;
import static net.osmand.search.core.ObjectType.CITY;
import static net.osmand.search.core.ObjectType.HOUSE;
import static net.osmand.search.core.ObjectType.POI;
import static net.osmand.search.core.ObjectType.POSTCODE;
import static net.osmand.search.core.ObjectType.STREET;
import static net.osmand.search.core.ObjectType.STREET_INTERSECTION;
import static net.osmand.search.core.ObjectType.VILLAGE;
import static net.osmand.search.core.SearchCoreFactory.MAX_DEFAULT_SEARCH_RADIUS;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.aidl.AidlSearchResultWrapper;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.aidl.search.SearchParams;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.TurnType;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ExternalApiHelper {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExternalApiHelper.class);

	public static final String API_CMD_SHOW_GPX = "show_gpx";
	public static final String API_CMD_NAVIGATE_GPX = "navigate_gpx";

	public static final String API_CMD_NAVIGATE = "navigate";
	public static final String API_CMD_NAVIGATE_SEARCH = "navigate_search";

	public static final String API_CMD_PAUSE_NAVIGATION = "pause_navigation";
	public static final String API_CMD_RESUME_NAVIGATION = "resume_navigation";
	public static final String API_CMD_STOP_NAVIGATION = "stop_navigation";
	public static final String API_CMD_MUTE_NAVIGATION = "mute_navigation";
	public static final String API_CMD_UNMUTE_NAVIGATION = "unmute_navigation";

	public static final String API_CMD_RECORD_AUDIO = "record_audio";
	public static final String API_CMD_RECORD_VIDEO = "record_video";
	public static final String API_CMD_RECORD_PHOTO = "record_photo";
	public static final String API_CMD_STOP_AV_REC = "stop_av_rec";

	public static final String API_CMD_GET_INFO = "get_info";

	public static final String API_CMD_ADD_FAVORITE = "add_favorite";
	public static final String API_CMD_ADD_MAP_MARKER = "add_map_marker";

	public static final String API_CMD_SHOW_LOCATION = "show_location";

	public static final String API_CMD_START_GPX_REC = "start_gpx_rec";
	public static final String API_CMD_STOP_GPX_REC = "stop_gpx_rec";
	public static final String API_CMD_SAVE_GPX = "save_gpx";
	public static final String API_CMD_CLEAR_GPX = "clear_gpx";

	public static final String API_CMD_EXECUTE_QUICK_ACTION = "execute_quick_action";
	public static final String API_CMD_GET_QUICK_ACTION_INFO = "get_quick_action_info";

	public static final String API_CMD_SUBSCRIBE_VOICE_NOTIFICATIONS = "subscribe_voice_notifications";
	public static final int VERSION_CODE = 1;


	public static final String PARAM_NAME = "name";
	public static final String PARAM_DESC = "desc";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_LAT = "lat";
	public static final String PARAM_LON = "lon";
	public static final String PARAM_MAP_LAT = "map_lat";
	public static final String PARAM_MAP_LON = "map_lon";
	public static final String PARAM_DESTINATION_LAT = "destination_lat";
	public static final String PARAM_DESTINATION_LON = "destination_lon";
	public static final String PARAM_COLOR = "color";
	public static final String PARAM_VISIBLE = "visible";

	public static final String PARAM_PATH = "path";
	public static final String PARAM_URI = "uri";
	public static final String PARAM_DATA = "data";
	public static final String PARAM_FORCE = "force";
	public static final String PARAM_LOCATION_PERMISSION = "location_permission";

	public static final String PARAM_START_NAME = "start_name";
	public static final String PARAM_DEST_NAME = "dest_name";
	public static final String PARAM_START_LAT = "start_lat";
	public static final String PARAM_START_LON = "start_lon";
	public static final String PARAM_DEST_LAT = "dest_lat";
	public static final String PARAM_DEST_LON = "dest_lon";
	public static final String PARAM_DEST_SEARCH_QUERY = "dest_search_query";
	public static final String PARAM_SEARCH_LAT = "search_lat";
	public static final String PARAM_SEARCH_LON = "search_lon";
	public static final String PARAM_SHOW_SEARCH_RESULTS = "show_search_results";
	public static final String PARAM_PROFILE = "profile";
	public static final String PARAM_ROUTING_DATA = "routing_data";

	public static final String PARAM_VERSION = "version";
	public static final String PARAM_ETA = "eta";
	public static final String PARAM_TIME_LEFT = "time_left";
	public static final String PARAM_DISTANCE_LEFT = "time_distance_left";
	public static final String PARAM_NT_DISTANCE = "turn_distance";
	public static final String PARAM_NT_IMMINENT = "turn_imminent";
	public static final String PARAM_NT_DIRECTION_NAME = "turn_name";
	public static final String PARAM_NT_DIRECTION_TURN = "turn_type";
	public static final String PARAM_NT_DIRECTION_LANES = "turn_lanes";
	public static final String PARAM_NT_DIRECTION_ANGLE = "turn_angle";
	public static final String PARAM_NT_DIRECTION_POSSIBLY_LEFT = "turn_possibly_left";
	public static final String PARAM_NT_DIRECTION_POSSIBLY_RIGHT = "turn_possibly_right";

	public static final String PARAM_CLOSE_AFTER_COMMAND = "close_after_command";

	public static final String PARAM_QUICK_ACTION_NAME = "quick_action_name";
	public static final String PARAM_QUICK_ACTION_TYPE = "quick_action_type";
	public static final String PARAM_QUICK_ACTION_PARAMS = "quick_action_params";
	public static final String PARAM_QUICK_ACTION_NUMBER = "quick_action_number";

	public static final String PARAM_PLUGINS_VERSIONS = "plugins_versions";
	public static final String PARAM_PROFILES_VERSIONS = "profiles_versions";

	// RESULT_OK == -1
	// RESULT_CANCELED == 0
	// RESULT_FIRST_USER == 1
	// from Activity
	public static final int RESULT_CODE_ERROR_UNKNOWN = 1001;
	public static final int RESULT_CODE_ERROR_NOT_IMPLEMENTED = 1002;
	public static final int RESULT_CODE_ERROR_PLUGIN_INACTIVE = 1003;
	public static final int RESULT_CODE_ERROR_GPX_NOT_FOUND = 1004;
	public static final int RESULT_CODE_ERROR_INVALID_PROFILE = 1005;
	public static final int RESULT_CODE_ERROR_EMPTY_SEARCH_QUERY = 1006;
	public static final int RESULT_CODE_ERROR_SEARCH_LOCATION_UNDEFINED = 1007;
	public static final int RESULT_CODE_ERROR_QUICK_ACTION_NOT_FOUND = 1008;

	private final MapActivity mapActivity;
	private int resultCode;
	private boolean finish;

	public int getResultCode() {
		return resultCode;
	}

	public boolean needFinish() {
		return finish;
	}

	public ExternalApiHelper(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public Intent processApiRequest(Intent intent) {

		Intent result = new Intent();
		OsmandApplication app = (OsmandApplication) mapActivity.getApplication();

		try {
			Uri uri = intent.getData();
			String cmd = uri.getHost().toLowerCase();
			if (API_CMD_SHOW_GPX.equals(cmd) || API_CMD_NAVIGATE_GPX.equals(cmd)) {
				boolean navigate = API_CMD_NAVIGATE_GPX.equals(cmd);
				String path = uri.getQueryParameter(PARAM_PATH);

				GpxFile gpx = null;
				if (path != null) {
					File f = new File(path);
					if (f.exists()) {
						gpx = SharedUtil.loadGpxFile(f);
					}
				} else if (intent.getStringExtra(PARAM_DATA) != null) {
					String gpxStr = intent.getStringExtra(PARAM_DATA);
					if (!Algorithms.isEmpty(gpxStr)) {
						gpx = SharedUtil.loadGpxFile(new ByteArrayInputStream(gpxStr.getBytes()));
					}
				} else if (uri.getBooleanQueryParameter(PARAM_URI, false)) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						LOG.debug("uriString=" + intent.getClipData().getItemAt(0).getUri());
						Uri gpxUri = intent.getClipData().getItemAt(0).getUri();

						ParcelFileDescriptor gpxParcelDescriptor = mapActivity.getContentResolver()
								.openFileDescriptor(gpxUri, "r");
						if (gpxParcelDescriptor != null) {
							FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
							gpx = SharedUtil.loadGpxFile(new FileInputStream(fileDescriptor));
						} else {
							finish = true;
							resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
						}
					} else {
						finish = true;
						resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
					}
				} else {
					finish = true;
					resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
				}

				if (gpx != null) {
					if (navigate) {
						GpxNavigationParams params = new GpxNavigationParams();
						params.setForce(uri.getBooleanQueryParameter(PARAM_FORCE, false));
						params.setCheckLocationPermission(uri.getBooleanQueryParameter(PARAM_LOCATION_PERMISSION, false));
						params.setImportedByApi(true);
						NavigateGpxHelper.saveAndNavigateGpx(mapActivity, gpx, params);
					} else {
						app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
					}
					resultCode = Activity.RESULT_OK;
				} else {
					finish = true;
					resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
				}

			} else if (API_CMD_NAVIGATE.equals(cmd)) {
				String profileStr = uri.getQueryParameter(PARAM_PROFILE);
				ApplicationMode profile = findNavigationProfile(app, profileStr);
				if (profile == null) {
					resultCode = RESULT_CODE_ERROR_INVALID_PROFILE;
				} else {
					String startName = uri.getQueryParameter(PARAM_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}
					String destName = uri.getQueryParameter(PARAM_DEST_NAME);
					if (Algorithms.isEmpty(destName)) {
						destName = "";
					}

					LatLon start;
					PointDescription startDesc;
					String startLatStr = uri.getQueryParameter(PARAM_START_LAT);
					String startLonStr = uri.getQueryParameter(PARAM_START_LON);
					if (!Algorithms.isEmpty(startLatStr) && !Algorithms.isEmpty(startLonStr)) {
						double lat = Double.parseDouble(startLatStr);
						double lon = Double.parseDouble(startLonStr);
						start = new LatLon(lat, lon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}

					String destLatStr = uri.getQueryParameter(PARAM_DEST_LAT);
					String destLonStr = uri.getQueryParameter(PARAM_DEST_LON);
					LatLon dest;
					if (!Algorithms.isEmpty(destLatStr) && !Algorithms.isEmpty(destLonStr)) {
						double destLat = Double.parseDouble(destLatStr);
						double destLon = Double.parseDouble(destLonStr);
						dest = new LatLon(destLat, destLon);
					} else {
						dest = null;
					}
					PointDescription destDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, destName);

					boolean force = uri.getBooleanQueryParameter(PARAM_FORCE, false);
					boolean locationPermission = uri.getBooleanQueryParameter(PARAM_LOCATION_PERMISSION, false);

					RoutingHelper routingHelper = app.getRoutingHelper();
					if (routingHelper.isFollowingMode() && !force) {
						mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
							if (!routingHelper.isFollowingMode()) {
								startNavigation(mapActivity, profile, start, startDesc, dest, destDesc, locationPermission);
							}
						});
					} else {
						startNavigation(mapActivity, profile, start, startDesc, dest, destDesc, locationPermission);
					}
				}
			} else if (API_CMD_NAVIGATE_SEARCH.equals(cmd)) {
				String profileStr = uri.getQueryParameter(PARAM_PROFILE);
				ApplicationMode profile = findNavigationProfile(app, profileStr);
				boolean showSearchResults = uri.getBooleanQueryParameter(PARAM_SHOW_SEARCH_RESULTS, false);
				String searchQuery = uri.getQueryParameter(PARAM_DEST_SEARCH_QUERY);
				if (Algorithms.isEmpty(searchQuery)) {
					resultCode = RESULT_CODE_ERROR_EMPTY_SEARCH_QUERY;
				} else if (profile == null) {
					resultCode = RESULT_CODE_ERROR_INVALID_PROFILE;
				} else {
					String startName = uri.getQueryParameter(PARAM_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}
					LatLon start;
					PointDescription startDesc;
					String startLatStr = uri.getQueryParameter(PARAM_START_LAT);
					String startLonStr = uri.getQueryParameter(PARAM_START_LON);
					if (!Algorithms.isEmpty(startLatStr) && !Algorithms.isEmpty(startLonStr)) {
						double lat = Double.parseDouble(startLatStr);
						double lon = Double.parseDouble(startLonStr);
						start = new LatLon(lat, lon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}
					LatLon searchLocation;
					String searchLatStr = uri.getQueryParameter(PARAM_SEARCH_LAT);
					String searchLonStr = uri.getQueryParameter(PARAM_SEARCH_LON);
					if (!Algorithms.isEmpty(searchLatStr) && !Algorithms.isEmpty(searchLonStr)) {
						double lat = Double.parseDouble(searchLatStr);
						double lon = Double.parseDouble(searchLonStr);
						searchLocation = new LatLon(lat, lon);
					} else {
						searchLocation = null;
					}

					if (searchLocation == null) {
						resultCode = RESULT_CODE_ERROR_SEARCH_LOCATION_UNDEFINED;
					} else {
						boolean force = uri.getBooleanQueryParameter(PARAM_FORCE, false);
						boolean locationPermission = uri.getBooleanQueryParameter(PARAM_LOCATION_PERMISSION, false);

						RoutingHelper routingHelper = app.getRoutingHelper();
						if (routingHelper.isFollowingMode() && !force) {
							mapActivity.getMapActions().stopNavigationActionConfirm(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
									if (!routingHelper.isFollowingMode()) {
										searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, showSearchResults, locationPermission);
									}
								}
							});
						} else {
							searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, showSearchResults, locationPermission);
						}
						resultCode = Activity.RESULT_OK;
					}
				}

			} else if (API_CMD_PAUSE_NAVIGATION.equals(cmd)) {
				RoutingHelper routingHelper = mapActivity.getRoutingHelper();
				if (routingHelper.isRouteCalculated() && !routingHelper.isRoutePlanningMode()) {
					routingHelper.pauseNavigation();
					resultCode = Activity.RESULT_OK;
				}
			} else if (API_CMD_RESUME_NAVIGATION.equals(cmd)) {
				RoutingHelper routingHelper = mapActivity.getRoutingHelper();
				if (routingHelper.isRouteCalculated() && routingHelper.isRoutePlanningMode()) {
					routingHelper.resumeNavigation();
					AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
					resultCode = Activity.RESULT_OK;
				}
			} else if (API_CMD_STOP_NAVIGATION.equals(cmd)) {
				RoutingHelper routingHelper = mapActivity.getRoutingHelper();
				if (routingHelper.isPauseNavigation() || routingHelper.isFollowingMode()) {
					mapActivity.getMapLayers().getMapActionsHelper().stopNavigationWithoutConfirm();
					resultCode = Activity.RESULT_OK;
				}
			} else if (API_CMD_MUTE_NAVIGATION.equals(cmd)) {
				mapActivity.getRoutingHelper().getVoiceRouter().setMute(true);
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_UNMUTE_NAVIGATION.equals(cmd)) {
				mapActivity.getRoutingHelper().getVoiceRouter().setMute(false);
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_RECORD_AUDIO.equals(cmd)
					|| API_CMD_RECORD_VIDEO.equals(cmd)
					|| API_CMD_RECORD_PHOTO.equals(cmd)
					|| API_CMD_STOP_AV_REC.equals(cmd)) {
				AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_PLUGIN_INACTIVE;
					finish = true;
				} else {
					if (API_CMD_STOP_AV_REC.equals(cmd)) {
						plugin.stopRecording(mapActivity, false);
					} else {
						double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
						double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
						if (API_CMD_RECORD_AUDIO.equals(cmd)) {
							plugin.recordAudio(lat, lon, mapActivity);
						} else if (API_CMD_RECORD_VIDEO.equals(cmd)) {
							plugin.recordVideo(lat, lon, mapActivity, false);
						} else if (API_CMD_RECORD_PHOTO.equals(cmd)) {
							plugin.takePhoto(lat, lon, mapActivity, true, false);
						}
					}

					resultCode = Activity.RESULT_OK;
				}

			} else if (API_CMD_GET_INFO.equals(cmd)) {

				Location location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
				if (location != null) {
					result.putExtra(PARAM_LAT, location.getLatitude());
					result.putExtra(PARAM_LON, location.getLongitude());
				}

				LatLon mapLocation = mapActivity.getMapLocation();
				if (location != null) {
					result.putExtra(PARAM_MAP_LAT, mapLocation.getLatitude());
					result.putExtra(PARAM_MAP_LON, mapLocation.getLongitude());
				}

				RoutingHelper routingHelper = app.getRoutingHelper();
				if (routingHelper.isRouteCalculated()) {
					LatLon finalLocation = routingHelper.getFinalLocation();
					result.putExtra(PARAM_DESTINATION_LAT, finalLocation.getLatitude());
					result.putExtra(PARAM_DESTINATION_LON, finalLocation.getLongitude());

					int time = routingHelper.getLeftTime();
					long eta = time + System.currentTimeMillis() / 1000;

					result.putExtra(PARAM_ETA, eta);
					result.putExtra(PARAM_TIME_LEFT, time);
					result.putExtra(PARAM_DISTANCE_LEFT, routingHelper.getLeftDistance());
					result.putExtras(getRouteDirectionsInfo(app));
				}
				List<String> routingData = app.getAnalyticsHelper().getRoutingRecordedData();
				if (!Algorithms.isEmpty(routingData)) {
					result.putStringArrayListExtra(PARAM_ROUTING_DATA, new ArrayList<>(routingData));
				}

				result.putExtra(PARAM_VERSION, VERSION_CODE);

				finish = true;
				resultCode = Activity.RESULT_OK;

			} else if (API_CMD_ADD_FAVORITE.equals(cmd)) {
				String name = uri.getQueryParameter(PARAM_NAME);
				String desc = uri.getQueryParameter(PARAM_DESC);
				String category = uri.getQueryParameter(PARAM_CATEGORY);
				double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
				double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
				String colorTag = uri.getQueryParameter(PARAM_COLOR);
				boolean visible = uri.getBooleanQueryParameter(PARAM_VISIBLE, true);

				if (name == null) {
					name = "";
				}
				if (desc == null) {
					desc = "";
				}
				if (category == null) {
					category = "";
				}

				int color = 0;
				if (!Algorithms.isEmpty(colorTag)) {
					color = DefaultColors.valueOf(colorTag);
					if (color == 0) {
						LOG.error("Wrong color tag: " + colorTag);
					}
				}

				FavouritePoint fav = new FavouritePoint(lat, lon, name, category);
				fav.setDescription(desc);
				fav.setColor(color);
				fav.setVisible(visible);

				FavouritesHelper helper = app.getFavoritesHelper();
				helper.addFavourite(fav);

				showOnMap(lat, lon, fav, mapActivity.getMapLayers().getFavouritesLayer().getObjectName(fav));
				resultCode = Activity.RESULT_OK;

			} else if (API_CMD_ADD_MAP_MARKER.equals(cmd)) {
				double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
				double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
				String name = uri.getQueryParameter(PARAM_NAME);

				PointDescription pd = new PointDescription(
						PointDescription.POINT_TYPE_MAP_MARKER, name != null ? name : "");

				MapMarkersHelper markersHelper = app.getMapMarkersHelper();
				markersHelper.addMapMarker(new LatLon(lat, lon), pd, null);

				MapMarker marker = markersHelper.getFirstMapMarker();
				if (marker != null) {
					showOnMap(lat, lon, marker, mapActivity.getMapLayers().getMapMarkersLayer().getObjectName(marker));
				}
				resultCode = Activity.RESULT_OK;

			} else if (API_CMD_SHOW_LOCATION.equals(cmd)) {
				double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
				double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
				showOnMap(lat, lon, null, null);
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_START_GPX_REC.equals(cmd)) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_PLUGIN_INACTIVE;
					finish = true;
				} else {
					plugin.startGPXMonitoring(null);
				}

				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_STOP_GPX_REC.equals(cmd)) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_PLUGIN_INACTIVE;
					finish = true;
				} else {
					plugin.stopRecording();
				}

				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_SAVE_GPX.equals(cmd)) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_PLUGIN_INACTIVE;
					finish = true;
				} else {
					plugin.saveCurrentTrack();
				}
				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_CLEAR_GPX.equals(cmd)) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_PLUGIN_INACTIVE;
					finish = true;
				} else {
					app.getSavingTrackHelper().clearRecordedData(true);
				}
				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
				resultCode = Activity.RESULT_OK;
			} else if (API_CMD_EXECUTE_QUICK_ACTION.equals(cmd)) {
				int actionNumber = Integer.parseInt(uri.getQueryParameter(PARAM_QUICK_ACTION_NUMBER));
				List<QuickAction> actionsList = app.getMapButtonsHelper().getFlattenedQuickActions();
				if (actionNumber >= 0 && actionNumber < actionsList.size()) {
					MapButtonsHelper.produceAction(actionsList.get(actionNumber)).execute(mapActivity);
					resultCode = Activity.RESULT_OK;
				} else {
					resultCode = RESULT_CODE_ERROR_QUICK_ACTION_NOT_FOUND;
				}
				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
			} else if (API_CMD_GET_QUICK_ACTION_INFO.equals(cmd)) {
				int actionNumber = Integer.parseInt(uri.getQueryParameter(PARAM_QUICK_ACTION_NUMBER));
				List<QuickAction> actionsList = app.getMapButtonsHelper().getFlattenedQuickActions();
				if (actionNumber >= 0 && actionNumber < actionsList.size()) {
					QuickAction action = actionsList.get(actionNumber);

					Gson gson = new Gson();
					Type type = new TypeToken<HashMap<String, String>>() {}.getType();

					result.putExtra(PARAM_QUICK_ACTION_NAME, action.getName(app));
					result.putExtra(PARAM_QUICK_ACTION_TYPE, action.getActionType().getStringId());
					result.putExtra(PARAM_QUICK_ACTION_PARAMS, gson.toJson(action.getParams(), type));
					result.putExtra(PARAM_VERSION, VERSION_CODE);

					resultCode = Activity.RESULT_OK;
				} else {
					resultCode = RESULT_CODE_ERROR_QUICK_ACTION_NOT_FOUND;
				}
				if (uri.getBooleanQueryParameter(PARAM_CLOSE_AFTER_COMMAND, true)) {
					finish = true;
				}
			} else if (API_CMD_SUBSCRIBE_VOICE_NOTIFICATIONS.equals(cmd)) {
				// not implemented yet
				resultCode = RESULT_CODE_ERROR_NOT_IMPLEMENTED;
			}

		} catch (Exception e) {
			LOG.error("Error processApiRequest:", e);
			resultCode = RESULT_CODE_ERROR_UNKNOWN;
		}

		return result;
	}

	@Nullable
	private ApplicationMode findNavigationProfile(@NonNull OsmandApplication app, @Nullable String profileStr) {
		if (!ApplicationMode.DEFAULT.getStringKey().equals(profileStr)) {
			ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, ApplicationMode.CAR);
			for (ApplicationMode mode : ApplicationMode.values(app)) {
				if (mode == profile && !Algorithms.isEmpty(mode.getRoutingProfile())) {
					return mode;
				}
			}
		}
		return null;
	}

	@Nullable
	public static ApplicationMode getNavigationProfile(@NonNull OsmandApplication app) {
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		List<ApplicationMode> modes = ApplicationMode.getModesForRouting(app);
		if (modes.size() > 0 && !modes.contains(appMode)) {
			return modes.iterator().next();
		}
		return appMode;
	}

	public static void updateTurnInfo(String prefix, Bundle bundle, NextDirectionInfo nextInfo) {
		bundle.putInt(prefix + PARAM_NT_DISTANCE, nextInfo.distanceTo);
		bundle.putInt(prefix + PARAM_NT_IMMINENT, nextInfo.imminent);
		if (nextInfo.directionInfo != null && nextInfo.directionInfo.getTurnType() != null) {
			updateRouteDirectionInfo(prefix, bundle, nextInfo.directionInfo);
		}
	}

	@NonNull
	public static Bundle getPluginAndProfileVersions() {
		Bundle bundle = new Bundle();
		List<CustomOsmandPlugin> plugins = PluginsHelper.getCustomPlugins();
		if (!Algorithms.isEmpty(plugins)) {
			HashMap<String, Integer> map = new HashMap<>();
			for (CustomOsmandPlugin plugin : plugins) {
				map.put(plugin.getId(), plugin.getVersion());
			}
			bundle.putSerializable(PARAM_PLUGINS_VERSIONS, map);
		}
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			HashMap<String, Integer> map = new HashMap<>();
			map.put(mode.getStringKey(), mode.getVersion());
			bundle.putSerializable(PARAM_PROFILES_VERSIONS, map);
		}
		return bundle;
	}

	@NonNull
	public static Bundle getRouteDirectionsInfo(@NonNull OsmandApplication app) {
		Bundle bundle = new Bundle();
		RoutingHelper routingHelper = app.getRoutingHelper();
		RouteDirectionInfo directionInfo = routingHelper.getRoute().getCurrentDirection();
		if (directionInfo != null) {
			updateRouteDirectionInfo("current_", bundle, directionInfo);
		}
		NextDirectionInfo ni = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		if (ni.distanceTo > 0) {
			updateTurnInfo("next_", bundle, ni);
			ni = routingHelper.getNextRouteDirectionInfoAfter(ni, new NextDirectionInfo(), true);
			if (ni.distanceTo > 0) {
				updateTurnInfo("after_next", bundle, ni);
			}
		}
		routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
		if (ni.distanceTo > 0) {
			updateTurnInfo("no_speak_next_", bundle, ni);
		}
		return bundle;
	}

	public static void updateRouteDirectionInfo(String prefix, Bundle bundle, RouteDirectionInfo info) {
		TurnType tt = info.getTurnType();
		bundle.putString(prefix + PARAM_NT_DIRECTION_NAME, RoutingHelperUtils.formatStreetName(info.getStreetName(), info.getRef(), info.getDestinationName(), ""));
		bundle.putString(prefix + PARAM_NT_DIRECTION_TURN, tt.toXmlString());
		bundle.putFloat(prefix + PARAM_NT_DIRECTION_ANGLE, tt.getTurnAngle());
		bundle.putBoolean(prefix + PARAM_NT_DIRECTION_POSSIBLY_LEFT, tt.isPossibleLeftTurn());
		bundle.putBoolean(prefix + PARAM_NT_DIRECTION_POSSIBLY_RIGHT, tt.isPossibleRightTurn());
		if (tt.getLanes() != null) {
			bundle.putString(prefix + PARAM_NT_DIRECTION_LANES, Arrays.toString(tt.getLanes()));
		}
	}

	private void showOnMap(double lat, double lon, Object object, PointDescription pointDescription) {
		MapContextMenu mapContextMenu = mapActivity.getContextMenu();
		mapContextMenu.setMapCenter(new LatLon(lat, lon));
		mapContextMenu.setCenterMarker(true);
		mapContextMenu.setMapZoom(15);
		mapContextMenu.show(new LatLon(lat, lon), pointDescription, object);
	}

	public static void searchAndNavigate(@NonNull MapActivity mapActivity, @NonNull LatLon searchLocation,
	                                     @Nullable LatLon from, @Nullable PointDescription fromDesc,
	                                     @NonNull ApplicationMode mode, @NonNull String searchQuery,
	                                     boolean showSearchResults, boolean checkLocationPermission) {

		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		OsmandApplication app = mapActivity.getMyApplication();
		if (showSearchResults) {
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
				mapActivity.getMapActions().enterRoutePlanningMode(from, fromDesc);
			} else {
				mapActivity.getRoutingHelper().setRoutePlanningMode(true);
				TargetPointsHelper targets = app.getTargetPointsHelper();
				targets.setStartPoint(from, false, fromDesc);
				mapActivity.getMapViewTrackingUtilities().switchRoutePlanningMode();
				mapActivity.refreshMap();
			}
			mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.DESTINATION_SELECTION_AND_START, true, searchQuery, searchLocation);
		} else {
			ProgressDialog dlg = new ProgressDialog(mapActivity);
			dlg.setTitle("");
			dlg.setMessage(mapActivity.getString(R.string.searching_address));
			dlg.show();
			WeakReference<ProgressDialog> dlgRef = new WeakReference<>(dlg);
			runSearch(app, searchQuery, SearchParams.SEARCH_TYPE_ALL,
					searchLocation.getLatitude(), searchLocation.getLongitude(),
					1, 1, new OsmandAidlApi.SearchCompleteCallback() {
						@Override
						public void onSearchComplete(List<AidlSearchResultWrapper> resultSet) {
							MapActivity mapActivity = mapActivityRef.get();
							if (mapActivity != null) {
								mapActivity.getMyApplication().runInUIThread(() -> {
									ProgressDialog dialog = dlgRef.get();
									if (dialog != null) {
										dialog.dismiss();
									}
									if (resultSet.size() > 0) {
										AidlSearchResultWrapper res = resultSet.get(0);
										LatLon to = new LatLon(res.getLatitude(), res.getLongitude());
										PointDescription toDesc = new PointDescription(
												PointDescription.POINT_TYPE_TARGET, res.getLocalName() + ", " + res.getLocalTypeName());
										startNavigation(mapActivity, mode, from, fromDesc, to, toDesc, checkLocationPermission);
									} else {
										mapActivity.getMyApplication().showToastMessage(mapActivity.getString(R.string.search_nothing_found));
									}
								});
							}
						}
					});
		}
	}

	public static void runSearch(OsmandApplication app, String searchQuery, int searchType,
	                             double latitude, double longitude, int radiusLevel,
	                             int totalLimit, OsmandAidlApi.SearchCompleteCallback callback) {
		if (radiusLevel < 1) {
			radiusLevel = 1;
		} else if (radiusLevel > MAX_DEFAULT_SEARCH_RADIUS) {
			radiusLevel = MAX_DEFAULT_SEARCH_RADIUS;
		}
		if (totalLimit <= 0) {
			totalLimit = -1;
		}
		int limit = totalLimit;

		SearchUICore core = app.getSearchUICore().getCore();
		core.setOnResultsComplete(() -> {
			List<AidlSearchResultWrapper> resultSet = new ArrayList<>();
			SearchUICore.SearchResultCollection resultCollection = core.getCurrentSearchResult();
			int count = 0;
			for (net.osmand.search.core.SearchResult r : resultCollection.getCurrentSearchResults()) {
				String name = QuickSearchListItem.getName(app, r);
				String typeName = QuickSearchListItem.getTypeName(app, r);
				AidlSearchResultWrapper result = new AidlSearchResultWrapper(r.location.getLatitude(), r.location.getLongitude(),
						name, typeName, r.alternateName, new ArrayList<>(r.otherNames));
				resultSet.add(result);
				count++;
				if (limit != -1 && count >= limit) {
					break;
				}
			}
			callback.onSearchComplete(resultSet);
		});

		SearchSettings searchSettings = new SearchSettings(core.getSearchSettings())
				.setRadiusLevel(radiusLevel)
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setOriginalLocation(new LatLon(latitude, longitude))
				.setTotalLimit(totalLimit);

		List<ObjectType> searchTypes = new ArrayList<>();
		if ((searchType & SearchParams.SEARCH_TYPE_POI) != 0) {
			searchTypes.add(POI);
		}
		if ((searchType & SearchParams.SEARCH_TYPE_ADDRESS) != 0) {
			searchTypes.add(CITY);
			searchTypes.add(VILLAGE);
			searchTypes.add(POSTCODE);
			searchTypes.add(STREET);
			searchTypes.add(HOUSE);
			searchTypes.add(STREET_INTERSECTION);
		}
		searchSettings = searchSettings.setSearchTypes(searchTypes.toArray(new ObjectType[0]));

		core.search(searchQuery, false, null, searchSettings);
	}

	public void testApi(OsmandApplication app, String command) {
		Uri uri = null;
		Intent intent = null;

		String lat = "44.98062";
		String lon = "34.09258";
		String destLat = "44.97799";
		String destLon = "34.10286";
		String gpxName = "xxx.gpx";

		try {

			if (API_CMD_GET_INFO.equals(command)) {
				uri = Uri.parse("osmand.api://get_info");
			}

			if (API_CMD_NAVIGATE.equals(command)) {
				// test navigate
				uri = Uri.parse("osmand.api://navigate" +
						"?start_lat=" + lat + "&start_lon=" + lon + "&start_name=Start" +
						"&dest_lat=" + destLat + "&dest_lon=" + destLon + "&dest_name=Finish" +
						"&profile=bicycle");
			}

			if (API_CMD_RECORD_AUDIO.equals(command)) {
				// test record audio
				uri = Uri.parse("osmand.api://record_audio?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_RECORD_VIDEO.equals(command)) {
				// test record video
				uri = Uri.parse("osmand.api://record_video?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_RECORD_PHOTO.equals(command)) {
				// test take photo
				uri = Uri.parse("osmand.api://record_photo?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_STOP_AV_REC.equals(command)) {
				// test record video
				uri = Uri.parse("osmand.api://stop_av_rec");
			}

			if (API_CMD_ADD_MAP_MARKER.equals(command)) {
				// test marker
				uri = Uri.parse("osmand.api://add_map_marker?lat=" + lat + "&lon=" + lon + "&name=Marker");
			}

			if (API_CMD_SHOW_LOCATION.equals(command)) {
				// test location
				uri = Uri.parse("osmand.api://show_location?lat=" + lat + "&lon=" + lon);
			}

			if (API_CMD_ADD_FAVORITE.equals(command)) {
				// test favorite
				uri = Uri.parse("osmand.api://add_favorite?lat=" + lat + "&lon=" + lon + "&name=Favorite&desc=Description&category=test2&color=red&visible=true");
			}

			if (API_CMD_START_GPX_REC.equals(command)) {
				// test start gpx recording
				uri = Uri.parse("osmand.api://start_gpx_rec");
			}

			if (API_CMD_STOP_GPX_REC.equals(command)) {
				// test stop gpx recording
				uri = Uri.parse("osmand.api://stop_gpx_rec");
			}

			if (API_CMD_SHOW_GPX.equals(command)) {
				// test show gpx (path)
				//File gpx = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName);
				//uri = Uri.parse("osmand.api://show_gpx?path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));

				// test show gpx (data)
				uri = Uri.parse("osmand.api://show_gpx");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.putExtra("data", Algorithms.getFileAsString(
						new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName)));
			}

			if (API_CMD_NAVIGATE_GPX.equals(command)) {
				// test navigate gpx (path)
				//File gpx = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName);
				//uri = Uri.parse("osmand.api://navigate_gpx?force=true&path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));

				// test navigate gpx (data)
				uri = Uri.parse("osmand.api://navigate_gpx?force=true");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.putExtra("data", Algorithms.getFileAsString(
						new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName)));
			}

			if (intent == null && uri != null) {
				intent = new Intent(Intent.ACTION_VIEW, uri);
			}

			if (intent != null) {
				AndroidUtils.startActivityIfSafe(mapActivity, intent);
			}

		} catch (Exception e) {
			LOG.error("Test failed", e);
		}
	}
}

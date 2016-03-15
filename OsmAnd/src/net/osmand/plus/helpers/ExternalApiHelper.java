package net.osmand.plus.helpers;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.File;

public class ExternalApiHelper {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExternalApiHelper.class);

	private static final String API_CMD_SHOW_GPX = "show_gpx";
	private static final String API_CMD_NAVIGATE_GPX = "navigate_gpx";

	private static final String API_CMD_CALC_ROUTE = "calc_route";
	private static final String API_CMD_REC_AV_NOTE = "rec_av_note";
	private static final String API_CMD_GET_INFO = "get_info";

	private static final String API_CMD_ADD_FAVORITE = "add_favorite";
	private static final String API_CMD_ADD_MAP_MARKER = "add_map_marker";

	private static final String API_CMD_START_GPX_REC = "start_gpx_rec";
	private static final String API_CMD_STOP_GPX_REC = "stop_gpx_rec";

	private static final String API_CMD_SUBSCRIBE_VOICE_NOTIFICATIONS = "subscribe_voice_notifications";

	private static final String PARAM_NAME = "name";
	private static final String PARAM_DESC = "desc";
	private static final String PARAM_CATEGORY = "category";
	private static final String PARAM_LAT = "lat";
	private static final String PARAM_LON = "lon";
	private static final String PARAM_COLOR = "color";
	private static final String PARAM_VISIBLE = "visible";

	private static final String PARAM_PATH = "path";
	private static final String PARAM_DATA = "data";


	private static final int RESULT_CODE_OK = 0;
	private static final int RESULT_CODE_ERROR_UNKNOWN = -1;
	private static final int RESULT_CODE_ERROR_GPX_PLUGIN_INACTIVE = 10;
	private static final int RESULT_CODE_ERROR_GPX_NOT_FOUND = 20;

	private MapActivity mapActivity;
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

			/*
			+ 1. Intent to show GPX file / start navigation with GPX
			2. Intent to calculate route between points (passing profile mode) and immediately start navigation
			3. Intent to request audio/video recording
			4. Intent (with result?) Current location, ETA, distance to go, time to go on the route
			+ 5. Intent to add Favorites / Markers
			+ 6. Intent to start/stop recording GPX
			Service:
			8. Subscribe to voice notifications


			// test marker
			Uri uri = Uri.parse("osmand.api://add_map_marker?lat=45.610677&lon=34.368430&name=Marker");

			// test favorite
			Uri uri = Uri.parse("osmand.api://add_favorite?lat=45.610677&lon=34.368430&name=Favorite&desc=Description&category=test2&color=red&visible=true");

			// test start gpx recording
			Uri uri = Uri.parse("osmand.api://start_gpx_rec");

			// test stop gpx recording
			Uri uri = Uri.parse("osmand.api://stop_gpx_rec");

			// test show gpx (path)
			File gpx = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), "xxx.gpx");
			Uri uri = Uri.parse("osmand.api://show_gpx?path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));
			Uri uri = Uri.parse("osmand.api://navigate_gpx?path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));

			// test show gpx (data)
			Uri uri = Uri.parse("osmand.api://show_gpx");
			Uri uri = Uri.parse("osmand.api://navigate_gpx");
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			intent.putExtra("data", AndroidUtils.getFileAsString(
					new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), "xxx.gpx")));

			 */

			Uri uri = intent.getData();
			String cmd = uri.getHost().toLowerCase();
			if (API_CMD_SHOW_GPX.equals(cmd) || API_CMD_NAVIGATE_GPX.equals(cmd)) {
				boolean navigate = API_CMD_NAVIGATE_GPX.equals(cmd);
				String path = uri.getQueryParameter(PARAM_PATH);
				GPXFile gpx = null;
				if (path != null) {
					File f = new File(path);
					if (f.exists()) {
						gpx = GPXUtilities.loadGPXFile(mapActivity, f);
					}
				} else if (intent.getStringExtra(PARAM_DATA) != null) {
					String gpxStr = intent.getStringExtra(PARAM_DATA);
					if (!Algorithms.isEmpty(gpxStr)) {
						gpx = GPXUtilities.loadGPXFile(mapActivity, new ByteArrayInputStream(gpxStr.getBytes()));
					}
				} else {
					resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
				}

				if (gpx != null) {
					if (navigate) {
						final RoutingHelper routingHelper = app.getRoutingHelper();
						if (routingHelper.isFollowingMode()) {
							final GPXFile gpxFile = gpx;
							AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
							dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
									if (!routingHelper.isFollowingMode()) {
										startNavigation(gpxFile);
									}
								}
							});
						} else {
							startNavigation(gpx);
						}
					} else {
						app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
					}
					resultCode = RESULT_CODE_OK;
				} else {
					resultCode = RESULT_CODE_ERROR_GPX_NOT_FOUND;
				}

			} else if (API_CMD_CALC_ROUTE.equals(cmd)) {

			} else if (API_CMD_REC_AV_NOTE.equals(cmd)) {

			} else if (API_CMD_GET_INFO.equals(cmd)) {

				finish = true;
				resultCode = RESULT_CODE_OK;

			} else if (API_CMD_ADD_FAVORITE.equals(cmd)) {
				String name = uri.getQueryParameter(PARAM_NAME);
				String desc = uri.getQueryParameter(PARAM_DESC);
				String category = uri.getQueryParameter(PARAM_CATEGORY);
				double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
				double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
				String colorTag = uri.getQueryParameter(PARAM_COLOR);
				String visibleStr = uri.getQueryParameter(PARAM_VISIBLE);

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
					color = ColorDialogs.getColorByTag(colorTag);
					if (color == 0) {
						LOG.error("Wrong color tag: " + colorTag);
					}
				}

				boolean visible = true;
				if (!Algorithms.isEmpty(visibleStr)) {
					visible = Boolean.parseBoolean(visibleStr);
				}

				FavouritePoint fav = new FavouritePoint(lat, lon, name, category);
				fav.setDescription(desc);
				fav.setColor(color);
				fav.setVisible(visible);

				FavouritesDbHelper helper = app.getFavorites();
				helper.addFavourite(fav);

				resultCode = RESULT_CODE_OK;

			} else if (API_CMD_ADD_MAP_MARKER.equals(cmd)) {
				double lat = Double.parseDouble(uri.getQueryParameter(PARAM_LAT));
				double lon = Double.parseDouble(uri.getQueryParameter(PARAM_LON));
				String name = uri.getQueryParameter(PARAM_NAME);

				PointDescription pd = new PointDescription(
						PointDescription.POINT_TYPE_MAP_MARKER, name != null ? name : "");

				MapMarkersHelper markersHelper = app.getMapMarkersHelper();
				markersHelper.addMapMarker(new LatLon(lat, lon), pd);

				resultCode = RESULT_CODE_OK;

			} else if (API_CMD_START_GPX_REC.equals(cmd)) {
				OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_GPX_PLUGIN_INACTIVE;
				} else {
					plugin.startGPXMonitoring(null);
				}

				resultCode = RESULT_CODE_OK;

			} else if (API_CMD_STOP_GPX_REC.equals(cmd)) {
				OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (plugin == null) {
					resultCode = RESULT_CODE_ERROR_GPX_PLUGIN_INACTIVE;
				} else {
					plugin.stopRecording();
				}

				resultCode = RESULT_CODE_OK;

			} else if (API_CMD_SUBSCRIBE_VOICE_NOTIFICATIONS.equals(cmd)) {

			}

		} catch (Exception e) {
			LOG.error("Error processApiRequest:", e);
			resultCode = RESULT_CODE_ERROR_UNKNOWN;
		}

		return result;
	}

	private void startNavigation(GPXFile gpx) {
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, null, null, false, false);
		if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
			mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().show();
		} else {
			app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
			mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
			app.getSettings().FOLLOW_THE_ROUTE.set(true);
			routingHelper.setFollowingMode(true);
			routingHelper.setRoutePlanningMode(false);
			mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
			app.getRoutingHelper().notifyIfRouteIsCalculated();
			routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
		}
	}
}

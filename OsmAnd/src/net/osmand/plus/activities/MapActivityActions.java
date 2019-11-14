package net.osmand.plus.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.ContextMenuItem.ItemBuilder;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.actions.OsmAndDialogs;
import net.osmand.plus.activities.actions.OsmandRestoreOrExitDialog;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MarkersPlanRouteContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.profiles.SettingsProfileActivity;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.WaypointsFragment;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.wikivoyage.WikivoyageWelcomeDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_MAP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_SCREEN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIRECTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_HELP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MAP_MARKERS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MY_PLACES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSMAND_LIVE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_PLUGINS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SEARCH_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRAVEL_GUIDES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AVOID_ROAD;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_EDIT_GPX_WP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SEARCH_NEARBY;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_CHOSEN_PROFILE_TAG;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_CONTROL_BUTTON_TAG;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_NORMAL_PROFILE_TAG;
import static net.osmand.plus.helpers.ImportHelper.GPX_SUFFIX;


public class MapActivityActions implements DialogProvider {
	private static final Log LOG = PlatformUtil.getLog(MapActivityActions.class);
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_NAME = "name";

	public static final String KEY_ZOOM = "zoom";

	public static final int REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION = 203;

	// Constants for determining the order of items in the additional actions context menu
	public static final int DIRECTIONS_FROM_ITEM_ORDER = 1000;
	public static final int SEARCH_NEAR_ITEM_ORDER = 2000;
	public static final int CHANGE_POSITION_ITEM_ORDER = 3000;
	public static final int EDIT_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int ADD_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int MEASURE_DISTANCE_ITEM_ORDER = 13000;
	public static final int AVOID_ROAD_ITEM_ORDER = 14000;

	private static final int DIALOG_ADD_FAVORITE = 100;
	private static final int DIALOG_REPLACE_FAVORITE = 101;
	private static final int DIALOG_ADD_WAYPOINT = 102;
	private static final int DIALOG_RELOAD_TITLE = 103;

	private static final int DIALOG_SAVE_DIRECTIONS = 106;
	
	private static final int DRAWER_MODE_NORMAL = 0;
	private static final int DRAWER_MODE_SWITCH_PROFILE = 1;

	// make static
	private static Bundle dialogBundle = new Bundle();

	private final MapActivity mapActivity;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;

	@NonNull
	private ImageView drawerLogoHeader;
	private View drawerOsmAndFooter;
	
	private int drawerMode = DRAWER_MODE_NORMAL;

	public MapActivityActions(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		drawerLogoHeader = new ImageView(mapActivity);
		drawerLogoHeader.setPadding(-AndroidUtils.dpToPx(mapActivity, 8f), AndroidUtils.dpToPx(mapActivity, 16f), 0,
				0);
		drawerOsmAndFooter = mapActivity.getLayoutInflater().inflate(R.layout.powered_by_osmand_item, null);
	}

	public void addAsTarget(double latitude, double longitude, PointDescription pd) {
		TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
		targets.navigateToPoint(new LatLon(latitude, longitude), true, targets.getIntermediatePoints().size() + 1,
				pd);
		openIntermediatePointsDialog();
	}


	public void addMapMarker(double latitude, double longitude, PointDescription pd, @Nullable String mapObjectName) {
		MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd, mapObjectName);
	}

	public void editWaypoints() {
		openIntermediatePointsDialog();
	}

	private Bundle enhance(Bundle aBundle, double latitude, double longitude, String name) {
		aBundle.putDouble(KEY_LATITUDE, latitude);
		aBundle.putDouble(KEY_LONGITUDE, longitude);
		aBundle.putString(KEY_NAME, name);
		return aBundle;
	}

	private Bundle enhance(Bundle bundle, double latitude, double longitude, final int zoom) {
		bundle.putDouble(KEY_LATITUDE, latitude);
		bundle.putDouble(KEY_LONGITUDE, longitude);
		bundle.putInt(KEY_ZOOM, zoom);
		return bundle;
	}

	private Dialog createAddWaypointDialog(final Bundle args) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		builder.setTitle(R.string.add_waypoint_dialog_title);

		View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.add_gpx_point_dialog, null);
		final EditText editText = (EditText) view.findViewById(android.R.id.edit);
		builder.setView(view);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				double latitude = args.getDouble(KEY_LATITUDE);
				double longitude = args.getDouble(KEY_LONGITUDE);
				String name = editText.getText().toString();
				SavingTrackHelper savingTrackHelper = mapActivity.getMyApplication().getSavingTrackHelper();
				savingTrackHelper.insertPointData(latitude, longitude, System.currentTimeMillis(), null, name, null, 0);
				Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
						.show();
				dialog.dismiss();
			}
		});
		final AlertDialog alertDialog = builder.create();
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		return alertDialog;
	}

	public void reloadTile(final int zoom, final double latitude, final double longitude) {
		enhance(dialogBundle, latitude, longitude, zoom);
		mapActivity.showDialog(DIALOG_RELOAD_TITLE);
	}

	protected String getString(int res) {
		return mapActivity.getString(res);
	}

	protected void showToast(final String msg) {
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

	protected Location getLastKnownLocation() {
		return getMyApplication().getLocationProvider().getLastKnownLocation();
	}

	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}

	public void saveDirections() {
		mapActivity.showDialog(DIALOG_SAVE_DIRECTIONS);
	}

	public static Dialog createSaveDirections(Activity activity, RoutingHelper routingHelper) {
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final File fileDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final Dialog dlg = new Dialog(activity);
		dlg.setTitle(R.string.shared_string_save_as_gpx);
		dlg.setContentView(R.layout.save_directions_dialog);
		final EditText edit = (EditText) dlg.findViewById(R.id.FileNameEdit);

		final GPXRouteParamsBuilder rp = routingHelper.getCurrentGPXRoute();
		final String editText;
		if (rp == null || rp.getFile() == null || rp.getFile().path == null) {
			editText = "_" + MessageFormat.format("{0,date,yyyy-MM-dd}", new Date()) + "_";
		} else {
			editText = new File(rp.getFile().path).getName();
		}
		edit.setText(editText);

		dlg.findViewById(R.id.Save).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = edit.getText().toString();
				//noinspection ResultOfMethodCallIgnored
				fileDir.mkdirs();
				File toSave = fileDir;
				if (name.length() > 0) {
					if (!name.endsWith(GPX_SUFFIX)) {
						name += GPX_SUFFIX;
					}
					toSave = new File(fileDir, name);
				}
				if (toSave.exists()) {
					dlg.findViewById(R.id.DuplicateFileName).setVisibility(View.VISIBLE);
				} else {
					dlg.dismiss();
					new SaveDirectionsAsyncTask(app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, toSave);
				}
			}
		});

		dlg.findViewById(R.id.Cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});


		return dlg;
	}

	private static class SaveDirectionsAsyncTask extends AsyncTask<File, Void, String> {

		private final OsmandApplication app;

		public SaveDirectionsAsyncTask(OsmandApplication app) {
			this.app = app;
		}

		@Override
		protected String doInBackground(File... params) {
			if (params.length > 0) {
				File file = params[0];
				String fileName = file.getName();
				GPXFile gpx = app.getRoutingHelper().generateGPXFileWithRoute(fileName.substring(0,fileName.length()-GPX_SUFFIX.length()));
				GPXUtilities.writeGpxFile(file, gpx);
				return app.getString(R.string.route_successfully_saved_at, file.getName());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				Toast.makeText(app, result, Toast.LENGTH_LONG).show();
			}
		}

	}

	public void contextMenuPoint(final double latitude, final double longitude, final ContextMenuAdapter iadapter, Object selectedObj) {
		final ContextMenuAdapter adapter = iadapter == null ? new ContextMenuAdapter() : iadapter;
		ItemBuilder itemBuilder = new ItemBuilder();

		adapter.addItem(itemBuilder
				.setTitleId(R.string.context_menu_item_directions_from, mapActivity)
				.setId(MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID)
				.setIcon(R.drawable.ic_action_route_direction_from_here)
				.setOrder(DIRECTIONS_FROM_ITEM_ORDER)
				.createItem());
		adapter.addItem(itemBuilder
				.setTitleId(R.string.context_menu_item_search, mapActivity)
				.setId(MAP_CONTEXT_MENU_SEARCH_NEARBY)
				.setIcon(R.drawable.ic_action_search_dark)
				.setOrder(SEARCH_NEAR_ITEM_ORDER)
				.createItem());

		OsmandPlugin.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj);

		ItemClickListener listener = new ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked, int[] viewCoordinates) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getContextMenu().addWptPt();
				} else if (resId == R.string.context_menu_item_edit_waypoint) {
					mapActivity.getContextMenu().editWptPt();
				}
				return true;
			}
		};

		if (selectedObj instanceof WptPt
				&& getMyApplication().getSelectedGpxHelper().getSelectedGPXFile((WptPt) selectedObj) != null) {
			adapter.addItem(new ItemBuilder()
					.setTitleId(R.string.context_menu_item_edit_waypoint, mapActivity)
					.setId(MAP_CONTEXT_MENU_EDIT_GPX_WP)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(EDIT_GPX_WAYPOINT_ITEM_ORDER)
					.setListener(listener).createItem());
		} else if (!getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles().isEmpty()
				|| (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null)) {
			adapter.addItem(new ItemBuilder()
					.setTitleId(R.string.context_menu_item_add_waypoint, mapActivity)
					.setId(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
					.setIcon(R.drawable.ic_action_gnew_label_dark)
					.setOrder(ADD_GPX_WAYPOINT_ITEM_ORDER)
					.setListener(listener).createItem());
		}

		adapter.addItem(itemBuilder
				.setTitleId(R.string.measurement_tool, mapActivity)
				.setId(MAP_CONTEXT_MENU_MEASURE_DISTANCE)
				.setIcon(R.drawable.ic_action_ruler)
				.setOrder(MEASURE_DISTANCE_ITEM_ORDER)
				.createItem());

		adapter.addItem(itemBuilder
				.setTitleId(R.string.avoid_road, mapActivity)
				.setId(MAP_CONTEXT_MENU_AVOID_ROAD)
				.setIcon(R.drawable.ic_action_alert)
				.setOrder(AVOID_ROAD_ITEM_ORDER)
				.createItem());

		adapter.sortItemsByOrder();

		final ArrayAdapter<ContextMenuItem> listAdapter =
				adapter.createListAdapter(mapActivity, getMyApplication().getSettings().isLightContent());

		AdditionalActionsBottomSheetDialogFragment actionsBottomSheetDialogFragment = new AdditionalActionsBottomSheetDialogFragment();
		actionsBottomSheetDialogFragment.setAdapter(adapter, new AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener() {
			@Override
			public void onItemClick(int position) {
				ContextMenuItem item = adapter.getItem(position);
				int standardId = item.getTitleId();
				ItemClickListener click = item.getItemClickListener();
				if (click != null) {
					click.onContextMenuClick(listAdapter, standardId, position, false, null);
				} else if (standardId == R.string.context_menu_item_search) {
					mapActivity.showQuickSearch(latitude, longitude);
				} else if (standardId == R.string.context_menu_item_directions_from) {
					if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
						enterDirectionsFromPoint(latitude, longitude);
					} else if (!ActivityCompat.shouldShowRequestPermissionRationale(mapActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
						mapActivity.getMyApplication().showToastMessage(R.string.ask_for_location_permission);
					} else {
						ActivityCompat.requestPermissions(mapActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION);
					}
				} else if (standardId == R.string.measurement_tool) {
					mapActivity.getContextMenu().close();
					MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), new LatLon(latitude, longitude));
				} else if (standardId == R.string.avoid_road) {
					getMyApplication().getAvoidSpecificRoads().addImpassableRoad(mapActivity, new LatLon(latitude, longitude), true, false);
				}
			}
		});
		actionsBottomSheetDialogFragment.show(mapActivity.getSupportFragmentManager(), AdditionalActionsBottomSheetDialogFragment.TAG);
	}

	public void enterDirectionsFromPoint(final double latitude, final double longitude) {
		mapActivity.getContextMenu().hide();
		if (!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isRoutePlanningMode()) {
			enterRoutePlanningMode(new LatLon(latitude, longitude),
					mapActivity.getContextMenu().getPointDescription());
		} else {
			getMyApplication().getTargetPointsHelper().setStartPoint(new LatLon(latitude, longitude),
					true, mapActivity.getContextMenu().getPointDescription());
		}
	}

	public void setGPXRouteParams(GPXFile result) {
		if (result == null) {
			mapActivity.getRoutingHelper().setGpxParams(null);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
		} else {
			GPXRouteParamsBuilder params = new GPXRouteParamsBuilder(result, mapActivity.getMyApplication()
					.getSettings());
			if (result.hasRtePt() && !result.hasTrkPt()) {
				settings.GPX_CALCULATE_RTEPT.set(true);
			} else {
				settings.GPX_CALCULATE_RTEPT.set(false);
			}
			params.setCalculateOsmAndRouteParts(settings.GPX_ROUTE_CALC_OSMAND_PARTS.get());
			params.setUseIntermediatePointsRTE(settings.GPX_CALCULATE_RTEPT.get());
			params.setCalculateOsmAndRoute(settings.GPX_ROUTE_CALC.get());
			List<Location> ps = params.getPoints();
			mapActivity.getRoutingHelper().setGpxParams(params);
			settings.FOLLOW_THE_GPX_ROUTE.set(result.path);
			if (!ps.isEmpty()) {
				Location startLoc = ps.get(0);
				Location finishLoc = ps.get(ps.size() - 1);
				TargetPointsHelper tg = mapActivity.getMyApplication().getTargetPointsHelper();
				tg.navigateToPoint(new LatLon(finishLoc.getLatitude(), finishLoc.getLongitude()), false, -1);
				if (startLoc != finishLoc) {
					tg.setStartPoint(new LatLon(startLoc.getLatitude(), startLoc.getLongitude()), false, null);
				} else {
					tg.clearStartPoint(false);
				}
			}
		}
	}

	public void enterRoutePlanningMode(final LatLon from, final PointDescription fromName) {
		enterRoutePlanningModeGivenGpx(null, from, fromName, true, true);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
											   boolean useIntermediatePointsByDefault, boolean showMenu) {
		enterRoutePlanningModeGivenGpx(gpxFile, from, fromName, useIntermediatePointsByDefault, showMenu, MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
											   boolean useIntermediatePointsByDefault, boolean showMenu, int menuState) {
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(useIntermediatePointsByDefault);
		OsmandApplication app = mapActivity.getMyApplication();
		TargetPointsHelper targets = app.getTargetPointsHelper();

		ApplicationMode mode = getRouteMode(from);
		//app.getSettings().APPLICATION_MODE.set(mode);
		app.getRoutingHelper().setAppMode(mode);
		app.initVoiceCommandPlayer(mapActivity, mode, true, null, false, false, showMenu);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		app.getRoutingHelper().setFollowingMode(false);
		app.getRoutingHelper().setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(from, false, fromName);
		// then set gpx
		setGPXRouteParams(gpxFile);
		// then update start and destination point  
		targets.updateRouteAndRefresh(true);

		mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		mapActivity.getMapView().refreshMap(true);
		if (showMenu) {
			mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoMenu(menuState);
		}
		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	public void recalculateRoute(boolean showDialog) {
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(true);
		OsmandApplication app = mapActivity.getMyApplication();
		TargetPointsHelper targets = app.getTargetPointsHelper();

		ApplicationMode mode = getRouteMode(null);
		//app.getSettings().APPLICATION_MODE.set(mode);
		app.getRoutingHelper().setAppMode(mode);
		//Test for #2810: No need to init player here?
		//app.initVoiceCommandPlayer(mapActivity, true, null, false, false);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		app.getRoutingHelper().setFollowingMode(false);
		app.getRoutingHelper().setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(null, false, null);
		// then update start and destination point
		targets.updateRouteAndRefresh(true);

		mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		mapActivity.getMapView().refreshMap(true);
		if (showDialog) {
			mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoMenu();
		}
		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	public ApplicationMode getRouteMode(LatLon from) {
		MarkersPlanRouteContext planRouteContext = mapActivity.getMyApplication().getMapMarkersHelper().getPlanRouteContext();
		if (planRouteContext.isNavigationFromMarkers() && planRouteContext.getSnappedMode() != ApplicationMode.DEFAULT) {
			planRouteContext.setNavigationFromMarkers(false);
			return planRouteContext.getSnappedMode();
		}
		ApplicationMode mode = settings.DEFAULT_APPLICATION_MODE.get();
		ApplicationMode selected = settings.APPLICATION_MODE.get();
		if (selected != ApplicationMode.DEFAULT) {
			mode = selected;
		} else if (mode == ApplicationMode.DEFAULT) {
			mode = ApplicationMode.CAR;
			if (settings.LAST_ROUTING_APPLICATION_MODE != null &&
					settings.LAST_ROUTING_APPLICATION_MODE != ApplicationMode.DEFAULT) {
				mode = settings.LAST_ROUTING_APPLICATION_MODE;
			}
		}
		return mode;
	}

	public void contextMenuPoint(final double latitude, final double longitude) {
		contextMenuPoint(latitude, longitude, null, null);
	}

	private Dialog createReloadTitleDialog(final Bundle args) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setMessage(R.string.context_menu_item_update_map_confirm);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		final OsmandMapTileView mapView = mapActivity.getMapView();
		builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int zoom = args.getInt(KEY_ZOOM);
				BaseMapLayer mainLayer = mapView.getMainLayer();
				if (!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()) {
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
				if (mapSource == null || !mapSource.couldBeDownloadedFromInternet()) {
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
				final QuadRect tilesRect = tb.getTileBounds();
				int left = (int) Math.floor(tilesRect.left);
				int top = (int) Math.floor(tilesRect.top);
				int width = (int) (Math.ceil(tilesRect.right) - left);
				int height = (int) (Math.ceil(tilesRect.bottom) - top);
				for (int i = 0; i < width; i++) {
					for (int j = 0; j < height; j++) {
						((OsmandApplication) mapActivity.getApplication()).getResourceManager().
								clearTileForMap(null, mapSource, i + left, j + top, zoom);
					}
				}


				mapView.refreshMap();
			}
		});
		return builder.create();
	}


	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				return FavoriteDialogs.createAddFavouriteDialog(mapActivity, args);
			case DIALOG_REPLACE_FAVORITE:
				return FavoriteDialogs.createReplaceFavouriteDialog(mapActivity, args);
			case DIALOG_ADD_WAYPOINT:
				return createAddWaypointDialog(args);
			case DIALOG_RELOAD_TITLE:
				return createReloadTitleDialog(args);
			case DIALOG_SAVE_DIRECTIONS:
				return createSaveDirections(mapActivity, mapActivity.getRoutingHelper());
		}
		return OsmAndDialogs.createDialog(id, mapActivity, args);
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				FavoriteDialogs.prepareAddFavouriteDialog(mapActivity, dialog, args,
						args.getDouble(KEY_LATITUDE), args.getDouble(KEY_LONGITUDE),
						new PointDescription(PointDescription.POINT_TYPE_FAVORITE, args.getString(KEY_NAME)));
				break;
			case DIALOG_ADD_WAYPOINT:
				EditText v = (EditText) dialog.getWindow().findViewById(android.R.id.edit);
				v.setPadding(5, 0, 5, 0);
				if (args.getString(KEY_NAME) != null) {
					v.setText(args.getString(KEY_NAME));
					v.selectAll();
				} else {
					v.setText("");
				}
				break;
		}
	}

	public ContextMenuAdapter createMainOptionsMenu() {
		final OsmandMapTileView mapView = mapActivity.getMapView();
		final OsmandApplication app = mapActivity.getMyApplication();
		ContextMenuAdapter optionsMenuHelper = new ContextMenuAdapter();
		boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		
		if (drawerMode == DRAWER_MODE_SWITCH_PROFILE) {
			return createSwitchProfileOptionsMenu(app, optionsMenuHelper, nightMode);
		}
		return createNormalOptionsMenu(app, optionsMenuHelper, nightMode);
	}

	private ContextMenuAdapter createSwitchProfileOptionsMenu(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {
		drawerMode = DRAWER_MODE_NORMAL;
		createProfilesController(app, optionsMenuHelper, nightMode, true);
		
		List<ApplicationMode> activeModes = ApplicationMode.values(app);
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();

		String modeDescription;
		
		for (final ApplicationMode appMode : activeModes) {
			if (appMode.isCustomProfile()) {
				modeDescription = String.format(app.getString(R.string.profile_type_descr_string),
						Algorithms.capitalizeFirstLetterAndLowercase(appMode.getParent().toHumanString(app)));
			} else {
				modeDescription = getString(R.string.profile_type_base_string);
			}

			int tag = currentMode.equals(appMode) ? PROFILES_CHOSEN_PROFILE_TAG : PROFILES_NORMAL_PROFILE_TAG;

			optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.profile_list_item)
					.setIcon(appMode.getIconRes())
					.setColor(appMode.getIconColorInfo().getColor(nightMode))
					.setTag(tag)
					.setTitle(appMode.toHumanString(app))
					.setDescription(modeDescription)
					.setListener(new ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
							app.getSettings().APPLICATION_MODE.set(appMode);
							updateDrawerMenu();
							return false;
						}
					})
					.createItem());
		}
		
		int activeColorPrimaryResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.profile_list_item)
				.setColor(activeColorPrimaryResId)
				.setTag(PROFILES_CONTROL_BUTTON_TAG)
				.setTitle(getString(R.string.shared_string_manage))
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						Intent intent = new Intent(app, SettingsProfileActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						app.startActivity(intent);
						return true;
					}
				})
				.createItem());
		
		return optionsMenuHelper;
	}

	private ContextMenuAdapter createNormalOptionsMenu(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {
		
		createProfilesController(app, optionsMenuHelper, nightMode, false);

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.home, mapActivity)
				.setId(DRAWER_DASHBOARD_ID)
				.setIcon(R.drawable.map_dashboard)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_dashboard_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.closeDrawer();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD, viewCoordinates);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.map_markers, mapActivity)
				.setId(DRAWER_MAP_MARKERS_ID)
				.setIcon(R.drawable.ic_action_flag_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_markers_open");
						MapActivity.clearPrevActivityIntent();
						MapMarkersDialogFragment.showInstance(mapActivity);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_my_places, mapActivity)
				.setId(DRAWER_MY_PLACES_ID)
				.setIcon(R.drawable.ic_action_fav_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_myplaces_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getFavoritesActivity());
						newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.search_button, mapActivity)
				.setId(DRAWER_SEARCH_ID)
				.setIcon(R.drawable.ic_action_search_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_search_open");
						mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW_IF_EXPIRED, false);
						return true;
					}
				}).createItem());


		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.get_directions, mapActivity)
				.setId(DRAWER_DIRECTIONS_ID)
				.setIcon(R.drawable.ic_action_gdirections_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_directions_open");
						MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
						if (mapControlsLayer != null) {
							mapControlsLayer.doRoute(false);
						}
						return true;
					}
				}).createItem());

		/*
		optionsMenuHelper.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.show_point_options, mapActivity)
				.setIcon(R.drawable.ic_action_marker_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						MapActivity.clearPrevActivityIntent();
						mapActivity.getMapLayers().getContextMenuLayer().showContextMenu(mapView.getLatitude(), mapView.getLongitude(), true);
						return true;
					}
				}).createItem());
		*/

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.configure_map, mapActivity)
				.setId(DRAWER_CONFIGURE_MAP_ID)
				.setIcon(R.drawable.ic_action_layers_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_config_map_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, viewCoordinates);
						return false;
					}
				}).createItem());

		String d = getString(R.string.welmode_download_maps);
		if (app.getDownloadThread().getIndexes().isDownloadedFromInternet) {
			List<IndexItem> updt = app.getDownloadThread().getIndexes().getItemsToUpdate();
			if (updt != null && updt.size() > 0) {
				d += " (" + updt.size() + ")";
			}
		}
		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.welmode_download_maps, null)
				.setId(DRAWER_DOWNLOAD_MAPS_ID)
				.setTitle(d).setIcon(R.drawable.ic_type_archive)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_download_maps_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getDownloadActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		if (Version.isGooglePlayEnabled(app) || Version.isDeveloperVersion(app)) {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.osm_live, mapActivity)
					.setId(DRAWER_OSMAND_LIVE_ID)
					.setIcon(R.drawable.ic_action_osm_live)
					.setListener(new ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							app.logEvent("drawer_osm_live_open");
							Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							mapActivity.startActivity(intent);
							return true;
						}
					}).createItem());
		}

		optionsMenuHelper.addItem(new ItemBuilder().setTitle(getString(R.string.shared_string_travel_guides) + " (Beta)")
				.setId(DRAWER_TRAVEL_GUIDES_ID)
				.setIcon(R.drawable.ic_action_travel)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						MapActivity.clearPrevActivityIntent();
						TravelDbHelper travelDbHelper = getMyApplication().getTravelDbHelper();
						travelDbHelper.initTravelBooks();
						if (travelDbHelper.getSelectedTravelBook() == null) {
							WikivoyageWelcomeDialogFragment.showInstance(mapActivity.getSupportFragmentManager());
						} else {
							Intent intent = new Intent(mapActivity, WikivoyageExploreActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							mapActivity.startActivity(intent);
						}
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.measurement_tool, mapActivity)
				.setId(DRAWER_MEASURE_DISTANCE_ID)
				.setIcon(R.drawable.ic_action_ruler)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}
				}).createItem());

		app.getAidlApi().registerNavDrawerItems(mapActivity, optionsMenuHelper);

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.layer_map_appearance, mapActivity)
				.setId(DRAWER_CONFIGURE_SCREEN_ID)
				.setIcon(R.drawable.ic_configure_screen_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_config_screen_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_SCREEN, viewCoordinates);
						return false;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.prefs_plugins, mapActivity)
				.setId(DRAWER_PLUGINS_ID)
				.setIcon(R.drawable.ic_extension_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_plugins_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getPluginsActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		/*
		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_settings, mapActivity)
				.setId(DRAWER_SETTINGS_ID)
				.setIcon(R.drawable.ic_action_settings)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_settings_open");
						final Intent settings = new Intent(mapActivity, getMyApplication().getAppCustomization()
								.getSettingsActivity());
						settings.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(settings);
						return true;
					}
				}).createItem());
		*/
		optionsMenuHelper.addItem(new ItemBuilder().setTitle(getString(R.string.shared_string_settings))
				.setId(DRAWER_SETTINGS_ID + ".new")
				.setIcon(R.drawable.ic_action_settings)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_settings_new_open");
						mapActivity.showSettings();
						return true;
					}
				}).createItem());

		/*
		optionsMenuHelper.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.configure_map, mapActivity)
				.setIcon(R.drawable.ic_action_layers_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP);
						return false;
					}
				}).createItem());
		*/

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_help, mapActivity)
				.setId(DRAWER_HELP_ID)
				.setIcon(R.drawable.ic_action_help)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_help_open");
						Intent intent = new Intent(mapActivity, HelpActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(intent);
						return true;
					}
				}).createItem());

		//////////// Others
		OsmandPlugin.registerOptionsMenu(mapActivity, optionsMenuHelper);

		// Place divider between functionality and configuration related menu items
		int dividerItemIndex = -1;
		for (int i = 0; i < optionsMenuHelper.length(); i++) {
			if (optionsMenuHelper.getItem(i).getTitleId() == R.string.layer_map_appearance) {
				dividerItemIndex = i;
				break;
			}
		}

		ItemBuilder divider = new ItemBuilder().setLayout(R.layout.drawer_divider);
		divider.setId(DRAWER_DIVIDER_ID);
		divider.setPosition(dividerItemIndex >= 0 ? dividerItemIndex : 8);
		optionsMenuHelper.addItem(divider.createItem());

		return optionsMenuHelper;
	}

	private void createProfilesController(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode, boolean listExpanded) {
		//switch profile button
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();
		String modeDescription;
		if (currentMode.isCustomProfile()) {
			modeDescription = String.format(app.getString(R.string.profile_type_descr_string),
					Algorithms.capitalizeFirstLetterAndLowercase(currentMode.getParent().toHumanString(app)));
		} else {
			modeDescription = getString(R.string.profile_type_base_string);
		}
		int icArrowResId = listExpanded ? R.drawable.ic_action_arrow_drop_up : R.drawable.ic_action_arrow_drop_down;
		final int nextMode = listExpanded ? DRAWER_MODE_NORMAL : DRAWER_MODE_SWITCH_PROFILE;
		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.main_menu_drawer_btn_switch_profile)
				.setIcon(currentMode.getIconRes())
				.setSecondaryIcon(icArrowResId)
				.setColor(currentMode.getIconColorInfo().getColor(nightMode))
				.setTitle(currentMode.toHumanString(app))
				.setDescription(modeDescription)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						drawerMode = nextMode;
						updateDrawerMenu();
						return false;
					}
				})
				.createItem());
		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.main_menu_drawer_btn_configure_profile)
				.setColor(currentMode.getIconColorInfo().getColor(nightMode))
				.setTitle(getString(R.string.configure_profile))
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						mapActivity.dismissSettingsScreens();
						BaseSettingsFragment.showInstance(mapActivity, BaseSettingsFragment.SettingsScreenType.CONFIGURE_PROFILE);
						return true;
					}
				})
				.createItem());
	}

	public void openIntermediatePointsDialog() {
		mapActivity.hideContextAndRouteInfoMenues();
		WaypointsFragment.showInstance(mapActivity.getSupportFragmentManager());
	}

	public void openRoutePreferencesDialog() {
		mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.ROUTE_PREFERENCES);
	}

	public void stopNavigationWithoutConfirm() {
		getMyApplication().stopNavigation();
		mapActivity.updateApplicationModeSettings();
		mapActivity.getDashboard().clearDeletedPoints();
		List<ApplicationMode> modes = ApplicationMode.values(getMyApplication());
		for (ApplicationMode mode : modes) {
			if (settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(mode)) {
				settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false).setModeValue(mode, false);
			}
		}
	}

	public AlertDialog stopNavigationActionConfirm() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		// Stop the navigation
		builder.setTitle(getString(R.string.cancel_route));
		builder.setMessage(getString(R.string.stop_routing_confirm));
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				stopNavigationWithoutConfirm();
			}
		});
		builder.setNegativeButton(R.string.shared_string_no, null);
		return builder.show();
	}

	public void whereAmIDialog() {
		final List<String> items = new ArrayList<>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.shared_string_show_details));
		AlertDialog.Builder menu = new AlertDialog.Builder(mapActivity);
		menu.setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				dialog.dismiss();
				switch (item) {
					case 0:
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
						break;
					case 1:
						OsmAndLocationProvider locationProvider = getMyApplication().getLocationProvider();
						locationProvider.showNavigationInfo(mapActivity.getPointToNavigate(), mapActivity);
						break;
					default:
						break;
				}
			}
		});
		menu.show();
	}

	public void restoreOrReturnDialog(final String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
        builder.setTitle("Restore OsmAnd");
        builder.setMessage("Do you want to Restore OsmAnd or get back to the Client App?");
        builder.setPositiveButton("Restore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                restoreOsmand();
            }
        });
        builder.setNeutralButton("Return", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                executeHeadersIntent(packageName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

	protected void updateDrawerMenu() {
		boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final ListView menuItemsListView = (ListView) mapActivity.findViewById(R.id.menuItems);
		if (nightMode) {
			menuItemsListView.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.list_background_color_dark));
		} else {
			menuItemsListView.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.list_background_color_light));
		}
		menuItemsListView.removeHeaderView(drawerLogoHeader);
		menuItemsListView.removeFooterView(drawerOsmAndFooter);
		Bitmap navDrawerLogo = getMyApplication().getAppCustomization().getNavDrawerLogo();
		final ArrayList<String> navDrawerLogoParams = getMyApplication().getAppCustomization().getNavDrawerLogoParams();

		boolean customHeader = false;
		if (navDrawerLogo != null) {
			customHeader = true;
			drawerLogoHeader.setImageBitmap(navDrawerLogo);
			menuItemsListView.addHeaderView(drawerLogoHeader);
		}
		menuItemsListView.setDivider(null);
		final ContextMenuAdapter contextMenuAdapter = createMainOptionsMenu();
		contextMenuAdapter.setDefaultLayoutId(R.layout.simple_list_menu_item);
		final ArrayAdapter<ContextMenuItem> simpleListAdapter = contextMenuAdapter.createListAdapter(mapActivity,
				!nightMode);
		menuItemsListView.setAdapter(simpleListAdapter);
		menuItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mapActivity.dismissCardDialog();
				boolean hasHeader = menuItemsListView.getHeaderViewsCount() > 0;
				boolean hasFooter = menuItemsListView.getFooterViewsCount() > 0;
				if (hasHeader && position ==0 || (hasFooter && position== menuItemsListView.getCount() - 1)) {
						if(navDrawerLogoParams!=null) executeHeadersIntent(navDrawerLogoParams.get(0));
						else restoreOsmand();
					} else {
						position -= menuItemsListView.getHeaderViewsCount();
						ContextMenuItem item = contextMenuAdapter.getItem(position);
						ItemClickListener click = item.getItemClickListener();
						if (click != null && click.onContextMenuClick(simpleListAdapter, item.getTitleId(),
								position, false, AndroidUtils.getCenterViewCoordinates(view))) {
							mapActivity.closeDrawer();
						}
			        }
			}

		});
		if (customHeader) {
			menuItemsListView.post(new Runnable() {
				public void run() {
					View footerLayout = mapActivity.findViewById(R.id.drawer_footer_layout);
					boolean showFooterLayout = false;
					if (menuItemsListView.getChildCount() > 0) {
						int numItemsVisible = menuItemsListView.getLastVisiblePosition() -
								menuItemsListView.getFirstVisiblePosition();
						View lastView = menuItemsListView.getChildAt(menuItemsListView.getLastVisiblePosition());
						boolean overlapped = lastView != null && lastView.getY() + lastView.getHeight() * 2 > menuItemsListView.getHeight();
						if (simpleListAdapter.getCount() - 1 > numItemsVisible || overlapped) {
							menuItemsListView.addFooterView(drawerOsmAndFooter);
						} else {
							showFooterLayout = true;
						}
					} else {
						showFooterLayout = true;
					}
					if (showFooterLayout) {
						footerLayout.setVisibility(View.VISIBLE);
						footerLayout.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (navDrawerLogoParams != null) {
									mapActivity.closeDrawer();
								}
								new OsmandRestoreOrExitDialog()
										.show(mapActivity.getSupportFragmentManager(), "dialog");
								//showReturnConfirmationDialog(navDrawerLogoParams.get(0));

							}
						});
					} else {
						footerLayout.setVisibility(View.GONE);
					}
				}
			});
		}
	}

	private void executeHeadersIntent(String packageName) {
		Intent launchIntent = mapActivity.getPackageManager().getLaunchIntentForPackage(packageName);
		if(launchIntent!=null) mapActivity.startActivity(launchIntent);
		mapActivity.closeDrawer();
	}

	private void showReturnConfirmationDialog(String packageName) {
		restoreOrReturnDialog(packageName);
	    mapActivity.closeDrawer();
	}

	private void restoreOsmand(){
		getMyApplication().getAppCustomization().restoreOsmand();
		mapActivity.closeDrawer();
	}
}

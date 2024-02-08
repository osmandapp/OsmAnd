package net.osmand.plus.activities;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_AV_NOTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BACKUP_RESTORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_MAP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_SCREEN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIRECTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_FAVORITES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_HELP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_LIVE_UPDATES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MAP_MARKERS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MY_PLACES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSMAND_VERSION_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSM_EDITS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_PLUGINS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SEARCH_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRACKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRAVEL_GUIDES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRIP_RECORDING_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AVOID_ROAD;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARKER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SEARCH_NEARBY;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SHARE_ID;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_CHOSEN_PROFILE_TAG;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_CONTROL_BUTTON_TAG;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_NORMAL_PROFILE_TAG;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.help.HelpActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.StartPlanRouteBottomSheet;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.DismissRouteBottomSheetFragment;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.profiles.data.RoutingProfilesHolder;
import net.osmand.plus.routepreparationmenu.WaypointsFragment;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.wikivoyage.WikivoyageWelcomeDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MapActivityActions extends MapActions {

	private static final Log LOG = PlatformUtil.getLog(MapActivityActions.class);

	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_NAME = "name";

	public static final int REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION = 203;

	// Constants for determining the order of items in the additional actions context menu
	public static final int DIRECTIONS_FROM_ITEM_ORDER = 1000;
	public static final int SEARCH_NEAR_ITEM_ORDER = 2000;
	public static final int CHANGE_POSITION_ITEM_ORDER = 3000;
	public static final int EDIT_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int ADD_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int MEASURE_DISTANCE_ITEM_ORDER = 13000;
	public static final int AVOID_ROAD_ITEM_ORDER = 14000;

	private static final int DRAWER_MODE_NORMAL = 0;
	private static final int DRAWER_MODE_SWITCH_PROFILE = 1;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;
	private final RoutingDataUtils routingDataUtils;

	@NonNull
	private final ImageView drawerLogoHeader;

	private int drawerMode = DRAWER_MODE_NORMAL;

	public MapActivityActions(@NonNull MapActivity mapActivity) {
		super(mapActivity.getMyApplication());
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.routingDataUtils = new RoutingDataUtils(app);
		this.drawerLogoHeader = new ImageView(mapActivity);
		this.drawerLogoHeader.setPadding(-AndroidUtils.dpToPx(mapActivity, 8f),
				AndroidUtils.dpToPx(mapActivity, 16f), 0, 0);
	}

	public void addAsTarget(double latitude, double longitude, PointDescription pd) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		targets.navigateToPoint(new LatLon(latitude, longitude), true, targets.getIntermediatePoints().size() + 1, pd);
		openIntermediatePointsDialog();
	}


	public void addMapMarker(double latitude, double longitude, PointDescription pd, @Nullable String mapObjectName) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd, mapObjectName);
	}

	public void editWaypoints() {
		openIntermediatePointsDialog();
	}

	protected String getString(int res) {
		return mapActivity.getString(res);
	}

	protected void showToast(String msg) {
		mapActivity.runOnUiThread(() -> {
			Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
		});
	}

	public static class SaveDirectionsAsyncTask extends AsyncTask<File, Void, GPXFile> {

		private final OsmandApplication app;
		boolean showOnMap;

		public SaveDirectionsAsyncTask(OsmandApplication app, boolean showOnMap) {
			this.app = app;
			this.showOnMap = showOnMap;
		}

		@Override
		protected GPXFile doInBackground(File... params) {
			if (params.length > 0) {
				File file = params[0];
				String fileName = Algorithms.getFileNameWithoutExtension(file);
				GPXFile gpx = app.getRoutingHelper().generateGPXFileWithRoute(fileName);
				gpx.error = GPXUtilities.writeGpxFile(file, gpx);
				app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(app, gpx));
				return gpx;
			}
			return null;
		}

		@Override
		protected void onPostExecute(GPXFile gpxFile) {
			if (gpxFile.error == null) {
				GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
				if (showOnMap) {
					params.showOnMap().selectedByUser().addToMarkers().addToHistory();
				} else {
					params.hideFromMap();
				}
				app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
				String result = app.getString(R.string.route_successfully_saved_at, gpxFile.tracks.get(0).name);
				Toast.makeText(app, result, Toast.LENGTH_LONG).show();
			} else {
				String errorMessage = gpxFile.error.getMessage();
				if (errorMessage == null) {
					errorMessage = app.getString(R.string.error_occurred_saving_gpx);
				}
				Toast.makeText(app, errorMessage, Toast.LENGTH_LONG).show();
			}
		}
	}

	public void addActionsToAdapter(double latitude,
	                                double longitude,
	                                ContextMenuAdapter adapter,
	                                Object selectedObj,
	                                boolean configureMenu) {
		GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_ADD_ID)
				.setTitleId(selectedObj instanceof FavouritePoint ? R.string.favourites_context_menu_edit : R.string.shared_string_add, mapActivity)
				.setIcon(selectedObj instanceof FavouritePoint ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_favorite_stroke)
				.setOrder(10));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MARKER_ID)
				.setTitleId(selectedObj instanceof MapMarker ? R.string.shared_string_edit : R.string.shared_string_marker, mapActivity)
				.setIcon(selectedObj instanceof MapMarker ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_flag_stroke)
				.setOrder(20));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_SHARE_ID)
				.setTitleId(R.string.shared_string_share, mapActivity)
				.setIcon(R.drawable.ic_action_gshare_dark)
				.setOrder(30));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MORE_ID)
				.setTitleId(R.string.shared_string_actions, mapActivity)
				.setIcon(R.drawable.ic_actions_menu)
				.setOrder(40));

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID)
				.setTitleId(R.string.context_menu_item_directions_from, mapActivity)
				.setIcon(R.drawable.ic_action_route_direction_from_here)
				.setOrder(DIRECTIONS_FROM_ITEM_ORDER));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_SEARCH_NEARBY)
				.setTitleId(R.string.context_menu_item_search, mapActivity)
				.setIcon(R.drawable.ic_action_search_dark)
				.setOrder(SEARCH_NEAR_ITEM_ORDER));

		PluginsHelper.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj, configureMenu);

		ItemClickListener listener = (callback, view, item, isChecked) -> {
			int resId = item.getTitleId();
			if (resId == R.string.context_menu_item_add_waypoint) {
				mapActivity.getContextMenu().addWptPt();
			} else if (resId == R.string.context_menu_item_edit_waypoint) {
				mapActivity.getContextMenu().editWptPt();
			}
			return true;
		};

		ContextMenuItem editGpxItem = new ContextMenuItem(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setTitleId(R.string.context_menu_item_edit_waypoint, mapActivity)
				.setIcon(R.drawable.ic_action_edit_dark)
				.setOrder(EDIT_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener);
		ContextMenuItem addGpxItem = new ContextMenuItem(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setTitleId(R.string.context_menu_item_add_waypoint, mapActivity)
				.setIcon(R.drawable.ic_action_gnew_label_dark)
				.setOrder(ADD_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener);

		if (configureMenu) {
			adapter.addItem(addGpxItem);
		} else if (selectedObj instanceof WptPt
				&& gpxHelper.getSelectedGPXFile((WptPt) selectedObj) != null) {
			adapter.addItem(editGpxItem);
		} else if (!gpxHelper.getSelectedGPXFiles().isEmpty()
				|| (PluginsHelper.isActive(OsmandMonitoringPlugin.class))) {
			adapter.addItem(addGpxItem);
		}

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MEASURE_DISTANCE)
				.setTitleId(R.string.plan_route, mapActivity)
				.setIcon(R.drawable.ic_action_ruler)
				.setOrder(MEASURE_DISTANCE_ITEM_ORDER));

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_AVOID_ROAD)
				.setTitleId(R.string.avoid_road, mapActivity)
				.setIcon(R.drawable.ic_action_alert)
				.setOrder(AVOID_ROAD_ITEM_ORDER));
	}

	public void contextMenuPoint(double latitude, double longitude, ContextMenuAdapter _adapter, Object selectedObj) {
		ContextMenuAdapter adapter = _adapter == null ? new ContextMenuAdapter(app) : _adapter;
		addActionsToAdapter(latitude, longitude, adapter, selectedObj, false);
		showAdditionalActionsFragment(adapter, getContextMenuItemClickListener(latitude, longitude, adapter));
	}

	public void showAdditionalActionsFragment(ContextMenuAdapter adapter, AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener listener) {
		AdditionalActionsBottomSheetDialogFragment actionsBottomSheetDialogFragment = new AdditionalActionsBottomSheetDialogFragment();
		actionsBottomSheetDialogFragment.setAdapter(adapter, listener);
		actionsBottomSheetDialogFragment.show(mapActivity.getSupportFragmentManager(), AdditionalActionsBottomSheetDialogFragment.TAG);
	}

	public ContextMenuItemClickListener getContextMenuItemClickListener(double latitude, double longitude, ContextMenuAdapter adapter) {
		ViewCreator viewCreator = new ViewCreator(mapActivity, !settings.isLightContent());
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(mapActivity, viewCreator);

		return new AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener() {
			@Override
			public void onItemClick(View view, int position) {
				ContextMenuItem item = adapter.getItem(position);
				int standardId = item.getTitleId();
				ItemClickListener click = item.getItemClickListener();
				if (click != null) {
					click.onContextMenuClick(listAdapter, view, item, false);
				} else if (standardId == R.string.context_menu_item_search) {
					mapActivity.getFragmentsHelper().showQuickSearch(latitude, longitude);
				} else if (standardId == R.string.context_menu_item_directions_from) {
					//if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
					enterDirectionsFromPoint(latitude, longitude);
					//} else {
					//	ActivityCompat.requestPermissions(mapActivity,
					//			new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					//			REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION);
					//}
				} else if (standardId == R.string.plan_route) {
					mapActivity.getContextMenu().close();
					MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), new LatLon(latitude, longitude));
				} else if (standardId == R.string.avoid_road) {
					app.getAvoidSpecificRoads().addImpassableRoad(mapActivity, new LatLon(latitude, longitude), true, false, null);
				} else if (standardId == R.string.shared_string_add || standardId == R.string.favourites_context_menu_edit) {
					mapActivity.getContextMenu().hide();
					mapActivity.getContextMenu().buttonFavoritePressed();
				} else if (standardId == R.string.shared_string_marker || standardId == R.string.shared_string_edit) {
					mapActivity.getContextMenu().buttonWaypointPressed();
				} else if (standardId == R.string.shared_string_share) {
					mapActivity.getContextMenu().buttonSharePressed();
				}
			}
		};
	}

	public void enterDirectionsFromPoint(double latitude, double longitude) {
		mapActivity.getContextMenu().hide();
		if (!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isRoutePlanningMode()) {
			enterRoutePlanningMode(new LatLon(latitude, longitude), mapActivity.getContextMenu().getPointDescription());
		} else {
			app.getTargetPointsHelper().setStartPoint(new LatLon(latitude, longitude),
					true, mapActivity.getContextMenu().getPointDescription());
		}
	}

	@Override
	public boolean hasUiContext() {
		return true;
	}
	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, ApplicationMode appMode, LatLon from,
	                                           PointDescription fromName, boolean useIntermediatePointsByDefault,
	                                           boolean showMenu, int menuState) {
		enterRoutePlanningModeGivenGpx(gpxFile, appMode, from, fromName,
				useIntermediatePointsByDefault, showMenu, menuState, false);
	}
	@Override
	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, ApplicationMode appMode, LatLon from,
	                                           PointDescription fromName, boolean useIntermediatePointsByDefault,
	                                           boolean showMenu, int menuState, boolean passWholeRoute) {
		super.enterRoutePlanningModeGivenGpx(gpxFile, appMode, from, fromName,
				useIntermediatePointsByDefault, showMenu, menuState, passWholeRoute);
		if (showMenu) {
			mapActivity.getMapRouteInfoMenu().setShowMenu(menuState);
		}
		if (!settings.SPEED_CAMERAS_ALERT_SHOWED.get()) {
			SpeedCamerasBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), null);
		}
	}

	@Override
	public void recalculateRoute(boolean showDialog) {
		super.recalculateRoute(showDialog);
		if (showDialog) {
			app.getOsmandMap().getMapLayers().getMapActionsHelper().showRouteInfoMenu();
		}
	}

	@Override
	protected void initVoiceCommandPlayer(@NonNull ApplicationMode mode, boolean showMenu) {
		app.initVoiceCommandPlayer(mapActivity, mode, null, true, false, false, showMenu);
	}

	public void contextMenuPoint(double latitude, double longitude) {
		contextMenuPoint(latitude, longitude, null, null);
	}

	public ContextMenuAdapter createMainOptionsMenu() {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		if (drawerMode == DRAWER_MODE_SWITCH_PROFILE) {
			return createSwitchProfileOptionsMenu(app, adapter, nightMode);
		}
		return createNormalOptionsMenu(app, adapter, nightMode);
	}

	private ContextMenuAdapter createSwitchProfileOptionsMenu(@NonNull OsmandApplication app,
	                                                          @NonNull ContextMenuAdapter adapter,
	                                                          boolean nightMode) {
		drawerMode = DRAWER_MODE_NORMAL;
		createProfilesController(app, adapter, nightMode, true);

		List<ApplicationMode> activeModes = ApplicationMode.values(app);
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();

		String modeDescription;

		RoutingProfilesHolder profiles = routingDataUtils.getRoutingProfiles();
		for (ApplicationMode appMode : activeModes) {
			if (appMode.isCustomProfile()) {
				modeDescription = getProfileDescription(app, appMode, profiles, getString(R.string.profile_type_user_string));
			} else {
				modeDescription = getProfileDescription(app, appMode, profiles, getString(R.string.profile_type_osmand_string));
			}

			int tag = currentMode.equals(appMode) ? PROFILES_CHOSEN_PROFILE_TAG : PROFILES_NORMAL_PROFILE_TAG;

			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.profile_list_item)
					.setIcon(appMode.getIconRes())
					.setColor(appMode.getProfileColor(nightMode))
					.setTag(tag)
					.setTitle(appMode.toHumanString())
					.setDescription(modeDescription)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.getSettings().setApplicationMode(appMode);
						updateDrawerMenu();
						return false;
					}));
		}

		adapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.profile_list_item)
				.setColor(ColorUtilities.getActiveColor(app, nightMode))
				.setTag(PROFILES_CONTROL_BUTTON_TAG)
				.setTitle(getString(R.string.shared_string_manage))
				.setListener((uiAdapter, view, item, isChecked) -> {
					BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.MAIN_SETTINGS);
					return true;
				}));

		return adapter;
	}

	private ContextMenuAdapter createNormalOptionsMenu(OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {

		createProfilesController(app, optionsMenuHelper, nightMode, false);

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DASHBOARD_ID)
				.setTitleId(R.string.home, mapActivity)
				.setIcon(R.drawable.ic_dashboard)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_dashboard_open");
					MapActivity.clearPrevActivityIntent();
					mapActivity.closeDrawer();
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD, AndroidUtils.getCenterViewCoordinates(view));
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_MAP_MARKERS_ID)
				.setTitleId(R.string.map_markers, mapActivity)
				.setIcon(R.drawable.ic_action_flag)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_markers_open");
					MapActivity.clearPrevActivityIntent();
					MapMarkersDialogFragment.showInstance(mapActivity);
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_MY_PLACES_ID)
				.setTitleId(R.string.shared_string_my_places, mapActivity)
				.setIcon(R.drawable.ic_action_favorite)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_myplaces_open");
					Intent newIntent = new Intent(mapActivity, app.getAppCustomization()
							.getMyPlacesActivity());
					newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					mapActivity.startActivity(newIntent);
					return true;
				}));

		addMyPlacesTabToDrawer(optionsMenuHelper, R.string.shared_string_my_favorites,
				R.drawable.ic_action_folder_favorites, DRAWER_FAVORITES_ID);
		addMyPlacesTabToDrawer(optionsMenuHelper, R.string.shared_string_tracks,
				R.drawable.ic_action_folder_tracks, DRAWER_TRACKS_ID);
		if (PluginsHelper.isActive(AudioVideoNotesPlugin.class)) {
			addMyPlacesTabToDrawer(optionsMenuHelper, R.string.notes,
					R.drawable.ic_action_folder_av_notes, DRAWER_AV_NOTES_ID);
		}
		if (PluginsHelper.isActive(OsmEditingPlugin.class)) {
			addMyPlacesTabToDrawer(optionsMenuHelper, R.string.osm_edits,
					R.drawable.ic_action_folder_osm_notes, DRAWER_OSM_EDITS_ID);
		}

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_BACKUP_RESTORE_ID)
				.setTitleId(R.string.backup_and_restore, mapActivity)
				.setIcon(R.drawable.ic_action_cloud_upload)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_backup_restore_open");
					if (app.getBackupHelper().isRegistered()) {
						BackupCloudFragment.showInstance(mapActivity.getSupportFragmentManager());
					} else {
						BackupAuthorizationFragment.showInstance(mapActivity.getSupportFragmentManager());
					}
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_SEARCH_ID)
				.setTitleId(R.string.search_button, mapActivity)
				.setIcon(R.drawable.ic_action_search_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_search_open");
					mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
					return true;
				}));

		OsmandMonitoringPlugin monitoringPlugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (monitoringPlugin != null) {
			optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_TRIP_RECORDING_ID)
					.setTitleId(R.string.map_widget_monitoring, mapActivity)
					.setIcon(R.drawable.ic_action_track_recordable)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.logEvent("trip_recording_open");
						MapActivity.clearPrevActivityIntent();
						monitoringPlugin.askShowTripRecordingDialog(mapActivity);
						return true;
					}));
		}

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DIRECTIONS_ID)
				.setTitleId(R.string.shared_string_navigation, mapActivity)
				.setIcon(R.drawable.ic_action_gdirections_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_directions_open");
					MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
					if (mapControlsLayer != null) {
						mapControlsLayer.getMapActionsHelper().doRoute();
					}
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_CONFIGURE_MAP_ID)
				.setTitleId(R.string.configure_map, mapActivity)
				.setIcon(R.drawable.ic_action_layers)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_config_map_open");
					MapActivity.clearPrevActivityIntent();
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, AndroidUtils.getCenterViewCoordinates(view));
					return false;
				}));

		String d = getString(R.string.welmode_download_maps);
		if (app.getDownloadThread().getIndexes().isDownloadedFromInternet) {
			List<IndexItem> updt = app.getDownloadThread().getIndexes().getItemsToUpdate();
			if (updt != null && updt.size() > 0) {
				d += " (" + updt.size() + ")";
			}
		}
		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DOWNLOAD_MAPS_ID)
				.setTitleId(R.string.welmode_download_maps, null)
				.setTitle(d).setIcon(R.drawable.ic_type_archive)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_download_maps_open");
					Intent newIntent = new Intent(mapActivity, app.getAppCustomization().getDownloadActivity());
					newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					mapActivity.startActivity(newIntent);
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_LIVE_UPDATES_ID)
				.setTitleId(R.string.live_updates, mapActivity)
				.setIcon(R.drawable.ic_action_map_update)
				.setListener((uiAdapter, view, item, isChecked) -> {
					LiveUpdatesFragment.showInstance(mapActivity.getSupportFragmentManager(), null);
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_TRAVEL_GUIDES_ID)
				.setTitle(getString(R.string.shared_string_travel_guides) + " (Beta)")
				.setIcon(R.drawable.ic_action_travel)
				.setListener((uiAdapter, view, item, isChecked) -> {
					MapActivity.clearPrevActivityIntent();
					TravelHelper travelHelper = app.getTravelHelper();
					travelHelper.initializeDataOnAppStartup();
					if (!travelHelper.isAnyTravelBookPresent() && !travelHelper.getBookmarksHelper().hasSavedArticles()) {
						WikivoyageWelcomeDialogFragment.showInstance(mapActivity.getSupportFragmentManager());
					} else {
						Intent intent = new Intent(mapActivity, WikivoyageExploreActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(intent);
					}
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_MEASURE_DISTANCE_ID)
				.setTitleId(R.string.plan_route, mapActivity)
				.setIcon(R.drawable.ic_action_plan_route)
				.setListener((uiAdapter, view, item, isChecked) -> {
					StartPlanRouteBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
					return true;
				}));

		app.getAidlApi().registerNavDrawerItems(mapActivity, optionsMenuHelper);

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DIVIDER_ID)
				.setLayout(R.layout.drawer_divider));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_CONFIGURE_SCREEN_ID)
				.setTitleId(R.string.layer_map_appearance, mapActivity)
				.setIcon(R.drawable.ic_configure_screen_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_config_screen_open");
					MapActivity.clearPrevActivityIntent();
					ConfigureScreenFragment.showInstance(mapActivity);
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_PLUGINS_ID)
				.setTitleId(R.string.prefs_plugins, mapActivity)
				.setIcon(R.drawable.ic_extension_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_plugins_open");
					PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_SETTINGS_ID)
				.setTitle(getString(R.string.shared_string_settings))
				.setIcon(R.drawable.ic_action_settings)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_settings_new_open");
					mapActivity.getFragmentsHelper().showSettings();
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_HELP_ID)
				.setTitleId(R.string.shared_string_help, mapActivity)
				.setIcon(R.drawable.ic_action_help)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_help_open");
					Intent intent = new Intent(mapActivity, HelpActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					mapActivity.startActivity(intent);
					return true;
				}));

		//////////// Others
		PluginsHelper.registerOptionsMenu(mapActivity, optionsMenuHelper);

		optionsMenuHelper.addItem(createOsmAndVersionDrawerItem());

		return optionsMenuHelper;
	}

	private void createProfilesController(OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode, boolean listExpanded) {
		//switch profile button
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();
		String modeDescription;
		RoutingProfilesHolder profiles = routingDataUtils.getRoutingProfiles();
		if (currentMode.isCustomProfile()) {
			modeDescription = getProfileDescription(app, currentMode, profiles, getString(R.string.profile_type_user_string));
		} else {
			modeDescription = getProfileDescription(app, currentMode, profiles, getString(R.string.profile_type_osmand_string));
		}

		int icArrowResId = listExpanded ? R.drawable.ic_action_arrow_drop_up : R.drawable.ic_action_arrow_drop_down;
		int nextMode = listExpanded ? DRAWER_MODE_NORMAL : DRAWER_MODE_SWITCH_PROFILE;
		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_SWITCH_PROFILE_ID)
				.setLayout(R.layout.main_menu_drawer_btn_switch_profile)
				.setIcon(currentMode.getIconRes())
				.setSecondaryIcon(icArrowResId)
				.setColor(currentMode.getProfileColor(nightMode))
				.setTitle(currentMode.toHumanString())
				.setDescription(modeDescription)
				.setListener((uiAdapter, view, item, isChecked) -> {
					drawerMode = nextMode;
					updateDrawerMenu();
					return false;
				}));
		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_CONFIGURE_PROFILE_ID)
				.setLayout(R.layout.main_menu_drawer_btn_configure_profile)
				.setColor(currentMode.getProfileColor(nightMode))
				.setTitle(getString(R.string.configure_profile))
				.setListener((uiAdapter, view, item, isChecked) -> {
					mapActivity.getFragmentsHelper().dismissSettingsScreens();
					BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.CONFIGURE_PROFILE);
					return true;
				}));
	}

	private String getProfileDescription(@NonNull OsmandApplication app, @NonNull ApplicationMode mode,
	                                     @NonNull RoutingProfilesHolder profiles,
	                                     @NonNull String defaultDescription) {
		String description = defaultDescription;
		String routingProfileKey = mode.getRoutingProfile();
		String derivedProfile = mode.getDerivedProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			ProfileDataObject profile = profiles.get(routingProfileKey, derivedProfile);
			if (profile != null) {
				description = String.format(app.getString(R.string.profile_type_descr_string),
						Algorithms.capitalizeFirstLetterAndLowercase(profile.getName()));
			}
		}
		return description;
	}

	private void addMyPlacesTabToDrawer(ContextMenuAdapter adapter, @StringRes int titleRes,
	                                    @DrawableRes int iconRes, String drawerId) {
		adapter.addItem(new ContextMenuItem(drawerId)
				.setTitleId(titleRes, mapActivity)
				.setIcon(iconRes)
				.setListener((uiAdapter, view, item, isChecked) -> {
					String itemLogName = drawerId.replace(DRAWER_ITEM_ID_SCHEME, "");
					app.logEvent("drawer_" + itemLogName + "_open");
					Intent newIntent = new Intent(mapActivity, app.getAppCustomization()
							.getMyPlacesActivity());
					newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					settings.FAVORITES_TAB.set(titleRes);
					mapActivity.startActivity(newIntent);
					return true;

				}));
	}

	@NonNull
	private ContextMenuItem createOsmAndVersionDrawerItem() {
		String osmAndVersion = Version.getFullVersion(app);
		String releasedString = getString(R.string.shared_string_release);
		String releaseDate = getString(R.string.app_edition);
		String releaseText = Algorithms.isEmpty(releaseDate)
				? null
				: app.getString(R.string.ltr_or_rtl_combine_via_colon, releasedString, releaseDate);

		return new ContextMenuItem(DRAWER_OSMAND_VERSION_ID)
				.setLayout(R.layout.main_menu_drawer_osmand_version)
				.setTitle(osmAndVersion)
				.setDescription(releaseText)
				.setListener((uiAdapter, view, item, isChecked) -> {
					String text = releaseText == null
							? osmAndVersion
							: app.getString(R.string.ltr_or_rtl_combine_via_comma, osmAndVersion, releaseText);
					ShareMenu.copyToClipboardWithToast(app, text, Toast.LENGTH_SHORT);
					return true;
				});
	}

	public void openIntermediatePointsDialog() {
		mapActivity.hideContextAndRouteInfoMenues();
		WaypointsFragment.showInstance(mapActivity.getSupportFragmentManager());
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener) {
		stopNavigationActionConfirm(listener, null);
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener, @Nullable Runnable onStopAction) {
		DismissRouteBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager(), listener, onStopAction);
	}

	public void whereAmIDialog() {
		List<String> items = new ArrayList<>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.shared_string_show_details));
		AlertDialog.Builder menu = new AlertDialog.Builder(mapActivity);
		menu.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				dialog.dismiss();
				switch (item) {
					case 0:
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
						break;
					case 1:
						OsmAndLocationProvider locationProvider = app.getLocationProvider();
						locationProvider.showNavigationInfo(mapActivity.getPointToNavigate(), mapActivity);
						break;
					default:
						break;
				}
			}
		});
		menu.show();
	}

	protected void updateDrawerMenu() {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		ListView menuItemsListView = mapActivity.findViewById(R.id.menuItems);
		menuItemsListView.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode));
		menuItemsListView.removeHeaderView(drawerLogoHeader);
		Bitmap navDrawerLogo = app.getAppCustomization().getNavDrawerLogo();

		if (navDrawerLogo != null) {
			drawerLogoHeader.setImageBitmap(navDrawerLogo);
			menuItemsListView.addHeaderView(drawerLogoHeader);
		}
		menuItemsListView.setDivider(null);

		ContextMenuAdapter cma = createMainOptionsMenu();
		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.simple_list_menu_item);
		ContextMenuListAdapter simpleListAdapter = cma.toListAdapter(mapActivity, viewCreator);

		menuItemsListView.setAdapter(simpleListAdapter);
		menuItemsListView.setOnItemClickListener((parent, view, position, id) -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			boolean hasHeader = menuItemsListView.getHeaderViewsCount() > 0;
			boolean hasFooter = menuItemsListView.getFooterViewsCount() > 0;
			if (hasHeader && position == 0 || (hasFooter && position == menuItemsListView.getCount() - 1)) {
				String drawerLogoParams = app.getAppCustomization().getNavDrawerLogoUrl();
				if (!Algorithms.isEmpty(drawerLogoParams)) {
					AndroidUtils.openUrl(mapActivity, Uri.parse(drawerLogoParams), nightMode);
				}
			} else {
				position -= menuItemsListView.getHeaderViewsCount();
				ContextMenuItem item = cma.getItem(position);
				ItemClickListener click = item.getItemClickListener();
				if (click != null && click.onContextMenuClick(simpleListAdapter, view, item, false)) {
					mapActivity.closeDrawer();
				}
			}
		});
	}
}

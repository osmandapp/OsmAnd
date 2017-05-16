package net.osmand.plus.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.Version;
import net.osmand.plus.activities.actions.OsmAndDialogs;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.router.GeneralRouter;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapActivityActions implements DialogProvider {
	private static final Log LOG = PlatformUtil.getLog(MapActivityActions.class);
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_NAME = "name";

	public static final String KEY_ZOOM = "zoom";

	private static final int DIALOG_ADD_FAVORITE = 100;
	private static final int DIALOG_REPLACE_FAVORITE = 101;
	private static final int DIALOG_ADD_WAYPOINT = 102;
	private static final int DIALOG_RELOAD_TITLE = 103;

	private static final int DIALOG_SAVE_DIRECTIONS = 106;
	// make static
	private static Bundle dialogBundle = new Bundle();

	private final MapActivity mapActivity;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;

	public MapActivityActions(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		routingHelper = mapActivity.getMyApplication().getRoutingHelper();
	}

	public void addAsTarget(double latitude, double longitude, PointDescription pd) {
		TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
		targets.navigateToPoint(new LatLon(latitude, longitude), true, targets.getIntermediatePoints().size() + 1,
				pd);
		openIntermediatePointsDialog();
	}


	public void addMapMarker(double latitude, double longitude, PointDescription pd) {
		MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.add_waypoint_dialog_title);
		View view = mapActivity.getLayoutInflater().inflate(R.layout.add_gpx_point_dialog, null);
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
					if (!name.endsWith(".gpx")) {
						name += ".gpx";
					}
					toSave = new File(fileDir, name);
				}
				if (toSave.exists()) {
					dlg.findViewById(R.id.DuplicateFileName).setVisibility(View.VISIBLE);
				} else {
					dlg.dismiss();
					new SaveDirectionsAsyncTask(app).execute(toSave);
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
				GPXFile gpx = app.getRoutingHelper().generateGPXFileWithRoute();
				GPXUtilities.writeGpxFile(file, gpx, app);
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
		adapter.addItem(itemBuilder.setTitleId(R.string.context_menu_item_search, mapActivity)
				.setIcon(R.drawable.ic_action_search_dark).createItem());
		adapter.addItem(itemBuilder.setTitleId(R.string.context_menu_item_directions_from, mapActivity)
				.setIcon(R.drawable.ic_action_gdirections_dark).createItem());
		if (getMyApplication().getTargetPointsHelper().getPointToNavigate() != null &&
				(mapActivity.getRoutingHelper().isFollowingMode() || mapActivity.getRoutingHelper().isRoutePlanningMode())) {
			adapter.addItem(itemBuilder.setTitleId(R.string.context_menu_item_last_intermediate_point, mapActivity)
					.setIcon(R.drawable.ic_action_intermediate).createItem());
		}
		OsmandPlugin.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj);

		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getContextMenu().addWptPt();
				} else if (resId == R.string.context_menu_item_edit_waypoint) {
					mapActivity.getContextMenu().editWptPt();
				}
				return true;
			}
		};

		if (!mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles().isEmpty()
				|| (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null)) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.context_menu_item_add_waypoint, mapActivity)
					.setIcon(R.drawable.ic_action_gnew_label_dark)
					.setListener(listener).createItem());
		}

		if (selectedObj instanceof GPXUtilities.WptPt) {
			GPXUtilities.WptPt pt = (GPXUtilities.WptPt) selectedObj;
			if (getMyApplication().getSelectedGpxHelper().getSelectedGPXFile(pt) != null) {
				adapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.context_menu_item_edit_waypoint, mapActivity)
						.setIcon(R.drawable.ic_action_edit_dark)
						.setListener(listener).createItem());
			}
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		final ArrayAdapter<ContextMenuItem> listAdapter =
				adapter.createListAdapter(mapActivity, getMyApplication().getSettings().isLightContent());
		builder.setTitle(R.string.shared_string_more_actions);
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContextMenuItem item = adapter.getItem(which);
				int standardId = item.getTitleId();
				ItemClickListener click = item.getItemClickListener();
				if (click != null) {
					click.onContextMenuClick(listAdapter, standardId, which, false);
				} else if (standardId == R.string.context_menu_item_last_intermediate_point) {
					mapActivity.getContextMenu().addAsLastIntermediate();
				} else if (standardId == R.string.context_menu_item_search) {
					mapActivity.showQuickSearch(latitude, longitude);
				} else if (standardId == R.string.context_menu_item_directions_from) {
					mapActivity.getContextMenu().hide();
					if (settings.USE_MAP_MARKERS.get()
							&& getMyApplication().getTargetPointsHelper().getPointToNavigate() == null) {
						setFirstMapMarkerAsTarget();
					}
					if (!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isRoutePlanningMode()) {
						enterRoutePlanningMode(new LatLon(latitude, longitude),
								mapActivity.getContextMenu().getPointDescription());
					} else {
						getMyApplication().getTargetPointsHelper().setStartPoint(new LatLon(latitude, longitude),
								true, mapActivity.getContextMenu().getPointDescription());
					}
				}
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
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
				Location loc = ps.get(ps.size() - 1);
				TargetPointsHelper tg = mapActivity.getMyApplication().getTargetPointsHelper();
				tg.navigateToPoint(new LatLon(loc.getLatitude(), loc.getLongitude()), false, -1);
				if (tg.getPointToStart() == null) {
					loc = ps.get(0);
					tg.setStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()), false, null);
				}
			}
		}
	}

	public void enterRoutePlanningMode(final LatLon from, final PointDescription fromName) {
		final boolean useIntermediatePointsByDefault = true;
		List<SelectedGpxFile> selectedGPXFiles = mapActivity.getMyApplication().getSelectedGpxHelper()
				.getSelectedGPXFiles();
		final List<GPXFile> gpxFiles = new ArrayList<>();
		for (SelectedGpxFile gs : selectedGPXFiles) {
			if (!gs.isShowCurrentTrack() && !gs.notShowNavigationDialog) {
				if (gs.getGpxFile().hasRtePt() || gs.getGpxFile().hasTrkPt()) {
					gpxFiles.add(gs.getGpxFile());
				}
			}
		}

		if (gpxFiles.size() > 0) {
			AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
			if (gpxFiles.size() == 1) {
				bld.setMessage(R.string.use_displayed_track_for_navigation);
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						enterRoutePlanningModeGivenGpx(gpxFiles.get(0), from, fromName, useIntermediatePointsByDefault, true);
					}
				});
			} else {
				bld.setTitle(R.string.navigation_over_track);
				ArrayAdapter<GPXFile> adapter = new ArrayAdapter<GPXFile>(mapActivity, R.layout.drawer_list_item, gpxFiles) {
					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						if (convertView == null) {
							convertView = mapActivity.getLayoutInflater().inflate(R.layout.drawer_list_item, null);
						}
						String path = getItem(position).path;
						String name = path.substring(path.lastIndexOf("/") + 1, path.length());
						((TextView) convertView.findViewById(R.id.title)).setText(name);
						convertView.findViewById(R.id.icon).setVisibility(View.GONE);
						convertView.findViewById(R.id.toggle_item).setVisibility(View.GONE);
						return convertView;
					}
				};
				bld.setAdapter(adapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						enterRoutePlanningModeGivenGpx(gpxFiles.get(i), from, fromName, useIntermediatePointsByDefault, true);
					}
				});
			}

			bld.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					enterRoutePlanningModeGivenGpx(null, from, fromName, useIntermediatePointsByDefault, true);
				}
			});
			bld.show();
		} else {
			enterRoutePlanningModeGivenGpx(null, from, fromName, useIntermediatePointsByDefault, true);
		}
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
											   boolean useIntermediatePointsByDefault, boolean showDialog) {
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(useIntermediatePointsByDefault);
		OsmandApplication app = mapActivity.getMyApplication();
		TargetPointsHelper targets = app.getTargetPointsHelper();

		ApplicationMode mode = getRouteMode(from);
		//app.getSettings().APPLICATION_MODE.set(mode);
		app.getRoutingHelper().setAppMode(mode);
		app.initVoiceCommandPlayer(mapActivity, mode, true, null, false, false);
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
		if (showDialog) {
			mapActivity.getMapLayers().getMapControlsLayer().showDialog();
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
			mapActivity.getMapLayers().getMapControlsLayer().showDialog();
		}
		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	public ApplicationMode getRouteMode(LatLon from) {
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

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.home, mapActivity)
				.setIcon(R.drawable.map_dashboard)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_dashboard_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.closeDrawer();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD);
						return true;
					}
				}).createItem());
		if (settings.USE_MAP_MARKERS.get()) {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.map_markers, mapActivity)
					.setIcon(R.drawable.ic_action_flag_dark)
					.setListener(new ContextMenuAdapter.ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
							app.logEvent(mapActivity, "drawer_markers_open");
							MapActivity.clearPrevActivityIntent();
							mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.MAP_MARKERS);
							return false;
						}
					}).createItem());
		} else {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.waypoints, mapActivity)
					.setIcon(R.drawable.ic_action_intermediate)
					.setListener(new ContextMenuAdapter.ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
							app.logEvent(mapActivity, "drawer_waypoints_open");
							MapActivity.clearPrevActivityIntent();
							mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.WAYPOINTS);
							return false;
						}
					}).createItem());
		}

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_my_places, mapActivity)
				.setIcon(R.drawable.ic_action_fav_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_myplaces_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getFavoritesActivity());
						newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.search_button, mapActivity)
				.setIcon(R.drawable.ic_action_search_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_search_open");
						mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW_IF_EXPIRED, false);
						return true;
					}
				}).createItem());

		if (settings.SHOW_LEGACY_SEARCH.get()) {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.legacy_search, mapActivity)
					.setIcon(R.drawable.ic_action_search_dark)
					.setListener(new ContextMenuAdapter.ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
							app.logEvent(mapActivity, "drawer_legacy_search_open");
							Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
									.getSearchActivity());
							LatLon loc = mapActivity.getMapLocation();
							newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
							newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
							if (mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
								newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
							}
							newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							mapActivity.startActivity(newIntent);
							return true;
						}
					}).createItem());
		}

		optionsMenuHelper.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.configure_map, mapActivity)
				.setIcon(R.drawable.ic_action_layers_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_config_map_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP);
						return false;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.get_directions, mapActivity)
				.setIcon(R.drawable.ic_action_gdirections_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_directions_open");
						MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
						if (mapControlsLayer != null) {
							mapControlsLayer.doRoute(false);
						}
						return true;
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
				.setTitle(d).setIcon(R.drawable.ic_type_archive)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_download_maps_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getDownloadActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		if (Version.isGooglePlayEnabled(app) || Version.isDeveloperVersion(app)) {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.osm_live, mapActivity)
					.setIcon(R.drawable.ic_action_osm_live)
					.setListener(new ContextMenuAdapter.ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
							app.logEvent(mapActivity, "drawer_osm_live_open");
							Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							mapActivity.startActivity(intent);
							return false;
						}
					}).createItem());
		}

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

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.prefs_plugins, mapActivity)
				.setIcon(R.drawable.ic_extension_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_plugins_open");
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getPluginsActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.layer_map_appearance, mapActivity)
				.setIcon(R.drawable.ic_configure_screen_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_config_screen_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_SCREEN);
						return false;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_settings, mapActivity)
				.setIcon(R.drawable.ic_action_settings)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_settings_open");
						final Intent settings = new Intent(mapActivity, getMyApplication().getAppCustomization()
								.getSettingsActivity());
						settings.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(settings);
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
				.setIcon(R.drawable.ic_action_help)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
						app.logEvent(mapActivity, "drawer_help_open");
						Intent intent = new Intent(mapActivity, HelpActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(intent);
						return true;
					}
				}).createItem());

		//////////// Others
		OsmandPlugin.registerOptionsMenu(mapActivity, optionsMenuHelper);

		int pluginsItemIndex = -1;
		for (int i = 0; i < optionsMenuHelper.length(); i++) {
			if (optionsMenuHelper.getItem(i).getTitleId() == R.string.prefs_plugins) {
				pluginsItemIndex = i;
				break;
			}
		}

		ItemBuilder divider = new ItemBuilder().setLayout(R.layout.drawer_divider);
		divider.setPosition(pluginsItemIndex >= 0 ? pluginsItemIndex : 7);
		optionsMenuHelper.addItem(divider.createItem());

		getMyApplication().getAppCustomization().prepareOptionsMenu(mapActivity, optionsMenuHelper);
		return optionsMenuHelper;
	}

	public void openIntermediatePointsDialog() {
		mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.WAYPOINTS);
	}

	public void openRoutePreferencesDialog() {
		mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.ROUTE_PREFERENCES);
	}

	public void stopNavigationWithoutConfirm() {
		getMyApplication().stopNavigation();
		mapActivity.updateApplicationModeSettings();
		mapActivity.getDashboard().clearDeletedPoints();
		List<ApplicationMode> modes = ApplicationMode.values(settings);
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

	protected void updateDrawerMenu() {
		boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final ListView menuItemsListView = (ListView) mapActivity.findViewById(R.id.menuItems);
		if (nightMode) {
			menuItemsListView.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.bg_color_dark));
		} else {
			menuItemsListView.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.bg_color_light));
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
				ContextMenuItem item = contextMenuAdapter.getItem(position);
				ContextMenuAdapter.ItemClickListener click = item.getItemClickListener();
				if (click != null && click.onContextMenuClick(simpleListAdapter, item.getTitleId(),
						position, false)) {
					mapActivity.closeDrawer();
				}
			}
		});
	}

	public void setFirstMapMarkerAsTarget() {
		if (getMyApplication().getMapMarkersHelper().getMapMarkers().size() > 0) {
			MapMarkersHelper.MapMarker marker = getMyApplication().getMapMarkersHelper().getMapMarkers().get(0);
			PointDescription pointDescription = marker.getOriginalPointDescription();
			if (pointDescription.isLocation()
					&& pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			}
			TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
			targets.navigateToPoint(new LatLon(marker.getLatitude(), marker.getLongitude()),
					true, targets.getIntermediatePoints().size() + 1, pointDescription);
		}
	}
}

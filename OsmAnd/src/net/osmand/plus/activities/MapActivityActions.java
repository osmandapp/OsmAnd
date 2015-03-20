package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.actions.OsmAndDialogs;
import net.osmand.plus.activities.actions.ShareLocation;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.osmo.OsMoPositionLayer;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivityActions implements DialogProvider {
	
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

	private boolean refreshDrawer = false;
	private WaypointDialogHelper waypointDialogHelper;

	public enum DrawerType{
		WAYPOINTS,
		CONFIGURE_SCREEN,
		CONFIGURE_MAP,
		MAIN_MENU
	}

	private DrawerType currentDrawer = DrawerType.MAIN_MENU;

	public MapActivityActions(MapActivity mapActivity){
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		waypointDialogHelper = new WaypointDialogHelper(mapActivity);
	}
	

	protected void addFavouritePoint(final double latitude, final double longitude){
		String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
		enhance(dialogBundle, latitude, longitude, name);
		mapActivity.showDialog(DIALOG_ADD_FAVORITE);
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


    public void addWaypoint(final double latitude, final double longitude){
    	String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
    	enhance(dialogBundle,latitude,longitude, name);
    	mapActivity.showDialog(DIALOG_ADD_WAYPOINT);
    }

    private Dialog createAddWaypointDialog(final Bundle args) {
    	Builder builder = new AlertDialog.Builder(mapActivity);
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
				savingTrackHelper.insertPointData(latitude, longitude, System.currentTimeMillis(), name);
				AccessibleToast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
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
    
    public void reloadTile(final int zoom, final double latitude, final double longitude){
    	enhance(dialogBundle,latitude,longitude,zoom);
    	mapActivity.showDialog(DIALOG_RELOAD_TITLE);
    }

	public DrawerType getDrawerType(){
		return currentDrawer;
	}

	public void setDrawerType(DrawerType type){
		this.currentDrawer = type;
		prepareStartOptionsMenu();
	}
    
    protected String getString(int res){
    	return mapActivity.getString(res);
    }
    
    protected void showToast(final String msg){
    	mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
			}
		});
    }
    
        
    protected void aboutRoute() {
    	Intent intent = new Intent(mapActivity, ShowRouteInfoActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mapActivity.startActivity(intent);
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
	
	public static  Dialog createSaveDirections(Activity activity, RoutingHelper routingHelper) {
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

		((Button) dlg.findViewById(R.id.Save)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = edit.getText().toString();
				fileDir.mkdirs();
				File toSave = fileDir;
				if(name.length() > 0) {
					if(!name.endsWith(".gpx")){
						name += ".gpx";
					}
					toSave = new File(fileDir, name);
				}
				if(toSave.exists()){
					dlg.findViewById(R.id.DuplicateFileName).setVisibility(View.VISIBLE);					
				} else {
					dlg.dismiss();
					new SaveDirectionsAsyncTask(app).execute(toSave);
				}
			}
		});
		
		((Button) dlg.findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener() {
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
			if(result != null){
				AccessibleToast.makeText(app, result, Toast.LENGTH_LONG).show();
			}
		}
		
	}
	
	public void contextMenuPoint(final double latitude, final double longitude, final ContextMenuAdapter iadapter, Object selectedObj) {
		final ContextMenuAdapter adapter = iadapter == null ? new ContextMenuAdapter(mapActivity) : iadapter;

		if(!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isRoutePlanningMode()) {
			adapter.item(R.string.context_menu_item_directions_to).iconColor(
					R.drawable.ic_action_gdirections_dark).reg();
			adapter.item(R.string.context_menu_item_directions_from).iconColor(
					R.drawable.ic_action_gdirections_dark).reg();
		}
		final TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
		if(targets.getPointToNavigate() != null) {
			adapter.item(R.string.context_menu_item_destination_point).iconColor(R.drawable.ic_action_flag_dark).reg();
			adapter.item(R.string.context_menu_item_intermediate_point).iconColor(R.drawable.ic_action_flage_dark).reg();
		// For button-less search UI
		} else {
			adapter.item(R.string.context_menu_item_destination_point).iconColor(R.drawable.ic_action_flag_dark).reg();
		}
		adapter.item(R.string.context_menu_item_search).iconColor(R.drawable.ic_action_search_dark).reg();
		adapter.item(R.string.context_menu_item_share_location).iconColor(
				R.drawable.ic_action_gshare_dark).reg();
		adapter.item(R.string.shared_string_add_to_favorites).iconColor(
				R.drawable.ic_action_fav_dark).reg();
		
		

		OsmandPlugin.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj);
		getMyApplication().getAppCustomization().prepareLocationMenu(mapActivity, adapter);
		
		final Builder builder = new AlertDialog.Builder(mapActivity);
		final ArrayAdapter<?>  listAdapter =
				adapter.createListAdapter(mapActivity, getMyApplication().getSettings().isLightContent());
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				int standardId = adapter.getElementId(which);
				OnContextMenuClick click = adapter.getClickAdapter(which);
				if (click != null) {
					click.onContextMenuClick(listAdapter, standardId, which, false);
				} else if (standardId == R.string.context_menu_item_search) {
					Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getSearchActivity());
					intent.putExtra(SearchActivity.SEARCH_LAT, latitude);
					intent.putExtra(SearchActivity.SEARCH_LON, longitude);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					mapActivity.startActivity(intent);
				} else if (standardId == R.string.context_menu_item_directions_to) {
					targets.navigateToPoint(new LatLon(latitude, longitude), true, -1, null);
					enterRoutePlanningMode(null, null, false);
				} else if (standardId == R.string.context_menu_item_directions_from) {
					List<PointDescription> nms = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectNames();
					enterRoutePlanningMode(new LatLon(latitude, longitude), nms.isEmpty() ? null : nms.get(0), false);
				} else if (standardId == R.string.context_menu_item_intermediate_point || 
						standardId == R.string.context_menu_item_destination_point) {
					boolean dest = standardId == R.string.context_menu_item_destination_point;
					List<PointDescription> nms = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectNames();
					targets.navigateToPoint(new LatLon(latitude, longitude), true,
							dest ? -1 : targets.getIntermediatePoints().size(), nms.size() == 0?null : 
								nms.get(0));
					if(targets.getIntermediatePoints().size() > 0) {
						openIntermediatePointsDialog();
					}
				} else if (standardId == R.string.context_menu_item_share_location) {
					enhance(dialogBundle,latitude,longitude,mapActivity.getMapView().getZoom());
					new ShareLocation(mapActivity).run();
				} else if (standardId == R.string.shared_string_add_to_favorites) {
					addFavouritePoint(latitude, longitude);
				}
			}
		});
		builder.create().show();
	}
	
	public void setGPXRouteParams(GPXFile result) {
		if(result == null) {
			mapActivity.getRoutingHelper().setGpxParams(null);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);	
		} else {
			GPXRouteParamsBuilder params = new GPXRouteParamsBuilder(result, mapActivity.getMyApplication()
					.getSettings());
			if (result.hasRtePt() && !result.hasTrkpt()) {
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
	
	public void enterRoutePlanningMode(final LatLon from, final PointDescription fromName, boolean useCurrentGPX) {
		List<SelectedGpxFile> selectedGPXFiles = mapActivity.getMyApplication().getSelectedGpxHelper()
				.getSelectedGPXFiles();
		final List<GPXFile> gpxFiles = new ArrayList<GPXFile>();
		for (SelectedGpxFile gs : selectedGPXFiles) {
			if (!gs.isShowCurrentTrack() && !gs.notShowNavigationDialog) {
				if (gs.getGpxFile().hasRtePt() || gs.getGpxFile().hasTrkpt()) {
					gpxFiles.add(gs.getGpxFile());
				}
			}
		}

		if (gpxFiles.size() > 0 && !useCurrentGPX) {
			Builder bld = new AlertDialog.Builder(mapActivity);
			if (gpxFiles.size() == 1){
				bld.setMessage(R.string.use_displayed_track_for_navigation);
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						enterRoutePlanningModeImpl(gpxFiles.get(0), from, fromName);
					}
				});
			} else {
				bld.setTitle(R.string.navigation_over_track);
				ArrayAdapter<GPXFile> adapter =new ArrayAdapter<GPXFile>(mapActivity, R.layout.drawer_list_item, gpxFiles){
					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						if (convertView == null){
							convertView = mapActivity.getLayoutInflater().inflate(R.layout.drawer_list_item, null);
						}
						String path = getItem(position).path;
						String name = path.substring(path.lastIndexOf("/") + 1,path.length());
						((TextView)convertView.findViewById(R.id.title)).setText(name);
						convertView.findViewById(R.id.icon).setVisibility(View.GONE);
						convertView.findViewById(R.id.check_item).setVisibility(View.GONE);
						return convertView;
					}
				};
				bld.setAdapter(adapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						enterRoutePlanningModeImpl(gpxFiles.get(i), from, fromName);
					}
				});
			}

			bld.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					enterRoutePlanningModeImpl(null, from, fromName);
				}
			});
			bld.show();
		} else {
			enterRoutePlanningModeImpl(useCurrentGPX ? gpxFiles.get(0) : null, from, fromName);
		}
	}
	
	private void enterRoutePlanningModeImpl(GPXFile gpxFile, LatLon from, PointDescription fromName) {

		ApplicationMode mode = settings.DEFAULT_APPLICATION_MODE.get();
		ApplicationMode selected = settings.APPLICATION_MODE.get();
		if( selected != ApplicationMode.DEFAULT) {
			mode = selected;
		} else if (mode == ApplicationMode.DEFAULT) {
			mode = ApplicationMode.CAR ;
		}

		OsmandApplication app = mapActivity.getMyApplication();
		TargetPointsHelper targets = app.getTargetPointsHelper();
		app.getSettings().APPLICATION_MODE.set(mode);
		app.getRoutingHelper().setAppMode(mode);
		app.initVoiceCommandPlayer(mapActivity);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		app.getRoutingHelper().setFollowingMode(false);
		app.getRoutingHelper().setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(from, false, fromName);
		// then set gpx
		setGPXRouteParams(gpxFile);
		// then update start and destination point  
		targets.updateRouteAndReferesh(true);
		
		mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		mapActivity.getMapView().refreshMap(true);
		mapActivity.getMapLayers().getMapControlsLayer().showDialog();
		if(targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long);
		}
	}
	
	public void contextMenuPoint(final double latitude, final double longitude){
		contextMenuPoint(latitude, longitude, null, null);
	}
	
	private Dialog createReloadTitleDialog(final Bundle args) {
    	Builder builder = new AccessibleAlertBuilder(mapActivity);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.shared_string_cancel, null);
    	final OsmandMapTileView mapView = mapActivity.getMapView();
    	builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int zoom = args.getInt(KEY_ZOOM);
				BaseMapLayer mainLayer = mapView.getMainLayer();
				if(!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()){
					AccessibleToast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
				if(mapSource == null || !mapSource.couldBeDownloadedFromInternet()){
					AccessibleToast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
				final QuadRect tilesRect = tb.getTileBounds();
				int left = (int) Math.floor(tilesRect.left);
				int top = (int) Math.floor(tilesRect.top);
				int width = (int) (Math.ceil(tilesRect.right) - left);
				int height = (int) (Math.ceil(tilesRect.bottom) - top);
				for (int i = 0; i <width; i++) {
					for (int j = 0; j< height; j++) {
						((OsmandApplication)mapActivity.getApplication()).getResourceManager().
								clearTileImageForMap(null, mapSource, i + left, j + top, zoom);	
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
			if(args.getString(KEY_NAME) != null) {
				v.setText(args.getString(KEY_NAME));
			} else {
				v.setText("");
			}
			break;
		}
	}

	public void prepareStartOptionsMenu(){
		refreshDrawer();
	}

	public void refreshDrawer(){
		switch (currentDrawer){
			case MAIN_MENU:
				prepareOptionsMenu(createMainOptionsMenu());
				break;
			case CONFIGURE_MAP:
				prepareConfigureMap();
				break;
			case CONFIGURE_SCREEN:
				prepareConfigureScreen();
				break;
			case WAYPOINTS:
				showWaypointsInDrawer(false);
				break;
		}
	}

	public void prepareOptionsMenu(final ContextMenuAdapter cm) {
		refreshDrawer = false;
		final ArrayAdapter<?> listAdapter =
				cm.createListAdapter(mapActivity, getMyApplication().getSettings().isLightContent());
		OnItemClickListener listener = getOptionsMenuOnClickListener(cm, listAdapter);
		mapActivity.getDashboard().setListAdapter(listAdapter, listener);
		
	}


	private OnItemClickListener getOptionsMenuOnClickListener(final ContextMenuAdapter cm,
			final ArrayAdapter<?> listAdapter) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
				OnContextMenuClick click = cm.getClickAdapter(which);
				if(click instanceof OnRowItemClick) {
					boolean cl = ((OnRowItemClick) click).onRowItemClick(listAdapter, view, cm.getElementId(which), which);
					if(cl) {
						closeDrawer();
					}
				} else if (click != null) {
					CompoundButton btn = (CompoundButton) view.findViewById(R.id.check_item);
					if (btn != null && btn.getVisibility() == View.VISIBLE) {
						btn.setChecked(!btn.isChecked());
					} else {
						if (click.onContextMenuClick(listAdapter, cm.getElementId(which), which, false)) {
							closeDrawer();
						}
					}
				} else {
					closeDrawer();
				}
			}
		};
	}
	
	public void closeDrawer() {
		mapActivity.getDashboard().setDashboardVisibility(false);
	}

	public void prepareConfigureMap() {
		currentDrawer = DrawerType.CONFIGURE_MAP;
		prepareOptionsMenu(new ConfigureMapMenu().createListAdapter(mapActivity, true));
	}

	public void onDrawerBack() {
		currentDrawer = DrawerType.MAIN_MENU;
		prepareStartOptionsMenu();
	}

	private ContextMenuAdapter createMainOptionsMenu() {
		final OsmandMapTileView mapView = mapActivity.getMapView();
		final OsmandApplication app = mapActivity.getMyApplication();
		ContextMenuAdapter optionsMenuHelper = new ContextMenuAdapter(app);
		currentDrawer = DrawerType.MAIN_MENU;

		// 2-4. Navigation related (directions, mute, cancel navigation)
		boolean muteVisible = routingHelper.getFinalLocation() != null && routingHelper.isFollowingMode();
		if (muteVisible) {
			boolean mute = routingHelper.getVoiceRouter().isMute();
			int t = mute ? R.string.menu_mute_on : R.string.menu_mute_off;
			int icon;
			if(mute) {
				icon = R.drawable.a_10_device_access_volume_muted_dark;
			} else{
				icon = R.drawable.a_10_device_access_volume_on_dark;
			}
			optionsMenuHelper.item(t).iconColor(icon)
				.listen(new OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					routingHelper.getVoiceRouter().setMute(!routingHelper.getVoiceRouter().isMute());
					return true;
				}
			}).reg();
		}
		if(!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
			optionsMenuHelper.item(R.string.get_directions)
				.iconColor(R.drawable.ic_action_gdirections_dark)
				.listen(new OnContextMenuClick() {
					@Override
						public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
							enterRoutePlanningMode(null, null, false);
							return true;
						}
				}).reg();
		} else if(routingHelper.isRouteCalculated()) {
			optionsMenuHelper.item(
					routingHelper.isRoutePlanningMode() ? R.string.continue_navigation :
					R.string.pause_navigation)
			.iconColor(R.drawable.ic_action_gdirections_dark)
			.listen(new OnContextMenuClick() {
				@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						if(routingHelper.isRoutePlanningMode()) {
							routingHelper.setRoutePlanningMode(false);
							routingHelper.setFollowingMode(true);
						} else {
							routingHelper.setRoutePlanningMode(true);
							routingHelper.setFollowingMode(false);
							routingHelper.setPauseNaviation(true);
						}
						mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
						mapActivity.refreshMap();
						return true;
					}
			}).reg();
		}
		if (mapActivity.getPointToNavigate() != null) {
			int nav;
			if(routingHelper.isFollowingMode()) {
				nav = R.string.cancel_navigation;
			} else if(routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated()) {
				nav = R.string.cancel_route;
			} else {
				nav = R.string.clear_destination;
			}
			optionsMenuHelper.item(nav).iconColor(R.drawable.ic_action_remove_dark) 
				.listen(new OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					stopNavigationActionConfirm(mapView);
					OsMoPositionLayer osMoPositionLayer = mapActivity.getMapView().getLayerByClass(OsMoPositionLayer.class);
					if (osMoPositionLayer != null) {
						OsMoPositionLayer.setFollowDestination(null);						
					}
					return true;
				}
			}).reg();
		}
		if (getTargets().getPointToNavigate() != null) {
			optionsMenuHelper.item(R.string.target_points).iconColor(R.drawable.ic_action_flage_dark)
					.listen(new OnContextMenuClick() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos,
								boolean isChecked) {
							showWaypointsInDrawer(false);
							return false;
						}
					}).reg();
		}
		if(routingHelper.isRouteCalculated()) {
			optionsMenuHelper.item(R.string.impassable_road)
			.iconColor(R.drawable.ic_action_road_works_dark)
			.listen(new OnContextMenuClick() {
				@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						app.getAvoidSpecificRoads().showDialog(mapActivity);
						return true;
					}
			}).reg();
		}
		
		// 5-9. Default actions (Layers, Configure Map screen, Settings, Search, Favorites) 
		optionsMenuHelper.item(R.string.search_button)
				.iconColor(R.drawable.ic_action_search_dark)
				.listen(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getSearchActivity());
						// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						LatLon loc = mapActivity.getMapLocation();
						newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
						newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
						newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).reg();

		optionsMenuHelper.item(R.string.shared_string_my_places)
				.iconColor(R.drawable.ic_action_fav_dark).listen(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
								.getFavoritesActivity());
						// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).reg();
		

		optionsMenuHelper.item(R.string.show_point_options)
		.iconColor(R.drawable.ic_action_marker_dark)
		.listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
				return true;
			}
		}).reg();

		optionsMenuHelper.item(R.string.configure_map).iconColor(R.drawable.ic_action_layers_dark)
				.listen(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						prepareConfigureMap();
						return false;
					}
				}).reg();

		optionsMenuHelper.item(R.string.layer_map_appearance).iconColor(R.drawable.ic_configure_screen_dark)
			.listen(new OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					prepareConfigureScreen();
					return false;
				}
			}).reg();

		optionsMenuHelper.item(R.string.shared_string_settings)
		.iconColor(R.drawable.ic_action_settings_enabled_dark)
		.listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				final Intent settings = new Intent(mapActivity, getMyApplication().getAppCustomization().getSettingsActivity());
				mapActivity.startActivity(settings);
				return true;
			}
		}).reg();

		//////////// Others
		OsmandPlugin.registerOptionsMenu(mapActivity, optionsMenuHelper);
		optionsMenuHelper.item(R.string.shared_string_exit).icon(R.drawable.ic_action_quit_dark )
					.listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				// 1. Work for almost all cases when user open apps from main menu
				Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getMainMenuActivity());
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// not exit
				newIntent.putExtra(AppInitializer.APP_EXIT_KEY, AppInitializer.APP_EXIT_CODE);
				mapActivity.startActivity(newIntent);
				// In future when map will be main screen this should change
				// app.closeApplication(mapActivity);
				return true;
			}
		}).reg();

		getMyApplication().getAppCustomization().prepareOptionsMenu(mapActivity, optionsMenuHelper);
		return optionsMenuHelper;
	}

	public void prepareConfigureScreen() {
		currentDrawer = DrawerType.CONFIGURE_SCREEN;
		ContextMenuAdapter cm = mapActivity.getMapLayers().getMapInfoLayer().getViewConfigureMenuAdapter();
		prepareOptionsMenu(cm);
	}

	public void showWaypointsInDrawer(boolean flat) {
		currentDrawer = DrawerType.WAYPOINTS;
		final int[] running = new int[] { -1 };
		ArrayAdapter<Object> listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(mapActivity, running, flat);
		OnItemClickListener listener = waypointDialogHelper.getDrawerItemClickListener(mapActivity, running,
				listAdapter, null);
		refreshDrawer = false;
		mapActivity.getDashboard().setListAdapter(listAdapter, listener);
	}
	
	public void showWaypointsDialog(boolean flat) {
		if(flat) {
			waypointDialogHelper.showWaypointsDialogFlat(mapActivity, false);
		} else {
			waypointDialogHelper.showWaypointsDialog(mapActivity, false);
		}
	}

	public void openIntermediatePointsDialog(){
		waypointDialogHelper.showWaypointsDialog(mapActivity, false);
	}
	
	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}
	
	public void stopNavigationWithoutConfirm() {
		if(getMyApplication().getLocationProvider().getLocationSimulation().isRouteAnimating()) {
			getMyApplication().getLocationProvider().getLocationSimulation().startStopRouteAnimation(mapActivity);
		}
		routingHelper.getVoiceRouter().interruptRouteCommands();
		routingHelper.clearCurrentRoute(null, new ArrayList<LatLon>());
		routingHelper.setRoutePlanningMode(false);
		settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
		mapActivity.updateApplicationModeSettings();
	}
	
	public void stopNavigationActionConfirm(final OsmandMapTileView mapView){
		Builder builder = new AlertDialog.Builder(mapActivity);
		
		if (routingHelper.isRouteCalculated() || routingHelper.isFollowingMode() || routingHelper.isRouteBeingCalculated()) {
			// Stop the navigation
			builder.setTitle(getString(R.string.cancel_route));
			builder.setMessage(getString(R.string.stop_routing_confirm));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stopNavigationWithoutConfirm();
				}

				
			});
		} else {
			// Clear the destination point
			builder.setTitle(getString(R.string.cancel_navigation));
			builder.setMessage(getString(R.string.clear_dest_confirm));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getTargets().clearPointToNavigate(true);
					mapView.refreshMap();
				}
			});
		}

		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.show();
	}
	

	
	public void whereAmIDialog() {
		final List<String> items = new ArrayList<String>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.show_details));
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
	
	
    
    

	public static void showObjectContextMenu(final ContextMenuAdapter qa, final Activity activity,
			final OnClickListener onShow) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if(app.accessibilityEnabled()) {
			Builder builder = new AlertDialog.Builder(activity);
			String[] values = qa.getItemNames();
			builder.setItems(values, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					OnContextMenuClick clk = qa.getClickAdapter(which);
					if (clk != null) {
						clk.onContextMenuClick(null, qa.getElementId(which), which, false);
					}
				}

			});
			builder.show();
		} else {
			final QuickAction view = new QuickAction(qa.getAnchor());
			for (int i = 0; i < qa.length(); i++) {

				ActionItem ai = new ActionItem();
				Drawable id = qa.getImage(app, i, true);
				if (id != null) {
					ai.setIcon(id);
				}
				final int ki = i;
				ai.setTitle(qa.getItemName(i));
				ai.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (onShow != null) {
							onShow.onClick(v);
						}
						view.dismiss();
						qa.getClickAdapter(ki).onContextMenuClick(null, qa.getElementId(ki), ki, false);

					}
				});
				view.addActionItem(ai);
			}
			view.show();
		}
	}
	
    
}
